package com.ers.ingestion.parser;

import com.ers.ingestion.domain.FileFormat;

import java.io.InputStream;
import java.util.List;

/**
 * Extension point for normalizing a source file into the standardized RawTransactionRecord schema.
 * Add new implementations (e.g. a real SWIFT/MT940 parser) and register them here as the system grows.
 */
public interface RecordParser {

    FileFormat supportedFormat();

    List<NormalizedRecord> parse(InputStream inputStream);
}
