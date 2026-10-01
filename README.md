# Spring AI 电商智能客服 RAG Demo

一个面向学习和作品集展示的轻量智能客服项目。系统读取本地 Markdown 知识库，完成文档切片、向量入库、相似度检索和 LLM 流式回答；知识库未命中时创建模拟人工客服工单。

## 核心功能

- Spring Boot 4 + Java 17
- Spring AI + Ollama 本地模型
- PGVector 向量存储
- Markdown 文档读取与 Token 切片
- Top-K 相似度检索和来源返回
- SSE 流式输出
- 知识库未命中自动创建人工工单
- Spring AI `@Tool` 受控工具调用（本地演示订单查询）
- 提示注入基础拦截与工具结果边界
- Actuator、Prometheus 指标和 OpenTelemetry 链路追踪
- GitHub Actions 自动编译测试
- 本地 Docker Compose 环境，无网络爬虫

## 处理流程

```text
本地 Markdown 文档
  → TokenTextSplitter 文档切片
  → Ollama Embedding
  → PGVector

用户问题
  → 输入安全检查 → 相似度检索
  ├─ 命中：拼装上下文 → Ollama → SSE 流式回答
  │                         └─ 需要订单状态：调用本地 Order Tool
  └─ 未命中：创建内存工单 → 返回 handoff 事件
```

## 环境要求

- Java 17
- Docker Desktop 或兼容的 Docker 环境

## 启动

一键构建并启动应用、PGVector、Ollama、Prometheus、Grafana 和 OTel Collector：

```powershell
docker compose up -d --build
```

首次运行建议预先下载模型：

```powershell
docker exec customer-service-ollama ollama pull granite3.3:2b
docker exec customer-service-ollama ollama pull granite-embedding:278m
```

如需在 IDEA 中调试，只启动基础设施并本地运行应用：

```powershell
docker compose up -d pgvector ollama otel-collector prometheus grafana
.\gradlew.bat bootRun
```

应用启动时会读取 `src/main/resources/documents` 中的 Markdown 文件并写入 PGVector。

## 接口

### 流式客服问答

```http
POST /api/customer-service/chat/stream
Content-Type: application/json
Accept: text/event-stream
```

请求示例：

```json
{
  "sessionId": "demo-session-001",
  "question": "退款审核通过后多久可以到账？"
}
```

命中知识库时依次返回 `sources`、多个 `message` 和 `done` 事件。未命中时返回 `handoff` 事件及工单编号。

### 查询人工工单

```http
GET /api/customer-service/tickets/{ticketId}
```

当前工单存储在内存中，仅用于演示；重启应用后数据会清空。

可用演示订单为 `ORDER-1001`、`ORDER-1002` 和 `ORDER-1003`。模型只有在回答订单状态问题时才允许调用本地查询工具；工具不会连接真实电商系统。

## 可观测性

- 健康检查：`/actuator/health`
- Prometheus 指标：`/actuator/prometheus`
- Prometheus 控制台：本机 `9090`
- Grafana：本机 `3000`（默认账号和密码均为 `admin`）
- OTel Collector 接收应用 OTLP/HTTP Trace，并在容器日志中输出调试信息

## 配置项

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/customer_service` | PostgreSQL 地址 |
| `DB_USERNAME` | `postgres` | 数据库用户名 |
| `DB_PASSWORD` | `postgres` | 数据库密码 |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama 地址 |
| `OLLAMA_CHAT_MODEL` | `granite3.3:2b` | 对话模型 |
| `OLLAMA_EMBEDDING_MODEL` | `granite-embedding:278m` | 向量模型 |
| `KNOWLEDGE_INGESTION_ENABLED` | `true` | 是否在启动时执行知识入库 |

## 测试

```powershell
.\gradlew.bat test
```

## 本人改造内容

- 从上游多模块示例中提取并整理单一 RAG 应用
- 将故事文档替换为电商订单、物流和售后知识库
- 增加显式向量检索、来源返回及 SSE 流式事件
- 增加知识库未命中的人工工单兜底
- 增加本地订单工具调用、提示注入防护及对应单元测试
- 增加 Docker Compose、CI、Prometheus 和 OpenTelemetry
- 删除外部搜索和与业务无关的示例模块

## 开源说明

本项目基于 `ThomasVitale/llm-apps-java-spring-ai` 中的 RAG 示例改造，保留原项目提交历史、版权信息和 Apache License 2.0。新增客服业务模型、接口、知识库内容、人工兜底流程及运行文档由本仓库维护者实现。
