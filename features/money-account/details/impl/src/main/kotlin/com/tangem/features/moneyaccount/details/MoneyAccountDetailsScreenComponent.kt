package com.tangem.features.moneyaccount.details

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
import com.tangem.features.moneyaccount.details.addfunds.options.MoneyAccountAddFundsOptionsComponent
import com.tangem.features.moneyaccount.details.component.MoneyAccountDetailsComponent
import com.tangem.features.tangempay.components.TangemPayBankingDetailsErrorComponent
import com.tangem.features.tokenreceive.TokenReceiveComponent
import com.tangem.features.txhistory.component.TxHistoryComponent

internal class MoneyAccountDetailsScreenComponent(
    appComponentContext: AppComponentContext,
    private val params: MoneyAccountDetailsComponent.Params,
    txHistoryComponentFactory: TxHistoryComponent.Factory,
    private val tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
    private val bankingDetailsErrorComponentFactory: TangemPayBankingDetailsErrorComponent.Factory,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: MoneyAccountDetailsModel = getOrCreateModel(params = params)

    private val txHistorySlot = childSlot(
        key = "txHistorySlot",
        source = model.txHistorySlotNavigation,
        serializer = null,
        childFactory = { currency, ctx ->
            txHistoryComponentFactory.create(
                context = childByContext(ctx),
                params = TxHistoryComponent.Params(
                    userWalletId = params.userWalletId,
                    currency = currency,
                    openExplorer = model::onExploreClick,
                ),
            )
        },
    )

    private val bottomSheetSlot = childSlot(
        key = "bottomSheetSlot",
        source = model.bottomSheetNavigation,
        serializer = MoneyAccountDetailsNavigation.serializer(),
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val txHistorySlotState by txHistorySlot.subscribeAsState()
        val bottomSheet by bottomSheetSlot.subscribeAsState()
        val txHistoryComponent = txHistorySlotState.child?.instance
        MoneyAccountDetailsScreen(
            state = state,
            txHistoryComponent = txHistoryComponent,
            modifier = modifier,
        )
        txHistoryComponent?.Content(modifier)
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        navigation: MoneyAccountDetailsNavigation,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        val context = childByContext(componentContext)
        return when (navigation) {
            is MoneyAccountDetailsNavigation.AddFundsOptions -> MoneyAccountAddFundsOptionsComponent(
                appComponentContext = context,
                params = MoneyAccountAddFundsOptionsComponent.Params(
                    userWalletId = params.userWalletId,
                    depositAddress = navigation.depositAddress,
                    listener = model,
                ),
            )
            is MoneyAccountDetailsNavigation.Receive -> tokenReceiveComponentFactory.create(
                context = context,
                params = TokenReceiveComponent.Params(
                    config = navigation.config,
                    onDismiss = model.bottomSheetNavigation::dismiss,
                ),
            )
            MoneyAccountDetailsNavigation.BankDetailsError -> bankingDetailsErrorComponentFactory.create(
                context = context,
                params = TangemPayBankingDetailsErrorComponent.Params(
                    retry = model::retryBankCredentials,
                    onDismiss = model.bottomSheetNavigation::dismiss,
                    onContactSupport = model::onBankDetailsSupportClick,
                    onResolved = model::onBankDetailsLoaded,
                ),
            )
        }
    }
}