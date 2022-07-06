package com.tangem.tap.domain.extensions

import com.tangem.blockchain.common.Token

/**
 * Created by Anton Zhilenkov on 02/02/2022.
 */

private val tokenCustomIconUrls = mutableMapOf<String, String>()

fun Token.getCustomIconUrl(): String? {
    return tokenCustomIconUrls[this.contractAddress]
}

fun Token.setCustomIconUrl(url: String) {
    tokenCustomIconUrls[this.contractAddress] = url
}
