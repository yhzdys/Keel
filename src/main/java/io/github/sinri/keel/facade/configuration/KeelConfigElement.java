package io.github.sinri.keel.facade.configuration;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class KeelConfigElement {
    @Nonnull
    private final String name;
    @Nonnull
    private final Map<String, KeelConfigElement> children;
    @Nullable
    private String value;

    public KeelConfigElement(@Nonnull String name) {
        this.name = name;
        this.value = null;
        this.children = new ConcurrentHashMap<>();
    }

    public KeelConfigElement(@Nonnull KeelConfigElement another) {
        this.name = another.getName();
        this.children = another.getChildren();
        this.value = another.getValueAsString();
    }

    public static KeelConfigElement fromJsonObject(@Nonnull JsonObject jsonObject) {
        String name = jsonObject.getString("name");
        KeelConfigElement keelConfigElement = new KeelConfigElement(name);
        if (jsonObject.containsKey("value")) {
            keelConfigElement.value = jsonObject.getString("value");
        }
        JsonArray children = jsonObject.getJsonArray("children");
        children.forEach(child -> {
            if (child instanceof JsonObject) {
                keelConfigElement.addChild(fromJsonObject((JsonObject) child));
            } else {
                throw new IllegalArgumentException();
            }
        });
        return keelConfigElement;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nullable
    public String getValueAsString() {
        return value;
    }

    @Nullable
    public String getValueAsString(@Nullable String def) {
        return Objects.requireNonNullElse(value, def);
    }

    @Nullable
    public String getValueAsString(@Nonnull List<String> keychain, @Nullable String def) {
        KeelConfigElement extracted = this.extractConfigElement(keychain);
        if (extracted == null) return def;
        return extracted.getValueAsString(def);
    }

    @Nullable
    public String getValueAsString(@Nonnull String keychain, @Nullable String def) {
        return getValueAsString(List.of(keychain), def);
    }

    @Nullable
    public Integer getValueAsInteger() {
        if (value == null) return null;
        return Integer.parseInt(value);
    }

    public int getValueAsInteger(int def) {
        if (value == null) return def;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public int getValueAsInteger(@Nonnull List<String> keychain, int def) {
        KeelConfigElement extracted = this.extractConfigElement(keychain);
        if (extracted == null) return def;
        return extracted.getValueAsInteger(def);
    }

    public int getValueAsInteger(@Nonnull String keychain, int def) {
        return getValueAsInteger(List.of(keychain), def);
    }

    @Nullable
    public Long getValueAsLong() {
        if (value == null) return null;
        return Long.parseLong(value);
    }

    public long getValueAsLong(long def) {
        if (value == null) return def;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public long getValueAsLong(@Nonnull List<String> keychain, long def) {
        KeelConfigElement extracted = this.extractConfigElement(keychain);
        if (extracted == null) return def;
        return extracted.getValueAsLong(def);
    }

    public long getValueAsLong(@Nonnull String keychain, long def) {
        return getValueAsLong(List.of(keychain), def);
    }

    @Nullable
    public Float getValueAsFloat() {
        if (value == null) return null;
        return Float.parseFloat(value);
    }

    public float getValueAsFloat(float def) {
        if (value == null) return def;
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public float getValueAsFloat(@Nonnull List<String> keychain, float def) {
        KeelConfigElement extracted = this.extractConfigElement(keychain);
        if (extracted == null) return def;
        return extracted.getValueAsFloat(def);
    }

    public float getValueAsFloat(@Nonnull String keychain, float def) {
        return getValueAsFloat(List.of(keychain), def);
    }

    @Nullable
    public Double getValueAsDouble() {
        if (value == null) return null;
        return Double.parseDouble(value);
    }

    public double getValueAsDouble(double def) {
        if (value == null) return def;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public double getValueAsDouble(@Nonnull List<String> keychain, double def) {
        KeelConfigElement extracted = this.extractConfigElement(keychain);
        if (extracted == null) return def;
        return extracted.getValueAsDouble(def);
    }

    public double getValueAsDouble(@Nonnull String keychain, double def) {
        return getValueAsDouble(List.of(keychain), def);
    }

    @Nullable
    public Boolean getValueAsBoolean() {
        if (value == null) return null;
        return "YES".equalsIgnoreCase(value) || "TRUE".equalsIgnoreCase(value);
    }

    public boolean getValueAsBoolean(boolean def) {
        if (value == null) return def;
        return "YES".equalsIgnoreCase(value)
                || "TRUE".equalsIgnoreCase(value)
                || "ON".equalsIgnoreCase(value)
                || "1".equalsIgnoreCase(value)
                ;
    }

    public boolean getValueAsBoolean(@Nonnull List<String> keychain, boolean def) {
        KeelConfigElement extracted = this.extractConfigElement(keychain);
        if (extracted == null) return def;
        return extracted.getValueAsBoolean(def);
    }

    public boolean getValueAsBoolean(@Nonnull String keychain, boolean def) {
        return getValueAsBoolean(List.of(keychain), def);
    }

    public KeelConfigElement ensureChild(@Nonnull String childName) {
        return this.children.computeIfAbsent(childName, x -> new KeelConfigElement(childName));
    }

    public KeelConfigElement addChild(@Nonnull KeelConfigElement child) {
        this.children.put(child.getName(), child);
        return this;
    }

    public KeelConfigElement removeChild(@Nonnull KeelConfigElement child) {
        this.children.remove(child.getName());
        return this;
    }

    public KeelConfigElement removeChild(@Nonnull String childName) {
        this.children.remove(childName);
        return this;
    }

    public KeelConfigElement setValue(@Nonnull String value) {
        this.value = value;
        return this;
    }

    @Nonnull
    public Map<String, KeelConfigElement> getChildren() {
        return children;
    }

    @Nullable
    public KeelConfigElement getChild(@Nonnull String childName) {
        return children.get(childName);
    }

    public JsonObject toJsonObject() {
        JsonArray childArray = new JsonArray();
        children.forEach((cName, c) -> childArray.add(c.toJsonObject()));
        var x = new JsonObject()
                .put("name", name)
                .put("children", childArray);
        if (value != null) {
            x.put("value", value);
        }
        return x;
    }

    public @Nullable KeelConfigElement extractConfigElement(@Nonnull List<String> split) {
        if (split.isEmpty()) return null;
        if (split.size() == 1) return this.children.get(split.get(0));
        KeelConfigElement keelConfigElement = this.children.get(split.get(0));
        if (keelConfigElement == null) {
            return null;
        }
        for (int i = 1; i < split.size(); i++) {
            keelConfigElement = keelConfigElement.getChild(split.get(i));
            if (keelConfigElement == null) {
                return null;
            }
        }
        return keelConfigElement;
    }

    public @Nullable KeelConfigElement extractConfigElement(@Nonnull String... split) {
        List<String> list = Arrays.asList(split);
        return this.extractConfigElement(list);
    }

    public KeelConfigElement loadProperties(@Nonnull Properties properties) {
        properties.forEach((k, v) -> {
            String fullKey = k.toString();
            String[] keyArray = fullKey.split("\\.");
            KeelConfigElement keelConfigElement = null;
            for (int i = 0; i < keyArray.length; i++) {
                String key = keyArray[i];
                if (i == 0) {
                    keelConfigElement = children.computeIfAbsent(key, x -> new KeelConfigElement(key));
                } else {
                    keelConfigElement = keelConfigElement.ensureChild(key);
                }
                if (i == keyArray.length - 1) {
                    keelConfigElement.setValue(properties.getProperty(fullKey));
                }
            }
        });
        return this;
    }

    /**
     * @since 3.0.1
     */
    public @Nonnull KeelConfigElement loadPropertiesFile(@Nonnull String propertiesFileName) {
        return loadPropertiesFile(propertiesFileName, StandardCharsets.UTF_8);
    }

    public @Nonnull KeelConfigElement loadPropertiesFile(@Nonnull String propertiesFileName, @Nonnull Charset charset) {
        Properties properties = new Properties();
        try {
            // here, the file named as `propertiesFileName` should be put along with JAR
            properties.load(new FileReader(propertiesFileName, charset));
        } catch (IOException e) {
            System.err.println("Cannot find the file config.properties. Use the embedded one.");
            try {
                properties.load(getClass().getClassLoader().getResourceAsStream(propertiesFileName));
            } catch (IOException ex) {
                throw new RuntimeException("Cannot find the embedded file config.properties.", ex);
            }
        }

        return loadProperties(properties);
    }

    /**
     * @since 3.0.6
     */
    public @Nonnull KeelConfigElement loadPropertiesFileContent(@Nonnull String content) {
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(content));
        } catch (IOException e) {
            throw new RuntimeException("Cannot load given properties content.", e);
        }
        return loadProperties(properties);
    }
}
