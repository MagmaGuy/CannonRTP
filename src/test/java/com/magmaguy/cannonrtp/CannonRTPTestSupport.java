package com.magmaguy.cannonrtp;

import org.mockbukkit.mockbukkit.MockBukkit;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

final class CannonRTPTestSupport {
    private static final String PLUGIN_YML = """
            name: CannonRTPTest
            version: 1.0.0
            main: com.magmaguy.cannonrtp.MockCannonRTPPlugin
            api-version: '1.21'
            commands:
              cannonrtp:
                description: Test CannonRTP command.
                usage: /cannonrtp
                aliases: [wc]
            permissions:
              cannonrtp.admin:
                default: op
              cannonrtp.use:
                default: true
            """;

    private CannonRTPTestSupport() {
    }

    static MockCannonRTPPlugin loadPlugin() {
        return MockBukkit.loadWith(
                MockCannonRTPPlugin.class,
                new ByteArrayInputStream(PLUGIN_YML.getBytes(StandardCharsets.UTF_8)));
    }

    static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to set field " + fieldName, exception);
        }
    }

    static void setStaticField(Class<?> type, String fieldName, Object value) {
        try {
            Field field = findField(type, fieldName);
            field.setAccessible(true);
            field.set(null, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to set static field " + fieldName, exception);
        }
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
