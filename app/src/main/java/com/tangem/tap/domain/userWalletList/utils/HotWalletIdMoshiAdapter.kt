package com.tangem.tap.domain.userWalletList.utils

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.tangem.hot.sdk.model.HotWalletId
import kotlinx.serialization.json.Json

internal object HotWalletIdMoshiAdapter : JsonAdapter<HotWalletId>() {

    private val json = Json { ignoreUnknownKeys = true }

    override fun fromJson(reader: JsonReader): HotWalletId {
        val stored = reader.nextSource().use { it.readUtf8() }
        return json.decodeFromString(HotWalletId.serializer(), stored)
    }

    override fun toJson(writer: JsonWriter, value: HotWalletId?) {
        val stored = json.encodeToString(HotWalletId.serializer(), requireNotNull(value))
        writer.valueSink().use { it.writeUtf8(stored) }
    }
}