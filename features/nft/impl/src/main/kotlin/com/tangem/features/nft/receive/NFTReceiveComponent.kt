package com.tangem.features.nft.receive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.nft.receive.model.NFTReceiveModel
import com.tangem.features.nft.receive.ui.NFTReceive
import com.tangem.features.tokenreceive.TokenReceiveComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

internal class NFTReceiveComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
    private val tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: NFTReceiveModel = getOrCreateModel(params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = TokenReceiveConfig.serializer(),
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        NFTReceive(state, modifier)
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: TokenReceiveConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = tokenReceiveComponentFactory.create(
        context = childByContext(componentContext),
        params = TokenReceiveComponent.Params(
            config = config,
            onDismiss = model.bottomSheetNavigation::dismiss,
        ),
    )

    data class Params(
        val userWalletId: UserWalletId,
        val walletName: String,
        val onBackClick: () -> Unit,
    )
}