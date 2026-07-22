package com.tangem.features.send.sendnft.success

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.navigationButtons.DoneButtons
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationModelCallback
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.AppBarWithBackButtonAndIcon
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.features.nft.component.NFTDetailsBlockComponent
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.entity.PredefinedValues
import com.tangem.features.send.api.subcomponents.destination.SendDestinationBlockComponent
import com.tangem.features.send.api.subcomponents.destination.SendDestinationComponentParams.DestinationBlockParams
import com.tangem.features.send.impl.R
import com.tangem.features.send.sendnft.success.model.NFTSendSuccessModel
import com.tangem.features.send.sendnft.success.ui.NFTSendSuccessContent
import com.tangem.features.send.sendnft.ui.state.NFTSendUM
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class NFTSendSuccessComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: Params,
    nftDetailsBlockComponentFactory: NFTDetailsBlockComponent.Factory,
    sendDestinationBlockComponentFactory: SendDestinationBlockComponent.Factory,
) : ComposableModularContentComponent, AppComponentContext by appComponentContext {

    private val model: NFTSendSuccessModel = getOrCreateModel(params = params)

    private val nftDetailsBlockComponent = nftDetailsBlockComponentFactory.create(
        context = child("NFTDetailsSuccessBlock"),
        params = NFTDetailsBlockComponent.Params(
            userWalletId = params.userWallet.walletId,
            nftAsset = params.nftAsset,
            nftCollectionName = params.nftCollectionName,
            isSuccessScreen = true,
            account = params.account,
            isAccountsMode = params.isAccountsMode,
            walletTitle = resourceReference(R.string.nft_asset),
        ),
    )

    private val sendDestinationBlockComponent = sendDestinationBlockComponentFactory.create(
        context = child("NFTDestinationSuccessBlock"),
        params = DestinationBlockParams(
            state = model.uiState.value.destinationUM,
            analyticsCategoryName = params.analyticsCategoryName,
            analyticsSendSource = params.analyticsSendSource,
            userWalletId = params.userWallet.walletId,
            cryptoCurrency = params.cryptoCurrencyStatus.currency,
            blockClickEnableFlow = MutableStateFlow(false),
            predefinedValues = PredefinedValues.Empty,
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
        val state by model.uiState.collectAsStateWithLifecycle()
        NFTSendSuccessContent(
            nftSendUM = state,
            destinationBlockComponent = sendDestinationBlockComponent,
            nftDetailsBlockComponent = nftDetailsBlockComponent,
            modifier = modifier,
        )
    }

    @Composable
    override fun Footer() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
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
        val nftSendUMFlow: StateFlow<NFTSendUM>,
        val analyticsCategoryName: String,
        val analyticsSendSource: CommonSendAnalyticEvents.CommonSendSource,
        val cryptoCurrencyStatus: CryptoCurrencyStatus,
        val userWallet: UserWallet,
        val nftAsset: NFTAsset,
        val nftCollectionName: String,
        val txUrl: String,
        val account: Account.CryptoPortfolio?,
        val isAccountsMode: Boolean,
        val callback: ModelCallback,
    )

    interface ModelCallback : NavigationModelCallback {
        fun onResult(nftSendUM: NFTSendUM)
    }

    @AssistedFactory
    interface Factory {
        fun create(appComponentContext: AppComponentContext, params: Params): NFTSendSuccessComponent
    }
}