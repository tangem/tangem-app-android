package com.tangem.tap.domain.model

import com.tangem.tap.domain.model.WalletDataModel.Status
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.PendingTransaction
import com.tangem.tap.features.wallet.redux.AddressData
import java.math.BigDecimal

/**
 * Contains info about wallet store currency
 * @param currency Wallet's [Currency]
 * @param status Wallet's [Status], represents current status of that currency
 * @param walletAddresses List of wallet data [AddressData]
 * @param existentialDeposit Amount that must be held on currency's balance, if balance is below that amount all
 * founds will be destroyed. Null if currency don't have existential deposit
 * @param fiatRate Wallet's fiat rate, used to calculate fiat balance. Null if not provided
 * @param isCardSingleToken shows that [Currency] is a card token
 * */
data class WalletDataModel(
    val currency: Currency,
    val status: Status,
    val walletAddresses: List<AddressData>,
    val existentialDeposit: BigDecimal?,
    val fiatRate: BigDecimal?,
    val isCardSingleToken: Boolean,
) {

    /**
     * Represent current status of currency
     * @property amount Currency amount
     * @property pendingTransactions List of currency [PendingTransaction] sent in currency's blockchain
     * @property errorMessage Status error message, null if not provided
     * @property isErrorStatus true if current status is error status, false otherwise
     * */
    sealed class Status {
        open val amount: BigDecimal = BigDecimal.ZERO
        open val pendingTransactions: List<PendingTransaction> = emptyList()
        open val errorMessage: String? = null
        open val isErrorStatus: Boolean = false
    }

    object Loading : Status()

    data class VerifiedOnline(
        override val amount: BigDecimal,
    ) : Status()

    data class TransactionInProgress(
        override val amount: BigDecimal,
        override val pendingTransactions: List<PendingTransaction>,
    ) : Status()

    data class SameCurrencyTransactionInProgress(
        override val amount: BigDecimal,
        override val pendingTransactions: List<PendingTransaction>,
    ) : Status()

    data class NoAccount(
        val amountToCreateAccount: BigDecimal?,
    ) : Status() {
        override val isErrorStatus: Boolean = true
    }

    data class Unreachable(
        override val errorMessage: String?,
    ) : Status() {
        override val isErrorStatus: Boolean = true
    }

    object MissedDerivation : Status() {
        override val isErrorStatus: Boolean = true
    }
}
