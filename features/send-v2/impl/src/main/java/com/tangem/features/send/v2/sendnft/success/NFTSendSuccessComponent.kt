package com.tangem.features.send.v2.sendnft.success

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.navigationButtons.NavigationModelCallback
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.features.nft.component.NFTDetailsBlockComponent
import com.tangem.features.send.v2.api.entity.PredefinedValues
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationBlockComponent
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationComponentParams.DestinationBlockParams
import com.tangem.features.send.v2.common.CommonSendRoute
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.sendnft.success.model.NFTSendSuccessModel
import com.tangem.features.send.v2.sendnft.success.ui.NFTSendSuccessContent
import com.tangem.features.send.v2.sendnft.ui.state.NFTSendUM
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class NFTSendSuccessComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Params,
    nftDetailsBlockComponentFactory: NFTDetailsBlockComponent.Factory,
    sendDestinationBlockComponentFactory: SendDestinationBlockComponent.Factory,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: NFTSendSuccessModel = getOrCreateModel(params = params)

    private val nftDetailsBlockComponent = nftDetailsBlockComponentFactory.create(
        context = child("NFTDetailsSuccessBlock"),
        params = NFTDetailsBlockComponent.Params(
            userWalletId = params.userWallet.walletId,
            nftAsset = params.nftAsset,
            nftCollectionName = params.nftCollectionName,
            isSuccessScreen = true,
            title = resourceReference(R.string.nft_asset),
        ),
    )

    private val sendDestinationBlockComponent = sendDestinationBlockComponentFactory.create(
        context = child("NFTDestinationSuccessBlock"),
        params = DestinationBlockParams(
            state = model.uiState.value.destinationUM,
            analyticsCategoryName = params.analyticsCategoryName,
            userWalletId = params.userWallet.walletId,
            cryptoCurrency = params.cryptoCurrencyStatus.currency,
            blockClickEnableFlow = MutableStateFlow(false),
            predefinedValues = PredefinedValues.Empty,
        ),
        onResult = {},
        onClick = {},
    )

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

    data class Params(
        val nftSendUMFlow: StateFlow<NFTSendUM>,
        val analyticsCategoryName: String,
        val currentRoute: Flow<CommonSendRoute>,
        val cryptoCurrencyStatus: CryptoCurrencyStatus,
        val userWallet: UserWallet,
        val nftAsset: NFTAsset,
        val nftCollectionName: String,
        val txUrl: String,
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