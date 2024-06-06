package io.github.sinri.keel.facade.configuration;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
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

import static io.github.sinri.keel.facade.KeelInstance.Keel;


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

    /**
     * @since 3.2.10
     */
    public static Future<KeelConfigElement> retrieve(@Nonnull ConfigRetrieverOptions configRetrieverOptions) {
//        ConfigRetrieverOptions configRetrieverOptions = new ConfigRetrieverOptions()
//                .addStore(configStoreOptions);
        ConfigRetriever configRetriever = ConfigRetriever.create(Keel.getVertx(), configRetrieverOptions);
        return configRetriever.getConfig()
                .compose(jsonObject -> {
                    KeelConfigElement element = fromJsonObject(jsonObject);
                    return Future.succeededFuture(element);
                })
                .andThen(ar -> {
                    configRetriever.close();
                });
    }

    public static ConfigStoreOptions buildConfigStoreOptionsForPropertiesFile(@Nonnull String file) {
        return new ConfigStoreOptions()
                .setType("file")
                .setFormat("properties")
                .setConfig(new JsonObject()
                        .put("path", file)
                );
    }

    public static ConfigStoreOptions buildConfigStoreOptionsForJsonObject(@Nonnull JsonObject jsonObject) {
        return new ConfigStoreOptions()
                .setType("json")
                .setConfig(jsonObject);
    }

    public static ConfigStoreOptions buildConfigStoreOptionsForEnvironment(boolean useRawData, @Nullable List<String> envKeys) {
        var x = new ConfigStoreOptions()
                .setType("env");
        var c = new JsonObject();
        if (useRawData) {
            c.put("raw-data", true);
        }
        if (envKeys != null) {
            c.put("keys", new JsonArray(envKeys));
        }
        if (!c.isEmpty()) {
            x.setConfig(c);
        }
        return x;
    }

    public static ConfigStoreOptions buildConfigStoreOptionsForSystemProperties(
            boolean alwaysUseCache,
            boolean useRawData,
            boolean hierarchical
    ) {
        var x = new ConfigStoreOptions()
                .setType("sys");
        var c = new JsonObject();
        if (!alwaysUseCache) {
            c.put("cache", false);
        }
        if (useRawData) {
            c.put("raw-data", true);
        }
        if (hierarchical) {
            c.put("hierarchical", true);
        }
        if (!c.isEmpty()) {
            x.setConfig(c);
        }
        return x;
    }

    // http, event bus, directory

    @Nonnull
    public String getName() {
        return name;
    }

    @Nullable
    public String getValueAsString() {
        return value;
    }

    @Nullable
    public String getValueAsStringElse(@Nullable String def) {
        return Objects.requireNonNullElse(value, def);
    }

    @Nullable
    public String readString() {
        return readString(List.of());
    }

    @Nullable
    public String readString(@Nonnull List<String> keychain) {
        var x = extract(keychain);
        if (x == null) return null;
        return x.getValueAsString();
    }

    @Nullable
    public String readString(@Nonnull List<String> keychain, @Nullable String def) {
        KeelConfigElement extracted = this.extract(keychain);
        if (extracted == null) return def;
        return extracted.getValueAsStringElse(def);
    }

    @Nullable
    public String readString(@Nonnull String keychain, @Nullable String def) {
        return readString(List.of(keychain), def);
    }

    @Nullable
    public Integer getValueAsInteger() {
        if (value == null) return null;
        return Integer.parseInt(value);
    }

    public int getValueAsIntegerElse(int def) {
        if (value == null) return def;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @Nullable
    public Integer readInteger() {
        return readInteger(List.of());
    }

    @Nullable
    public Integer readInteger(@Nonnull List<String> keychain) {
        var x = this.extract(keychain);
        if (x == null) return null;
        return x.getValueAsInteger();
    }

    public int readInteger(@Nonnull List<String> keychain, int def) {
        KeelConfigElement extracted = this.extract(keychain);
        if (extracted == null) return def;
        return extracted.getValueAsIntegerElse(def);
    }

    public int readInteger(@Nonnull String keychain, int def) {
        return readInteger(List.of(keychain), def);
    }

    @Nullable
    public Long getValueAsLong() {
        if (value == null) return null;
        return Long.parseLong(value);
    }

    public long getValueAsLongElse(long def) {
        if (value == null) return def;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @Nullable
    public Long readLong() {
        return readLong(List.of());
    }

    @Nullable
    public Long readLong(@Nonnull List<String> keychain) {
        KeelConfigElement extracted = this.extract(keychain);
        if (extracted != null) {
            return extracted.getValueAsLong();
        } else {
            return null;
        }
    }

    public long readLong(@Nonnull List<String> keychain, long def) {
        KeelConfigElement extracted = this.extract(keychain);
        if (extracted == null) return def;
        return extracted.getValueAsLongElse(def);
    }

    public long readLong(@Nonnull String keychain, long def) {
        return readLong(List.of(keychain), def);
    }

    @Nullable
    public Float getValueAsFloat() {
        if (value == null) return null;
        return Float.parseFloat(value);
    }

    public float getValueAsFloatElse(float def) {
        if (value == null) return def;
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @Nullable
    public Float readFloat() {
        return readFloat(List.of());
    }

    @Nullable
    public Float readFloat(@Nonnull List<String> keychain) {
        var x = extract(keychain);
        if (x == null) return null;
        return x.getValueAsFloat();
    }

    public float readFloat(@Nonnull List<String> keychain, float def) {
        KeelConfigElement extracted = this.extract(keychain);
        if (extracted == null) return def;
        return extracted.getValueAsFloatElse(def);
    }

    public float readFloat(@Nonnull String keychain, float def) {
        return readFloat(List.of(keychain), def);
    }

    @Nullable
    public Double getValueAsDouble() {
        if (value == null) return null;
        return Double.parseDouble(value);
    }

    public double getValueAsDoubleElse(double def) {
        if (value == null) return def;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @Nullable
    public Double readDouble() {
        return readDouble(List.of());
    }

    @Nullable
    public Double readDouble(@Nonnull List<String> keychain) {
        var x = extract(keychain);
        if (x == null) return null;
        return x.getValueAsDouble();
    }

    public double readDouble(@Nonnull List<String> keychain, double def) {
        KeelConfigElement extracted = this.extract(keychain);
        if (extracted == null) return def;
        return extracted.getValueAsDoubleElse(def);
    }

    public double readDouble(@Nonnull String keychain, double def) {
        return readDouble(List.of(keychain), def);
    }

    @Nullable
    public Boolean getValueAsBoolean() {
        if (value == null) return null;
        return "YES".equalsIgnoreCase(value) || "TRUE".equalsIgnoreCase(value);
    }

    public boolean getValueAsBooleanElse(boolean def) {
        if (value == null) return def;
        return "YES".equalsIgnoreCase(value)
                || "TRUE".equalsIgnoreCase(value)
                || "ON".equalsIgnoreCase(value)
                || "1".equalsIgnoreCase(value)
                ;
    }

    @Nullable
    public Boolean readBoolean() {
        return readBoolean(List.of());
    }

    @Nullable
    public Boolean readBoolean(@Nonnull List<String> keychain) {
        var x = extract(keychain);
        if (x == null) return null;
        return x.getValueAsBoolean();
    }

    public boolean readBoolean(@Nonnull List<String> keychain, boolean def) {
        KeelConfigElement extracted = this.extract(keychain);
        if (extracted == null) return def;
        return extracted.getValueAsBooleanElse(def);
    }

    public boolean readBoolean(@Nonnull String keychain, boolean def) {
        return readBoolean(List.of(keychain), def);
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

    /**
     * @param split The list of keys. If empty, give this, or dig in.
     */
    public @Nullable KeelConfigElement extract(@Nonnull List<String> split) {
        if (split.isEmpty()) return this;
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

    public @Nullable KeelConfigElement extract(@Nonnull String... split) {
        List<String> list = Arrays.asList(split);
        return this.extract(list);
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
