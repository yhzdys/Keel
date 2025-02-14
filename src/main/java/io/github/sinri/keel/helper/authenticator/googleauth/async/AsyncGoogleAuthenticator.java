/*
 * Copyright (c) 2014-2018 Enrico M. Crisostomo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *   * Neither the name of the author nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.github.sinri.keel.helper.authenticator.googleauth.async;

//import org.apache.commons.codec.binary.Base32;
//import org.apache.commons.codec.binary.Base64;

import io.github.sinri.keel.helper.authenticator.googleauth.GoogleAuthenticatorConfig;
import io.github.sinri.keel.helper.authenticator.googleauth.GoogleAuthenticatorException;
import io.github.sinri.keel.helper.authenticator.googleauth.GoogleAuthenticatorKey;
import io.github.sinri.keel.helper.authenticator.googleauth.ReseedingSecureRandom;
import io.vertx.core.Future;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.github.sinri.keel.helper.KeelHelpersInterface.KeelHelpers;

/**
 * This class implements the functionality described in RFC 6238 (TOTP: Time
 * based one-time password algorithm) and has been tested again Google's
 * implementation of such algorithm in its Google Authenticator application.
 * <p>
 * This class lets users create a new 16-bit base32-encoded secret key with
 * the validation code calculated at {@code time = 0} (the UNIX epoch) and the
 * URL of a Google-provided QR barcode to let an user load the generated
 * information into Google Authenticator.
 * <p>
 * The random number generator used by this class uses the default algorithm and
 * provider.  Users can override them by setting the following system properties
 * to the algorithm and provider name of their choice:
 * <ul>
 * <li>{@link #RNG_ALGORITHM}.</li>
 * <li>{@link #RNG_ALGORITHM_PROVIDER}.</li>
 * </ul>
 * <p>
 * This class does not store in any way either the generated keys nor the keys
 * passed during the authorization process.
 * <p>
 * Java Server side class for Google Authenticator's TOTP generator was inspired
 * by an author's blog post.
 *
 * @author Enrico M. Crisostomo
 * @author Warren Strange
 * @author Sinri Edogawa
 * @version 1.1.4
 * @see <a href="http://thegreyblog.blogspot.com/2011/12/google-authenticator-using-it-in-your.html"></a>
 * @see <a href="http://code.google.com/p/google-authenticator"></a>
 * @see <a href="http://tools.ietf.org/id/draft-mraihi-totp-timebased-06.txt"></a>
 * @since 0.3.0
 * @since 3.2.9 Support Async!
 */
public final class AsyncGoogleAuthenticator implements AsyncIGoogleAuthenticator {

    /**
     * The system property to specify the random number generator algorithm to use.
     *
     * @since 0.5.0
     */
    public static final String RNG_ALGORITHM = "com.warrenstrange.googleauth.rng.algorithm";

    /**
     * The system property to specify the random number generator provider to use.
     *
     * @since 0.5.0
     */
    public static final String RNG_ALGORITHM_PROVIDER = "com.warrenstrange.googleauth.rng.algorithmProvider";

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = Logger.getLogger(AsyncGoogleAuthenticator.class.getName());

    /**
     * Number of digits of a scratch code represented as a decimal integer.
     */
    private static final int SCRATCH_CODE_LENGTH = 8;

    /**
     * Modulus used to truncate the scratch code.
     */
    public static final int SCRATCH_CODE_MODULUS = (int) Math.pow(10, SCRATCH_CODE_LENGTH);

    /**
     * Magic number representing an invalid scratch code.
     */
    private static final int SCRATCH_CODE_INVALID = -1;

    /**
     * Length in bytes of each scratch code. We're using Google's default of
     * using 4 bytes per scratch code.
     */
    private static final int BYTES_PER_SCRATCH_CODE = 4;

    /**
     * The default SecureRandom algorithm to use if none is specified.
     *
     * @see java.security.SecureRandom#getInstance(String)
     * @since 0.5.0
     */
    private static final String DEFAULT_RANDOM_NUMBER_ALGORITHM = "SHA1PRNG";

    /**
     * The default random number algorithm provider to use if none is specified.
     *
     * @see java.security.SecureRandom#getInstance(String)
     * @since 0.5.0
     */
    private static final String DEFAULT_RANDOM_NUMBER_ALGORITHM_PROVIDER = "SUN";

    /**
     * The configuration used by the current instance.
     */
    private final GoogleAuthenticatorConfig config;

    /**
     * The internal SecureRandom instance used by this class.  Since Java 7
     * {@link Random} instances are required to be thread-safe, no synchronisation is
     * required in the methods of this class using this instance.  Thread-safety
     * of this class was a de-facto standard in previous versions of Java so
     * that it is expected to work correctly in previous versions of the Java
     * platform as well.
     */
    private ReseedingSecureRandom secureRandom;

    private AsyncICredentialRepository credentialRepository;
    private boolean credentialRepositorySearched;

    /**
     * The constructor that uses the default config, random number algorithm, and random number algorithm provider.
     */
    public AsyncGoogleAuthenticator() {
        config = new GoogleAuthenticatorConfig();

        this.secureRandom = new ReseedingSecureRandom(
                getRandomNumberAlgorithm(),
                getRandomNumberAlgorithmProvider());
    }

    /**
     * The constructor that allows a user to specify the config and uses the default randomNumberAlgorithm and randomNumberAlgorithmProvider.
     *
     * @param config The configuration used by the current instance.
     */
    public AsyncGoogleAuthenticator(GoogleAuthenticatorConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null.");
        }

        this.config = config;

        this.secureRandom = new ReseedingSecureRandom(
                getRandomNumberAlgorithm(),
                getRandomNumberAlgorithmProvider()
        );
    }

    /**
     * The constructor that allows a user the randomNumberAlgorithm, the randomNumberAlgorithmProvider, and uses the default config.
     *
     * @param randomNumberAlgorithm         The random number algorithm to define the secure random number generator. If this is null the randomNumberAlgorithmProvider must be null.
     * @param randomNumberAlgorithmProvider The random number algorithm provider to define the secure random number generator. This value may be null.
     */
    public AsyncGoogleAuthenticator(final String randomNumberAlgorithm, final String randomNumberAlgorithmProvider) {
        this(new GoogleAuthenticatorConfig(), randomNumberAlgorithm, randomNumberAlgorithmProvider);

    }

    /**
     * The constructor that allows a user to specify the config, the randomNumberAlgorithm, and the randomNumberAlgorithmProvider.
     *
     * @param config                        The configuration used by the current instance.
     * @param randomNumberAlgorithm         The random number algorithm to define the secure random number generator. If this is null the randomNumberAlgorithmProvider must be null.
     * @param randomNumberAlgorithmProvider The random number algorithm provider to define the secure random number generator. This value may be null.
     */
    public AsyncGoogleAuthenticator(GoogleAuthenticatorConfig config, final String randomNumberAlgorithm, final String randomNumberAlgorithmProvider) {
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null.");
        }

        this.config = config;

        if (randomNumberAlgorithm == null && randomNumberAlgorithmProvider == null) {
            this.secureRandom = new ReseedingSecureRandom();
        } else if (randomNumberAlgorithm == null) {
            throw new IllegalArgumentException("RandomNumberAlgorithm must not be null. If the RandomNumberAlgorithm is null, the RandomNumberAlgorithmProvider must also be null.");
        } else if (randomNumberAlgorithmProvider == null) {
            this.secureRandom = new ReseedingSecureRandom(randomNumberAlgorithm);
        }
    }

    /**
     * @return the default random number generator algorithm.
     * @since 0.5.0
     */
    private String getRandomNumberAlgorithm() {
        return System.getProperty(
                RNG_ALGORITHM,
                DEFAULT_RANDOM_NUMBER_ALGORITHM);
    }

    /**
     * @return the default random number generator algorithm provider.
     * @since 0.5.0
     */
    private String getRandomNumberAlgorithmProvider() {
        return System.getProperty(
                RNG_ALGORITHM_PROVIDER,
                DEFAULT_RANDOM_NUMBER_ALGORITHM_PROVIDER);
    }

    /**
     * Calculates the verification code of the provided key at the specified
     * instant of time using the algorithm specified in RFC 6238.
     *
     * @param key the secret key in binary format.
     * @param tm  the instant of time.
     * @return the validation code for the provided key at the specified instant
     * of time.
     */
    int calculateCode(byte[] key, long tm) {
        // Allocating an array of bytes to represent the specified instant
        // of time.
        byte[] data = new byte[8];
        long value = tm;

        // Converting the instant of time from the long representation to a
        // big-endian array of bytes (RFC4226, 5.2. Description).
        for (int i = 8; i-- > 0; value >>>= 8) {
            data[i] = (byte) value;
        }

        // Building the secret key specification for the HmacSHA1 algorithm.
        SecretKeySpec signKey = new SecretKeySpec(key, config.getHmacHashFunction().toString());

        try {
            // Getting an HmacSHA1/HmacSHA256 algorithm implementation from the JCE.
            Mac mac = Mac.getInstance(config.getHmacHashFunction().toString());

            // Initializing the MAC algorithm.
            mac.init(signKey);

            // Processing the instant of time and getting the encrypted data.
            byte[] hash = mac.doFinal(data);

            // Building the validation code performing dynamic truncation
            // (RFC4226, 5.3. Generating an HOTP value)
            int offset = hash[hash.length - 1] & 0xF;

            // We are using a long because Java hasn't got an unsigned integer type
            // and we need 32 unsigned bits).
            long truncatedHash = 0;

            for (int i = 0; i < 4; ++i) {
                truncatedHash <<= 8;

                // Java bytes are signed but we need an unsigned integer:
                // cleaning off all but the LSB.
                truncatedHash |= (hash[offset + i] & 0xFF);
            }

            // Clean bits higher than the 32nd (inclusive) and calculate the
            // module with the maximum validation code value.
            truncatedHash &= 0x7FFFFFFF;
            truncatedHash %= config.getKeyModulus();

            // Returning the validation code to the caller.
            return (int) truncatedHash;
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            // Logging the exception.
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);

            // We're not disclosing internal error details to our clients.
            throw new GoogleAuthenticatorException("The operation cannot be performed now.");
        }
    }

    private long getTimeWindowFromTime(long time) {
        return time / this.config.getTimeStepSizeInMillis();
    }

    /**
     * This method implements the algorithm specified in RFC 6238 to check if a
     * validation code is valid in a given instant of time for the given secret
     * key.
     *
     * @param secret    the Base32 encoded secret key.
     * @param code      the code to validate.
     * @param timestamp the instant of time to use during the validation process.
     * @param window    the window size to use during the validation process.
     * @return <code>true</code> if the validation code is valid,
     * <code>false</code> otherwise.
     */
    private Future<Boolean> checkCode(
            String secret,
            long code,
            long timestamp,
            int window) {
        byte[] decodedKey = decodeSecret(secret);

        // convert unix time into a 30 second "window" as specified by the
        // TOTP specification. Using Google's default interval of 30 seconds.
        final long timeWindow = getTimeWindowFromTime(timestamp);

        // Calculating the verification code of the given key in each of the
        // time intervals and returning true if the provided code is equal to
        // one of them.
        for (int i = -((window - 1) / 2); i <= window / 2; ++i) {
            // Calculating the verification code for the current time interval.
            long hash = calculateCode(decodedKey, timeWindow + i);

            // Checking if the provided code is equal to the calculated one.
            if (hash == code) {
                // The verification code is valid.
                return Future.succeededFuture(true);
            }
        }

        // The verification code is invalid.
        return Future.succeededFuture(false);
    }

    private byte[] decodeSecret(String secret) {
        // Decoding the secret key to get its raw byte representation.
        switch (config.getKeyRepresentation()) {
            case BASE32:
                return KeelHelpers.stringHelper().decodeWithBase32ToBytes(secret.toUpperCase());

//                Base32 codec32 = new Base32();
            // See: https://issues.apache.org/jira/browse/CODEC-234
            // Commons Codec Base32::decode does not support lowercase letters.
//                return codec32.decode(secret.toUpperCase());
            case BASE64:
                return KeelHelpers.stringHelper().decodeWithBase32ToBytes(secret);

//                Base64 codec64 = new Base64();
//                return codec64.decode(secret);
            default:
                throw new IllegalArgumentException("Unknown key representation type.");
        }
    }

    @Override
    public GoogleAuthenticatorKey createCredentials() {
        // Allocating a buffer sufficiently large to hold the bytes required by
        // the secret key.
        int bufferSize = config.getSecretBits() / 8;
        byte[] buffer = new byte[bufferSize];

        secureRandom.nextBytes(buffer);

        // Extracting the bytes making up the secret key.
        byte[] secretKey = Arrays.copyOf(buffer, bufferSize);
        String generatedKey = calculateSecretKey(secretKey);

        // Generating the verification code at time = 0.
        int validationCode = calculateValidationCode(secretKey);

        // Calculate scratch codes
        List<Integer> scratchCodes = calculateScratchCodes();

        return
                new GoogleAuthenticatorKey
                        .Builder(generatedKey)
                        .setConfig(config)
                        .setVerificationCode(validationCode)
                        .setScratchCodes(scratchCodes)
                        .build();
    }

    @Override
    public Future<GoogleAuthenticatorKey> createCredentials(String userName) {
        return Future.succeededFuture()
                .compose(v -> {
                    // Further validation will be performed by the configured provider.
                    if (userName == null) {
                        throw new IllegalArgumentException("User name cannot be null.");
                    }

                    GoogleAuthenticatorKey key = createCredentials();
                    return Future.succeededFuture(key);
                })
                .compose(key -> {
                    AsyncICredentialRepository repository = getValidCredentialRepository();
                    return repository.saveUserCredentials(
                                    userName,
                                    key.getKey(),
                                    key.getVerificationCode(),
                                    key.getScratchCodes()
                            )
                            .compose(v -> {
                                return Future.succeededFuture(key);
                            });
                });
    }

    private List<Integer> calculateScratchCodes() {
        final List<Integer> scratchCodes = new ArrayList<>();

        for (int i = 0; i < config.getNumberOfScratchCodes(); ++i) {
            scratchCodes.add(generateScratchCode());
        }

        return scratchCodes;
    }

    /**
     * This method calculates a scratch code from a random byte buffer of
     * suitable size <code>#BYTES_PER_SCRATCH_CODE</code>.
     *
     * @param scratchCodeBuffer a random byte buffer whose minimum size is
     *                          <code>#BYTES_PER_SCRATCH_CODE</code>.
     * @return the scratch code.
     */
    private int calculateScratchCode(byte[] scratchCodeBuffer) {
        if (scratchCodeBuffer.length < BYTES_PER_SCRATCH_CODE) {
            throw new IllegalArgumentException(
                    String.format(
                            "The provided random byte buffer is too small: %d.",
                            scratchCodeBuffer.length));
        }

        int scratchCode = 0;

        for (int i = 0; i < BYTES_PER_SCRATCH_CODE; ++i) {
            scratchCode = (scratchCode << 8) + (scratchCodeBuffer[i] & 0xff);
        }

        scratchCode = (scratchCode & 0x7FFFFFFF) % SCRATCH_CODE_MODULUS;

        // Accept the scratch code only if it has exactly
        // SCRATCH_CODE_LENGTH digits.
        if (validateScratchCode(scratchCode)) {
            return scratchCode;
        } else {
            return SCRATCH_CODE_INVALID;
        }
    }

    /* package */ boolean validateScratchCode(int scratchCode) {
        return (scratchCode >= SCRATCH_CODE_MODULUS / 10);
    }

    /**
     * This method creates a new random byte buffer from which a new scratch
     * code is generated. This function is invoked if a scratch code generated
     * from the main buffer is invalid because it does not satisfy the scratch
     * code restrictions.
     *
     * @return A valid scratch code.
     */
    private int generateScratchCode() {
        while (true) {
            byte[] scratchCodeBuffer = new byte[BYTES_PER_SCRATCH_CODE];
            secureRandom.nextBytes(scratchCodeBuffer);

            int scratchCode = calculateScratchCode(scratchCodeBuffer);

            if (scratchCode != SCRATCH_CODE_INVALID) {
                return scratchCode;
            }
        }
    }

    /**
     * This method calculates the validation code at time 0.
     *
     * @param secretKey The secret key to use.
     * @return the validation code at time 0.
     */
    private int calculateValidationCode(byte[] secretKey) {
        return calculateCode(secretKey, 0);
    }


    public int getTotpPassword(String secret) {
        return getTotpPassword(secret, new Date().getTime());
    }

    public int getTotpPassword(String secret, long time) {
        return calculateCode(decodeSecret(secret), getTimeWindowFromTime(time));
    }

    public Future<Integer> getTotpPasswordOfUser(String userName) {
        return getTotpPasswordOfUser(userName, new Date().getTime());
    }

    public Future<Integer> getTotpPasswordOfUser(String userName, long time) {
        AsyncICredentialRepository repository = getValidCredentialRepository();
        return repository.getSecretKey(userName)
                .compose(secretKey -> {
                    byte[] x = decodeSecret(secretKey);
                    int totpPassword = calculateCode(x, getTimeWindowFromTime(time));
                    return Future.succeededFuture(totpPassword);
                });
    }

    /**
     * This method calculates the secret key given a random byte buffer.
     *
     * @param secretKey a random byte buffer.
     * @return the secret key.
     */
    private String calculateSecretKey(byte[] secretKey) {
        switch (config.getKeyRepresentation()) {
            case BASE32:
                return KeelHelpers.binaryHelper().encodeWithBase32ToString(secretKey);
//                return new Base32().encodeToString(secretKey);
            case BASE64:
                return KeelHelpers.binaryHelper().encodeWithBase64ToString(secretKey);
//                return new Base64().encodeToString(secretKey);
            default:
                throw new IllegalArgumentException("Unknown key representation type.");
        }
    }

    @Override
    public Future<Boolean> authorize(String secret, int verificationCode) {
        return authorize(secret, verificationCode, new Date().getTime());
    }

    @Override
    public Future<Boolean> authorize(String secret, int verificationCode, long time) {
        // Checking user input and failing if the secret key was not provided.
        if (secret == null) {
            throw new IllegalArgumentException("Secret cannot be null.");
        }

        // Checking if the verification code is between the legal bounds.
        if (verificationCode <= 0 || verificationCode >= this.config.getKeyModulus()) {
            return Future.succeededFuture(false);
        }

        // Checking the validation code using the current UNIX time.
        return checkCode(
                secret,
                verificationCode,
                time,
                this.config.getWindowSize());
    }

    @Override
    public Future<Boolean> authorizeUser(String userName, int verificationCode) {
        return authorizeUser(userName, verificationCode, new Date().getTime());
    }

    @Override
    public Future<Boolean> authorizeUser(String userName, int verificationCode, long time) {
        AsyncICredentialRepository repository = getValidCredentialRepository();
        return repository.getSecretKey(userName)
                .compose(secretKey -> {
                    return authorize(secretKey, verificationCode, time);
                });
    }

    /**
     * This method loads the first available and valid ICredentialRepository
     * registered using the Java service loader API.
     *
     * @return the first registered ICredentialRepository.
     * @throws UnsupportedOperationException if no valid service is
     *                                       found.
     */
    private AsyncICredentialRepository getValidCredentialRepository() {
        AsyncICredentialRepository repository = getCredentialRepository();

        if (repository == null) {
            throw new UnsupportedOperationException(
                    String.format("An instance of the %s service must be " +
                                    "configured in order to use this feature.",
                            AsyncICredentialRepository.class.getName()
                    )
            );
        }

        return repository;
    }

    /**
     * This method loads the first available ICredentialRepository
     * registered using the Java service loader API.
     *
     * @return the first registered ICredentialRepository or <code>null</code>
     * if none is found.
     */
    public AsyncICredentialRepository getCredentialRepository() {
        if (this.credentialRepositorySearched) return this.credentialRepository;

        this.credentialRepositorySearched = true;

        ServiceLoader<AsyncICredentialRepository> loader =
                ServiceLoader.load(AsyncICredentialRepository.class);

        for (AsyncICredentialRepository repository : loader) {
            this.credentialRepository = repository;
            break;
        }

        return this.credentialRepository;
    }

    @Override
    public void setCredentialRepository(AsyncICredentialRepository repository) {
        this.credentialRepository = repository;
        this.credentialRepositorySearched = true;
    }
}
