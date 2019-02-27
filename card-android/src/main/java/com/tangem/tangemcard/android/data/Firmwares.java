package com.tangem.tangemcard.android.data;

import android.content.Context;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tangem.tangemcommon.data.external.FirmwaresDigestsProvider;
import com.tangem.tangemcommon.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Firmwares implements FirmwaresDigestsProvider {
    private static JsonArray jaFirmwares = null;

    public Firmwares(Context context) {
        try (InputStream is = context.getAssets().open("fw_hashes.json")) {
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                JsonParser parser = new JsonParser();
                jaFirmwares = parser.parse(reader).getAsJsonArray();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public FirmwaresDigestsProvider.VerifyCodeRecord selectRandomVerifyCodeBlock(String firmwareVersion) {

        try {
            for (int i = 0; i < jaFirmwares.size(); i++) {
                JsonObject jsVersion = jaFirmwares.get(i).getAsJsonObject();
                if (jsVersion.get("fw").getAsString().equals(firmwareVersion)) {
                    FirmwaresDigestsProvider.VerifyCodeRecord result = new FirmwaresDigestsProvider.VerifyCodeRecord();
                    result.hashAlg = "sha-256";
                    JsonArray jsHashes = jsVersion.get(result.hashAlg).getAsJsonArray();
                    result.challenge = Util.hexToBytes(jsVersion.get("challenge").getAsString());
                    int caseIndex = (Util.byteArrayToInt(Util.generateRandomBytes(4)) & 0xFFFFFF) % jsHashes.size();
                    JsonObject jsRecord = jsHashes.get(caseIndex).getAsJsonObject();
                    result.blockIndex = jsRecord.get("block").getAsInt();
                    result.blockCount = jsRecord.get("count").getAsInt();
                    result.digest = Util.hexToBytes(jsRecord.get("digest").getAsString());
                    return result;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
