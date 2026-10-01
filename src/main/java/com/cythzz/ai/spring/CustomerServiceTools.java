package com.cythzz.ai.spring;

import java.util.Locale;
import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class CustomerServiceTools {

    private static final Map<String, OrderStatus> DEMO_ORDERS = Map.of(
            "ORDER-1001", new OrderStatus("ORDER-1001", "SHIPPED", "SF123456789", "预计明天送达"),
            "ORDER-1002", new OrderStatus("ORDER-1002", "PAID", "", "仓库正在拣货"),
            "ORDER-1003", new OrderStatus("ORDER-1003", "REFUNDING", "", "退款审核中"));

    @Tool(description = "查询本地演示订单的状态、物流单号和进度。仅支持 ORDER-1001、ORDER-1002、ORDER-1003。")
    public OrderStatus queryOrderStatus(
            @ToolParam(description = "订单编号，例如 ORDER-1001") String orderNumber) {
        if (orderNumber == null) {
            return OrderStatus.notFound("");
        }
        String normalized = orderNumber.trim().toUpperCase(Locale.ROOT);
        return DEMO_ORDERS.getOrDefault(normalized, OrderStatus.notFound(normalized));
    }

    public record OrderStatus(String orderNumber, String status, String trackingNumber, String message) {
        static OrderStatus notFound(String orderNumber) {
            return new OrderStatus(orderNumber, "NOT_FOUND", "", "未找到该演示订单，请核对订单号或转人工客服");
        }
    }
}
