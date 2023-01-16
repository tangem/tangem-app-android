package com.tangem.tap.features.tokens.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.blockchain.common.Token
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.extensions.canHandleToken
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.tap.domain.tokens.Currency
import com.tangem.tap.features.wallet.redux.WalletData
import org.rekotlin.StateType
import com.tangem.tap.features.wallet.models.Currency.Token as CurrencyToken

data class TokensState(
    val addedWallets: List<WalletData> = emptyList(),
    val addedTokens: List<TokenWithBlockchain> = emptyList(),
    val addedBlockchains: List<Blockchain> = emptyList(),
    val currencies: List<Currency> = emptyList(),
    val searchInput: String? = null,
    val allowToAdd: Boolean = true,
    val derivationStyle: DerivationStyle? = null,
    val scanResponse: ScanResponse? = null,
    val needToLoadMore: Boolean = true,
    val pageToLoad: Int = 0,
    val loadCoinsState: LoadCoinsState = LoadCoinsState.LOADING,
) : StateType {

    fun canHandleToken(token: TokenWithBlockchain): Boolean {
        return scanResponse?.card?.canHandleToken(token.blockchain) ?: false
    }
}

typealias ContractAddress = String

fun List<WalletData>.toTokensContractAddresses(): List<ContractAddress> {
    return mapNotNull { (it.currency as? CurrencyToken)?.token?.contractAddress }.distinct()
}

fun List<WalletData>.toNonCustomTokens(derivationStyle: DerivationStyle?): List<Token> {
    return filter { !it.currency.isCustomCurrency(derivationStyle) }
        .mapNotNull { (it.currency as? CurrencyToken)?.token }
        .distinct()
}

fun List<WalletData>.toNonCustomTokensWithBlockchains(derivationStyle: DerivationStyle?): List<TokenWithBlockchain> {
    return mapNotNull {
        if (it.currency !is CurrencyToken) return@mapNotNull null
        if (it.currency.isCustomCurrency(derivationStyle)) return@mapNotNull null
        TokenWithBlockchain(it.currency.token, it.currency.blockchain)
    }.distinct()
}

fun List<WalletData>.toNonCustomBlockchains(derivationStyle: DerivationStyle?): List<Blockchain> {
    return mapNotNull {
        if (it.currency.isCustomCurrency(derivationStyle)) {
            null
        } else {
            (it.currency as? com.tangem.tap.features.wallet.models.Currency.Blockchain)?.blockchain
        }
    }.distinct()
}

data class TokenWithBlockchain(
    val token: Token,
    val blockchain: Blockchain,
)

fun List<Currency>.filter(supportedBlockchains: Set<Blockchain>?): List<Currency> {
    if (supportedBlockchains == null) return this
    return map {
        it.copy(
            contracts =
            it.contracts.filter {
                supportedBlockchains.contains(it.blockchain) &&
                    (it.blockchain.canHandleTokens() || it.address == null)
            },
        )
    }.filterNot {
        it.contracts.isNullOrEmpty() && !supportedBlockchains.contains(Blockchain.fromNetworkId(it.id))
    }
}

enum class LoadCoinsState {
    LOADING, LOADED, ERROR
}
