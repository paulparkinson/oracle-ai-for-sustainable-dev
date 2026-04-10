package oracleai;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.events.QueueManager;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.TaskStore;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.AgentCard;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.Task;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TextPart;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import org.springaicommunity.a2a.server.controller.AgentCardController;
import org.springaicommunity.a2a.server.controller.MessageController;
import org.springaicommunity.a2a.server.controller.TaskController;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory-action")
public class InventoryActionA2AController {

    private final AgentCardController agentCardController;
    private final MessageController messageController;
    private final TaskController taskController;

    public InventoryActionA2AController(
            Environment environment,
            InventoryActionAdkService inventoryActionAdkService,
            TaskStore taskStore,
            QueueManager queueManager,
            PushNotificationConfigStore pushNotificationConfigStore,
            PushNotificationSender pushNotificationSender
    ) {
        AgentCard inventoryActionCard = InventoryActionCardFactory.buildInventoryActionAgentCard(environment);
        RequestHandler requestHandler = DefaultRequestHandler.create(
                buildAgentExecutor(inventoryActionAdkService),
                taskStore,
                queueManager,
                pushNotificationConfigStore,
                pushNotificationSender,
                ForkJoinPool.commonPool()
        );

        this.agentCardController = new AgentCardController(inventoryActionCard);
        this.messageController = new MessageController(requestHandler);
        this.taskController = new TaskController(requestHandler);
    }

    @GetMapping(
            path = "/.well-known/agent-card.json",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public AgentCard getAgentCard() {
        return agentCardController.getAgentCard();
    }

    @GetMapping(
            path = "/card",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public AgentCard getAgentCardV1() {
        return agentCardController.getAgentCardV1();
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public SendMessageResponse sendMessage(@RequestBody SendMessageRequest request) throws JSONRPCError {
        return messageController.sendMessage(request);
    }

    @GetMapping(
            path = "/tasks/{taskId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Task getTask(@PathVariable String taskId) throws JSONRPCError {
        return taskController.getTask(taskId);
    }

    @PostMapping(
            path = "/tasks/{taskId}/cancel",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Task cancelTask(@PathVariable String taskId) throws JSONRPCError {
        return taskController.cancelTask(taskId);
    }

    private static AgentExecutor buildAgentExecutor(InventoryActionAdkService inventoryActionAdkService) {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                TaskUpdater updater = new TaskUpdater(context, eventQueue);
                if (context.getTask() == null) {
                    updater.submit();
                }

                updater.startWork();

                try {
                    String userInput = context.getUserInput("");
                    InventoryActionAdkService.InventoryActionResult result =
                            inventoryActionAdkService.run(userInput, context.getContextId());

                    updater.complete(
                            updater.newAgentMessage(
                                    List.of(new TextPart(result.responseText())),
                                    Map.of(
                                            "coordinator", result.orchestrationMode(),
                                            "traceCount", result.trace().size()
                                    )
                            )
                    );
                } catch (Exception exception) {
                    updater.fail(
                            updater.newAgentMessage(
                                    List.of(new TextPart("Inventory action coordinator failed: " + exception.getMessage())),
                                    Map.of("error", "inventory_action_execution_failed")
                            )
                    );
                    throw new JSONRPCError(
                            -32603,
                            "Inventory action coordinator failed: " + exception.getMessage(),
                            null
                    );
                }
            }

            @Override
            public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                throw new TaskNotCancelableError();
            }
        };
    }
}
