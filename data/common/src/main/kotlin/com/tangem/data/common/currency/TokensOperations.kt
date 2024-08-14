package com.tangem.data.common.currency

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.IconsUtil
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrency.ID
import com.tangem.domain.tokens.model.Network
import com.tangem.blockchain.common.Token as SdkToken
import com.tangem.domain.tokens.model.CryptoCurrency.ID.Body as CurrencyIdBody
import com.tangem.domain.tokens.model.CryptoCurrency.ID.Prefix.COIN_PREFIX as COIN_ID_PREFIX
import com.tangem.domain.tokens.model.CryptoCurrency.ID.Prefix.TOKEN_PREFIX as TOKEN_ID_PREFIX
import com.tangem.domain.tokens.model.CryptoCurrency.ID.Suffix.ContractAddress as CustomCurrencyIdSuffix
import com.tangem.domain.tokens.model.CryptoCurrency.ID.Suffix.RawID as CurrencyIdSuffix

private const val DEFAULT_TOKENS_ICONS_HOST = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins"
private const val TOKEN_ICON_SIZE = "large"
private const val TOKEN_ICON_EXT = "png"

fun isCustomToken(tokenId: ID, network: Network): Boolean {
    return network.derivationPath is Network.DerivationPath.Custom || tokenId.rawCurrencyId == null
}

fun isCustomCoin(network: Network): Boolean {
    return network.derivationPath is Network.DerivationPath.Custom
}

fun getCoinId(network: Network, coinId: String): ID {
    return ID(COIN_ID_PREFIX, getCurrencyIdBody(network), CurrencyIdSuffix(rawId = coinId))
}

fun getTokenId(network: Network, sdkToken: SdkToken): ID {
    val sdkTokenId = sdkToken.id

    return getTokenId(network, sdkTokenId, sdkToken.contractAddress)
}

fun getTokenId(network: Network, rawTokenId: String?, contractAddress: String): ID {
    val suffix = if (rawTokenId == null) {
        CustomCurrencyIdSuffix(contractAddress)
    } else {
        CurrencyIdSuffix(rawTokenId, contractAddress)
    }

    return ID(TOKEN_ID_PREFIX, getCurrencyIdBody(network), suffix)
}

fun getTokenIconUrl(blockchain: Blockchain, token: SdkToken): String? {
    val tokenId = token.id

    return if (tokenId == null) {
        IconsUtil.getTokenIconUri(blockchain, token)?.toString()
    } else {
        getTokenIconUrlFromDefaultHost(tokenId)
    }
}

fun getCoinIconUrl(blockchain: Blockchain): String? {
    val coinId = when (blockchain) {
        Blockchain.Unknown -> null
        else -> blockchain.toCoinId()
    }

    return coinId?.let(::getTokenIconUrlFromDefaultHost)
}

fun List<UserTokensResponse.Token>.hasCoinForToken(token: CryptoCurrency.Token): Boolean {
    return any {
        val blockchain = getBlockchain(networkId = token.network.id)
        val tokenDerivation = token.network.derivationPath.value
        it.id == blockchain.toCoinId() && it.derivationPath == tokenDerivation
    }
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