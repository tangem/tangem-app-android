package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

import arrow.core.getOrElse
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.tokenreceive.mapToAddressModels
import com.tangem.domain.tokens.GetCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.visa.GetVisaCurrencyUseCase
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.converter.BalancesAndLimitsBottomSheetConverter
import com.tangem.feature.wallet.presentation.wallet.state2.utils.WalletEventSender
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.collections.immutable.toImmutableList
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
    private val getCurrencyStatusUseCase: GetCryptoCurrencyStatusSyncUseCase,
    private val getVisaCurrencyUseCase: GetVisaCurrencyUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
) : BaseWalletClickIntents(), VisaWalletIntents {

    private val balancesAndLimitsBottomSheetConverter by lazy(mode = LazyThreadSafetyMode.NONE) {
        BalancesAndLimitsBottomSheetConverter(eventSender)
    }

    override fun onDepositClick() {
        val userWalletId = stateController.getSelectedWalletId()

        viewModelScope.launch(dispatchers.main) {
            val currencyStatus = getPrimaryCurrencyStatus(userWalletId) ?: return@launch

            createReceiveBottomSheetContent(currencyStatus)?.let { content ->
                stateController.showBottomSheet(content)
            }
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
        viewModelScope.launch(dispatchers.main) {
            val userWalletId = stateController.getSelectedWalletId()
            val balancesAndLimits = getVisaCurrencyUseCase(userWalletId)
                .getOrElse {
                    Timber.e("Unable to get balances and limits: $it")
                    return@launch
                }

            val bottomSheetContent = balancesAndLimitsBottomSheetConverter.convert(
                value = balancesAndLimits,
            )

            stateController.showBottomSheet(bottomSheetContent)
        }
    }

    private suspend fun getPrimaryCurrencyStatus(userWalletId: UserWalletId): CryptoCurrencyStatus? {
        return getCurrencyStatusUseCase(userWalletId)
            .getOrElse {
                Timber.e("Failed to get primary currency $it")
                null
            }
    }
}