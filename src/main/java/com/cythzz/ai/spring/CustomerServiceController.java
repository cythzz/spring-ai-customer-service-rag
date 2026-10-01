package com.cythzz.ai.spring;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer-service")
public class CustomerServiceController {

    private static final double SIMILARITY_THRESHOLD = 0.65;
    private static final int TOP_K = 3;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final HandoffTicketService handoffTicketService;

    public CustomerServiceController(ChatClient.Builder chatClientBuilder,
                                     VectorStore vectorStore,
                                     HandoffTicketService handoffTicketService) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.handoffTicketService = handoffTicketService;
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatEvent> chat(@RequestBody ChatRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            return Flux.just(ChatEvent.error("请输入需要咨询的问题。"));
        }

        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(request.question())
                .topK(TOP_K)
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .build());

        if (documents.isEmpty()) {
            var ticket = handoffTicketService.create(
                    normalizeSessionId(request.sessionId()),
                    request.question(),
                    "KNOWLEDGE_BASE_MISS");
            return Flux.just(ChatEvent.handoff(ticket));
        }

        String context = documents.stream()
                .map(Document::getText)
                .filter(Objects::nonNull)
                .filter(text -> !text.isBlank())
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");

        if (context.isBlank()) {
            var ticket = handoffTicketService.create(
                    normalizeSessionId(request.sessionId()),
                    request.question(),
                    "EMPTY_DOCUMENT_CONTENT");
            return Flux.just(ChatEvent.handoff(ticket));
        }

        List<String> sources = documents.stream()
                .map(document -> document.getMetadata().get("source"))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .distinct()
                .toList();

        Flux<ChatEvent> answer = chatClient.prompt()
                .system("""
                        你是电商平台智能客服。
                        只能根据提供的知识库内容回答用户问题。
                        回答应准确、简洁、友好，不得编造政策、库存或退款承诺。
                        如果上下文不足以回答，请明确说明并建议用户转人工客服。
                        """)
                .user(user -> user.text("""
                                用户问题：
                                {question}

                                知识库内容：
                                {context}
                                """)
                        .param("question", request.question())
                        .param("context", context))
                .stream()
                .content()
                .map(ChatEvent::message);

        return Flux.concat(
                Flux.just(ChatEvent.sources(sources)),
                answer,
                Flux.just(ChatEvent.done()));
    }

    @GetMapping("/tickets/{ticketId}")
    public HandoffTicketService.HandoffTicket getTicket(@PathVariable String ticketId) {
        return handoffTicketService.find(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在"));
    }

    private String normalizeSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return "anonymous-" + UUID.randomUUID();
        }
        return sessionId;
    }

    public record ChatRequest(String question, String sessionId) {
    }

    public record ChatEvent(String type, String content, String ticketId, List<String> sources) {

        static ChatEvent message(String content) {
            return new ChatEvent("message", content, null, List.of());
        }

        static ChatEvent sources(List<String> sources) {
            return new ChatEvent("sources", "", null, sources);
        }

        static ChatEvent handoff(HandoffTicketService.HandoffTicket ticket) {
            return new ChatEvent(
                    "handoff",
                    "知识库暂未匹配到答案，已为您创建人工客服工单。",
                    ticket.id(),
                    List.of());
        }

        static ChatEvent error(String content) {
            return new ChatEvent("error", content, null, List.of());
        }

        static ChatEvent done() {
            return new ChatEvent("done", "", null, List.of());
        }
    }
}
