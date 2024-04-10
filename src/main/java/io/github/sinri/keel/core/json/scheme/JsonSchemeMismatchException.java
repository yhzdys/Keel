package io.github.sinri.keel.core.json.scheme;


import java.util.ArrayList;
import java.util.List;

import static io.github.sinri.keel.helper.KeelHelpersInterface.KeelHelpers;

@Deprecated(since = "3.2.4")
public class JsonSchemeMismatchException extends Exception {
    public static final String RuleSchemeError = "SchemeError";
    public static final String RuleEmptyArrayNotAllowed = "EmptyArrayNotAllowed";
    public static final String RuleNullableNotAllowed = "NullableNotAllowed";
    public static final String RuleValueTypeNotExpected = "ValueTypeNotExpected";
    public static final String RuleValueNotExpected = "ValueNotExpected";
    public static final String RuleValueLacked = "ValueLacked";
    public static final String RuleFieldsMismatchInStrictMode = "FieldsMismatchInStrictMode";

    protected final List<String> keychain = new ArrayList<>();

    public JsonSchemeMismatchException(String reason) {
        super(reason);
    }

    public JsonSchemeMismatchException(String key, String reason) {
        super(reason);

        keychain.add(key);
    }

    public JsonSchemeMismatchException(String key, JsonSchemeMismatchException deepException) {
        super(deepException.getMessage(), deepException);

        keychain.add(key);
        keychain.addAll(deepException.getKeychain());
    }

    public List<String> getKeychain() {
        return keychain;
    }

    public String getDesc() {
        String msg = getMessage();
        if (!getKeychain().isEmpty()) {
            msg += " keychain: " + KeelHelpers.stringHelper().joinStringArray(getKeychain(), " → ");
        }
        return msg;
    }
}
