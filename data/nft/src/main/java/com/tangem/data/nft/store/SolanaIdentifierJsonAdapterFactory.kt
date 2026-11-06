package com.tangem.data.nft.store

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.tangem.blockchain.nft.models.NFTAsset
import java.lang.reflect.Type

/**
 * Normalizes [NFTAsset.Identifier.Solana.tokenStandard] on the Moshi cache path the same way
 * [normalizeLegacySolanaTokenStandard] does on the kotlinx one: a legacy numeric value (Moshi's generated adapter
 * reads it as e.g. `"4"`) maps to its name. Delegates to the generated adapter for everything else, so the rest of
 * the shape stays exactly what Moshi codegen produces.
 */
internal object SolanaIdentifierJsonAdapterFactory : JsonAdapter.Factory {

    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (type != NFTAsset.Identifier.Solana::class.java) return null

        val delegate = moshi.nextAdapter<NFTAsset.Identifier.Solana>(this, type, annotations)
        return object : JsonAdapter<NFTAsset.Identifier.Solana>() {

            override fun fromJson(reader: JsonReader): NFTAsset.Identifier.Solana {
                val value = requireNotNull(delegate.fromJson(reader)) { "Solana NFT identifier can't be null" }
                return value.copy(tokenStandard = normalizeLegacySolanaTokenStandard(value.tokenStandard))
            }

            override fun toJson(writer: JsonWriter, value: NFTAsset.Identifier.Solana?) {
                delegate.toJson(writer, value)
            }
        }
    }
}