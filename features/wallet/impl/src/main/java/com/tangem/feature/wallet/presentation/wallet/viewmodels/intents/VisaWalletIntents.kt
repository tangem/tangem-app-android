package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

import arrow.core.getOrElse
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.tokenreceive.mapToAddressModels
import com.tangem.domain.tokens.GetPrimaryCurrencyStatusUpdatesUseCase
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.WalletAlertState
import com.tangem.feature.wallet.presentation.wallet.state.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state2.model.BalancesAndLimitsBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state2.utils.WalletEventSender
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal interface VisaWalletIntents {

    fun onDepositClick()

    fun onBalancesAndLimitsClick()
}

@ViewModelScoped
internal class VisaWalletIntentsImplementor @Inject constructor(
    private val stateController: WalletStateController,
    private val eventSender: WalletEventSender,
    private val getPrimaryCurrencyUseCase: GetPrimaryCurrencyStatusUpdatesUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
) : BaseWalletClickIntents(), VisaWalletIntents {

    override fun onDepositClick() {
        val userWalletId = stateController.getSelectedWalletId()

        viewModelScope.launch(dispatchers.main) {
            val currencyStatus = getPrimaryCurrencyStatus(userWalletId) ?: return@launch

            createReceiveBottomSheetContent(currencyStatus)?.let { content ->
                stateController.showBottomSheet(content)
            }
        }
    }

    private suspend fun getPrimaryCurrencyStatus(userWalletId: UserWalletId): CryptoCurrencyStatus? {
        return getPrimaryCurrencyUseCase(userWalletId)
            .firstOrNull()
            ?.getOrElse {
                Timber.e("Failed to get primary currency $it")
                null
            }
    }

    private fun createReceiveBottomSheetContent(currencyStatus: CryptoCurrencyStatus): TangemBottomSheetConfigContent? {
        val currency = currencyStatus.currency
        val addresses = currencyStatus.value.networkAddress?.availableAddresses

        if (addresses == null) {
            Timber.e("Addresses should not be null")
            return null
        }

        return TokenReceiveBottomSheetConfig(
            name = currency.name,
            symbol = currency.symbol,
            network = currency.network.name,
            addresses = addresses.mapToAddressModels(currency).toImmutableList(),
            onCopyClick = { /* no-op */ },
            onShareClick = { /* no-op */ },
        )
    }

    override fun onBalancesAndLimitsClick() {
        stateController.showBottomSheet(getBalancesAndLimitsConfig())
    }

    // TODO: Implement
    private fun getBalancesAndLimitsConfig() = BalancesAndLimitsBottomSheetConfig(
        currency = "USDT",
        balance = BalancesAndLimitsBottomSheetConfig.Balance(
            totalBalance = "492.45",
            availableBalance = "392.45",
            blockedBalance = "36.00",
            debit = "00.00",
            pending = "20.99",
            amlVerified = "356.45",
        ),
        limit = BalancesAndLimitsBottomSheetConfig.Limit(
            availableBy = "Nov, 11",
            inStore = "563.00",
            other = "100.00",
            singleTransaction = "100.00",
        ),
        onBalanceInfoClick = this::showBalanceInfo,
        onLimitInfoClick = this::showLimitInfo,
    )

    private fun showBalanceInfo() {
        eventSender.send(WalletEvent.ShowAlert(WalletAlertState.VisaBalancesInfo))
    }

    private fun showLimitInfo() {
        eventSender.send(WalletEvent.ShowAlert(WalletAlertState.VisaLimitsInfo))
    }
}