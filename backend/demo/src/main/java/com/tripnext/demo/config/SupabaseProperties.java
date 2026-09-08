package com.tripnext.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "supabase")
public class SupabaseProperties {

    private String url;
    private String serviceKey;

    private Bucket bucket = new Bucket();

    @Getter
    @Setter
    public static class Bucket {
        private String documents;
        private String media;
    }
}