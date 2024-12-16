package com.tangem.domain.models.scan.serialization

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import com.tangem.common.extensions.guard
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.crypto.hdWallet.DerivationNode
import com.tangem.crypto.hdWallet.DerivationPath

class DerivationPathAdapterWithMigration {
    @ToJson
    fun toJson(src: DerivationPath): String = src.rawPath

    @FromJson
    fun fromJson(json: String): DerivationPath {
        val jsonMap = MoshiJsonConverter.default().toMap(json)
        return if (jsonMap.isEmpty()) {
            DerivationPath(json)
        } else {
            fromLegacyScheme(jsonMap)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun fromLegacyScheme(jsonMap: Map<String, Any>): DerivationPath {
        val rawPath = jsonMap["rawPath"] as String
        val nodeIndexes = (jsonMap["nodes"] as? List<Number>).guard {
            return DerivationPath(rawPath)
        }
        val nodes = nodeIndexes.map { DerivationNode.fromIndex(it.toLong()) }
        return DerivationPath(rawPath, nodes)
    }
}