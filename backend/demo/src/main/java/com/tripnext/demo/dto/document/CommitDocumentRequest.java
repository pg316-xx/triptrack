package com.tripnext.demo.dto.document;

import com.tripnext.demo.model.help.DocType;

/** Phase 2: the file is in storage under storageKey; save its metadata. */
public record CommitDocumentRequest(
        String storageKey,
        String fileName,
        DocType documentType
) {}
