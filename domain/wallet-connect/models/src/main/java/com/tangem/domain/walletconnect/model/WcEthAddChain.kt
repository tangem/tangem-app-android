package com.tangem.domain.walletconnect.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WcEthAddChain(
    /**
     * chainId are identified by EIP-155 integers expressed in hexadecimal notation,
     * with 0x prefix and no leading zeroes for the chainId value.
     * For more information https://eips.ethereum.org/EIPS/eip-5792#atomicbatch-capability
     */
    @Json(name = "chainId")
    val chainId: String,
)