package com.tangem.domain.models.wallet

import com.squareup.moshi.JsonClass
import com.tangem.common.extensions.calculateSha256
import com.tangem.utils.extensions.hexToBytes
import com.tangem.utils.extensions.toHexString

/**
 * Represents a unique identifier for a user wallet.
 *
 * @property stringValue the string representation of the wallet ID in hexadecimal format
 * @property value       the byte array representation of the wallet ID, derived from the hexadecimal string
 */
@JsonClass(generateAdapter = true)
@kotlinx.serialization.Serializable
data class UserWalletId(
    val stringValue: String,
) {

    /**
     * The byte array representation of the wallet ID, derived from the hexadecimal string.
     */
    val value: ByteArray = stringValue.hexToBytes()

    /**
     * Secondary constructor to create a [UserWalletId] from a byte array
     *
     * @param value the byte array to be converted into a hexadecimal string
     */
    constructor(value: ByteArray?) : this(stringValue = value?.toHexString() ?: "")

    override fun toString() = "UserWalletId(${stringValue.take(n = 3)}...${stringValue.takeLast(n = 3)})"
}

/** SHA-256 of [UserWalletId.value] as uppercase hex — the user id Amplitude Experiment targets on both platforms. */
val UserWalletId.hashedStringValue: String
    get() = value.calculateSha256().toHexString()