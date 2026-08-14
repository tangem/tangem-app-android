package com.tangem.features.tangempay.addfunds

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.models.ReceiveAddressModel
import com.tangem.domain.models.ReceiveAddressModel.DisplayType
import com.tangem.domain.pay.model.TangemPayTopUpData
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayAddFundsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    tangemPayFeatureToggles: TangemPayFeatureToggles,
    analytics: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<TangemPayAddFundsComponent.Params>()

    private val isBankTransferShown = params.virtualAccountOnramp != null

    private val isMultichainEnabled = tangemPayFeatureToggles.isAccountMultichainEnabled

    val uiState: TangemPayAddFundsUM = getInitialState()

    init {
        if (isBankTransferShown) {
            analytics.send(TangemPayAnalyticsEvents.VaTopupButtonShowed())
        }
    }

    private fun getInitialState(): TangemPayAddFundsUM {
        val data = TangemPayTopUpData(
            currency = params.cryptoCurrency,
            walletId = params.walletId,
            cryptoBalance = params.cryptoBalance,
            fiatBalance = params.fiatBalance,
            depositAddress = params.depositAddress,
            receiveAddress = listOf(
                ReceiveAddressModel(
                    displayType = DisplayType.Default,
                    value = params.depositAddress,
                ),
            ),
        )
        return TangemPayAddFundsUMConverter(
            listener = params.listener,
            shouldShowBankTransfer = isBankTransferShown,
            isMultichainEnabled = isMultichainEnabled,
        ).convert(data)
    }

    fun onDismiss() {
        params.listener.onDismissAddFunds()
    }
}