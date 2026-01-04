package com.tangem.feature.wallet.child.wallet

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.essenty.lifecycle.doOnResume
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.decompose.ComposableDialogComponent
import com.tangem.domain.tokens.model.details.TokenAction
import com.tangem.feature.wallet.child.wallet.model.WalletModel
import com.tangem.feature.wallet.navigation.WalletRoute
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletDialogConfig
import com.tangem.feature.wallet.presentation.wallet.ui.WalletScreen
import com.tangem.feature.walletsettings.component.RenameWalletComponent
import com.tangem.features.biometry.AskBiometryComponent
import com.tangem.features.feed.entry.components.FeedEntryComponent
import com.tangem.features.feed.entry.featuretoggle.FeedFeatureToggle
import com.tangem.features.markets.entry.MarketsEntryComponent
import com.tangem.features.pushnotifications.api.PushNotificationsBottomSheetComponent
import com.tangem.features.pushnotifications.api.PushNotificationsParams
import com.tangem.features.tokenreceive.TokenReceiveComponent
import com.tangem.features.yield.supply.api.YieldSupplyDepositedWarningComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
internal class WalletComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted navigate: (WalletRoute) -> Unit,
    marketsEntryComponentFactory: MarketsEntryComponent.Factory,
    feedEntryComponentFactory: FeedEntryComponent.Factory,
    private val renameWalletComponentFactory: RenameWalletComponent.Factory,
    private val askBiometryComponentFactory: AskBiometryComponent.Factory,
    private val pushNotificationsBottomSheetComponent: PushNotificationsBottomSheetComponent.Factory,
    private val tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
    private val yieldSupplyDepositedWarningComponent: YieldSupplyDepositedWarningComponent.Factory,
    private val feedFeatureToggle: FeedFeatureToggle,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: WalletModel = getOrCreateModel()

    private val feedEntryComponent by lazy {
        feedEntryComponentFactory.create(
            context = child("feedEntryComponent"),
            entryRoute = null,
        )
    }
    private val marketsEntryComponent by lazy {
        marketsEntryComponentFactory.create(child("marketsEntryComponent"))
    }

    init {
        lifecycle.subscribe(model.screenLifecycleProvider)
        doOnResume { model.onResume() }
        componentScope.launch { model.innerWalletRouter.navigateToFlow.collect { navigate(it) } }
    }

    private val dialog = childSlot(
        source = model.innerWalletRouter.dialogNavigation,
        serializer = WalletDialogConfig.serializer(),
        handleBackButton = true,
        childFactory = { dialogConfig, componentContext ->
            when (dialogConfig) {
                is WalletDialogConfig.RenameWallet -> {
                    renameWalletComponentFactory.create(
                        context = childByContext(componentContext),
                        params = RenameWalletComponent.Params(
                            userWalletId = dialogConfig.userWalletId,
                            currentName = dialogConfig.currentName,
                            onDismiss = model.innerWalletRouter.dialogNavigation::dismiss,
                        ),
                    )
                }
                is WalletDialogConfig.AskForBiometry -> {
                    askBiometryComponentFactory.create(
                        context = childByContext(componentContext),
                        params = AskBiometryComponent.Params(
                            isBottomSheetVariant = true,
                            modelCallbacks = model.askBiometryModelCallbacks,
                        ),
                    )
                }
                WalletDialogConfig.AskForPushNotifications -> pushNotificationsBottomSheetComponent.create(
                    context = childByContext(componentContext),
                    params = PushNotificationsParams(
                        isBottomSheet = true,
                        modelCallbacks = model.askForPushNotificationsModelCallbacks,
                        source = AppRoute.PushNotification.Source.Main,
                    ),
                )
                is WalletDialogConfig.TokenReceive -> {
                    tokenReceiveComponentFactory.create(
                        context = childByContext(componentContext),
                        params = TokenReceiveComponent.Params(
                            config = dialogConfig.tokenReceiveConfig,
                            onDismiss = model.innerWalletRouter.dialogNavigation::dismiss,
                        ),
                    )
                }
                is WalletDialogConfig.YieldSupplyWarning -> {
                    yieldSupplyDepositedWarningComponent.create(
                        context = childByContext(componentContext),
                        params = YieldSupplyDepositedWarningComponent.Params(
                            cryptoCurrency = dialogConfig.cryptoCurrency,
                            onDismiss = model.innerWalletRouter.dialogNavigation::dismiss,
                            tokenAction = dialogConfig.tokenAction,
                            modelCallback = object : YieldSupplyDepositedWarningComponent.ModelCallback {
                                override fun onYieldSupplyWarningAcknowledged(tokenAction: TokenAction) {
                                    dialogConfig.onWarningAcknowledged(tokenAction)
                                }
                            },
                        ),
                    )
                }
            }
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val bottomSheetState = remember { mutableStateOf(BottomSheetState.COLLAPSED) }
        var headerSize by remember { mutableStateOf(0.dp) }
        val dialog by dialog.subscribeAsState()

        WalletScreen(
            state = model.uiState.collectAsStateWithLifecycle().value,
            bottomSheetContent = {
                BottomSheetContent(
                    bottomSheetState = bottomSheetState,
                    onHeaderSizeChange = { headerSize = it },
                    modifier = modifier,
                )
            },
            bottomSheetHeaderHeightProvider = { headerSize },
            onBottomSheetStateChange = { bottomSheetState.value = it },
        )

        when (val dialog = dialog.child?.instance) {
            is ComposableDialogComponent -> dialog.Dialog()
            is ComposableBottomSheetComponent -> dialog.BottomSheet()
            else -> {}
        }
    }

    @Composable
    private fun BottomSheetContent(
        bottomSheetState: State<BottomSheetState>,
        onHeaderSizeChange: (Dp) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        if (feedFeatureToggle.isFeedEnabled) {
            feedEntryComponent.BottomSheetContent(
                bottomSheetState = bottomSheetState,
                onHeaderSizeChange = onHeaderSizeChange,
                modifier = modifier,
            )
        } else {
            marketsEntryComponent.BottomSheetContent(
                bottomSheetState = bottomSheetState,
                onHeaderSizeChange = onHeaderSizeChange,
                modifier = modifier,
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(appComponentContext: AppComponentContext, navigate: (WalletRoute) -> Unit): WalletComponent
    }
}