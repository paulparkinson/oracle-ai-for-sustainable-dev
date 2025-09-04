package oracleai.aiholo;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.oracle.CreateOption;
import dev.langchain4j.store.embedding.oracle.EmbeddingTable;
import dev.langchain4j.store.embedding.oracle.Index;
import dev.langchain4j.store.embedding.oracle.OracleEmbeddingStore;
import org.json.JSONObject;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.vectorstore.SearchRequest;
//import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.protobuf.ByteString;
import com.google.cloud.texttospeech.v1.AudioConfig;

import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.*;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/aiholo")
// @CrossOrigin(origins = "*")
public class AIHoloController {
    private String theValue = "mirrorme";
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final String SANDBOX_API_URL = System.getenv("SANDBOX_API_URL");
    private static final String AI_OPTIMZER = System.getenv("AI_OPTIMZER");
    // Langflow API configuration. These environment variables should be set
    // when deploying the application. LANGFLOW_SERVER_URL is the base URL for
    // the Langflow server (for example, http://host:7860/api), FLOW_ID is the
    // ID of the flow you wish to call, and LANGFLOW_API_KEY is your API key.
    private static final String LANGFLOW_SERVER_URL = System.getenv("LANGFLOW_SERVER_URL");
    private static final String LANGFLOW_FLOW_ID = System.getenv("LANGFLOW_FLOW_ID");
    private static final String LANGFLOW_API_KEY = System.getenv("LANGFLOW_API_KEY");
    static final String AUDIO_DIR_PATH = System.getenv("AUDIO_DIR_PATH");
    private static int currentAnswerIntro = 0;
    private static String aiholo_prompt_additions = "";

    static {
        // Check for aiholo_prompt_additions.txt in AUDIO_DIR_PATH at startup
        if (AUDIO_DIR_PATH != null) {
            try {
                java.nio.file.Path additionsPath = Paths.get(AUDIO_DIR_PATH, "aiholo_prompt_additions.txt");
                if (Files.exists(additionsPath)) {
                    aiholo_prompt_additions = new String(Files.readAllBytes(additionsPath), StandardCharsets.UTF_8).trim();
                    System.out.println("Loaded aiholo_prompt_additions: " + aiholo_prompt_additions);
                }
            } catch (Exception e) {
                System.err.println("Could not load aiholo_prompt_additions.txt: " + e.getMessage());
            }
        }
    }

    private static final String DEFAULT_LANGUAGE_CODE = "es-ES";
    private static final String DEFAULT_VOICE_NAME = "es-ES-Wavenet-D";
    private final static String sql = """
                SELECT DBMS_CLOUD_AI.GENERATE(
                    prompt       => ?,
                    profile_name => 'VIDEOGAMES_PROFILE',
                    action       => ?
                ) FROM dual
            """;

    @Autowired
    private DataSource dataSource;

    private static final Object metahumanLock = new Object();
    private static boolean isRecentQuestionProcessed;
    private static String languageCode = "es";

    public AIHoloController() {
//        startInactivityMonitor();
    }

    private void startInactivityMonitor() {
        System.out.println("startInactivityMonitor...");
        scheduler.scheduleAtFixedRate(() -> {
            if (isRecentQuestionProcessed) {
                System.out.println("isRecentQuestionProcessed true so skipping the timecheck/keepalive");
                isRecentQuestionProcessed = false;
            }
//            String fileName = "currenttime.wav"; //testing123-brazil.wav
//            TTSAndAudio2Face.processMetahuman(
//                        fileName,  TimeInWords.getTimeInWords(languageCode),
//                    DEFAULT_LANGUAGE_CODE, DEFAULT_VOICE_NAME);
            TTSAndAudio2Face.sendToAudio2Face("explainer.wav");
        }, 1, 15, TimeUnit.MINUTES);
    }


    @GetMapping("")
    public String home(@RequestParam(value = "languageCode", defaultValue = "en-US") String languageCode, Model model) {
        System.out.println("AIHolo root languageCode = " + languageCode);
        this.languageCode = languageCode;
        model.addAttribute("languageCode", languageCode);
        if (languageCode.equals("pt-BR"))
            model.addAttribute("voiceName", "pt-BR-Wavenet-D");
        else if (languageCode.equals("es-ES"))
            model.addAttribute("voiceName", "es-ES-Wavenet-D");
        else if (languageCode.equals("zh-SG"))
            model.addAttribute("voiceName", "cmn-CN-Wavenet-A");
        else if (languageCode.equals("de-DE"))
            model.addAttribute("voiceName", "de-DE-Wavenet-A");
        else if (languageCode.equals("es-MX"))
            model.addAttribute("voiceName", "es-US-Wavenet-A");
        else if (languageCode.equals("it-IT"))
            model.addAttribute("voiceName", "it-IT-Wavenet-A");
        else if (languageCode.equals("fr-FR"))
            model.addAttribute("voiceName", "fr-FR-Wavenet-A");
        else if (languageCode.equals("ro-RO"))
            model.addAttribute("voiceName", "ro-RO-Wavenet-A");
        else if (languageCode.equals("en-AU"))
            model.addAttribute("voiceName", "en-AU-Wavenet-A");
        else if (languageCode.equals("ga-GA"))
            model.addAttribute("voiceName", "ga-GA-Wavenet-A");
        else if (languageCode.equals("ar-AE"))
            model.addAttribute("voiceName", "ar-AE-Wavenet-A");
        else if (languageCode.equals("ja-JP"))
            model.addAttribute("voiceName", "ja-JP-Wavenet-A");
        else if (languageCode.equals("hi-IN"))
            model.addAttribute("voiceName", "hi-IN-Wavenet-A");
        else if (languageCode.equals("he-IL"))
            model.addAttribute("voiceName", "he-IL-Wavenet-A");
        else if (languageCode.equals("en-US"))
            model.addAttribute("voiceName", "en-US-Chirp3-HD-Aoede");
            //   model.addAttribute("voiceName", "Aoede");
//            model.addAttribute("voiceName", "en-US-Chirp3-HD-Aoede");
        else if (languageCode.equals("en-GB"))
            model.addAttribute("voiceName", "en-GB-Wavenet-A");
        else model.addAttribute("voiceName", "en-US-Wavenet-A");
        return "aiholo";
    }


    @GetMapping("/explainer")
    @ResponseBody
    public String explainer() throws Exception {
        System.out.println("AIHoloController.explainer");
        theValue = "explainer";
        String filePath = "C:/Users/paulp/aiholo_output.txt";
        try (FileWriter writer = new FileWriter(filePath)) {
            JSONObject json = new JSONObject();
            json.put("data", theValue);
            writer.write(json.toString());
            writer.flush();
        } catch (IOException e) {
            return "Error writing to file: " + e.getMessage();
        }
        TTSAndAudio2Face.sendToAudio2Face("explainer.wav");
        return "Explained";
    }

    @GetMapping("/leia")
    @ResponseBody
    public String leia() throws Exception {
        System.out.println("AIHoloController.leia");
        theValue = "leia";
        String filePath = "C:/Users/paulp/aiholo_output.txt";
        try (FileWriter writer = new FileWriter(filePath)) {
            JSONObject json = new JSONObject();
            json.put("data", theValue);
            writer.write(json.toString());
            writer.flush();
        } catch (IOException e) {
            return "Error writing to file: " + e.getMessage();
        }
        //     TTSAndAudio2Face.sendToAudio2Face("explainer-leia.wav");
        return "leia hologram";
    }


    @GetMapping("/play")
    @ResponseBody
    public String play(@RequestParam("question") String question,
                       @RequestParam("selectedMode") String selectedMode,
                       @RequestParam("languageCode") String languageCode,
                       @RequestParam("voiceName") String voicename) throws Exception {
        System.out.println(
                "play question: " + question + " selectedMode: " + selectedMode +
                        " languageCode:" + languageCode + " voicename:" + voicename);
        System.out.println("modified question: " + question);
        theValue = "question";
        String filePath = "C:/Users/paulp/aiholo_output.txt";
        try (FileWriter writer = new FileWriter(filePath)) {
            JSONObject json = new JSONObject();
            json.put("data", theValue); // Store the response inside JSON
            writer.write(json.toString());
            writer.flush();
        } catch (IOException e) {
            return "Error writing to file: " + e.getMessage();
        }

        // Start a new thread to call TTSAndAudio2Face.sendToAudio2Face with intro switching
        new Thread(() -> {
            try {
                // languagecode:es-MX voicename:es-US-Wavenet-A
                if (languageCode.equals("es-MX")) {
                    TTSAndAudio2Face.sendToAudio2Face("tts-es-USFEMALEes-US-Wavenet-A_¡Claro!¡U.wav");
                } else {
                    // Switch for currentAnswerIntro
                    switch (currentAnswerIntro) {
                        case 0:
                            TTSAndAudio2Face.sendToAudio2Face("tts-en-USFEMALEAoede_Sure!Illcheck.wav");
                            break;
                        case 1:
                            TTSAndAudio2Face.sendToAudio2Face("tts-en-USFEMALEAoede_on_it.wav");
                            break;
                        case 2:
                            TTSAndAudio2Face.sendToAudio2Face("tts-en-USFEMALEAoede_one_sec.wav");
                            break;
                        case 3:
                            TTSAndAudio2Face.sendToAudio2Face("tts-en-USFEMALEAoede_hmm.wav");
                            break;
                        default:
                            TTSAndAudio2Face.sendToAudio2Face("tts-en-USFEMALEAoede_Sure!Illcheck.wav");
                    }
                    currentAnswerIntro++;
                    if (currentAnswerIntro > 3) currentAnswerIntro = 0;
                }
            } catch (Exception e) {
                System.err.println("Error in sendToAudio2Face: " + e.getMessage());
            }
        }).start();

        String action = "chat";
        String answer;
        if (languageCode.equals("pt-BR")) answer = "Desculpe. Não consegui encontrar uma resposta no banco de dados";
        else if (languageCode.equals("es-ES"))
            answer = "Lo siento, no pude encontrar una respuesta en la base de datos.";
        else if (languageCode.equals("en-GB")) answer = "Sorry, I couldn't find an answer in the database.";
        else if (languageCode.equals("zh-SG")) answer = "抱歉，我在数据库中找不到答案";
        else answer = "I'm sorry. I couldn't find an answer in the database";
        if (selectedMode.contains("use vector")) {
            question = question.replace("use vectorrag", "").trim();
            question += ". Respond in 25 words or less. " + aiholo_prompt_additions;
            // If the user asks about a financial agent, call the Langflow financial agent
            // instead of the generic sandbox. The comparison is case-insensitive to
            // capture variations like "Financial Agent" or "financial agent".
            String normalized = question.toLowerCase();
// Check if the string contains "financ" and "agent"
            boolean financialAgentIntent = normalized.contains("financ") && normalized.contains("agent");
            if (financialAgentIntent) {
                answer = executeFinancialAgent(question);
            } else {
                answer = executeSandbox(question);
            }

        } else {
            if (selectedMode.contains("use narrate")) {
                action = "narrate";
                question = question.replace("use narrate", "").trim();
            } else {
                question = question.replace("use chat", "").trim();
            }
            question += ". Respond in 25 words or less. " + aiholo_prompt_additions;
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                System.out.println("Database Connection : " + connection);
                String response = null;
                preparedStatement.setString(1, question);
                preparedStatement.setString(2, action);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        response = resultSet.getString(1); // Retrieve AI response from the first column
                    }
                }
                answer = response;
            } catch (SQLException e) {
                System.err.println("Failed to connect to the database: " + e.getMessage());
                return "Database Connection Failed!";
            }
        }
        String fileName = "output.wav";

        // Strip out any "A:", "A2:", "A3:", etc. at the beginning of the answer string
        if (answer != null) {
            answer = answer.replaceFirst("^A\\d*:\\s*", "");
        }

        System.out.println("about to TTS and sendAudioToAudio2Face for answer: " + answer);
        TTSAndAudio2Face.processMetahuman(fileName, answer, languageCode, voicename);
        if(answer.toLowerCase().contains("leia") || answer.toLowerCase().contains("star wars")) {
            Thread.sleep(5);
            leia();
        }
        return answer;
    }


    /**
     * curl -X 'POST' \
     * 'http://host/v1/chat/completions?client=server' \
     * -H 'accept: application/json' \
     * -H 'Authorization: Bearer bearer' \
     * -H 'Content-Type: application/json' \
     * -d '{
     * "messages": [
     * {
     * "role": "user",
     * "content": "What are Alternative Dispute Resolution"
     * }
     * ]
     * }'
     */

    public String executeSandbox(String cummulativeResult) {
        System.out.println("using AI sandbox: " + cummulativeResult);
        Map<String, Object> payload = new HashMap<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", cummulativeResult);
        payload.put("messages", new Object[]{message});
        JSONObject jsonPayload = new JSONObject(payload);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", AI_OPTIMZER);
        headers.set("Accept", "application/json");
        headers.set("client", "server");
        HttpEntity<String> request = new HttpEntity<>(jsonPayload.toString(), headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(SANDBOX_API_URL, HttpMethod.POST, request, String.class);
        String latestAnswer;
        if (response.getStatusCode() == HttpStatus.OK) {
            JSONObject responseData = new JSONObject(response.getBody());
            latestAnswer = responseData.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
                    .getString("content");
            System.out.println("RAG Full Response latest_answer: " + latestAnswer);
            return latestAnswer;
        } else {
            System.out.println("Failed to fetch data: " + response.getStatusCode() + " " + response.getBody());
            return " I'm sorry, I couldn't find an answer";
        }
    }

    /**
     * Invoke a Langflow flow that acts as a financial agent. This method builds
     * a JSON payload containing the user's question and sends it to the
     * Langflow /v1/run endpoint for the configured flow. It uses the
     * LANGFLOW_API_KEY for authentication and parses the nested response to
     * extract the chat message. The environment variables LANGFLOW_SERVER_URL
     * and LANGFLOW_FLOW_ID must be defined; otherwise, this method will
     * return an error message.
     *
     * @param question The user's question to send to the financial agent.
     * @return The agent's textual response or an error message if the call fails.
     */
    public String executeFinancialAgent(String question) {
        System.out.println("using financial agent: " + question);
        if (LANGFLOW_SERVER_URL == null || LANGFLOW_FLOW_ID == null || LANGFLOW_API_KEY == null) {
            return "Error: Langflow configuration is not set";
        }
        // Build the URL. The /v1/run endpoint executes a flow by ID. The
        // stream=false query parameter disables token streaming.
        String url = LANGFLOW_SERVER_URL + "/v1/run/" + LANGFLOW_FLOW_ID + "?stream=false";
        // Construct the request payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("output_type", "chat");
        payload.put("input_type", "chat");
        payload.put("input_value", question);

        JSONObject jsonPayload = new JSONObject(payload);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Provide the API key for authentication
        headers.set("x-api-key", LANGFLOW_API_KEY);
        HttpEntity<String> request = new HttpEntity<>(jsonPayload.toString(), headers);
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                // Ensure the response body is JSON before attempting to parse it.
                String body = response.getBody();
                if (body == null) {
                    return "Error: Empty response from Langflow";
                }
                String trimmedBody = body.trim();
                if (!trimmedBody.startsWith("{") && !trimmedBody.startsWith("[")) {
                    // Langflow returned a non‑JSON response (e.g. HTML error page).
                    // Return the raw body so the caller can log or display it.
                    return trimmedBody;
                }
                JSONObject responseData;
                try {
                    responseData = new JSONObject(trimmedBody);
                } catch (Exception e) {
                    // The body looked like JSON but failed to parse.
                    return "Error parsing Langflow response: " + e.getMessage();
                }
                // Parse the JSON response and extract the message. The message is
                // nested under outputs[0].outputs[0].outputs.message.message or
                // outputs[0].outputs[0].results.message.text depending on the
                // flow configuration. We'll attempt multiple paths.
                try {
                    // Older flows: outputs[0].outputs[0].outputs.message.message
                    String message = responseData
                            .getJSONArray("outputs")
                            .getJSONObject(0)
                            .getJSONArray("outputs")
                            .getJSONObject(0)
                            .getJSONObject("outputs")
                            .getJSONObject("message")
                            .getString("message");
                    if (message != null && !message.isEmpty()) {
                        return message;
                    }
                } catch (Exception ignore) {
                    // fall through to next attempt
                }
                try {
                    // Newer flows: outputs[0].outputs[0].results.message.text
                    String message = responseData
                            .getJSONArray("outputs")
                            .getJSONObject(0)
                            .getJSONArray("outputs")
                            .getJSONObject(0)
                            .getJSONObject("results")
                            .getJSONObject("message")
                            .getString("text");
                    if (message != null && !message.isEmpty()) {
                        return message;
                    }
                } catch (Exception ignore) {
                    // fall through to next attempt
                }
                try {
                    // Fallback: check nested data text (results.message.data.text)
                    String message = responseData
                            .getJSONArray("outputs")
                            .getJSONObject(0)
                            .getJSONArray("outputs")
                            .getJSONObject(0)
                            .getJSONObject("results")
                            .getJSONObject("message")
                            .getJSONObject("data")
                            .getString("text");
                    if (message != null && !message.isEmpty()) {
                        return message;
                    }
                } catch (Exception ignore) {
                    // no more attempts
                }
                return "Error parsing Langflow JSON response";
            } else {
                return "Error: " + response.getStatusCode() + " " + response.getBody();
            }
        } catch (Exception e) {
            return "Error calling Langflow: " + e.getMessage();
        }
    }

    /**
     * Utilites not required by Interactive AI Holograms from here to end...
     */


    // `https://host:port/aiholo/tts?textToConvert=${encodeURIComponent(textToConvert)}
    // &languageCode=${encodeURIComponent(languageCode)}&ssmlGender=${encodeURIComponent(ssmlGender)}
    // &voiceName=${encodeURIComponent(voiceName)}`;
    @GetMapping("/tts")
    public ResponseEntity<byte[]> ttsAndReturnAudioFile(@RequestParam("textToConvert") String textToConvert,
                                                        @RequestParam("languageCode") String languageCode,
                                                        @RequestParam("ssmlGender") String ssmlGender,
                                                        @RequestParam("voiceName") String voiceName) throws Exception {
        System.out.println("TTS GCP  textToConvert = " + textToConvert + ", languageCode = " + languageCode +
                ", ssmlGender = " + ssmlGender + ", voiceName = " + voiceName);
        try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
            System.out.println("in TTS GCP textToSpeechClient:" + textToSpeechClient + " languagecode:" + languageCode);
            SynthesisInput input = SynthesisInput.newBuilder().setText(textToConvert).build();
            VoiceSelectionParams voice =
                    VoiceSelectionParams.newBuilder()
                            .setLanguageCode(languageCode)
                            .setSsmlGender(SsmlVoiceGender.FEMALE) // SsmlVoiceGender.NEUTRAL SsmlVoiceGender.MALE
                            .setName(voiceName) //eg "pt-BR-Wavenet-A"
                            .build();
            AudioConfig audioConfig =
                    AudioConfig.newBuilder()
                            .setAudioEncoding(AudioEncoding.LINEAR16) // wav AudioEncoding.MP3 being another
                            .build();
            SynthesizeSpeechResponse response =
                    textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);
            ByteString audioContents = response.getAudioContent();
            byte[] audioData = audioContents.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_TYPE, "audio/mpeg");
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"tts-" + languageCode + "" + ssmlGender + "" + voiceName + "_" +
                            getFirst10Chars(textToConvert) + ".mp3\"");
            return new ResponseEntity<>(audioData, headers, HttpStatus.OK);
        }
    }


    // Vector embedding, store, langchain, etc. stuff...


//    @Autowired
//    VectorStore vectorStore;
//
//    @GetMapping("/vectorstoretest")
//    @ResponseBody
//    public String vectorstoretest(@RequestParam("question") String question,
//                                  @RequestParam("selectedMode") String selectedMode,
//                                  @RequestParam("languageCode") String languageCode,
//                                  @RequestParam("voiceName") String voicename) throws Exception {
////        System.out.println(
//        List<Document> documents = List.of(
//                new Document("Spring AI rocks!! Spring AI rocks!!", Map.of("meta1", "meta1")),
//                new Document("The World is Big and Salvation Lurks Around the Corner"),
//                new Document("You walk forward facing the past and you turn back toward the future.",  Map.of("meta2", "meta2")));
//        // Add the documents to Oracle Vector Store
//        vectorStore.add(documents);
//        // Retrieve documents similar to a query
//        List<Document> results =
//                vectorStore.similaritySearch(SearchRequest.builder().query(question).topK(5).build());
////                vectorStore.similaritySearch(SearchRequest.builder().query("Spring").topK(5).build());
//        return "test";
//        //results.getFirst().getFormattedContent(); give s cannot find symbol
//        //[ERROR]   symbol:   method getFirst()
//        //[ERROR]   location: variable results of type java.util.List<org.springframework.ai.document.Document>
//    }

    @GetMapping("/langchain")
    @ResponseBody
    public String langchain(@RequestParam("question") String question,
                            @RequestParam("selectedMode") String selectedMode,
                            @RequestParam("languageCode") String languageCode,
                            @RequestParam("voiceName") String voicename) throws Exception {
        EmbeddingSearchRequest embeddingSearchRequest = null;
        OracleEmbeddingStore embeddingStore =
                OracleEmbeddingStore.builder()
                        .dataSource(dataSource)
                        .embeddingTable(EmbeddingTable.builder()
                                .createOption(CreateOption.CREATE_OR_REPLACE)
                                .name("my_embedding_table")
                                .idColumn("id_column_name")
                                .embeddingColumn("embedding_column_name")
                                .textColumn("text_column_name")
                                .metadataColumn("metadata_column_name")
                                .build())
                        .index(Index.ivfIndexBuilder()
                                .createOption(CreateOption.CREATE_OR_REPLACE).build())
                        .build();
        EmbeddingSearchResult<TextSegment> embeddingSearchResult = embeddingStore.search(embeddingSearchRequest);
        return "langchain";
    }


    //set/get etc utilites to end....


    public static String getFirst10Chars(String textToConvert) {
        if (textToConvert == null || textToConvert.isEmpty()) {
            return "";
        }
        return textToConvert.length() > 10 ? textToConvert.substring(0, 10) : textToConvert;
    }


    @GetMapping("/set")
    @ResponseBody
    public String setValue(@RequestParam("value") String value) {
        theValue = value;
        System.out.println("EchoController set: " + theValue);
        String filePath = "C:/Users/paulp/aiholo_output.txt";
        try (FileWriter writer = new FileWriter(filePath)) {
            JSONObject json = new JSONObject();
            json.put("data", value); // Store the response inside JSON
            writer.write(json.toString());
            writer.flush();
        } catch (IOException e) {
            return "Error writing to file: " + e.getMessage();
        }

        return "set successfully: " + theValue;

    }

    @GetMapping("/get")
    @ResponseBody
    public String getValue() {
        System.out.println("EchoController get: " + theValue);
        return theValue;
    }

    @GetMapping("/playarbitrary")
    @ResponseBody
    public String playArbitrary(
            @RequestParam("answer") String answer,
            @RequestParam("languageCode") String languageCode,
            @RequestParam("voiceName") String voicename) {
        System.out.println("playarbitrary answer = " + answer + ", languageCode = " + languageCode + ", voicename = " + voicename);
        try {
            theValue = "question";
            String filePath = "C:/Users/paulp/aiholo_output.txt";
            try (FileWriter writer = new FileWriter(filePath)) {
                JSONObject json = new JSONObject();
                json.put("data", theValue); // Store the response inside JSON
                writer.write(json.toString());
                writer.flush();
            } catch (IOException e) {
                return "Error writing to file: " + e.getMessage();
            }
            TTSAndAudio2Face.processMetahuman("output.wav", answer, languageCode, voicename);
            return "OK";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}

/**
 * en-US (American English):
 * •	en-US-Neural2-F
 * •	en-US-Neural2-G
 * •	en-US-Neural2-H
 * •	en-US-Neural2-I
 * •	en-US-Neural2-J
 * •	en-US-Standard-C
 * •	en-US-Standard-E
 * •	en-US-Standard-G
 * •	en-US-Standard-I
 * •	en-US-Wavenet-C
 * •	en-US-Wavenet-E
 * •	en-US-Wavenet-G
 * •	en-US-Wavenet-I
 * <p>
 * en-GB (British English):
 * •	en-GB-Neural2-C
 * •	en-GB-Neural2-E
 * •	en-GB-Standard-A
 * •	en-GB-Standard-C
 * •	en-GB-Standard-E
 * •	en-GB-Wavenet-A
 * •	en-GB-Wavenet-C
 * •	en-GB-Wavenet-E
 */