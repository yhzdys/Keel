# AES in Keel

Java provided `PKCS5Padding`.

If you needs `PKCS7Padding`, you may need BC-PROV.

Append this to the pom file.

````xml
<!-- pom.xml -->
<dependencies>
    <!-- Bouncy Castle Provider (提供基本的加密算法和功能) -->
    <!-- https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk18on -->
    <dependency>
        <groupId>org.bouncycastle</groupId>
        <artifactId>bcprov-jdk18on</artifactId>
        <version>1.78.1</version>
    </dependency>
    <!-- Bouncy Castle PKIX/CMS/EAC/PKCS/TSP/OPENSSL (提供用于证书生成和管理等高级功能的库) -->
    <!-- https://mvnrepository.com/artifact/org.bouncycastle/bcpkix-jdk18on -->
    <dependency>
        <groupId>org.bouncycastle</groupId>
        <artifactId>bcpkix-jdk18on</artifactId>
        <version>1.78.1</version>
    </dependency>
</dependencies>
````

Then call `io.github.sinri.keel.core.helper.encryption.aes.KeelAesUsingPkcs7Padding.requireBouncyCastleProvider();`
in the initialization code of your program.

A sample of main class with main method to startup:

```java
import io.github.sinri.keel.helper.encryption.aes.KeelAesUsingPkcs7Padding;

public class Main {
    public static void main(String[] args) {
        KeelAesUsingPkcs7Padding.requireBouncyCastleProvider();
    }
}
```

