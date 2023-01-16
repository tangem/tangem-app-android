package com.tangem.tap.domain.totalBalance.implementation

import com.tangem.tap.common.extensions.toFiatValue
import com.tangem.tap.domain.model.TotalFiatBalance
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.domain.totalBalance.TotalFiatBalanceCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

internal class DefaultTotalFiatBalanceCalculator : TotalFiatBalanceCalculator {
    override suspend fun calculate(walletStores: List<WalletStoreModel>, initial: TotalFiatBalance): TotalFiatBalance {
        return calculateOrNull(walletStores) ?: initial
    }

    override suspend fun calculateOrNull(walletStores: List<WalletStoreModel>): TotalFiatBalance? {
        return if (walletStores.isEmpty()) {
            null
        } else {
            withContext(Dispatchers.Default) {
                val walletsData = walletStores
                    .asSequence()
                    .flatMap { it.walletsData }
                val calculateAmount = { walletsData.calculateTotalFiatAmount() }

                when (walletsData.findStatus()) {
                    TotalFiatBalanceStatus.Loading -> TotalFiatBalance.Loading
                    TotalFiatBalanceStatus.Error -> TotalFiatBalance.Error(calculateAmount())
                    TotalFiatBalanceStatus.Loaded -> TotalFiatBalance.Loaded(calculateAmount())
                }
            }
        }
    }

    private fun Sequence<WalletDataModel>.findStatus(): TotalFiatBalanceStatus {
        return this
            .mapToStatus()
            .reduce { prevStatus, newStatus ->
                getCurrentStatus(prevStatus, newStatus)
            }
    }

    private fun Sequence<WalletDataModel>.mapToStatus(): Sequence<TotalFiatBalanceStatus> {
        return this.map { walletData ->
            when (walletData.status) {
                is WalletDataModel.VerifiedOnline,
                is WalletDataModel.SameCurrencyTransactionInProgress,
                is WalletDataModel.TransactionInProgress,
                is WalletDataModel.NoAccount,
                -> TotalFiatBalanceStatus.Loaded
                is WalletDataModel.Unreachable,
                is WalletDataModel.MissedDerivation,
                -> TotalFiatBalanceStatus.Error
                is WalletDataModel.Loading -> TotalFiatBalanceStatus.Loading
            }
        }
    }

    private fun Sequence<WalletDataModel>.calculateTotalFiatAmount(): BigDecimal {
        return this
            .map { walletData ->
                walletData.fiatRate
                    ?.let { walletData.status.amount.toFiatValue(it) }
                    ?: BigDecimal.ZERO
            }
            .reduce(BigDecimal::plus)
    }

    private fun getCurrentStatus(
        prevStatus: TotalFiatBalanceStatus,
        newStatus: TotalFiatBalanceStatus,
    ): TotalFiatBalanceStatus {
        return when (prevStatus) {
            TotalFiatBalanceStatus.Loading -> prevStatus
            TotalFiatBalanceStatus.Loaded,
            TotalFiatBalanceStatus.Error,
            -> when (newStatus) {
                TotalFiatBalanceStatus.Loading,
                TotalFiatBalanceStatus.Error,
                -> newStatus
                TotalFiatBalanceStatus.Loaded -> prevStatus
            }
        }
    }

    private enum class TotalFiatBalanceStatus {
        Loading,
        Error,
        Loaded,
    }
}
