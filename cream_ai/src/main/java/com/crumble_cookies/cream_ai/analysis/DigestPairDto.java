package com.crumble_cookies.cream_ai.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DigestPairDto(List<String> requirementIds, String reason) {
}
