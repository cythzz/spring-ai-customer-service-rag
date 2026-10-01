package com.cythzz.ai.spring;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PromptGuardrail {

    private static final List<String> BLOCKED_PATTERNS = List.of(
            "ignore previous instructions",
            "ignore all instructions",
            "reveal system prompt",
            "show system prompt",
            "忽略之前的指令",
            "忽略所有指令",
            "泄露系统提示词",
            "显示系统提示词",
            "输出数据库密码",
            "输出密钥");

    public boolean isAllowed(String question) {
        if (question == null || question.isBlank() || question.length() > 2_000) {
            return false;
        }
        String normalized = question.toLowerCase(Locale.ROOT);
        return BLOCKED_PATTERNS.stream().noneMatch(normalized::contains);
    }
}
