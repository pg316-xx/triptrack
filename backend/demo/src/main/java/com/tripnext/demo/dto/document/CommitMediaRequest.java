package com.tripnext.demo.dto.document;

import com.tripnext.demo.model.help.MediaFileType;

/** Phase 2: the file is in storage under storageKey; save its metadata. */
public record CommitMediaRequest(
        String storageKey,
        MediaFileType mediaFileType,
        String caption
) {}
