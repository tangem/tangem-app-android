package com.tangem.tap.common.analytics.topup

import com.tangem.common.extensions.guard
import com.tangem.common.extensions.isZero
import com.tangem.core.analytics.Analytics
import com.tangem.data.source.preferences.model.DataSourceTopupInfo
import com.tangem.data.source.preferences.storage.ToppedUpWalletStorage
import com.tangem.domain.common.BlockchainNetwork
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.userwallets.UserWalletIdBuilder
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.tap.common.analytics.converters.TopUpEventConverter
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.extensions.copy
import com.tangem.tap.domain.model.TotalFiatBalance
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.domain.walletCurrencies.WalletCurrenciesManager
import com.tangem.tap.domain.walletStores.WalletStoresManager
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.scope
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
* [REDACTED_AUTHOR]
 */
class TopUpController(
    var scanResponseProvider: (() -> ScanResponse?)? = null,
    var walletStoresManagerProvider: (() -> WalletStoresManager)? = null,
    private val topupWalletStorage: ToppedUpWalletStorage,
) : WalletCurrenciesManager.Listener {

    private var hadMissedDerivations: Boolean = false
    private val addedCurrencies = mutableListOf<Currency>()

    override fun didUpdate(userWallet: UserWallet, currency: Currency) {
        tryToSend()
    }

    override fun willCurrenciesAdd(userWallet: UserWallet, currenciesToAdd: List<Currency>) {
        addedCurrencies.addAll(currenciesToAdd.distinct())
    }

    fun walletStoresChanged(walletStores: List<WalletStoreModel>) {
        val missedDerivations = walletStores
            .flatMap { it.walletsData }
            .map { it.status }
            .filterIsInstance<WalletDataModel.MissedDerivation>()

        hadMissedDerivations = missedDerivations.isNotEmpty()
    }

    fun scanToGetDerivations() {
        hadMissedDerivations = true
    }

    fun addMissingDerivations(blockchains: List<BlockchainNetwork>) {
        hadMissedDerivations = blockchains.isNotEmpty()
    }

    fun totalBalanceStateChanged(totalFiatBalance: TotalFiatBalance) {
        if (totalFiatBalance is TotalFiatBalance.Loaded) tryToSend()
    }

    fun loadDataSuccess() {
        tryToSend()
    }

    private fun tryToSend() {
        if (hadMissedDerivations) return
        val scanResponse = scanResponseProvider?.invoke() ?: return
        val userWalletId = UserWalletIdBuilder.scanResponse(scanResponse).build() ?: return
        val walletStoresManager = walletStoresManagerProvider?.invoke() ?: return

        scope.launch {
            val walletDataModels = walletStoresManager.getSync(userWalletId).flatMap { it.walletsData }
            if (walletDataModels.isEmpty()) return@launch

            val isCorrectStatus = walletDataModels.any {
                it.status is WalletDataModel.Loading ||
                    it.status is WalletDataModel.NoAccount ||
                    it.status is WalletDataModel.Unreachable ||
                    it.status is WalletDataModel.MissedDerivation ||
                    it.status.isErrorStatus
            }
            if (isCorrectStatus) return@launch

            val isToppedUpInPast = findToppedUpCurrenciesInPast(walletDataModels).isNotEmpty()
            if (isToppedUpInPast) {
                val newWalletInfo = DataSourceTopupInfo(
                    walletId = userWalletId.stringValue,
                    cardBalanceState = DataSourceTopupInfo.CardBalanceState.Full,
                )
                topupWalletStorage.save(newWalletInfo)
                return@launch
            }

            val cardBalanceState = BalanceCalculator(walletDataModels).calculate().toCardBalanceState()
            send(userWalletId, cardBalanceState, scanResponse.cardTypesResolver)
        }
    }

    /**
     * A UserWalletId registration should be after creating wallets
     */
    fun registerEmptyWallet(scanResponse: ScanResponse) {
        UserWalletIdBuilder.scanResponse(scanResponse).build()?.let {
            topupWalletStorage.save(
                DataSourceTopupInfo(
                    walletId = it.stringValue,
                    cardBalanceState = DataSourceTopupInfo.CardBalanceState.Empty,
                ),
            )
        }
    }

    fun send(scanResponse: ScanResponse, cardBalanceState: AnalyticsParam.CardBalanceState) {
        UserWalletIdBuilder.scanResponse(scanResponse).build()?.let {
            send(it, cardBalanceState, scanResponse.cardTypesResolver)
        }
    }

    fun send(
        userWalletId: UserWalletId,
        cardBalanceState: AnalyticsParam.CardBalanceState,
        cardTypesResolver: CardTypesResolver,
    ) {
        val topupInfo = topupWalletStorage.restore(userWalletId.stringValue).guard {
            val topupInfo = DataSourceTopupInfo(
                walletId = userWalletId.stringValue,
                cardBalanceState = when (cardBalanceState) {
                    AnalyticsParam.CardBalanceState.BlockchainError ->
                        DataSourceTopupInfo.CardBalanceState.BlockchainError
                    AnalyticsParam.CardBalanceState.CustomToken ->
                        DataSourceTopupInfo.CardBalanceState.CustomToken
                    AnalyticsParam.CardBalanceState.Empty ->
                        DataSourceTopupInfo.CardBalanceState.Empty
                    AnalyticsParam.CardBalanceState.Full ->
                        DataSourceTopupInfo.CardBalanceState.Full
                },
            )
            topupWalletStorage.save(topupInfo)
            return
        }

        val isToppedUp = topupInfo.cardBalanceState == DataSourceTopupInfo.CardBalanceState.Full
        if (isToppedUp) return

        if (cardBalanceState.isToppedUp()) {
            topupWalletStorage.save(topupInfo.copy(cardBalanceState = DataSourceTopupInfo.CardBalanceState.Full))
            TopUpEventConverter().convert(cardTypesResolver)?.let {
                Analytics.send(it)
            }
        }
    }

    private fun findToppedUpCurrenciesInPast(walletDataModels: List<WalletDataModel>): List<WalletDataModel> {
        val currenciesToppedUpInPast = addedCurrencies.copy()
            .mapNotNull { currency ->
                val foundCurrencyModel = walletDataModels
                    .find { it.currency == currency }
                    ?: return@mapNotNull null

                if (foundCurrencyModel.status.amount.isZero()) null else foundCurrencyModel
            }
        addedCurrencies.clear()

        return currenciesToppedUpInPast
    }

    private fun BigDecimal.toCardBalanceState(): AnalyticsParam.CardBalanceState = when {
        isZero() -> AnalyticsParam.CardBalanceState.Empty
        else -> AnalyticsParam.CardBalanceState.Full
    }

    private fun AnalyticsParam.CardBalanceState.isToppedUp(): Boolean = this == AnalyticsParam.CardBalanceState.Full
}

private interface IBalanceCalculator {
    fun calculate(): BigDecimal
}

private class BalanceCalculator(
    private val walletDataModels: List<WalletDataModel>,
) : IBalanceCalculator {

    override fun calculate(): BigDecimal {
        val singleToken = walletDataModels
            .filter { it.currency.isToken() }
            .firstOrNull { it.isCardSingleToken }

        val totalAmount = singleToken?.status?.amount
            ?: walletDataModels.calculateTotalCryptoAmount()

        return totalAmount
    }

    private fun List<WalletDataModel>.calculateTotalCryptoAmount(): BigDecimal = this
        .map { it.status.amount }
        .reduce(BigDecimal::plus)
}
