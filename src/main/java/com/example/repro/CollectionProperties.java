package com.example.repro;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("collection")
public record CollectionProperties(Set<Integer> ids) {
}
