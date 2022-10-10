package com.sync.lib.data;

import java.nio.charset.StandardCharsets;

public class KeySpec {
    private String encryptionPassword;
    private String hmacPassword;
    private boolean authWithHMac;
    private boolean isSymmetric;

    KeySpec() {
        encryptionPassword = "";
        hmacPassword = "";
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

        public Builder setHmacPassword(String hmacPassword) {
            keySpec.hmacPassword = hmacPassword;
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

    public String getHmacPassword() {
        return hmacPassword;
    }

    public boolean isAuthWithHMac() {
        return authWithHMac;
    }

    public boolean isSymmetric() {
        return isSymmetric;
    }

    public byte[] getEncryptionPasswordBytes() {
        return parseToken(encryptionPassword).getBytes(StandardCharsets.UTF_8);
    }

    public byte[] getHmacPasswordBytes() {
        return parseToken(hmacPassword).getBytes(StandardCharsets.UTF_8);
    }

    public boolean isValidKey() {
        boolean isPasswordValid = encryptionPassword != null && !encryptionPassword.isEmpty();
        return authWithHMac ? isPasswordValid && hmacPassword != null && !hmacPassword.isEmpty() : isPasswordValid;
    }

    private String parseToken(String string) {
        if (string.length() == 32) return string;
        string += "D~L*e/`/Q*a&h~e0jy$zU!sg?}X`CU*I";
        return string.substring(0, 32);
    }
}
