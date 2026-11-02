package com.example.repro;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("scalar")
public record ScalarProperties(Integer timeout) {
}
