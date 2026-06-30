package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.models.ReceiveAddressModel
import com.tangem.domain.models.ReceiveAddressModel.DisplayType
import com.tangem.domain.pay.model.TangemPayTopUpData
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.components.TangemPayAddFundsComponent
import com.tangem.features.tangempay.entity.TangemPayAddFundsUM
import com.tangem.features.tangempay.model.transformers.TangemPayAddFundsUMConverter
import com.tangem.features.virtualaccount.VirtualAccountFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayAddFundsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val tangemPayFeatureToggles: TangemPayFeatureToggles,
    private val virtualAccountToggles: VirtualAccountFeatureToggles,
) : Model() {

    private val params = paramsContainer.require<TangemPayAddFundsComponent.Params>()

    val uiState: TangemPayAddFundsUM = getInitialState()

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
            isRedesignEnabled = tangemPayFeatureToggles.isRedesignEnabled,
            shouldShowBankTransfer = virtualAccountToggles.isVaMvp0Enabled && params.virtualAccountOnramp != null,
        ).convert(data)
    }

    fun isRedesignEnabled(): Boolean = tangemPayFeatureToggles.isRedesignEnabled

    fun onDismiss() {
        params.listener.onDismissAddFunds()
    }
}