package com.tangem.features.tangempay.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.NavigationBar3ButtonsScrim
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.components.cardDetails.DefaultTangemPayCardDetailsBlockComponent
import com.tangem.features.tangempay.components.cardDetails.TangemPayCardDetailsBlockComponent
import com.tangem.features.tangempay.entity.TangemPayCardNavigation
import com.tangem.features.tangempay.model.TangemPayCardPageModel
import com.tangem.features.tangempay.ui.TangemPayCardPageScreen
import com.tangem.features.tokenreceive.TokenReceiveComponent

internal class TangemPayCardPageScreenComponent(
    private val appComponentContext: AppComponentContext,
    private val params: TangemPayCardPageComponent.Params,
    private val tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: TangemPayCardPageModel = getOrCreateModel(params = params)

    private val containerParams = TangemPayDetailsContainerComponent.Params(
        userWalletId = params.userWalletId,
        config = params.config,
    )

    private val cardDetailsBlockComponent = DefaultTangemPayCardDetailsBlockComponent(
        appComponentContext = child("cardDetailsBlockComponent"),
        params = TangemPayCardDetailsBlockComponent.Params(
            params = containerParams,
            isEditingNameEnabled = true,
        ),
    )

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = TangemPayCardNavigation.serializer(),
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val cardDetailsState by cardDetailsBlockComponent.state.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        NavigationBar3ButtonsScrim()
        TangemPayCardPageScreen(
            state = state,
            cardDetailsBlockComponent = cardDetailsBlockComponent,
            cardDetailsState = cardDetailsState.copy(
                isActive = !state.isReissueInProgress,
            ),
            modifier = modifier,
        )
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        navigation: TangemPayCardNavigation,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        val context = childByContext(componentContext)
        return when (navigation) {
            is TangemPayCardNavigation.ViewPinCode -> TangemPayViewPinComponent(
                appComponentContext = context,
                params = TangemPayViewPinComponent.Params(
                    walletId = navigation.userWalletId,
                    cardId = navigation.cardId,
                    listener = model,
                ),
            )
            is TangemPayCardNavigation.ReissueCard -> TangemPayReissueCardComponent(
                appComponentContext = context,
                params = TangemPayReissueCardComponent.Params(
                    listener = model,
                    userWalletId = params.userWalletId,
                    cardId = params.config.cardId,
                ),
            )
            is TangemPayCardNavigation.AddFunds -> TangemPayAddFundsComponent(
                appComponentContext = context,
                params = TangemPayAddFundsComponent.Params(
                    listener = model,
                    walletId = navigation.walletId,
                    cryptoBalance = navigation.cryptoBalance,
                    fiatBalance = navigation.fiatBalance,
                    depositAddress = navigation.depositAddress,
                    chainId = navigation.chainId,
                ),
            )
            is TangemPayCardNavigation.Receive -> tokenReceiveComponentFactory.create(
                context = context,
                params = TokenReceiveComponent.Params(
                    config = navigation.config,
                    onDismiss = model.bottomSheetNavigation::dismiss,
                ),
            )
        }
    }
}