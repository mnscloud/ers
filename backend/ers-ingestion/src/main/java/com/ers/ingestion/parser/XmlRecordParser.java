package com.ers.ingestion.parser;

import com.ers.ingestion.domain.FileFormat;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * Placeholder for a follow-up iteration: XML source feeds (e.g. SWIFT/ISO 20022) need a schema-aware
 * mapping layer that CSV/JSON don't. Wired into the RecordParser registry now so the upload endpoint
 * already dispatches correctly once a real implementation lands here.
 */
@Component
public class XmlRecordParser implements RecordParser {

    @Override
    public FileFormat supportedFormat() {
        return FileFormat.XML;
    }

    @Override
    public List<NormalizedRecord> parse(InputStream inputStream) {
        throw new UnsupportedOperationException("XML ingestion is not yet implemented");
    }
}
