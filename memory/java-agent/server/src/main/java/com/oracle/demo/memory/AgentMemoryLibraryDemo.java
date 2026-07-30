package com.oracle.demo.memory;

import com.oracle.ojdbc.agentmemory.Agent;
import com.oracle.ojdbc.agentmemory.Memory;
import com.oracle.ojdbc.agentmemory.Message;
import com.oracle.ojdbc.agentmemory.OracleAgentMemory;
import com.oracle.ojdbc.agentmemory.OracleSearchResult;
import com.oracle.ojdbc.agentmemory.OracleThread;
import com.oracle.ojdbc.agentmemory.SearchScope;
import com.oracle.ojdbc.agentmemory.User;
import com.oracle.ojdbc.agentmemory.internal.DatabaseEmbedder;
import com.oracle.ojdbc.agentmemory.internal.OllamaLlm;
import com.oracle.ojdbc.agentmemory.records.ScopedRecord;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AgentMemoryLibraryDemo {
    private static final String AGENT_ID = "FLYNNS_THEME_PARK_CONCIERGE";
    private static final String AVA_ID = "AVA";
    private static final String LEO_ID = "LEO";
    private static final String THREAD_ID = "flynns_theme_park_ava_visit";
    private static final String TABLE_PREFIX = "OAMJ_CONCIERGE_";
    private static final List<String> RECORD_TYPES =
            List.of("memory", "guideline", "fact", "preference");

    private final DataSource dataSource;
    private final OracleAgentMemory memory;
    private List<Map<String, Object>> lastSearchResults = List.of();
    private String lastContextCard;

    AgentMemoryLibraryDemo(DataSource pooledDataSource, Map<String, String> environment) {
        this.dataSource = new AutoCommitDataSource(pooledDataSource);
        String embeddingModel =
                environment.getOrDefault("MEMORY_EMBEDDING_MODEL", "allminilm");
        String ollamaUrl =
                environment.getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");
        String ollamaModel =
                environment.getOrDefault("OLLAMA_MODEL", "llama3.2:latest");
        this.memory = OracleAgentMemory.builder()
                .dataSource(dataSource)
                .tablePrefix(TABLE_PREFIX)
                .autoCreateSchema(true)
                .persistMessages(true)
                .extractMemories(true)
                .embedder(new DatabaseEmbedder(dataSource, embeddingModel))
                .llm(new OllamaLlm(ollamaUrl, ollamaModel, 0.0d))
                .build();
    }

    synchronized Map<String, Object> reset() {
        memory.clearAll();
        lastSearchResults = List.of();
        lastContextCard = null;
        Map<String, Object> state = state();
        state.put(
                "message",
                "The library-managed message and memory tables are empty. "
                        + "No browser-only state is carrying the demonstration.");
        return state;
    }

    synchronized Map<String, Object> retain() {
        memory.deleteThread(THREAD_ID);
        User user = User.builder()
                .id(AVA_ID)
                .information("Returning Flynn's Theme Park visitor")
                .build();
        Agent agent = Agent.builder()
                .id(AGENT_ID)
                .information("Resort concierge")
                .build();
        memory.addUser(user);
        memory.addAgent(agent);

        OracleThread thread =
                memory.createThread(THREAD_ID, user.id(), agent.id(), null, true);
        thread.addMessages(List.of(
                new Message(
                        "user",
                        "I prefer a quiet breakfast before 8 AM, need routes with minimal "
                                + "stairs, and want to see the lantern show. Keep itineraries "
                                + "short and use bullet points."),
                new Message(
                        "assistant",
                        "I will remember those preferences for your Flynn's Theme Park visit.")));
        thread.addMemory(Memory.builder()
                .content(
                        "The covered atrium route worked well during Ava's previous rainy visit.")
                .build());
        lastSearchResults = List.of();
        lastContextCard = thread.getContextCard().content();

        Map<String, Object> state = state();
        state.put(
                "message",
                "addMessages() persisted the conversation, Ollama extracted durable records, "
                        + "and Oracle AI Database generated an embedding for every memory.");
        return state;
    }

    synchronized Map<String, Object> recall(String query) {
        String resolvedQuery = query == null || query.isBlank()
                ? "Plan an accessible quiet morning and rainy evening"
                : query.trim();
        OracleThread thread = memory.getThread(THREAD_ID);
        lastSearchResults = searchRows(
                thread.search(resolvedQuery, 8, RECORD_TYPES));
        lastContextCard = thread.getContextCard().content();
        int leoResults = memory.search(
                resolvedQuery,
                SearchScope.userScope(LEO_ID),
                8,
                RECORD_TYPES).size();

        Map<String, Object> state = state();
        state.put("query", resolvedQuery);
        state.put("leoPrivateResults", leoResults);
        state.put(
                "message",
                "search() embedded the request inside Oracle AI Database, ranked Ava's scoped "
                        + "records by vector distance, and returned zero Ava records for Leo.");
        return state;
    }

    synchronized Map<String, Object> state() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("library", "ojdbc-agent-memory");
        result.put("tablePrefix", TABLE_PREFIX);
        result.put("embeddingModel", "ALLMINILM");
        result.put("embeddingDimensions", embeddingDimensions());
        result.put("messages", messages());
        result.put("records", records());
        result.put("searchResults", lastSearchResults);
        result.put("contextCard", lastContextCard);
        return result;
    }

    private List<Map<String, Object>> messages() {
        try {
            OracleThread thread = memory.getThread(THREAD_ID);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Message message : thread.getMessages()) {
                rows.add(Map.of(
                        "role", message.role(),
                        "content", message.content()));
            }
            return rows;
        } catch (IllegalArgumentException exception) {
            return List.of();
        }
    }

    private List<Map<String, Object>> records() {
        try {
            OracleThread thread = memory.getThread(THREAD_ID);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ScopedRecord record : thread.getRecords()) {
                if ("message".equals(record.recordType())) {
                    continue;
                }
                rows.add(Map.of(
                        "id", record.id(),
                        "type", record.recordType(),
                        "content", record.content(),
                        "userId", record.userId(),
                        "agentId", record.agentId(),
                        "threadId", record.threadId()));
            }
            return rows;
        } catch (IllegalArgumentException exception) {
            return List.of();
        }
    }

    private static List<Map<String, Object>> searchRows(
            List<OracleSearchResult> results) {
        List<Map<String, Object>> rows = new ArrayList<>();
        int rank = 1;
        for (OracleSearchResult result : results) {
            rows.add(Map.of(
                    "rank", rank++,
                    "type", result.record().recordType(),
                    "content", result.content(),
                    "distance", result.score()));
        }
        return rows;
    }

    private int embeddingDimensions() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT VECTOR_DIMENSION_COUNT("
                             + "VECTOR_EMBEDDING(allminilm USING "
                             + "'Flynn''s Theme Park concierge' AS DATA)) FROM DUAL");
             ResultSet results = statement.executeQuery()) {
            results.next();
            return results.getInt(1);
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Unable to verify ALLMINILM: " + exception.getMessage(),
                    exception);
        }
    }
}
