package com.tangem.tap.common.analytics.converters

import com.tangem.common.Converter
import com.tangem.common.extensions.isZero
import com.tangem.core.analytics.Analytics
import com.tangem.domain.common.ScanResponse
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Basic
import com.tangem.tap.common.analytics.filters.BasicTopUpFilter
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.domain.model.builders.UserWalletIdBuilder
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.redux.reducers.calculateTotalCryptoAmount
import timber.log.Timber
import java.math.BigDecimal

/**
 * Created by Anton Zhilenkov on 02.11.2022.
 */
class BasicEventsPreChecker {

    fun tryToSend(converterData: BasicEventsSourceData) {
        if (!isReadyToSend(converterData)) return

        BasicSignInEventConverter().convert(converterData)?.let { Analytics.send(it) }
        BasicTopUpEventConverter().convert(converterData)?.let { Analytics.send(it) }
    }

    @Suppress("ComplexMethod")
    private fun isReadyToSend(data: BasicEventsSourceData): Boolean {
        val (scanResponse, walletState, biometricsWalletDataModels) = data
        if (walletState.derivationsCheckIsScheduled) {
            Timber.d("FAILED: derivationsCheckIsScheduled")
            return false
        }
        if (scanResponse.cardTypesResolver.isMultiwalletAllowed() && walletState.missingDerivations.isNotEmpty()) {
            Timber.d("FAILED: isMultiwalletAllowed || missingDerivations.isNotEmpty")
            return false
        }

        if (biometricsWalletDataModels == null) {
            Timber.d("SWITCH: OLD")
            val walletsDataFromStores = data.walletState.walletsDataFromStores
            if (walletsDataFromStores.isEmpty()) {
                Timber.d("FAILED: walletsDataFromStores.isEmpty")
                return false
            }

            val totalBalanceState = data.walletState.totalBalance?.state
            if (totalBalanceState == null || totalBalanceState == ProgressState.Loading ||
                totalBalanceState == ProgressState.Refreshing
            ) {
                Timber.d("FAILED: totalBalanceState: ${totalBalanceState?.name}")
                return false
            }

            val balancesCount = walletsDataFromStores
                .map { if (it.currencyData.amount == null) 0 else 1 }
                .reduce { acc, i -> acc + i }

            if (balancesCount != walletsDataFromStores.size) {
                Timber.d("FAILED: balancesCount != walletsDataFromStores.size")
                return false
            }
        } else {
            Timber.d("SWITCH: BIOMETRICS")
            if (biometricsWalletDataModels.isEmpty()) {
                Timber.d("FAILED: biometricsWalletDataModels.isEmpty")
                return false
            }

            val isCorrectStatus = biometricsWalletDataModels.any {
                it.status is WalletDataModel.Loading ||
                    it.status is WalletDataModel.NoAccount ||
                    it.status is WalletDataModel.Unreachable ||
                    it.status is WalletDataModel.MissedDerivation ||
                    it.status.isErrorStatus
            }
            if (isCorrectStatus) {
                Timber.d("FAILED: by status")
                return false
            }
        }

        Timber.d("SUCCESS")
        return true
    }
}

/**
 *  With biometrics enabled, we should check its storage instead of "WalletState.walletsDataFromStores"
 *  because the latter is updated after some time.
 *  @property biometricsWalletDataModels - wallet data models from the 'WalletStoresManager'. If null, then
 *  the "WalletState.walletsDataFromStores" will be used to determine appropriate state
 */
data class BasicEventsSourceData(
    val scanResponse: ScanResponse,
    val walletState: WalletState,
    val biometricsWalletDataModels: List<WalletDataModel>?,
) {
    val batchId: String by lazy { scanResponse.card.batchId }

    val userWalletIdStringValue: String? by lazy {
        UserWalletIdBuilder.scanResponse(scanResponse)
            .build()
            ?.stringValue
    }

    val paramCardCurrency: AnalyticsParam.CardCurrency? by lazy {
        ParamCardCurrencyConverter().convert(scanResponse.cardTypesResolver)
    }

    val paramCardBalanceState: AnalyticsParam.CardBalanceState by lazy { calculateAmount().toCardBalanceState() }

    private fun calculateAmount(): BigDecimal {
        return biometricsWalletDataModels?.calculateTotalCryptoAmount()
            ?: walletState.walletsDataFromStores.calculateTotalCryptoAmount()
    }

    private fun BigDecimal.toCardBalanceState(): AnalyticsParam.CardBalanceState = when {
        isZero() -> AnalyticsParam.CardBalanceState.Empty
        else -> AnalyticsParam.CardBalanceState.Full
    }

    private fun List<WalletDataModel>.calculateTotalCryptoAmount(): BigDecimal {
        return this
            .map { it.status.amount }
            .reduce(BigDecimal::plus)
    }
}

class BasicSignInEventConverter : Converter<BasicEventsSourceData, Basic.SignedIn?> {

    override fun convert(value: BasicEventsSourceData): Basic.SignedIn? {
        if (value.paramCardCurrency == null || value.userWalletIdStringValue == null) return null

        return Basic.SignedIn(
            state = value.paramCardBalanceState,
            currency = value.paramCardCurrency!!,
            batch = value.batchId,
        ).apply {
            filterData = value.userWalletIdStringValue
        }
    }
}

class BasicTopUpEventConverter : Converter<BasicEventsSourceData, Basic.ToppedUp?> {

    override fun convert(value: BasicEventsSourceData): Basic.ToppedUp? {
        if (value.paramCardCurrency == null || value.userWalletIdStringValue == null) return null

        val data = BasicTopUpFilter.Data(
            walletId = value.userWalletIdStringValue!!,
            cardBalanceState = value.paramCardBalanceState,
        )

        return Basic.ToppedUp(value.paramCardCurrency!!).apply { filterData = data }
    }
}
