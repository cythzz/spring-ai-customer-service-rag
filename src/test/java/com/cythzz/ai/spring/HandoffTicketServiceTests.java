package com.cythzz.ai.spring;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandoffTicketServiceTests {

    private final HandoffTicketService service = new HandoffTicketService();

    @Test
    void createsAndFindsTicket() {
        var ticket = service.create("session-001", "订单为什么没有物流？", "KNOWLEDGE_BASE_MISS");

        assertTrue(ticket.id().startsWith("CS-"));
        assertEquals(HandoffTicketService.TicketStatus.OPEN, ticket.status());
        assertEquals(ticket, service.find(ticket.id()).orElseThrow());
    }

    @Test
    void createsUniqueTicketIds() {
        var first = service.create("session-001", "问题一", "MISS");
        var second = service.create("session-001", "问题二", "MISS");

        assertNotEquals(first.id(), second.id());
    }
}
