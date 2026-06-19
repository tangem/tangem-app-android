package com.tangem.features.commonfeatures.impl.managefunds

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.*
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import com.tangem.features.commonfeatures.api.managefunds.ManageFundsComponent
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenComponent
import com.tangem.features.commonfeatures.impl.managefunds.model.ManageFundsModel
import com.tangem.features.commonfeatures.impl.managefunds.model.uiSpec
import com.tangem.common.ui.markets.action.TokenActionsContext
import com.tangem.features.commonfeatures.impl.tokenactions.TokenActionsComponent
import com.tangem.features.commonfeatures.impl.userportfolio.UserPortfolioComponent
import com.tangem.features.commonfeatures.impl.R
import com.tangem.features.wallet.featuretoggles.WalletFeatureToggles
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("LongParameterList")
internal class DefaultManageFundsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: ManageFundsComponent.Params,
    chooseTokenComponentFactory: ChooseTokenComponent.Factory,
    tokenActionsComponentFactory: TokenActionsComponent.Factory,
    userPortfolioComponentFactory: UserPortfolioComponent.Factory,
    walletFeatureToggles: WalletFeatureToggles,
) : AppComponentContext by appComponentContext, ManageFundsComponent {

    private val model: ManageFundsModel = getOrCreateModel(params)

    private val isCompactTokenActions: Boolean = params.launchMode is ManageFundsComponent.LaunchMode.TokenActionsOnly

    private val isAddFundsStage1Enabled: Boolean = walletFeatureToggles.isAddFundsStage1Enabled

    private val tokenActionsComponent: TokenActionsComponent by lazy {
        tokenActionsComponentFactory.create(
            context = child(key = "manageFundsTokenActions"),
            params = TokenActionsComponent.Params(
                data = model.tokenActionsData,
                callbacks = model,
                bottomAction = model.currentBottomAction,
                isRedesignForced = true,
                isCompact = isCompactTokenActions,
                context = when (model.flowType) {
                    ManageFundsComponent.FlowType.AddFunds -> TokenActionsContext.AddFunds
                    ManageFundsComponent.FlowType.Transfer -> TokenActionsContext.Transfer
                },
            ),
        )
    }

    private val chooseTokenComponent: ChooseTokenComponent? by lazy {
        (params.launchMode as? ManageFundsComponent.LaunchMode.ChooseToken)?.let {
            chooseTokenComponentFactory.create(
                context = child(key = "manageFundsChooseToken"),
                params = ChooseTokenComponent.Params(
                    bridge = model.chooseTokenBridge,
                ),
            )
        }
    }

    private val userPortfolioComponent: UserPortfolioComponent by lazy {
        userPortfolioComponentFactory.create(
            context = child(key = "manageFundsUserPortfolio"),
            params = UserPortfolioComponent.Params(
                uiState = model.userPortfolioStateController.uiState,
                callbacks = object : UserPortfolioComponent.Callbacks {
                    override fun onContinueFromUserPortfolio() = Unit
                },
            ),
        )
    }

    override fun dismiss() = model.onDismiss()

    @Composable
    override fun BottomSheet() {
        val route by model.uiRoute.collectAsStateWithLifecycle()
        val canGoBack by model.canGoBack.collectAsStateWithLifecycle()

        LaunchedEffect(route) {
            if (route != ManageFundsModel.UiRoute.UserPortfolio) return@LaunchedEffect
            val mode = params.launchMode as? ManageFundsComponent.LaunchMode.FilteredByRawId ?: return@LaunchedEffect
            model.userPortfolioStateController.updateAndWaitNotNullState(
                allAvailableData = model.buildAvailableToAddDataForChooser(),
                rawCurrencyId = mode.rawCurrencyId,
            )
        }

        WithOptionalRedesignTheme(isEnabled = isAddFundsStage1Enabled) {
            TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
                onBack = if (canGoBack) model::onBack else ::dismiss,
                config = TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = ::dismiss,
                    content = TangemBottomSheetConfigContent.Empty,
                ),
                type = when (params.launchMode) {
                    is ManageFundsComponent.LaunchMode.TokenActionsOnly -> TangemBottomSheetType.Modal
                    is ManageFundsComponent.LaunchMode.ChooseToken -> TangemBottomSheetType.Default
                    is ManageFundsComponent.LaunchMode.FilteredByRawId ->
                        if (route is ManageFundsModel.UiRoute.TokenActions) {
                            TangemBottomSheetType.Default
                        } else {
                            TangemBottomSheetType.Modal
                        }
                },
                containerColor = TangemTheme.colors2.surface.level2,
                title = {
                    ManageFundsBottomSheetTitle(
                        route = route,
                        canGoBack = canGoBack,
                        onBackClick = model::onBack,
                        onCloseClick = ::dismiss,
                    )
                },
                content = {
                    val animatedContentModifier =
                        if (params.launchMode is ManageFundsComponent.LaunchMode.ChooseToken) {
                            Modifier.fillMaxSize()
                        } else {
                            Modifier
                        }
                    AnimatedContent(
                        targetState = route,
                        modifier = animatedContentModifier,
                        label = "ManageFundsContentAnimation",
                    ) { animatedRoute ->
                        ManageFundsRouteContent(
                            route = animatedRoute,
                            shouldFillHeight = !isCompactTokenActions &&
                                animatedRoute.uiSpec(model.flowType).shouldFillHeight,
                        )
                    }
                },
            )
        }
    }

    @Composable
    private fun ManageFundsRouteContent(route: ManageFundsModel.UiRoute, shouldFillHeight: Boolean) {
        val spec = route.uiSpec(model.flowType)
        val horizontalPadding = if (spec.shouldApplyHorizontalPadding) {
            Modifier.padding(horizontal = TangemTheme.dimens2.x4)
        } else {
            Modifier
        }
        val sizeModifier = if (shouldFillHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth()
        RenderRoute(route, horizontalPadding.then(sizeModifier))
    }

    @Composable
    private fun RenderRoute(route: ManageFundsModel.UiRoute, modifier: Modifier = Modifier) {
        when (route) {
            ManageFundsModel.UiRoute.Loading -> Unit
            ManageFundsModel.UiRoute.ChooseToken -> chooseTokenComponent?.Content(modifier)
            ManageFundsModel.UiRoute.UserPortfolio -> CompositionLocalProvider(
                LocalTangemBottomSheetContentBottomInset provides TangemTheme.dimens2.x4,
            ) {
                userPortfolioComponent.Content(modifier)
            }
            ManageFundsModel.UiRoute.TokenActions -> tokenActionsComponent.Content(modifier)
        }
    }

    @Composable
    private fun ManageFundsBottomSheetTitle(
        route: ManageFundsModel.UiRoute,
        canGoBack: Boolean,
        onBackClick: () -> Unit,
        onCloseClick: () -> Unit,
    ) {
        TangemTopBar(
            title = route.uiSpec(model.flowType).title,
            type = TangemTopBarType.BottomSheet,
            startContent = if (canGoBack) {
                {
                    TangemButton(
                        iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_arrow_back_28),
                        onClick = onBackClick,
                        size = TangemButton.Size.X11,
                        variant = TangemButton.Variant.Material,
                    )
                }
            } else {
                null
            },
            endContent = {
                TangemButton(
                    modifier = Modifier.testTag(BaseBottomSheetTestTags.CLOSE_BUTTON),
                    iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_close_24),
                    onClick = onCloseClick,
                    size = TangemButton.Size.X11,
                    variant = TangemButton.Variant.Material,
                )
            },
        )
    }

    @Composable
    private fun WithOptionalRedesignTheme(isEnabled: Boolean, content: @Composable () -> Unit) {
        if (isEnabled) {
            TangemThemeRedesign(content = content)
        } else {
            content()
        }
    }

    @AssistedFactory
    interface Factory : ManageFundsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: ManageFundsComponent.Params,
        ): DefaultManageFundsComponent
    }
}