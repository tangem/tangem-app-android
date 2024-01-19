package com.tangem.tap.domain.walletconnect

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

object EthSignHelper {
    private val gson: Gson by lazy {
        GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create()
    }

    fun tryToParseEthTypedMessageString(message: String): String? {
        return try {
            val messageString = message
                .replace("\\", "")
                .removePrefix("\"")
                .removeSuffix("\"")
            val messageJson = JsonParser().parse(messageString)
            val filteredMap = gson.fromJson<Map<*, *>>(messageJson)
                .filterKeys { it == "domain" || it == "message" }

            gson.toJson(filteredMap)
        } catch (exception: Exception) {
            null
        }
    }
}