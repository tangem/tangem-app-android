package com.tangem.data.walletmanager

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.isJoint
import com.tangem.domain.txhistory.TxHistoryFeatureToggles
import com.tangem.domain.yield.supply.YieldSupplyWrappedNativeFactory
import javax.inject.Inject

/** Whether a currency's history needs the internal transfers the explorer leaves out by default */
internal class InternalTransfersPolicy @Inject constructor(
    private val txHistoryFeatureToggles: TxHistoryFeatureToggles,
    private val wrappedNativeFactory: YieldSupplyWrappedNativeFactory,
) {

    fun shouldInclude(currency: CryptoCurrency): Boolean {
        if (currency !is CryptoCurrency.Coin) return false

        // A joint account's coin moves only in the Safe's internal calls: the relayer sends the transaction
        if (currency.isJoint) return true

        // A Lending withdrawal pays the coin out from the Yield Module in an internal call
        return txHistoryFeatureToggles.isLendingHistoryEnabled && wrappedNativeFactory.create(currency.network) != null
    }
}