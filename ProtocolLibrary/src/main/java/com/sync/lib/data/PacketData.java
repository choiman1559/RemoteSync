package com.sync.lib.data;

import com.sync.lib.util.AESCrypto;
import com.sync.lib.util.ArrayConverter;
import com.sync.lib.util.CompressStringUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Objects;

public class PacketData {
    Map<String, String> rawData;
    public final String actionType;

    public PacketData(Map<String, String> map) {
        this.rawData = map;
        if(rawData == null) throw new NullPointerException("Map data is null!");

        this.actionType = rawData.get("type");
    }

    public boolean isEncrypted() {
        if(rawData == null) throw new NullPointerException("Map data is null!");
        return Objects.requireNonNull(rawData.get("encrypted")).equals("true");
    }

    public PacketData decryptData(String password) throws JSONException, GeneralSecurityException {
        JSONObject object = new JSONObject(AESCrypto.decrypt(CompressStringUtil.decompressString(rawData.get("encryptedData")), password));
        return new PacketData(ArrayConverter.toMap(object));
    }
}
