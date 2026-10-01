package com.cythzz.ai.spring;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HandoffTicketService {

    private final Map<String, HandoffTicket> tickets = new ConcurrentHashMap<>();

    public HandoffTicket create(String sessionId, String question, String reason) {
        String ticketId = "CS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        HandoffTicket ticket = new HandoffTicket(
                ticketId,
                sessionId,
                question,
                reason,
                TicketStatus.OPEN,
                Instant.now());
        tickets.put(ticketId, ticket);
        return ticket;
    }

    public Optional<HandoffTicket> find(String ticketId) {
        return Optional.ofNullable(tickets.get(ticketId));
    }

    public enum TicketStatus {
        OPEN,
        IN_PROGRESS,
        CLOSED
    }

    public record HandoffTicket(String id,
                                String sessionId,
                                String question,
                                String reason,
                                TicketStatus status,
                                Instant createdAt) {
    }
}
