package com.tangem.features.tangempay.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.tangem.core.ui.res.LocalVisaRedesignEnabled
import com.tangem.features.tangempay.components.txHistory.DefaultTangemPayTxHistoryComponent
import com.tangem.features.tangempay.components.txHistory.TangemPayTxHistoryDetailsComponent
import com.tangem.features.tangempay.entity.TangemPayDetailsNavigation
import com.tangem.features.tangempay.model.TangemPayDetailsModel
import com.tangem.features.tangempay.ui.TangemPayDetailsScreen
import com.tangem.features.tangempay.ui.TangemPayDetailsScreenV2
import com.tangem.features.tangempay.utils.requireLoaded
import com.tangem.features.tangempay.utils.userWalletId
import com.tangem.features.tokendetails.ExpressTransactionsComponent
import com.tangem.features.tokenreceive.TokenReceiveComponent

internal class TangemPayDetailsComponent(
    private val appComponentContext: AppComponentContext,
    private val params: TangemPayDetailsContainerComponent.Params,
    private val tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
    private val expressTransactionsComponentFactory: ExpressTransactionsComponent.Factory,
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
            userWalletId = params.initialStatus.userWalletId,
            uiActions = model,
        ),
    )

    private val expressTransactionsComponent by lazy {
        expressTransactionsComponentFactory.create(
            context = child("expressTransactionsComponent"),
            params = ExpressTransactionsComponent.Params(
                userWalletId = params.initialStatus.userWalletId,
                currency = model.cryptoCurrency,
            ),
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
        CompositionLocalProvider(LocalVisaRedesignEnabled provides model.isRedesignEnabled()) {
            NavigationBar3ButtonsScrim()
            if (LocalVisaRedesignEnabled.current) {
                TangemPayDetailsScreenV2(
                    state = state,
                    txHistoryComponent = txHistoryComponent,
                    expressTransactionsComponent = expressTransactionsComponent,
                    modifier = modifier,
                )
            } else {
                TangemPayDetailsScreen(
                    state = state,
                    txHistoryComponent = txHistoryComponent,
                    expressTransactionsComponent = expressTransactionsComponent,
                    modifier = modifier,
                )
            }
            bottomSheet.child?.instance?.BottomSheet()
        }
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
                params = TangemPayTransactionBottomSheetComponent.Params(
                    transaction = navigation.transaction,
                    isBalanceHidden = navigation.isBalanceHidden,
                    userWalletId = params.initialStatus.userWalletId,
                    customerId = params.initialStatus.requireLoaded().customerId,
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
                    cryptoCurrency = navigation.cryptoCurrency,
                    listener = model,
                ),
            )
        }
    }
}