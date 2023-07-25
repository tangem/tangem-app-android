package com.tangem.data.tokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.blockchain.common.IconsUtil
import com.tangem.domain.common.TapWorkarounds.derivationStyle
import com.tangem.domain.common.extensions.toCoinId
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.tokens.model.Token
import com.tangem.blockchain.common.Token as SdkToken

private const val DEFAULT_TOKENS_ICONS_HOST = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins"
private const val TOKEN_ICON_SIZE = "large"
private const val TOKEN_ICON_EXT = "png"

internal fun isCustomToken(
    tokenId: String?,
    tokenDerivationPath: String?,
    cardDerivationStyle: DerivationStyle?,
    blockchain: Blockchain,
): Boolean {
    if (tokenId == null) return true

    if (tokenDerivationPath == null || cardDerivationStyle == null) return false

    return tokenDerivationPath != blockchain.derivationPath(cardDerivationStyle)?.rawPath
}

internal fun getTokenId(token: SdkToken?, blockchain: Blockchain): Token.ID {
    val tokenId = token?.id

    return when {
        token == null -> getCoinId(blockchain)
        tokenId == null -> getCustomTokenId(token.contractAddress, blockchain)
        else -> Token.ID(tokenId)
    }
}

internal fun getCoinId(blockchain: Blockchain): Token.ID {
    val value = blockchain.toCoinId()

    return Token.ID(value)
}

internal fun getCoinOrTokenIconUrl(token: SdkToken?, blockchain: Blockchain): String? {
    val tokenId = token?.id

    return when {
        token == null -> getCoinIconId(blockchain)
        tokenId == null -> IconsUtil.getTokenIconUri(blockchain, token)?.toString()
        else -> getTokenIconUrlFromDefaultHost(tokenId)
    }
}

internal fun getCoinIconId(blockchain: Blockchain): String {
    return getTokenIconUrlFromDefaultHost(blockchain.toCoinId())
}

internal fun getDerivationPath(blockchain: Blockchain, card: CardDTO): String? {
    return if (card.settings.isHDWalletAllowed) {
        blockchain.derivationPath(card.derivationStyle)?.rawPath
    } else {
        null
    }
}

private fun getCustomTokenId(contractAddress: String, blockchain: Blockchain): Token.ID {
    val value = "custom_${blockchain.id}_$contractAddress"

    return Token.ID(value)
}

private fun getTokenIconUrlFromDefaultHost(tokenId: String): String {
    return "$DEFAULT_TOKENS_ICONS_HOST/$TOKEN_ICON_SIZE/$tokenId.$TOKEN_ICON_EXT"
}