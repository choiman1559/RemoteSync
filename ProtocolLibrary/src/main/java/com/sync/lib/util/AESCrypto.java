package com.sync.lib.util;

import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESCrypto {
    public static String encrypt(String plain, String TOKEN_KEY, String MacKey) throws Exception {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(parseAESToken(TOKEN_KEY).getBytes(StandardCharsets.UTF_8), "AES"), new IvParameterSpec(iv));
        byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

        final String HMacAlgorithm = "HmacSHA256";
        SecretKeySpec secretKey = new SecretKeySpec(MacKey.getBytes(StandardCharsets.UTF_8), HMacAlgorithm);
        Mac hasher = Mac.getInstance(HMacAlgorithm);

        hasher.init(secretKey);
        hasher.update(iv);
        hasher.update(cipherText);

        byte[] hash = hasher.doFinal();
        byte[] ivAndHash = getCombinedArray(iv, hash);
        byte[] finalByteArray = getCombinedArray(ivAndHash, cipherText);
        return Base64.encodeToString(finalByteArray, Base64.NO_WRAP);
    }

    public static String decrypt(String encoded, String TOKEN_KEY, String MacKey) throws GeneralSecurityException {
        byte[] rawByteArray = Base64.decode(encoded, Base64.NO_WRAP);
        byte[] iv = Arrays.copyOfRange(rawByteArray, 0, 16);
        byte[] hash = Arrays.copyOfRange(rawByteArray, 16, 48);
        byte[] cipherText = Arrays.copyOfRange(rawByteArray, 48, rawByteArray.length);

        final String HMacAlgorithm = "HmacSHA256";
        SecretKeySpec secretKey = new SecretKeySpec(MacKey.getBytes(StandardCharsets.UTF_8), HMacAlgorithm);
        Mac hasher = Mac.getInstance(HMacAlgorithm);

        hasher.init(secretKey);
        hasher.update(iv);
        hasher.update(cipherText);

        byte[] referenceHash = hasher.doFinal();
        if (!MessageDigest.isEqual(referenceHash, hash)) {
            throw new GeneralSecurityException("Could not authenticate! Please check if data is modified by unknown attacker or sent from unpaired (or maybe itself?) device");
        }

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(parseAESToken(TOKEN_KEY).getBytes(StandardCharsets.UTF_8), "AES"), new IvParameterSpec(iv));
        return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
    }

    public static String encrypt(String plain, String TOKEN_KEY) throws Exception {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(parseAESToken(TOKEN_KEY).getBytes(StandardCharsets.UTF_8), "AES"), new IvParameterSpec(iv));
        byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

        byte[] finalByteArray = getCombinedArray(iv, cipherText);
        return Base64.encodeToString(finalByteArray, Base64.NO_WRAP);
    }

    public static String decrypt(String encoded, String TOKEN_KEY) throws GeneralSecurityException {
        byte[] rawByteArray = Base64.decode(encoded, Base64.NO_WRAP);
        byte[] iv = Arrays.copyOfRange(rawByteArray, 0, 16);
        byte[] cipherText = Arrays.copyOfRange(rawByteArray, 16, rawByteArray.length);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(parseAESToken(TOKEN_KEY).getBytes(StandardCharsets.UTF_8), "AES"), new IvParameterSpec(iv));
        return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
    }

    private static byte[] getCombinedArray(byte[] one, byte[] two) {
        byte[] combined = new byte[one.length + two.length];
        for (int i = 0; i < combined.length; ++i) {
            combined[i] = i < one.length ? one[i] : two[i - one.length];
        }
        return combined;
    }

    static String parseAESToken(String string) {
        if (string.length() == 32) return string;
        string += "D~L*e/`/Q*a&h~e0jy$zU!sg?}X`CU*I";
        return string.substring(0, 32);
    }
}