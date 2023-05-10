package com.tangem.tap.domain.userWalletList.utils.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import com.tangem.common.extensions.guard
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.crypto.hdWallet.DerivationNode
import com.tangem.crypto.hdWallet.DerivationPath
import timber.log.Timber

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
            Timber.e("Unable to convert derivation path nodes from JSON")
            return DerivationPath(rawPath)
        }
        val nodes = nodeIndexes.map { DerivationNode.fromIndex(it.toLong()) }
        return DerivationPath(rawPath, nodes)
    }
}