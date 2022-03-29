package com.tangem.tap.features.tokens.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.tap.domain.tokens.Currency
import com.tangem.tap.features.wallet.redux.WalletData
import org.rekotlin.StateType

data class TokensState(
    val addedWallets: List<WalletData> = emptyList(),
    val addedTokens: List<Token> = emptyList(),
    val addedBlockchains: List<Blockchain> = emptyList(),
    val nonRemovableTokens: List<ContractAddress> = emptyList(),
    val nonRemovableBlockchains: List<Blockchain> = emptyList(),
    val currencies: List<Currency> = emptyList(),
    val allowToAdd: Boolean = true
) : StateType

typealias ContractAddress = String

fun List<WalletData>.toTokensContractAddresses(): List<ContractAddress> {
    return mapNotNull { (it.currency as? com.tangem.tap.features.wallet.redux.Currency.Token)?.token?.contractAddress }.distinct()
}

fun List<WalletData>.toTokens(): List<Token> {
    return mapNotNull { (it.currency as? com.tangem.tap.features.wallet.redux.Currency.Token)?.token }.distinct()
}

fun List<WalletData>.toBlockchains(): List<Blockchain> {
    return mapNotNull { (it.currency as? com.tangem.tap.features.wallet.redux.Currency.Blockchain)?.blockchain }.distinct()
}