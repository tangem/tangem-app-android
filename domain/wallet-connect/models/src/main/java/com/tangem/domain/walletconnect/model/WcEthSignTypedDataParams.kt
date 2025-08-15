package com.tangem.domain.walletconnect.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WcEthSignTypedDataParams(
    @Json(name = "domain")
    val domain: Domain?,
    @Json(name = "message")
    val message: Message?,
    @Json(name = "primaryType")
    val primaryType: String?,
    @Json(name = "types")
    val types: Map<String, List<Types.Type>>,
) {
    @JsonClass(generateAdapter = true)
    data class Domain(
        @Json(name = "chainId")
        val chainId: Int?,
        @Json(name = "name")
        val name: String?,
        @Json(name = "verifyingContract")
        val verifyingContract: String?,
        @Json(name = "version")
        val version: String?,
    )

    @JsonClass(generateAdapter = true)
    data class Message(
        @Json(name = "contents")
        val contents: String?,
        @Json(name = "from")
        val from: Address?,
        @Json(name = "to")
        val to: Address?,
    ) {
        @JsonClass(generateAdapter = true)
        data class Address(
            @Json(name = "name")
            val name: String,
            @Json(name = "wallet")
            val wallet: String,
        )
    }

    @JsonClass(generateAdapter = true)
    data class Types(
        @Json(name = "EIP712Domain")
        val eIP712Domain: List<Type> = listOf(),
        @Json(name = "Mail")
        val mail: List<Type> = listOf(),
        @Json(name = "Person")
        val person: List<Type> = listOf(),
    ) {
        @JsonClass(generateAdapter = true)
        data class Type(
            @Json(name = "name")
            val name: String,
            @Json(name = "type")
            val type: String,
        )
    }
}