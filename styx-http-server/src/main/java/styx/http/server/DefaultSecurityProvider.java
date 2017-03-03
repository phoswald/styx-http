package styx.http.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Logger;

import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.QueryStringEncoder;
import styx.http.QueryParam;

public class DefaultSecurityProvider implements SecurityProvider {

    private static final String HASH_NAME = "SHA-256";
    private static final int HASH_BYTES = 32;

    private static final Logger logger = Logger.getLogger(DefaultSecurityProvider.class.getName());

    private final byte[] secret;

    public DefaultSecurityProvider() {
        this("" + System.currentTimeMillis() + "/" + System.nanoTime()); // TODO: improve security (use more random bits)
    }

    public DefaultSecurityProvider(String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String signCookie(List<QueryParam> sessionParams, Instant expiry) {
        QueryStringEncoder queryStringEncoder = new QueryStringEncoder(Long.toString(expiry.getEpochSecond()));
        for(QueryParam param : sessionParams) {
            queryStringEncoder.addParam(param.name(), param.value());
        }
        return hashAndEncode(queryStringEncoder.toString());
    }

    @Override
    public Optional<List<QueryParam>> checkCookie(String cookie) {
        Optional<String> payloadText = decodeAndVerifyHash(cookie);
        if(!payloadText.isPresent()) {
            return Optional.empty();
        }
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(payloadText.get());
        List<QueryParam> sessionParams = new ArrayList<>();
        Map<String, List<String>> paramMap = queryStringDecoder.parameters();
        for (Entry<String, List<String>> paramEntry : paramMap.entrySet()) {
            for (String value : paramEntry.getValue()) {
                sessionParams.add(new QueryParam(paramEntry.getKey(), value));
            }
        }
        Instant expiry = Instant.ofEpochSecond(Long.parseLong(queryStringDecoder.path()));
        if(expiry.isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(sessionParams);
    }

    private String hashAndEncode(String payloadText) {
        try {
            byte[] payloadBytes = payloadText.getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            stream.write(payloadBytes);
            stream.write(computeHash(payloadBytes));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(stream.toByteArray());
        } catch(IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private Optional<String> decodeAndVerifyHash(String singedText) {
        try {
            byte[] signedBytes = Base64.getUrlDecoder().decode(singedText);
            byte[] payloadBytes = Arrays.copyOf(signedBytes, signedBytes.length - HASH_BYTES);
            byte[] hashBytes = Arrays.copyOfRange(signedBytes, signedBytes.length - HASH_BYTES, signedBytes.length);
            byte[] hashBytes2 = computeHash(payloadBytes);
            String payloadText = new String(payloadBytes, StandardCharsets.UTF_8);
            if(!Arrays.equals(hashBytes, hashBytes2)) {
                logger.warning("Hash mismatch (attack?) for: " + payloadText);
                return Optional.empty();
            }
            return Optional.of(payloadText);
        } catch(NoSuchAlgorithmException | RuntimeException e) {
            return Optional.empty();
        }
    }

    private byte[] computeHash(byte[] payloadBytes) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(HASH_NAME);
        digest.update(payloadBytes);
        digest.update(secret);
        return digest.digest();
    }
}
