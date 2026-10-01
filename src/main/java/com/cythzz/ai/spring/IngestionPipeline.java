package com.cythzz.ai.spring;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.ingestion.enabled", havingValue = "true", matchIfMissing = true)
class IngestionPipeline {

    private static final Logger logger = LoggerFactory.getLogger(IngestionPipeline.class);

    private final VectorStore vectorStore;

    @Value("classpath*:documents/*.md")
    Resource[] knowledgeBaseFiles;

    IngestionPipeline(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    void run() {
        logger.info("Loading {} customer-service knowledge files", knowledgeBaseFiles.length);

        List<Document> documents = Arrays.stream(knowledgeBaseFiles)
                .flatMap(resource -> {
                    String source = resource.getFilename() == null ? "unknown" : resource.getFilename();
                    var reader = new MarkdownDocumentReader(resource, MarkdownDocumentReaderConfig.builder()
                            .withAdditionalMetadata("source", source)
                            .withAdditionalMetadata("domain", "ecommerce-customer-service")
                            .build());
                    return reader.get().stream();
                })
                .toList();

        var splitter = TokenTextSplitter.builder()
                .withChunkSize(350)
                .withMinChunkSizeChars(80)
                .withMinChunkLengthToEmbed(20)
                .withKeepSeparator(true)
                .build();

        List<Document> chunks = splitter.split(documents);
        logger.info("Creating and storing embeddings for {} chunks", chunks.size());
        vectorStore.add(chunks);
    }

}
