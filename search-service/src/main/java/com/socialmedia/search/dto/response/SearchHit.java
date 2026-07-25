package com.socialmedia.search.dto.response;

import java.util.List;
import java.util.Map;

public record SearchHit<T>(T document, double score, Map<String, List<String>> highlight) {
}
