package com.tangem.tap.features.tokens.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.blockchain.common.Token
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.tap.domain.tasks.product.ScanResponse
import com.tangem.tap.domain.tokens.Currency
import com.tangem.tap.features.wallet.redux.WalletData
import org.rekotlin.StateType

data class TokensState(
    val addedWallets: List<WalletData> = emptyList(),
    val addedTokens: List<TokenWithBlockchain> = emptyList(),
    val addedBlockchains: List<Blockchain> = emptyList(),
    val nonRemovableTokens: List<ContractAddress> = emptyList(),
    val nonRemovableBlockchains: List<Blockchain> = emptyList(),
    val currencies: List<Currency> = emptyList(),
    val allowToAdd: Boolean = true,
    val derivationStyle: DerivationStyle? = null,
    val scanResponse: ScanResponse? = null
) : StateType

typealias ContractAddress = String

fun List<WalletData>.toTokensContractAddresses(): List<ContractAddress> {
    return mapNotNull { (it.currency as? com.tangem.tap.features.wallet.redux.Currency.Token)?.token?.contractAddress }.distinct()
}

fun List<WalletData>.toTokens(): List<Token> {
    return mapNotNull { (it.currency as? com.tangem.tap.features.wallet.redux.Currency.Token)?.token }.distinct()
}

fun List<WalletData>.toTokensWithBlockchains(derivationStyle: DerivationStyle?): List<TokenWithBlockchain> {
    return mapNotNull {
        if (it.currency !is com.tangem.tap.features.wallet.redux.Currency.Token) return@mapNotNull null
        if (it.currency.isCustomCurrency(derivationStyle)) return@mapNotNull null
        TokenWithBlockchain(it.currency.token, it.currency.blockchain)
    }.distinct()
}

fun List<WalletData>.toBlockchains(derivationStyle: DerivationStyle?): List<Blockchain> {
    return mapNotNull {
        if (it.currency.isCustomCurrency(derivationStyle)) {
            null
        } else {
            (it.currency as? com.tangem.tap.features.wallet.redux.Currency.Blockchain)?.blockchain
        }
    }.distinct()
}

data class TokenWithBlockchain(
    val token: Token,
    val blockchain: Blockchain
)

fun List<Currency>.filter(supportedBlockchains: Set<Blockchain>?): List<Currency> {
    if (supportedBlockchains == null) return this
    return map {
        it.copy(contracts =
        it.contracts?.filter {
            supportedBlockchains.contains(it.blockchain) && it.blockchain.canHandleTokens()
        }
        )
    }.filterNot {
        it.contracts.isNullOrEmpty() && !supportedBlockchains.contains(Blockchain.fromNetworkId(it.id))
    }
}