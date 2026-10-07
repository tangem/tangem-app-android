package com.tangem.data.nft.store

import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTCollection
import com.tangem.domain.models.serialization.SerializedBigDecimal
import com.tangem.domain.models.serialization.SerializedBigInteger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/*
 * kotlinx serializers for the Blockchain SDK NFT models, which can't be annotated. They keep the JSON the Moshi
 * adapters stored: the same field names, `null`s left out, big numbers as strings and identifiers tagged by a leading
 * `"bc"` label. A field added to the SDK models is not stored until it is mirrored here.
 */

/** Stores [NFTCollection] through [StoredNFTCollection]. */
internal object NFTCollectionSerializer : KSerializer<NFTCollection> {

    override val descriptor: SerialDescriptor = StoredNFTCollection.serializer().descriptor

    override fun serialize(encoder: Encoder, value: NFTCollection) {
        encoder.encodeSerializableValue(StoredNFTCollection.serializer(), StoredNFTCollection.from(value))
    }

    override fun deserialize(decoder: Decoder): NFTCollection {
        return decoder.decodeSerializableValue(StoredNFTCollection.serializer()).toSdk()
    }
}

/** Stores [NFTAsset.SalePrice] through [StoredSalePrice]. */
internal object NFTSalePriceSerializer : KSerializer<NFTAsset.SalePrice> {

    override val descriptor: SerialDescriptor = StoredSalePrice.serializer().descriptor

    override fun serialize(encoder: Encoder, value: NFTAsset.SalePrice) {
        encoder.encodeSerializableValue(StoredSalePrice.serializer(), StoredSalePrice.from(value))
    }

    override fun deserialize(decoder: Decoder): NFTAsset.SalePrice {
        return decoder.decodeSerializableValue(StoredSalePrice.serializer()).toSdk()
    }
}

/**
 * Stores [NFTCollection.Identifier] tagged by its `"bc"` label. An unknown label reads as
 * [NFTCollection.Identifier.Unknown], which itself can't be written, as with Moshi.
 */
internal object NFTCollectionIdentifierSerializer : LabeledSerializer<NFTCollection.Identifier>(
    serialName = "NFTCollection.Identifier",
) {

    override fun decode(label: String, json: JsonObject): NFTCollection.Identifier = when (label) {
        EVM_LABEL -> NFTCollection.Identifier.EVM(tokenAddress = json.requireString("tokenAddress"))
        TON_LABEL -> NFTCollection.Identifier.TON(contractAddress = json.optString("contractAddress"))
        SOLANA_LABEL -> NFTCollection.Identifier.Solana(collectionAddress = json.optString("collectionAddress"))
        else -> NFTCollection.Identifier.Unknown
    }

    override fun encode(value: NFTCollection.Identifier): JsonObject = when (value) {
        is NFTCollection.Identifier.EVM -> labeled(EVM_LABEL) { put("tokenAddress", value.tokenAddress) }
        is NFTCollection.Identifier.TON -> labeled(TON_LABEL) {
            value.contractAddress?.let { put("contractAddress", it) }
        }
        is NFTCollection.Identifier.Solana -> labeled(SOLANA_LABEL) {
            value.collectionAddress?.let { put("collectionAddress", it) }
        }
        NFTCollection.Identifier.Unknown -> throw SerializationException("Unknown NFT collection can't be stored")
    }
}

/**
 * Stores [NFTAsset.Identifier] tagged by its `"bc"` label. An unknown label reads as [NFTAsset.Identifier.Unknown],
 * which itself can't be written, as with Moshi.
 */
internal object NFTAssetIdentifierSerializer : LabeledSerializer<NFTAsset.Identifier>(
    serialName = "NFTAsset.Identifier",
) {

    override fun decode(label: String, json: JsonObject): NFTAsset.Identifier = when (label) {
        EVM_LABEL -> NFTAsset.Identifier.EVM(
            tokenId = json.requireString("tokenId").toBigInteger(),
            tokenAddress = json.requireString("tokenAddress"),
            contractType = NFTAsset.Identifier.EVM.ContractType.valueOf(json.requireString("contractType")),
        )
        TON_LABEL -> NFTAsset.Identifier.TON(tokenAddress = json.requireString("tokenAddress"))
        SOLANA_LABEL -> NFTAsset.Identifier.Solana(
            tokenAddress = json.requireString("tokenAddress"),
            tokenStandard = json.optString("tokenStandard")?.toInt(),
        )
        else -> NFTAsset.Identifier.Unknown
    }

    override fun encode(value: NFTAsset.Identifier): JsonObject = when (value) {
        is NFTAsset.Identifier.EVM -> labeled(EVM_LABEL) {
            put("tokenId", value.tokenId.toString())
            put("tokenAddress", value.tokenAddress)
            put("contractType", value.contractType.name)
        }
        is NFTAsset.Identifier.TON -> labeled(TON_LABEL) { put("tokenAddress", value.tokenAddress) }
        is NFTAsset.Identifier.Solana -> labeled(SOLANA_LABEL) {
            put("tokenAddress", value.tokenAddress)
            value.tokenStandard?.let { put("tokenStandard", it) }
        }
        NFTAsset.Identifier.Unknown -> throw SerializationException("Unknown NFT asset can't be stored")
    }
}

/** Writes a JSON object whose first field is the `"bc"` label, the layout of Moshi's polymorphic adapter. */
internal abstract class LabeledSerializer<T : Any>(serialName: String) : KSerializer<T> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(serialName)

    protected abstract fun decode(label: String, json: JsonObject): T

    protected abstract fun encode(value: T): JsonObject

    override fun deserialize(decoder: Decoder): T {
        val json = (decoder as JsonDecoder).decodeJsonElement().jsonObject
        val label = json[LABEL_KEY]?.jsonPrimitive?.contentOrNull
            ?: throw SerializationException("Missing \"$LABEL_KEY\" label")

        return decode(label = label, json = json)
    }

    override fun serialize(encoder: Encoder, value: T) {
        (encoder as JsonEncoder).encodeJsonElement(encode(value))
    }

    protected fun labeled(label: String, fields: JsonObjectBuilder.() -> Unit): JsonObject = buildJsonObject {
        put(LABEL_KEY, label)
        fields()
    }

    protected fun JsonObject.requireString(key: String): String {
        return optString(key) ?: throw SerializationException("Missing \"$key\"")
    }

    protected fun JsonObject.optString(key: String): String? {
        return this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull
    }

    protected companion object {
        const val LABEL_KEY = "bc"
        const val EVM_LABEL = "evm"
        const val TON_LABEL = "ton"
        const val SOLANA_LABEL = "sol"
    }
}

@Serializable
private data class StoredNFTCollection(
    @Serializable(with = NFTCollectionIdentifierSerializer::class) val identifier: NFTCollection.Identifier,
    val blockchainId: String,
    val name: String?,
    val description: String?,
    val logoUrl: String?,
    val count: Int,
    val assets: List<StoredNFTAsset> = emptyList(),
) {

    fun toSdk(): NFTCollection = NFTCollection(
        identifier = identifier,
        blockchainId = blockchainId,
        name = name,
        description = description,
        logoUrl = logoUrl,
        count = count,
        assets = assets.map(StoredNFTAsset::toSdk),
    )

    companion object {
        fun from(value: NFTCollection) = StoredNFTCollection(
            identifier = value.identifier,
            blockchainId = value.blockchainId,
            name = value.name,
            description = value.description,
            logoUrl = value.logoUrl,
            count = value.count,
            assets = value.assets.map(StoredNFTAsset::from),
        )
    }
}

@Serializable
private data class StoredNFTAsset(
    @Serializable(with = NFTAssetIdentifierSerializer::class) val identifier: NFTAsset.Identifier,
    @Serializable(with = NFTCollectionIdentifierSerializer::class) val collectionIdentifier: NFTCollection.Identifier,
    val blockchainId: String,
    val contractType: String,
    val owner: String?,
    val name: String?,
    val description: String?,
    val amount: SerializedBigInteger?,
    val decimals: Int?,
    val salePrice: StoredSalePrice?,
    val rarity: StoredRarity?,
    val media: StoredMedia?,
    val traits: List<StoredTrait>,
) {

    fun toSdk(): NFTAsset = NFTAsset(
        identifier = identifier,
        collectionIdentifier = collectionIdentifier,
        blockchainId = blockchainId,
        contractType = contractType,
        owner = owner,
        name = name,
        description = description,
        amount = amount,
        decimals = decimals,
        salePrice = salePrice?.toSdk(),
        rarity = rarity?.let { NFTAsset.Rarity(rank = it.rank, label = it.label) },
        media = media?.let { NFTAsset.Media(animationUrl = it.animationUrl, imageUrl = it.imageUrl) },
        traits = traits.map { NFTAsset.Trait(name = it.name, value = it.value) },
    )

    companion object {
        fun from(value: NFTAsset) = StoredNFTAsset(
            identifier = value.identifier,
            collectionIdentifier = value.collectionIdentifier,
            blockchainId = value.blockchainId,
            contractType = value.contractType,
            owner = value.owner,
            name = value.name,
            description = value.description,
            amount = value.amount,
            decimals = value.decimals,
            salePrice = value.salePrice?.let(StoredSalePrice::from),
            rarity = value.rarity?.let { StoredRarity(rank = it.rank, label = it.label) },
            media = value.media?.let { StoredMedia(animationUrl = it.animationUrl, imageUrl = it.imageUrl) },
            traits = value.traits.map { StoredTrait(name = it.name, value = it.value) },
        )
    }
}

@Serializable
private data class StoredSalePrice(val value: SerializedBigDecimal, val symbol: String?, val decimals: Int?) {

    fun toSdk() = NFTAsset.SalePrice(value = value, symbol = symbol, decimals = decimals)

    companion object {
        fun from(value: NFTAsset.SalePrice) = StoredSalePrice(
            value = value.value,
            symbol = value.symbol,
            decimals = value.decimals,
        )
    }
}

@Serializable
private data class StoredRarity(val rank: String, val label: String)

@Serializable
private data class StoredMedia(val animationUrl: String?, val imageUrl: String?)

@Serializable
private data class StoredTrait(val name: String, val value: String)