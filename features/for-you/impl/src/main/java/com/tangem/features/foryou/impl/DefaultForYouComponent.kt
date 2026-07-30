package com.tangem.features.foryou.impl

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_left_20
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioComponent
import com.tangem.features.commonfeatures.api.managefunds.ManageFundsComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.foryou.ForYouComponent
import com.tangem.features.foryou.impl.entity.ForYouBottomSheetConfig
import com.tangem.features.foryou.impl.model.ForYouModel
import com.tangem.features.foryou.impl.ui.ForYouContent
import com.tangem.features.promobanners.api.PromoBannersBlockComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultForYouComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: ForYouComponent.Params,
    private val promoBannersBlockComponentFactory: PromoBannersBlockComponent.Factory,
    private val addToPortfolioComponentFactory: AddToPortfolioComponent.Factory,
    private val manageFundsComponentFactory: ManageFundsComponent.Factory,
    private val portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
) : AppComponentContext by context, ForYouComponent {

    private val model: ForYouModel = getOrCreateModel(params = params)

    private val promoBannersBlockComponent: PromoBannersBlockComponent by lazy {
        promoBannersBlockComponentFactory.create(
            context = child("promoBannersBlockComponent"),
            params = PromoBannersBlockComponent.Params(
                placeholder = PromoBannersBlockComponent.Placeholder.FEED,
                isInitiallyVisibleOnScreen = false,
            ),
        )
    }

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = { config, componentContext ->
            when (config) {
                ForYouBottomSheetConfig.AddToPortfolio -> addToPortfolioChild(componentContext)
                ForYouBottomSheetConfig.PortfolioSelector -> portfolioSelectorChild(componentContext)
                is ForYouBottomSheetConfig.ManageFunds -> manageFundsChild(
                    componentContext = componentContext,
                    launchMode = ManageFundsComponent.LaunchMode.FilteredByRawId(config.rawCurrencyId),
                )
                is ForYouBottomSheetConfig.AddFunds -> manageFundsChild(
                    componentContext = componentContext,
                    launchMode = ManageFundsComponent.LaunchMode.ChooseToken(config.userWalletId),
                )
            }
        },
    )

    @Composable
    override fun Title(bottomSheetState: State<BottomSheetState>) {
        TangemTopBar(
            title = resourceReference(R.string.for_you_title),
            type = TangemTopBarType.BottomSheet,
            startContent = {
                Icon(
                    imageVector = Icons.ic_chevron_left_20,
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.primary,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .hazeEffectTangem { blurRadius = 8.dp }
                        .clickableSingle(
                            onClick = router::pop,
                            enabled = bottomSheetState.value == BottomSheetState.EXPANDED,
                        )
                        .padding(8.dp),
                )
            },
        )
    }

    @Composable
    override fun Content(
        bottomSheetState: State<BottomSheetState>,
        contentPadding: PaddingValues,
        modifier: Modifier,
    ) {
        val uiState by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        ForYouContent(
            forYouUM = uiState,
            bottomSheetState = bottomSheetState,
            promoBannersBlockComponent = promoBannersBlockComponent,
            contentPadding = contentPadding,
            modifier = modifier.navigationBarsPadding(),
        )

        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun addToPortfolioChild(componentContext: ComponentContext): ComposableBottomSheetComponent =
        addToPortfolioComponentFactory.create(
            context = childByContext(componentContext),
            params = AddToPortfolioComponent.Params(
                addToPortfolioManager = checkNotNull(model.addToPortfolioManager) {
                    "addToPortfolioManager must be set before activating AddToPortfolio slot"
                },
            ),
        )

    private fun portfolioSelectorChild(componentContext: ComponentContext): ComposableBottomSheetComponent =
        portfolioSelectorComponentFactory.create(
            context = childByContext(componentContext),
            params = PortfolioSelectorComponent.Params(
                portfolioFetcher = model.portfolioFetcher,
                controller = model.portfolioSelectorController,
                bsCallback = model.portfolioSelectorCallback,
                settings = PortfolioSelectorComponent.Settings(isMultiChoice = true),
            ),
        )

    private fun manageFundsChild(
        launchMode: ManageFundsComponent.LaunchMode,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = manageFundsComponentFactory.create(
        context = childByContext(componentContext),
        params = ManageFundsComponent.Params(
            launchMode = launchMode,
            onDismiss = { model.bottomSheetNavigation.dismiss() },
        ),
    )

    @AssistedFactory
    interface Factory : ForYouComponent.Factory {
        override fun create(context: AppComponentContext, params: ForYouComponent.Params): DefaultForYouComponent
    }
}