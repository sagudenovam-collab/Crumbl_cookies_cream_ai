package com.crumble_cookies.cream_ai.extraction;

import com.crumble_cookies.cream_ai.model.Requirement;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Присваивает внутренние ID требованиям (REQ-001, REQ-002, ...) и выполняет
 * базовую нормализацию: обнаружение требований без исходного ID,
 * пустых/вырожденных текстов.
 */
@Component
public class RequirementNormalizer {

    public List<Requirement> normalize(List<Requirement> extracted) {
        List<Requirement> result = new ArrayList<>();
        Set<String> seenTexts = new HashSet<>();
        int counter = 1;
        for (Requirement req : extracted) {
            if (req.getOriginalText() == null || req.getOriginalText().isBlank()) {
                continue;
            }
            String normalizedKey = req.getOriginalText().trim().toLowerCase();
            if (!seenTexts.add(normalizedKey)) {
                // точный текстовый дубликат — пропускаем, настоящий Duplicate Detection
                // выполняется отдельно на уровне набора для менее очевидных случаев.
                continue;
            }
            req.setInternalId("REQ-%03d".formatted(counter++));
            result.add(req);
        }
        return result;
    }
}
