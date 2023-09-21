package com.tangem.data.tokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.IconsUtil
import com.tangem.domain.common.extensions.toCoinId
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.tokens.models.CryptoCurrency.ID
import com.tangem.domain.tokens.models.Network
import com.tangem.blockchain.common.Token as SdkToken
import com.tangem.domain.tokens.models.CryptoCurrency.ID.Body as CurrencyIdBody
import com.tangem.domain.tokens.models.CryptoCurrency.ID.Prefix.COIN_PREFIX as COIN_ID_PREFIX
import com.tangem.domain.tokens.models.CryptoCurrency.ID.Prefix.TOKEN_PREFIX as TOKEN_ID_PREFIX
import com.tangem.domain.tokens.models.CryptoCurrency.ID.Suffix.ContractAddress as CustomCurrencyIdSuffix
import com.tangem.domain.tokens.models.CryptoCurrency.ID.Suffix.RawID as CurrencyIdSuffix

private const val DEFAULT_TOKENS_ICONS_HOST = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins"
private const val TOKEN_ICON_SIZE = "large"
private const val TOKEN_ICON_EXT = "png"

internal fun isCustomToken(tokenId: ID, network: Network): Boolean {
    return network.derivationPath is Network.DerivationPath.Custom || tokenId.rawCurrencyId == null
}

internal fun isCustomCoin(network: Network): Boolean {
    return network.derivationPath is Network.DerivationPath.Custom
}

internal fun getCoinId(network: Network, coinId: String): ID {
    return ID(COIN_ID_PREFIX, getCurrencyIdBody(network), CurrencyIdSuffix(rawId = coinId))
}

internal fun getTokenId(network: Network, sdkToken: SdkToken): ID {
    val sdkTokenId = sdkToken.id
    val suffix = if (sdkTokenId == null) {
        CustomCurrencyIdSuffix(contractAddress = sdkToken.contractAddress)
    } else {
        CurrencyIdSuffix(rawId = sdkTokenId)
    }

    return ID(TOKEN_ID_PREFIX, getCurrencyIdBody(network), suffix)
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

private fun getCurrencyIdBody(network: Network): CurrencyIdBody {
    return when (val path = network.derivationPath) {
        is Network.DerivationPath.Custom -> CurrencyIdBody.NetworkIdWithDerivationPath(
            rawId = network.id.value,
            derivationPath = path.value,
        )
        is Network.DerivationPath.Card,
        is Network.DerivationPath.None,
        -> CurrencyIdBody.NetworkId(network.id.value)
    }
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