package com.magmaguy.cannonrtp.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared helper for coercing raw YAML lists into string lists.
 */
final class ConfigLists {
    private ConfigLists() {
    }

    static List<String> asStringList(List<?> rawList) {
        List<String> values = new ArrayList<>();
        for (Object object : rawList) {
            if (object != null) {
                values.add(String.valueOf(object));
            }
        }
        return values;
    }
}
