//package ua.vbielskyi.bmf.core.telegram.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import ua.vbielskyi.bmf.core.telegram.model.BotMessage;
//import ua.vbielskyi.bmf.core.telegram.model.BotResponse;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.function.Function;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class ConversationFlowService {
//
//    // Cache of active conversation flows
//    private final Map<String, ConversationFlow> activeFlows = new ConcurrentHashMap<>();
//
//    // Registry of conversation flow templates
//    private final Map<String, ConversationFlowTemplate> flowTemplates = new ConcurrentHashMap<>();
//
//    /**
//     * Start a new conversation flow
//     */
//    public BotResponse startFlow(String flowType, BotMessage message) {
//        ConversationFlowTemplate template = flowTemplates.get(flowType);
//        if (template == null) {
//            log.error("No conversation flow template found for type: {}", flowType);
//            return BotResponse.text(message.getChatId(), "Sorry, this conversation flow is not available.");
//        }
//
//        // Create flow key
//        String flowKey = buildFlowKey(message.getUserId(), message.getTenantId());
//
//        // Initialize new flow instance
//        ConversationFlow flow = template.createFlow();
//        flow.setUserId(message.getUserId());
//        flow.setTenantId(message.getTenantId());
//        flow.setCurrentStep(template.getInitialStep());
//        flow.setContext(new HashMap<>());
//
//        // Store in active flows
//        activeFlows.put(flowKey, flow);
//
//        // Execute the initial step
//        return executeFlowStep(flow, message);
//    }
//
//    /**
//     * Process a message within an active flow
//     */
//    public BotResponse processFlowMessage(BotMessage message) {
//        String flowKey = buildFlowKey(message.getUserId(), message.getTenantId());
//        ConversationFlow flow = activeFlows.get(flowKey);
//
//        if (flow == null) {
//            log.debug("No active flow found for user: {}, tenant: {}", message.getUserId(), message.getTenantId());
//            return null; // No active flow
//        }
//
//        // Check for flow cancellation command
//        if (message.getText() != null && message.getText().equalsIgnoreCase("/cancel")) {
//            return cancelFlow(flowKey, message.getChatId());
//        }
//
//        // Process the message with the current step
//        return executeFlowStep(flow, message);
//    }
//
//    /**
//     * Execute the current flow step
//     */
//    private BotResponse executeFlowStep(ConversationFlow flow, BotMessage message) {
//        ConversationStep currentStep = flow.getCurrentStep();
//
//        try {
//            // Process the input
//            if (currentStep.getInputProcessor() != null) {
//                currentStep.getInputProcessor().apply(flow);
//            }
//
//            // Generate response
//            BotResponse response = currentStep.getResponseGenerator().apply(flow, message);
//
//            // Determine next step
//            ConversationStep nextStep = currentStep.getNextStepSelector().apply(flow, message);
//            flow.setCurrentStep(nextStep);
//
//            // Check if flow is complete
//            if (nextStep == null) {
//                String flowKey = buildFlowKey(message.getUserId(), message.getTenantId());
//                activeFlows.remove(flowKey);
//                log.debug("Conversation flow completed: {}", flowKey);
//            }
//
//            return response;
//        } catch (Exception e) {
//            log.error("Error executing conversation flow step", e);
//            return BotResponse.text(message.getChatId(),
//                    "Sorry, there was an error processing your request. Please try again or use /cancel to reset.");
//        }
//    }
//
//    /**
//     * Cancel an active flow
//     */
//    private BotResponse cancelFlow(String flowKey, Long chatId) {
//        activeFlows.remove(flowKey);
//        log.debug("Conversation flow cancelled: {}", flowKey);
//        return BotResponse.text(chatId, "The current operation has been cancelled. What would you like to do next?");
//    }
//
//    /**
//     * Register a flow template
//     */
//    public void registerFlowTemplate(String flowType, ConversationFlowTemplate template) {
//        flowTemplates.put(flowType, template);
//        log.info("Registered conversation flow template: {}", flowType);
//    }
//
//    /**
//     * Check if user has an active flow
//     */
//    public boolean hasActiveFlow(Long userId, UUID tenantId) {
//        String flowKey = buildFlowKey(userId, tenantId);
//        return activeFlows.containsKey(flowKey);
//    }
//
//    /**
//     * Build a unique key for the flow
//     */
//    private String buildFlowKey(Long userId, UUID tenantId) {
//        return tenantId + ":" + userId;
//    }
//
//    /**
//     * Conversation flow template
//     */
//    public interface ConversationFlowTemplate {
//        ConversationFlow createFlow();
//        ConversationStep getInitialStep();
//    }
//
//    /**
//     * Active conversation flow instance
//     */
//    @lombok.Data
//    public static class ConversationFlow {
//        private Long userId;
//        private UUID tenantId;
//        private ConversationStep currentStep;
//        private Map<String, Object> context;
//    }
//
//    /**
//     * Step in a conversation flow
//     */
//    @lombok.Data
//    @lombok.Builder
//    public static class ConversationStep {
//        private String id;
//        private Function<ConversationFlow, BotMessage> inputProcessor;
//        private Function<ConversationFlow, BotMessage> responseGenerator;
//        private Function<ConversationFlow, BotMessage> nextStepSelector;
//    }
//}