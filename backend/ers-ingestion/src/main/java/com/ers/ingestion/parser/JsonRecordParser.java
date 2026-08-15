package com.ers.ingestion.parser;

import com.ers.common.exception.BusinessException;
import com.ers.ingestion.domain.FileFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class JsonRecordParser implements RecordParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public FileFormat supportedFormat() {
        return FileFormat.JSON;
    }

    @Override
    public List<NormalizedRecord> parse(InputStream inputStream) {
        List<NormalizedRecord> records = new ArrayList<>();
        try {
            JsonNode root = MAPPER.readTree(inputStream);
            if (!root.isArray()) {
                throw new BusinessException("JSON_PARSE_ERROR", "Expected a top-level JSON array of transaction records");
            }
            for (JsonNode node : root) {
                records.add(new NormalizedRecord(
                        require(node, "externalId"),
                        LocalDate.parse(require(node, "transactionDate")),
                        new BigDecimal(require(node, "amount")),
                        require(node, "currency").toUpperCase(),
                        node.path("description").asText(""),
                        node.toString()
                ));
            }
        } catch (IOException ex) {
            throw new BusinessException("JSON_PARSE_ERROR", "Failed to parse JSON file: " + ex.getMessage());
        } catch (RuntimeException ex) {
            throw new BusinessException("JSON_PARSE_ERROR", "Malformed JSON record: " + ex.getMessage());
        }
        return records;
    }

    private String require(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new BusinessException("JSON_PARSE_ERROR", "Missing required field '" + field + "'");
        }
        return value.asText();
    }
}
