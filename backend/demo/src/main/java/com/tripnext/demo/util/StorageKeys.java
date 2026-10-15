package com.tripnext.demo.util;

import java.util.UUID;

/** Builds object keys like "{prefix}/{uuid}-{safe-file-name}". */
public final class StorageKeys {

    private StorageKeys() {
    }

    public static String newKey(Object prefix, String fileName) {
        return prefix + "/" + UUID.randomUUID() + "-" + safeName(fileName);
    }

    public static String safeName(String fileName) {
        String name = fileName == null || fileName.isBlank() ? "file" : fileName;
        // keep only the last path segment, then strip anything URL-unfriendly
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        return name.length() > 100 ? name.substring(name.length() - 100) : name;
    }
}
