package com.tangem.domain.walletmanager.utils

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.WalletManager
import com.tangem.domain.common.extensions.amountToCreateAccount
import com.tangem.domain.walletmanager.model.TokenAmount
import com.tangem.domain.walletmanager.model.UpdateWalletManagerResult
import java.math.BigDecimal
import com.tangem.domain.tokens.model.Token as DomainToken

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

    fun getDemoResult(demoAmount: Amount, tokens: Set<DomainToken>): UpdateWalletManagerResult.Verified {
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

    private fun getTokensAmounts(amounts: Set<Amount>): Set<TokenAmount> {
        val mutableAmounts = hashSetOf<TokenAmount>()

        return amounts.mapNotNullTo(mutableAmounts, ::getTokenAmount)
    }

    private fun getDemoTokensAmounts(demoAmount: Amount, tokens: Set<DomainToken>): Set<TokenAmount> {
        val amountValue = demoAmount.value ?: BigDecimal.ZERO

        return tokens.map { token ->
            val tokenContractAddress = token.contractAddress

            if (tokenContractAddress == null) {
                TokenAmount.Coin(amountValue)
            } else {
                TokenAmount.Token(tokenContractAddress, amountValue)
            }
        }.toSet()
    }

    private fun getTokenAmount(amount: Amount): TokenAmount? {
        return when (val type = amount.type) {
            is AmountType.Token -> TokenAmount.Token(
                tokenContractAddress = type.token.contractAddress,
                value = getAmountValue(amount),
            )
            is AmountType.Coin -> TokenAmount.Coin(
                value = getAmountValue(amount),
            )
            is AmountType.Reserve,
            -> null
        }
    }

    private fun getAmountValue(amount: Amount): BigDecimal {
        return requireNotNull(amount.value) {
            "Amount not found for currency: ${amount.currencySymbol}"
        }
    }
}
