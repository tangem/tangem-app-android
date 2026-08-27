package com.tangem.features.tangempay.card.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.essenty.lifecycle.doOnPause
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.NavigationBar3ButtonsScrim
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.addfunds.TangemPayAddFundsComponent
import com.tangem.features.tangempay.addfunds.va.bank.TangemPayVaBankingDetailsErrorComponent
import com.tangem.features.tangempay.addfunds.va.bank.VA_DAILY_DEPOSIT_LIMIT_PLACEHOLDER
import com.tangem.features.tangempay.addfunds.va.bank.toRequisitesRows
import com.tangem.features.tangempay.addfunds.va.deposit.TangemPayVirtualAccountDepositComponent
import com.tangem.features.tangempay.card.closure.TangemPayCloseCardComponent
import com.tangem.features.tangempay.card.pin.TangemPayViewPinComponent
import com.tangem.features.tangempay.card.reissue.TangemPayReissueCardComponent
import com.tangem.features.tangempay.common.userWalletId
import com.tangem.features.tangempay.multichain.choosenetwork.PaymentChooseNetworkComponent
import com.tangem.features.tangempay.multichain.othernetworks.PaymentOtherNetworksComponent
import com.tangem.features.tangempay.multichain.receive.PaymentReceiveComponent
import com.tangem.features.tokenreceive.TokenReceiveComponent
import com.tangem.features.virtualaccount.details.component.VirtualAccountAddFundsBottomSheetComponent
import com.tangem.features.virtualaccount.details.component.VirtualAccountAddFundsListener

internal class TangemPayCardPageScreenComponent(
    private val appComponentContext: AppComponentContext,
    private val params: TangemPayCardPageComponent.Params,
    private val tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
    private val virtualAccountAddFundsComponentFactory: VirtualAccountAddFundsBottomSheetComponent.Factory,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: TangemPayCardPageModel = getOrCreateModel(params = params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = TangemPayCardNavigation.serializer(),
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    init {
        lifecycle.doOnPause {
            if (bottomSheetSlot.value.child?.configuration is TangemPayCardNavigation.ViewPinCode) {
                model.bottomSheetNavigation.dismiss()
            }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val cardControllers by model.cardControllersState.collectAsStateWithLifecycle()
        val selectedCardId by model.selectedCardId.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        NavigationBar3ButtonsScrim()
        TangemPayCardPageScreen(
            state = state,
            cardControllers = cardControllers,
            selectedCardId = selectedCardId,
            onCardSelect = model::onCardPageSelected,
            modifier = modifier,
        )
        bottomSheet.child?.instance?.BottomSheet()
    }

    @Suppress("LongMethod")
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
                    userWalletId = params.initialStatus.userWalletId,
                    cardId = navigation.cardId,
                ),
            )
            is TangemPayCardNavigation.CloseCard -> TangemPayCloseCardComponent(
                appComponentContext = context,
                params = TangemPayCloseCardComponent.Params(
                    listener = model,
                    userWalletId = navigation.userWalletId,
                    cardId = navigation.cardId,
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
                    cryptoCurrency = navigation.cryptoCurrency,
                    virtualAccountOnramp = navigation.virtualAccountOnramp,
                ),
            )
            is TangemPayCardNavigation.VirtualAccountDeposit -> TangemPayVirtualAccountDepositComponent(
                appComponentContext = context,
                params = TangemPayVirtualAccountDepositComponent.Params(
                    virtualAccountOnramp = navigation.virtualAccountOnramp,
                    userWalletId = navigation.userWalletId,
                    paymentAccountAddress = navigation.paymentAccountAddress,
                    onDismiss = model.bottomSheetNavigation::dismiss,
                    onShowDetails = model::onShowVirtualAccountRequisites,
                    onShowBankingDetailsError = model::showVaBankingDetailsError,
                    onOrderCreated = model::onVirtualAccountOrderCreated,
                ),
            )
            is TangemPayCardNavigation.VirtualAccountRequisites -> virtualAccountAddFundsComponentFactory.create(
                context = context,
                params = VirtualAccountAddFundsBottomSheetComponent.Params(
                    userWalletId = navigation.userWalletId,
                    requisites = navigation.bankCredentials.toRequisitesRows(),
                    dailyDepositLimit = VA_DAILY_DEPOSIT_LIMIT_PLACEHOLDER,
                    shouldSkipIntro = true,
                    listener = VirtualAccountAddFundsListener { model.bottomSheetNavigation.dismiss() },
                    onDetailsShown = model::onVaBankingDetailsShown,
                    onShareClicked = model::onVaShareDetailsClicked,
                    onFieldCopied = model::onVaFieldCopied,
                ),
            )
            is TangemPayCardNavigation.VaBankingDetailsError -> TangemPayVaBankingDetailsErrorComponent(
                appComponentContext = context,
                params = TangemPayVaBankingDetailsErrorComponent.Params(
                    userWalletId = navigation.userWalletId,
                    productInstanceId = navigation.productInstanceId,
                    onDismiss = model.bottomSheetNavigation::dismiss,
                    onContactSupport = model::onContactSupportClicked,
                    onResolved = model::onVaBankingDetailsResolved,
                ),
            )
            is TangemPayCardNavigation.Receive -> tokenReceiveComponentFactory.create(
                context = context,
                params = TokenReceiveComponent.Params(
                    config = navigation.config,
                    onDismiss = model.bottomSheetNavigation::dismiss,
                ),
            )
            is TangemPayCardNavigation.ChooseNetwork -> PaymentChooseNetworkComponent(
                appComponentContext = context,
                params = PaymentChooseNetworkComponent.Params(
                    walletId = navigation.walletId,
                    listener = model,
                ),
            )
            is TangemPayCardNavigation.OtherNetworks -> PaymentOtherNetworksComponent(
                appComponentContext = context,
                params = PaymentOtherNetworksComponent.Params(
                    onDismiss = model.bottomSheetNavigation::dismiss,
                ),
            )
            is TangemPayCardNavigation.PaymentReceive -> PaymentReceiveComponent(
                appComponentContext = context,
                params = PaymentReceiveComponent.Params(
                    walletId = navigation.walletId,
                    networkRawId = navigation.networkRawId,
                    onDismiss = model.bottomSheetNavigation::dismiss,
                ),
            )
        }
    }
}