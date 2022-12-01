package com.tangem.tap.domain.userWalletList.utils.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import com.tangem.domain.common.CardDTO

internal class CardBackupStatusAdapter {
    @ToJson
    fun toJson(
        writer: JsonWriter,
        src: CardDTO.BackupStatus?,
        mapAdapter: JsonAdapter<Map<String, String>>,
    ) {
        val jsonMap = mutableMapOf<String, String>()

        when (src) {
            is CardDTO.BackupStatus.Active -> {
                jsonMap["status"] = "active"
                jsonMap["cardCount"] = src.cardCount.toString()
            }
            is CardDTO.BackupStatus.CardLinked -> {
                jsonMap["status"] = "card_linked"
                jsonMap["cardCount"] = src.cardCount.toString()
            }
            is CardDTO.BackupStatus.NoBackup -> {
                jsonMap["status"] = "no_backup"
            }
            null -> {
                jsonMap["status"] = "null"
            }
        }

        mapAdapter.toJson(writer, jsonMap)
    }

    @FromJson
    fun fromJson(
        reader: JsonReader,
        mapAdapter: JsonAdapter<Map<String, String>>,
    ): CardDTO.BackupStatus? {
        val map = mapAdapter.fromJson(reader) ?: return null

        return when (map["status"]) {
            "active" -> CardDTO.BackupStatus.Active(
                cardCount = map["cardCount"]?.toInt() ?: 0,
            )
            "card_linked" -> CardDTO.BackupStatus.CardLinked(
                cardCount = map["cardCount"]?.toInt() ?: 0,
            )
            "no_backup" -> CardDTO.BackupStatus.NoBackup
            else -> null
        }
    }
}
