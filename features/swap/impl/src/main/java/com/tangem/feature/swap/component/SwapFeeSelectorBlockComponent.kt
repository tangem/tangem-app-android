package com.tangem.feature.swap.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import arrow.core.Either
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SwapFeeSelectorBlockComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Params,
    feeSelectorBlockComponentFactory: FeeSelectorBlockComponent.Factory,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val feeSelectorBlockComponent =
        feeSelectorBlockComponentFactory.create(
            context = child("swapFeeSelectorBlock"),
            params = FeeSelectorParams.FeeSelectorBlockParams(
                state = params.repository.state.value,
                userWalletId = params.userWalletId,
                onLoadFee = params.repository::loadFee,
                onLoadFeeExtended = if (params.repository is ModelRepositoryExtended) {
                    params.repository::loadFeeExtended
                } else {
                    null
                },
                feeDisplaySource = FeeSelectorParams.FeeDisplaySource.Screen,
                feeStateConfiguration = FeeSelectorParams.FeeStateConfiguration.ExcludeLow,
                feeCryptoCurrencyStatus = params.feeCryptoCurrencyStatus,
                cryptoCurrencyStatus = params.sendingCryptoCurrencyStatus,
                analyticsCategoryName = params.analyticsParams.analyticsCategoryName,
                analyticsSendSource = params.analyticsParams.analyticsSendSource,
                bottomSheetShown = params.repository::choosingInProgress,
            ),
            onResult = params.repository::onResult,
        )

    init {
        params.repository.state
            .onEach(feeSelectorBlockComponent::updateState)
            .launchIn(componentScope)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        feeSelectorBlockComponent.Content(modifier = modifier)
    }

    interface ModelRepository {
        val state: StateFlow<FeeSelectorUM>
            get() = MutableStateFlow<FeeSelectorUM>(FeeSelectorUM.Loading)

        fun onResult(newState: FeeSelectorUM)

        suspend fun loadFee(): Either<GetFeeError, TransactionFee>

        fun choosingInProgress(updatedState: Boolean)
    }

    interface ModelRepositoryExtended : ModelRepository {
        suspend fun loadFeeExtended(
            selectedToken: CryptoCurrencyStatus? = null,
        ): Either<GetFeeError, TransactionFeeExtended>
    }

    class AnalyticsParams(
        val analyticsCategoryName: String,
        val analyticsSendSource: CommonSendAnalyticEvents.CommonSendSource,
    )

    class Params(
        val userWalletId: UserWalletId,
        val sendingCryptoCurrencyStatus: CryptoCurrencyStatus,
        val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
        val analyticsParams: AnalyticsParams,
        val repository: ModelRepository,
    )

    @AssistedFactory
    interface Factory : ComponentFactory<Params, SwapFeeSelectorBlockComponent>
}