package com.tangem.features.send.send.success

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.navigationButtons.DoneButtons
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.AppBarWithBackButtonAndIcon
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.entity.PredefinedValues
import com.tangem.features.send.api.subcomponents.destination.SendDestinationBlockComponent
import com.tangem.features.send.api.subcomponents.destination.SendDestinationComponentParams
import com.tangem.features.send.impl.R
import com.tangem.features.send.send.success.model.SendConfirmSuccessModel
import com.tangem.features.send.send.success.ui.SendConfirmSuccessContent
import com.tangem.features.send.send.ui.state.SendUM
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class SendConfirmSuccessComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: Params,
    destinationBlockComponentFactory: SendDestinationBlockComponent.Factory,
) : ComposableModularContentComponent, AppComponentContext by appComponentContext {

    private val model: SendConfirmSuccessModel = getOrCreateModel(params = params)
    private val destinationBlockComponent: SendDestinationBlockComponent = destinationBlockComponentFactory.create(
        context = child("sendConfirmDestinationBlock"),
        params = SendDestinationComponentParams.DestinationBlockParams(
            state = model.uiState.value.destinationUM,
            analyticsCategoryName = params.analyticsCategoryName,
            analyticsSendSource = CommonSendAnalyticEvents.CommonSendSource.Send,
            userWalletId = params.userWalletId,
            cryptoCurrency = params.cryptoCurrency,
            blockClickEnableFlow = MutableStateFlow(true),
            predefinedValues = params.predefinedValues,
            isAddContactAvailable = true,
        ),
        onResult = {},
        onClick = {},
    )

    @Composable
    override fun Title() {
        AppBarWithBackButtonAndIcon(
            onBackClick = {
                model.onBackClick()
                router.pop()
            },
            backIconRes = R.drawable.ic_close_24,
            backgroundColor = TangemTheme.colors.background.tertiary,
            modifier = Modifier.height(TangemTheme.dimens.size56),
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsState()
        SendConfirmSuccessContent(
            sendUM = state,
            destinationBlockComponent = destinationBlockComponent,
        )
    }

    @Composable
    override fun Footer() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
        ) {
            DoneButtons(
                (NavigationButton(
                    textReference = resourceReference(R.string.common_explore),
                    iconRes = R.drawable.ic_web_24,
                    onClick = model::onExploreClick,
                ) to NavigationButton(
                    textReference = resourceReference(R.string.common_share),
                    iconRes = R.drawable.ic_share_24,
                    onClick = model::onShareClick,
                )).takeIf { params.txUrl.isNotEmpty() },
            )
            PrimaryButton(
                text = stringResourceSafe(R.string.common_close),
                onClick = router::pop,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    data class Params(
        val sendUMFlow: StateFlow<SendUM>,
        val userWalletId: UserWalletId,
        val cryptoCurrency: CryptoCurrency,
        val analyticsCategoryName: String,
        val predefinedValues: PredefinedValues,
        val txUrl: String,
        val callback: ModelCallback,
    )

    interface ModelCallback {
        fun onResult(sendUM: SendUM)
    }

    @AssistedFactory
    interface Factory {
        fun create(appComponentContext: AppComponentContext, params: Params): SendConfirmSuccessComponent
    }
}