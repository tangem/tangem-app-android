package com.tangem.domain.walletmanager.utils

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.WalletManager
import com.tangem.domain.common.extensions.amountToCreateAccount
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.walletmanager.model.CryptoCurrencyAmount
import com.tangem.domain.walletmanager.model.UpdateWalletManagerResult
import timber.log.Timber
import java.math.BigDecimal

internal class UpdateWalletManagerResultFactory {

    fun getResult(walletManager: WalletManager): UpdateWalletManagerResult.Verified {
        val hasNotConfirmedTransactions = walletManager.wallet
            .recentTransactions
            .any { it.status != TransactionStatus.Confirmed }

        val amounts = walletManager.wallet.amounts

        return UpdateWalletManagerResult.Verified(
            tokensAmounts = getTokensAmounts(amounts.values.toSet()),
            hasTransactionsInProgress = hasNotConfirmedTransactions,
        )
    }

    fun getDemoResult(demoAmount: Amount, tokens: Set<CryptoCurrency.Token>): UpdateWalletManagerResult.Verified {
        return UpdateWalletManagerResult.Verified(
            tokensAmounts = getDemoTokensAmounts(demoAmount, tokens),
            hasTransactionsInProgress = false,
        )
    }

    fun getNoAccountResult(walletManager: WalletManager): UpdateWalletManagerResult.NoAccount {
        val wallet = walletManager.wallet
        val blockchain = wallet.blockchain
        val amountToCreateAccount = blockchain.amountToCreateAccount(
            token = wallet.getTokens().firstOrNull(),
        )

        requireNotNull(amountToCreateAccount) {
            "Unable to get required amount to create account for: $blockchain"
        }

        return UpdateWalletManagerResult.NoAccount(amountToCreateAccount)
    }

    private fun getTokensAmounts(amounts: Set<Amount>): Set<CryptoCurrencyAmount> {
        val mutableAmounts = hashSetOf<CryptoCurrencyAmount>()

        return amounts.mapNotNullTo(mutableAmounts, ::getTokenAmount)
    }

    private fun getDemoTokensAmounts(demoAmount: Amount, tokens: Set<CryptoCurrency.Token>): Set<CryptoCurrencyAmount> {
        val amountValue = demoAmount.value ?: BigDecimal.ZERO
        val demoAmounts = hashSetOf<CryptoCurrencyAmount>(CryptoCurrencyAmount.Coin(amountValue))

        return tokens.mapTo(demoAmounts) { token ->
            CryptoCurrencyAmount.Token(token.contractAddress, amountValue)
        }
    }

    private fun getTokenAmount(amount: Amount): CryptoCurrencyAmount? {
        return when (val type = amount.type) {
            is AmountType.Token -> CryptoCurrencyAmount.Token(
                tokenContractAddress = type.token.contractAddress,
                value = getAmountValue(amount) ?: return null,
            )
            is AmountType.Coin -> CryptoCurrencyAmount.Coin(
                value = getAmountValue(amount) ?: return null,
            )
            is AmountType.Reserve -> null
        }
    }

    private fun getAmountValue(amount: Amount): BigDecimal? {
        val value = amount.value

        if (value == null) {
            Timber.e("Amount not found for currency: ${amount.currencySymbol}")
        }

        return value
    }
}
