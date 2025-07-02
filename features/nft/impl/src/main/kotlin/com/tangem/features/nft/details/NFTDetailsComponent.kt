package com.tangem.features.nft.details

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
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.nft.models.NFTCollection
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.nft.details.entity.NFTDetailsBottomSheetConfig
import com.tangem.features.nft.details.info.NFTDetailsInfoComponent
import com.tangem.features.nft.details.model.NFTDetailsModel
import com.tangem.features.nft.details.ui.NFTDetails
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

internal class NFTDetailsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
    private val nftDetailsInfoComponentFactory: NFTDetailsInfoComponent.Factory,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: NFTDetailsModel = getOrCreateModel(params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        NFTDetails(state, modifier)
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: NFTDetailsBottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = when (config) {
        is NFTDetailsBottomSheetConfig.Info -> nftDetailsInfoComponentFactory.create(
            context = childByContext(componentContext),
            params = NFTDetailsInfoComponent.Params(
                title = config.title,
                text = config.text,
                onDismiss = {
                    model.bottomSheetNavigation.dismiss()
                },
            ),
        )
    }

    data class Params(
        val userWalletId: UserWalletId,
        val nftAsset: NFTAsset,
        val nftCollection: NFTCollection,
        val onBackClick: () -> Unit,
        val onAllTraitsClick: () -> Unit,
    )
}