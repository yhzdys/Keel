package io.github.sinri.keel.test.lab.helper;

import static io.github.sinri.keel.helper.KeelHelpersInterface.KeelHelpers;

public class NyaCodecTest {

    public static void main(String[] args) throws Exception {
        var s = "for dp&bi, when value < 3 and age >4 阿嚏&#64;！！！@飞飞飞";
        String encoded = KeelHelpers.stringHelper().encodeToNyaCode(s);
        System.out.println(encoded);
        String decoded = KeelHelpers.stringHelper().decodeFromNyaCode(encoded);
        System.out.println(decoded);
        assert decoded.equals(s);
    }
}
