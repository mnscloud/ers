package com.ers.ingestion.parser;

import com.ers.common.exception.BusinessException;
import com.ers.ingestion.domain.FileFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CsvRecordParser implements RecordParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public FileFormat supportedFormat() {
        return FileFormat.CSV;
    }

    @Override
    public List<NormalizedRecord> parse(InputStream inputStream) {
        List<NormalizedRecord> records = new ArrayList<>();
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .setIgnoreSurroundingSpaces(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord row : parser) {
                Map<String, String> asMap = new LinkedHashMap<>(row.toMap());
                records.add(new NormalizedRecord(
                        require(asMap, "externalId", row),
                        LocalDate.parse(require(asMap, "transactionDate", row)),
                        new BigDecimal(require(asMap, "amount", row)),
                        require(asMap, "currency", row).toUpperCase(),
                        asMap.getOrDefault("description", ""),
                        MAPPER.writeValueAsString(asMap)
                ));
            }
        } catch (IOException ex) {
            throw new BusinessException("CSV_PARSE_ERROR", "Failed to parse CSV file: " + ex.getMessage());
        } catch (RuntimeException ex) {
            throw new BusinessException("CSV_PARSE_ERROR", "Malformed CSV row: " + ex.getMessage());
        }
        return records;
    }

    private String require(Map<String, String> row, String column, CSVRecord record) {
        String value = row.get(column);
        if (value == null || value.isBlank()) {
            throw new BusinessException("CSV_PARSE_ERROR",
                    "Missing required column '" + column + "' on CSV line " + record.getRecordNumber());
        }
        return value;
    }
}
