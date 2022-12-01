package com.tangem.tap.domain.userWalletList.utils.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.operations.derivation.ExtendedPublicKeysMap

internal class ScanResponseDerivedKeysMapAdapter {
    @ToJson
    fun toJson(
        writer: JsonWriter,
        src: Map<ByteArrayKey, ExtendedPublicKeysMap>,
        mapAdapter: JsonAdapter<Map<String, String>>,
        byteArrayKeyAdapter: JsonAdapter<ByteArrayKey>,
        extendedPublicKeysMapAdapter: JsonAdapter<ExtendedPublicKeysMap>,
    ) {
        val jsonMap = mutableMapOf<String, String>()

        src.forEach { (key, extendedPublicKeysMap) ->
            val keyJson = byteArrayKeyAdapter.toJson(key)
            val extendedPublicKeysMapJson = extendedPublicKeysMapAdapter.toJson(extendedPublicKeysMap)

            jsonMap[keyJson] = extendedPublicKeysMapJson
        }

        mapAdapter.toJson(writer, jsonMap)
    }

    @FromJson
    fun fromJson(
        reader: JsonReader,
        mapAdapter: JsonAdapter<Map<String, String>>,
        byteArrayKeyAdapter: JsonAdapter<ByteArrayKey>,
        extendedPublicKeysMapAdapter: JsonAdapter<ExtendedPublicKeysMap>,
    ): Map<ByteArrayKey, ExtendedPublicKeysMap> {
        val map = mutableMapOf<ByteArrayKey, ExtendedPublicKeysMap>()

        mapAdapter.fromJson(reader)?.forEach { (keyJson, extendedPublicKeysMapJson) ->
            val key = byteArrayKeyAdapter.fromJson(keyJson)
            val extendedPublicKeysMap = extendedPublicKeysMapAdapter.fromJson(extendedPublicKeysMapJson)

            if (key != null && extendedPublicKeysMap != null) {
                map[key] = extendedPublicKeysMap
            }
        }

        return map
    }
}
