package com.ers.ingestion.parser;

import com.ers.common.exception.BusinessException;
import com.ers.ingestion.domain.FileFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads the same normalized schema as the CSV parser (externalId, transactionDate, amount,
 * currency, description) from an .xlsx workbook's first sheet, header row first. */
@Component
public class XlsxRecordParser implements RecordParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<String> REQUIRED_COLUMNS = List.of("externalId", "transactionDate", "amount", "currency");

    @Override
    public FileFormat supportedFormat() {
        return FileFormat.XLSX;
    }

    @Override
    public List<NormalizedRecord> parse(InputStream inputStream) {
        List<NormalizedRecord> records = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new BusinessException("XLSX_PARSE_ERROR", "Sheet has no header row");
            }
            Map<String, Integer> columnIndex = new LinkedHashMap<>();
            for (Cell cell : headerRow) {
                columnIndex.put(cellToString(cell).trim(), cell.getColumnIndex());
            }
            for (String required : REQUIRED_COLUMNS) {
                if (!columnIndex.containsKey(required)) {
                    throw new BusinessException("XLSX_PARSE_ERROR", "Missing required column '" + required + "'");
                }
            }

            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                Map<String, String> rawValues = new LinkedHashMap<>();
                for (Map.Entry<String, Integer> col : columnIndex.entrySet()) {
                    rawValues.put(col.getKey(), cellToString(row.getCell(col.getValue())));
                }

                records.add(new NormalizedRecord(
                        require(rawValues, "externalId", r),
                        parseDate(row.getCell(columnIndex.get("transactionDate")), r),
                        parseAmount(row.getCell(columnIndex.get("amount")), r),
                        require(rawValues, "currency", r).toUpperCase(),
                        rawValues.getOrDefault("description", ""),
                        MAPPER.writeValueAsString(rawValues)
                ));
            }
        } catch (IOException ex) {
            throw new BusinessException("XLSX_PARSE_ERROR", "Failed to parse XLSX file: " + ex.getMessage());
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BusinessException("XLSX_PARSE_ERROR", "Malformed XLSX row: " + ex.getMessage());
        }
        return records;
    }

    private boolean isBlankRow(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.BLANK && !cellToString(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String require(Map<String, String> row, String column, int rowNum) {
        String value = row.get(column);
        if (value == null || value.isBlank()) {
            throw new BusinessException("XLSX_PARSE_ERROR", "Missing required value for '" + column + "' on row " + (rowNum + 1));
        }
        return value;
    }

    private String cellToString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double value = cell.getNumericCellValue();
                yield value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
            }
            case FORMULA -> cell.getCellFormula();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private LocalDate parseDate(Cell cell, int rowNum) {
        if (cell == null) {
            throw new BusinessException("XLSX_PARSE_ERROR", "Missing required value for 'transactionDate' on row " + (rowNum + 1));
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        return LocalDate.parse(cellToString(cell).trim());
    }

    private BigDecimal parseAmount(Cell cell, int rowNum) {
        if (cell == null) {
            throw new BusinessException("XLSX_PARSE_ERROR", "Missing required value for 'amount' on row " + (rowNum + 1));
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        return new BigDecimal(cellToString(cell).trim());
    }
}
