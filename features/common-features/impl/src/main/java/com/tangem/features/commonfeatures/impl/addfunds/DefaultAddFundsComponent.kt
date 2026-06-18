package com.tangem.features.commonfeatures.impl.addfunds

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
import com.tangem.core.ui.test.AddFundsBottomSheetTestTags
import com.tangem.features.commonfeatures.api.addfunds.AddFundsComponent
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenComponent
import com.tangem.features.commonfeatures.impl.addfunds.model.AddFundsModel
import com.tangem.features.commonfeatures.impl.addfunds.model.uiSpec
import com.tangem.features.commonfeatures.impl.tokenactions.TokenActionsComponent
import com.tangem.features.commonfeatures.impl.userportfolio.UserPortfolioComponent
import com.tangem.features.wallet.featuretoggles.WalletFeatureToggles
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import com.tangem.core.ui.R as CoreR

@Suppress("LongParameterList")
internal class DefaultAddFundsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: AddFundsComponent.Params,
    chooseTokenComponentFactory: ChooseTokenComponent.Factory,
    tokenActionsComponentFactory: TokenActionsComponent.Factory,
    userPortfolioComponentFactory: UserPortfolioComponent.Factory,
    walletFeatureToggles: WalletFeatureToggles,
) : AppComponentContext by appComponentContext, AddFundsComponent {

    private val model: AddFundsModel = getOrCreateModel(params)

    private val isCompactTokenActions: Boolean = params.launchMode is AddFundsComponent.LaunchMode.TokenActionsOnly

    private val isAddFundsStage1Enabled: Boolean = walletFeatureToggles.isAddFundsStage1Enabled

    private val tokenActionsComponent: TokenActionsComponent by lazy {
        tokenActionsComponentFactory.create(
            context = child(key = "addFundsTokenActions"),
            params = TokenActionsComponent.Params(
                data = model.tokenActionsData,
                callbacks = model,
                bottomAction = model.currentBottomAction,
                isRedesignForced = true,
                isCompact = isCompactTokenActions,
            ),
        )
    }

    private val chooseTokenComponent: ChooseTokenComponent? by lazy {
        (params.launchMode as? AddFundsComponent.LaunchMode.ChooseToken)?.let {
            chooseTokenComponentFactory.create(
                context = child(key = "addFundsChooseToken"),
                params = ChooseTokenComponent.Params(
                    bridge = model.chooseTokenBridge,
                ),
            )
        }
    }

    private val userPortfolioComponent: UserPortfolioComponent by lazy {
        userPortfolioComponentFactory.create(
            context = child(key = "addFundsUserPortfolio"),
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
            if (route != AddFundsModel.UiRoute.UserPortfolio) return@LaunchedEffect
            val mode = params.launchMode as? AddFundsComponent.LaunchMode.FilteredByRawId ?: return@LaunchedEffect
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
                    is AddFundsComponent.LaunchMode.TokenActionsOnly -> TangemBottomSheetType.Modal
                    is AddFundsComponent.LaunchMode.ChooseToken -> TangemBottomSheetType.Default
                    is AddFundsComponent.LaunchMode.FilteredByRawId ->
                        if (route is AddFundsModel.UiRoute.TokenActions) {
                            TangemBottomSheetType.Default
                        } else {
                            TangemBottomSheetType.Modal
                        }
                },
                containerColor = TangemTheme.colors2.surface.level2,
                title = {
                    AddFundsBottomSheetTitle(
                        route = route,
                        canGoBack = canGoBack,
                        onBackClick = model::onBack,
                        onCloseClick = ::dismiss,
                    )
                },
                content = {
                    val animatedContentModifier =
                        if (params.launchMode is AddFundsComponent.LaunchMode.ChooseToken) {
                            Modifier.fillMaxSize()
                        } else {
                            Modifier
                        }
                    AnimatedContent(
                        targetState = route,
                        modifier = animatedContentModifier,
                        label = "AddFundsContentAnimation",
                    ) { animatedRoute ->
                        AddFundsRouteContent(
                            route = animatedRoute,
                            shouldFillHeight = !isCompactTokenActions && animatedRoute.uiSpec().shouldFillHeight,
                        )
                    }
                },
            )
        }
    }

    @Composable
    private fun AddFundsRouteContent(route: AddFundsModel.UiRoute, shouldFillHeight: Boolean) {
        val spec = route.uiSpec()
        val horizontalPadding = if (spec.shouldApplyHorizontalPadding) {
            Modifier.padding(horizontal = TangemTheme.dimens2.x4)
        } else {
            Modifier
        }
        val sizeModifier = if (shouldFillHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth()
        RenderRoute(route, horizontalPadding.then(sizeModifier))
    }

    @Composable
    private fun RenderRoute(route: AddFundsModel.UiRoute, modifier: Modifier = Modifier) {
        when (route) {
            AddFundsModel.UiRoute.Loading -> Unit
            AddFundsModel.UiRoute.ChooseToken -> chooseTokenComponent?.Content(modifier)
            AddFundsModel.UiRoute.UserPortfolio -> CompositionLocalProvider(
                LocalTangemBottomSheetContentBottomInset provides TangemTheme.dimens2.x4,
            ) {
                userPortfolioComponent.Content(modifier)
            }
            AddFundsModel.UiRoute.TokenActions -> tokenActionsComponent.Content(modifier)
        }
    }

    @Composable
    private fun AddFundsBottomSheetTitle(
        route: AddFundsModel.UiRoute,
        canGoBack: Boolean,
        onBackClick: () -> Unit,
        onCloseClick: () -> Unit,
    ) {
        TangemTopBar(
            title = route.uiSpec().title,
            type = TangemTopBarType.BottomSheet,
            startContent = if (canGoBack) {
                {
                    TangemButton(
                        iconStart = TangemIconUM.Icon(iconRes = CoreR.drawable.ic_arrow_back_28),
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
                    modifier = Modifier.testTag(AddFundsBottomSheetTestTags.CLOSE_BUTTON),
                    iconStart = TangemIconUM.Icon(iconRes = CoreR.drawable.ic_close_24),
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
    interface Factory : AddFundsComponent.Factory {
        override fun create(context: AppComponentContext, params: AddFundsComponent.Params): DefaultAddFundsComponent
    }
}