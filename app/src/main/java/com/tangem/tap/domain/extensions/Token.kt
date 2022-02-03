package com.tangem.tap.domain.extensions

import com.tangem.blockchain.common.Token

/**
[REDACTED_AUTHOR]
 */

private val tokenCustomIconUrls = mutableMapOf<String, String>()

fun Token.getCustomIconUrl(): String? {
    return tokenCustomIconUrls[this.contractAddress]
}

fun Token.setCustomIconUrl(url: String) {
    tokenCustomIconUrls[this.contractAddress] = url
}