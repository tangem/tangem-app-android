package com.tangem.features.tangempay.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.essenty.lifecycle.subscribe
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.NavigationBar3ButtonsScrim
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.components.cardDetails.DefaultTangemPayCardDetailsBlockComponent
import com.tangem.features.tangempay.components.cardDetails.TangemPayCardDetailsBlockComponent
import com.tangem.features.tangempay.components.express.ExpressTransactionsComponentProvider
import com.tangem.features.tangempay.components.txHistory.DefaultTangemPayTxHistoryComponent
import com.tangem.features.tangempay.components.txHistory.TangemPayTxHistoryDetailsComponent
import com.tangem.features.tangempay.entity.TangemPayDetailsNavigation
import com.tangem.features.tangempay.model.TangemPayDetailsModel
import com.tangem.features.tangempay.ui.TangemPayDetailsScreen
import com.tangem.features.tokenreceive.TokenReceiveComponent

internal class TangemPayDetailsComponent(
    private val appComponentContext: AppComponentContext,
    private val params: TangemPayDetailsContainerComponent.Params,
    private val tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
    private val expressTransactionsComponentProvider: ExpressTransactionsComponentProvider,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: TangemPayDetailsModel = getOrCreateModel(params = params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = TangemPayDetailsNavigation.serializer(),
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )
    private val txHistoryComponent = DefaultTangemPayTxHistoryComponent(
        appComponentContext = child("txHistoryComponent"),
        params = DefaultTangemPayTxHistoryComponent.Params(
            userWalletId = params.userWalletId,
            customerWalletAddress = params.config.customerWalletAddress,
            uiActions = model,
        ),
    )

    private val cardDetailsBlockComponent = DefaultTangemPayCardDetailsBlockComponent(
        appComponentContext = child("cardDetailsBlockComponent"),
        params = TangemPayCardDetailsBlockComponent.Params(params = params),
    )

    private val expressTransactionsComponent by lazy {
        expressTransactionsComponentProvider.create(
            appComponentContext = child("expressTransactionsComponent"),
            userWalletId = params.userWalletId,
            cryptoCurrency = model.cryptoCurrency,
        )
    }

    init {
        lifecycle.subscribe(
            onPause = model::onPause,
            onResume = model::onResume,
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        NavigationBar3ButtonsScrim()
        TangemPayDetailsScreen(
            state = state,
            txHistoryComponent = txHistoryComponent,
            cardDetailsBlockComponent = cardDetailsBlockComponent,
            expressTransactionsComponent = expressTransactionsComponent,
            modifier = modifier,
        )
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        navigation: TangemPayDetailsNavigation,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        val context = childByContext(componentContext)
        return when (navigation) {
            is TangemPayDetailsNavigation.Receive -> tokenReceiveComponentFactory.create(
                context = context,
                params = TokenReceiveComponent.Params(
                    config = navigation.config,
                    onDismiss = model.bottomSheetNavigation::dismiss,
                ),
            )
            is TangemPayDetailsNavigation.TransactionDetails -> TangemPayTxHistoryDetailsComponent(
                appComponentContext = context,
                params = TangemPayTxHistoryDetailsComponent.Params(
                    transaction = navigation.transaction,
                    isBalanceHidden = navigation.isBalanceHidden,
                    userWalletId = params.userWalletId,
                    customerId = params.config.customerId,
                    onDismiss = model.bottomSheetNavigation::dismiss,
                ),
            )
            is TangemPayDetailsNavigation.AddFunds -> TangemPayAddFundsComponent(
                appComponentContext = context,
                params = TangemPayAddFundsComponent.Params(
                    walletId = navigation.walletId,
                    cryptoBalance = navigation.cryptoBalance,
                    fiatBalance = navigation.fiatBalance,
                    depositAddress = navigation.depositAddress,
                    chainId = navigation.chainId,
                    listener = model,
                ),
            )
            is TangemPayDetailsNavigation.ViewPinCode -> TangemPayViewPinComponent(
                appComponentContext = context,
                params = TangemPayViewPinComponent.Params(
                    walletId = navigation.userWalletId,
                    cardId = navigation.cardId,
                    listener = model,
                ),
            )
        }
    }
}