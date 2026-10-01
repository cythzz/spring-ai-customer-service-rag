package com.cythzz.ai.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CustomerServiceToolsTests {

    private final CustomerServiceTools tools = new CustomerServiceTools();

    @Test
    void returnsKnownDemoOrder() {
        var result = tools.queryOrderStatus("order-1001");
        assertEquals("SHIPPED", result.status());
        assertEquals("SF123456789", result.trackingNumber());
    }

    @Test
    void returnsExplicitNotFoundResult() {
        assertEquals("NOT_FOUND", tools.queryOrderStatus("ORDER-9999").status());
    }
}
