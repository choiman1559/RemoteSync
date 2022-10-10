package com.sync.lib.data;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class KeySpec {
    private String encryptionPassword;
    private String secondaryPassword;
    private boolean authWithHMac;
    private boolean isSymmetric;

    KeySpec() {
        encryptionPassword = "";
        secondaryPassword = "";
        authWithHMac = false;
        isSymmetric = false;
    }

    public static class Builder {
        private final KeySpec keySpec;

        public Builder() {
            this.keySpec = new KeySpec();
        }

        public Builder(KeySpec keySpec) {
            this.keySpec = keySpec;
        }

        public Builder setEncryptionPassword(String encryptionPassword) {
            keySpec.encryptionPassword = encryptionPassword;
            return this;
        }

        public Builder setAuthWithHMac(boolean authWithHMac) {
            keySpec.authWithHMac = authWithHMac;
            return this;
        }

        public Builder setIsSymmetric(boolean isSymmetric) {
            keySpec.isSymmetric = isSymmetric;
            return this;
        }

        public KeySpec build() {
            return keySpec;
        }
    }

    public String getEncryptionPassword() {
        return encryptionPassword;
    }

    public String getSecondaryPassword() {
        return secondaryPassword;
    }

    public boolean isAuthWithHMac() {
        return authWithHMac;
    }

    public boolean isSymmetric() {
        return isSymmetric;
    }

    public byte[] getAesPasswordInBytes() {
        return parseToken(encryptionPassword, secondaryPassword);
    }

    public byte[] getHashPasswordInBytes() {
        return parseToken(secondaryPassword, encryptionPassword);
    }

    public void setSecondaryPassword(String secondaryPassword) {
        this.secondaryPassword = secondaryPassword;
    }

    public boolean isValidKey() {
        boolean isPasswordValid = encryptionPassword != null && !encryptionPassword.isEmpty();
        return authWithHMac ? isPasswordValid && secondaryPassword != null && !secondaryPassword.isEmpty() : isPasswordValid;
    }

    private byte[] parseToken(String main, String sub) {
        if (isSymmetric) {
            byte[] mainBytes = main.getBytes(StandardCharsets.UTF_8);
            byte[] subBytes = sub.getBytes(StandardCharsets.UTF_8);

            return getCombinedArray(Arrays.copyOfRange(mainBytes, 0, 24), Arrays.copyOfRange(subBytes, subBytes.length - 8, subBytes.length));
        } else {
            if (main.length() == 32) return main.getBytes(StandardCharsets.UTF_8);
            main += "D~L*e/`/Q*a&h~e0jy$zU!sg?}X`CU*I";
            return main.substring(0, 32).getBytes(StandardCharsets.UTF_8);
        }
    }

    private static byte[] getCombinedArray(byte[] one, byte[] two) {
        byte[] combined = new byte[one.length + two.length];
        for (int i = 0; i < combined.length; ++i) {
            combined[i] = i < one.length ? one[i] : two[i - one.length];
        }
        return combined;
    }
}
