package com.tangem.data.tokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.IconsUtil
import com.tangem.domain.common.TapWorkarounds.derivationStyle
import com.tangem.domain.common.extensions.toCoinId
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.models.Network
import com.tangem.blockchain.common.Token as SdkToken

private const val DEFAULT_TOKENS_ICONS_HOST = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins"
private const val TOKEN_ICON_SIZE = "large"
private const val TOKEN_ICON_EXT = "png"

private const val COIN_ID_PREFIX = "coin_"
private const val TOKEN_ID_PREFIX = "token_"
private const val CUSTOM_TOKEN_ID_PREFIX = "custom_token_"
private const val TOKEN_ID_DELIMITER = '#'

internal fun isCustomToken(tokenId: CryptoCurrency.ID): Boolean {
    return tokenId.value.startsWith(CUSTOM_TOKEN_ID_PREFIX)
}

internal fun getDerivationPath(blockchain: Blockchain, card: CardDTO): String? {
    return if (card.settings.isHDWalletAllowed) {
        blockchain.derivationPath(card.derivationStyle)?.rawPath
    } else {
        null
    }
}

internal fun getBlockchain(networkId: Network.ID): Blockchain {
    return Blockchain.fromId(networkId.value)
}

internal fun getNetworkId(blockchain: Blockchain): Network.ID {
    val value = blockchain.id

    return Network.ID(value)
}

internal fun getCoinId(blockchain: Blockchain): CryptoCurrency.ID {
    return getTokenOrCoinId(blockchain, token = null)
}

internal fun getTokenId(blockchain: Blockchain, token: SdkToken): CryptoCurrency.ID {
    return getTokenOrCoinId(blockchain, token)
}

internal fun getTokenIdString(currencyId: CryptoCurrency.ID): String? {
    val idValue = currencyId.value

    return if (idValue.startsWith(CUSTOM_TOKEN_ID_PREFIX)) {
        null
    } else {
        idValue.substringAfter(TOKEN_ID_DELIMITER)
    }
}

internal fun getTokenIconUrl(blockchain: Blockchain, token: SdkToken): String? {
    val tokenId = token.id

    return if (tokenId == null) {
        IconsUtil.getTokenIconUri(blockchain, token)?.toString()
    } else {
        getTokenIconUrlFromDefaultHost(tokenId)
    }
}

internal fun getCoinIconUrl(blockchain: Blockchain): String? {
    val coinId = when (blockchain) {
        Blockchain.Unknown -> null
        Blockchain.TerraV1, Blockchain.TerraV2 -> blockchain.toCoinId()
        else -> blockchain.toNetworkId()
    }

    return coinId?.let(::getTokenIconUrlFromDefaultHost)
}

private fun getTokenOrCoinId(blockchain: Blockchain, token: SdkToken?): CryptoCurrency.ID {
    val sdkTokenId = token?.id
    val (prefix, suffix) = when {
        token == null -> COIN_ID_PREFIX to blockchain.toCoinId()
        sdkTokenId == null -> CUSTOM_TOKEN_ID_PREFIX to token.contractAddress
        else -> TOKEN_ID_PREFIX to sdkTokenId
    }

    val value = buildString {
        append(prefix)
        append(blockchain.id)
        append(TOKEN_ID_DELIMITER)
        append(suffix.lowercase())
    }

    return CryptoCurrency.ID(value)
}

private fun getTokenIconUrlFromDefaultHost(tokenId: String): String {
    return buildString {
        append(DEFAULT_TOKENS_ICONS_HOST)
        append('/')
        append(TOKEN_ICON_SIZE)
        append('/')
        append(tokenId)
        append('.')
        append(TOKEN_ICON_EXT)
    }
}
