package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.pay.TangemPayTopUpDataFactory
import com.tangem.features.tangempay.components.TangemPayAddFundsComponent
import com.tangem.features.tangempay.entity.TangemPayAddFundsUM
import com.tangem.features.tangempay.model.transformers.TangemPayAddFundsUMConverter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayAddFundsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val tangemPayTopUpDataFactory: TangemPayTopUpDataFactory,
) : Model() {

    private val params = paramsContainer.require<TangemPayAddFundsComponent.Params>()

    val uiState: TangemPayAddFundsUM = getInitialState()

    private fun getInitialState(): TangemPayAddFundsUM {
        val data = tangemPayTopUpDataFactory.create(
            depositAddress = params.depositAddress,
            chainId = params.chainId,
            cryptoBalance = params.cryptoBalance,
            fiatBalance = params.fiatBalance,
        ).getOrNull()

        return TangemPayAddFundsUMConverter(listener = params.listener).convert(data)
    }

    fun onDismiss() {
        params.listener.onDismissAddFunds()
    }
}