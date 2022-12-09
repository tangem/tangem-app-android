package com.tangem.tap.domain.userWalletList.utils.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.common.hdWallet.ExtendedPublicKey

internal class WalletDerivedKeysMapAdapter {
    @ToJson
    fun toJson(
        writer: JsonWriter,
        src: Map<DerivationPath, ExtendedPublicKey>,
        mapAdapter: JsonAdapter<Map<String, String>>,
        derivationPathAdapter: JsonAdapter<DerivationPath>,
        extendedPublicKeyAdapter: JsonAdapter<ExtendedPublicKey>,
    ) {
        val jsonMap = mutableMapOf<String, String>()

        src.forEach { (derivationPath, extendedPublicKey) ->
            val derivationPathJson = derivationPathAdapter.toJson(derivationPath)
            val derivationPathEncoded = derivationPathJson.encodeToByteArray().toHexString()
            val extendedPublicKeyJson = extendedPublicKeyAdapter.toJson(extendedPublicKey)

            jsonMap[derivationPathEncoded] = extendedPublicKeyJson
        }

        mapAdapter.toJson(writer, jsonMap)
    }

    @FromJson
    fun fromJson(
        reader: JsonReader,
        mapAdapter: JsonAdapter<Map<String, String>>,
        derivationPathAdapter: JsonAdapter<DerivationPath>,
        extendedPublicKeyAdapter: JsonAdapter<ExtendedPublicKey>,
    ): Map<DerivationPath, ExtendedPublicKey> {
        val map = mutableMapOf<DerivationPath, ExtendedPublicKey>()

        mapAdapter.fromJson(reader)?.forEach { (derivationPathEncoded, extendedPublicKeyJson) ->
            val derivationPathJson = derivationPathEncoded.hexToBytes().decodeToString()
            val derivationPath = derivationPathAdapter.fromJson(derivationPathJson)
            val extendedPublicKey = extendedPublicKeyAdapter.fromJson(extendedPublicKeyJson)

            if (derivationPath != null && extendedPublicKey != null) {
                map[derivationPath] = extendedPublicKey
            }
        }

        return map
    }
}
