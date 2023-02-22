package com.sync.lib.util;

import android.util.Base64;

import com.sync.lib.data.KeySpec;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {

    /**
     * encrypt string using AES-256-CBC then HMAC-256
     *
     * @param plain String to encode
     * @param keySpec Key parameters for encryption
     * @return encoded output
     * @throws GeneralSecurityException throws when error occurred during encrypt
     */
    public synchronized static String encrypt(String plain, KeySpec keySpec) throws GeneralSecurityException {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keySpec.getAesPasswordInBytes(), "AES"), new IvParameterSpec(iv));
        byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

        byte[] data = iv;
        if(keySpec.isAuthWithHMac()) {
            final String HMacAlgorithm = "HmacSHA256";
            SecretKeySpec secretKey = new SecretKeySpec(keySpec.getHashPasswordInBytes(), HMacAlgorithm);
            Mac hasher = Mac.getInstance(HMacAlgorithm);

            hasher.init(secretKey);
            hasher.update(iv);
            hasher.update(cipherText);

            data = getCombinedArray(data, hasher.doFinal());
        }

        data = getCombinedArray(data, cipherText);
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    /**
     * decrypt string using AES-256-CBC then HMAC-256
     *
     * @param plain String to decrypt
     * @param keySpec Key parameters for decryption
     * @return decrypt output
     * @throws GeneralSecurityException throws when error occurred during decrypt
     */
    public synchronized static String decrypt(String plain, KeySpec keySpec) throws GeneralSecurityException {
        byte[] rawByteArray = Base64.decode(plain, Base64.NO_WRAP);
        byte[] iv = Arrays.copyOfRange(rawByteArray, 0, 16);
        byte[] cipherText;

        if(keySpec.isAuthWithHMac()) {
            byte[] hash = Arrays.copyOfRange(rawByteArray, 16, 48);
            cipherText = Arrays.copyOfRange(rawByteArray, 48, rawByteArray.length);

            final String HMacAlgorithm = "HmacSHA256";
            SecretKeySpec secretKey = new SecretKeySpec(keySpec.getHashPasswordInBytes(), HMacAlgorithm);
            Mac hasher = Mac.getInstance(HMacAlgorithm);

            hasher.init(secretKey);
            hasher.update(iv);
            hasher.update(cipherText);

            byte[] referenceHash = hasher.doFinal();
            if (!MessageDigest.isEqual(referenceHash, hash)) {
                throw new GeneralSecurityException("Could not authenticate! Please check if data is modified by unknown attacker or sent from unpaired (or maybe itself?) device");
            }
        } else {
            cipherText = Arrays.copyOfRange(rawByteArray, 16, rawByteArray.length);
        }

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keySpec.getAesPasswordInBytes(), "AES"), new IvParameterSpec(iv));
        return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
    }

    private static byte[] getCombinedArray(byte[] one, byte[] two) {
        byte[] combined = new byte[one.length + two.length];
        for (int i = 0; i < combined.length; ++i) {
            combined[i] = i < one.length ? one[i] : two[i - one.length];
        }
        return combined;
    }
}