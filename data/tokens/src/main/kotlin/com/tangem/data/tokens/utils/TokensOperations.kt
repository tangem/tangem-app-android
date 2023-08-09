package com.tangem.data.tokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.IconsUtil
import com.tangem.domain.common.TapWorkarounds.derivationStyle
import com.tangem.domain.common.extensions.toCoinId
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.tokens.models.CryptoCurrency.ID
import com.tangem.domain.tokens.models.Network
import com.tangem.blockchain.common.Token as SdkToken
import com.tangem.domain.tokens.models.CryptoCurrency.ID.Prefix.COIN_PREFIX as COIN_ID_PREFIX
import com.tangem.domain.tokens.models.CryptoCurrency.ID.Prefix.CUSTOM_TOKEN_PREFIX as CUSTOM_TOKEN_ID_PREFIX
import com.tangem.domain.tokens.models.CryptoCurrency.ID.Prefix.TOKEN_PREFIX as TOKEN_ID_PREFIX
import com.tangem.domain.tokens.models.CryptoCurrency.ID.Suffix.ContractAddress as CustomCurrencyIdSuffix
import com.tangem.domain.tokens.models.CryptoCurrency.ID.Suffix.RawID as CurrencyIdSuffix

private const val DEFAULT_TOKENS_ICONS_HOST = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins"
private const val TOKEN_ICON_SIZE = "large"
private const val TOKEN_ICON_EXT = "png"

internal fun isCustomToken(tokenId: ID): Boolean {
    return tokenId.rawCurrencyId == null
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

internal fun getCoinId(blockchain: Blockchain): ID {
    return getTokenOrCoinId(blockchain, token = null)
}

internal fun getTokenId(blockchain: Blockchain, token: SdkToken): ID {
    return getTokenOrCoinId(blockchain, token)
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

private fun getTokenOrCoinId(blockchain: Blockchain, token: SdkToken?): ID {
    val sdkTokenId = token?.id
    val (prefix, suffix) = when {
        token == null -> COIN_ID_PREFIX to CurrencyIdSuffix(rawId = blockchain.toCoinId())
        sdkTokenId == null -> CUSTOM_TOKEN_ID_PREFIX to CustomCurrencyIdSuffix(contractAddress = token.contractAddress)
        else -> TOKEN_ID_PREFIX to CurrencyIdSuffix(rawId = sdkTokenId)
    }

    return ID(prefix, getNetworkId(blockchain), suffix)
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
