package com.tangem.features.promobanners.impl.campaigns.v2.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemAnimations
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.promo.models.PromoCampaign
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenComponent
import com.tangem.features.promobanners.impl.R
import com.tangem.features.promobanners.impl.campaigns.v2.model.PromoCampaignModel
import com.tangem.features.promobanners.impl.campaigns.v2.ui.PromoCampaignContent

/**
 * Backend-driven campaign sheet, rendered from the already fetched [Params.campaign] without another request.
 * Choosing the payout token is a step of the same modal ([PromoCampaignRoute]), not a second sheet.
 */
internal class PromoCampaignBottomSheetComponent(
    appComponentContext: AppComponentContext,
    private val chooseTokenComponentFactory: ChooseTokenComponent.Factory,
    private val params: Params,
    private val onDismiss: () -> Unit,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<PromoCampaignRoute>()

    private val innerRouter = InnerRouter<PromoCampaignRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val model: PromoCampaignModel = getOrCreateModel(params = params, router = innerRouter)

    private val innerStack = childStack(
        key = "promoCampaignStack",
        source = stackNavigation,
        serializer = null,
        initialConfiguration = PromoCampaignRoute.Campaign,
        handleBackButton = false,
        childFactory = { route, factoryContext ->
            createChild(route, childByContext(componentContext = factoryContext, router = innerRouter))
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val stack by innerStack.subscribeAsState()
        val isCampaignStep = stack.active.configuration == PromoCampaignRoute.Campaign

        var topBarHeight by remember { mutableIntStateOf(0) }

        TangemModal<TangemBottomSheetConfigContent.Empty>(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = onDismiss,
                content = TangemBottomSheetConfigContent.Empty,
            ),
            chromePlacement = TangemModal.ChromePlacement.Overlay,
            presentation = TangemModal.Presentation.Overlay,
            onBack = ::onChildBack,
            scrollableContent = false,
            containerColor = TangemTheme.colors3.bg.primary,
            title = {
                TangemModal.TopNavigation(
                    title = if (isCampaignStep) {
                        TextReference.EMPTY
                    } else {
                        resourceReference(R.string.choose_asset_title)
                    },
                    onClose = onDismiss,
                    modifier = Modifier.onGloballyPositioned { layout ->
                        topBarHeight = layout.size.height
                    },
                )
            },
        ) {
            AnimatedContent(
                targetState = stack.active,
                contentKey = { child -> child.configuration },
                modifier = Modifier.weight(weight = 1f, fill = false),
                label = "PromoCampaignStep",
                transitionSpec = TangemAnimations.AnimatedContent.slide { from, to ->
                    from.configuration == PromoCampaignRoute.Campaign &&
                        to.configuration == PromoCampaignRoute.ChooseToken
                },
            ) { child ->
                when (child.configuration) {
                    PromoCampaignRoute.Campaign -> child.instance.Content(modifier = Modifier)
                    PromoCampaignRoute.ChooseToken -> child.instance.Content(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(LocalWindowInfo.current.containerSize.height.dp)
                            .padding(top = 72.dp),
                    )
                }
            }
        }
    }

    private fun createChild(route: PromoCampaignRoute, context: AppComponentContext): ComposableContentComponent {
        return when (route) {
            PromoCampaignRoute.Campaign -> CampaignStep(model)
            PromoCampaignRoute.ChooseToken -> chooseTokenComponentFactory.create(
                context = context,
                params = ChooseTokenComponent.Params(bridge = model.bridge),
            )
        }
    }

    private fun onChildBack() {
        if (innerStack.value.backStack.isEmpty()) {
            onDismiss()
        } else {
            stackNavigation.pop()
        }
    }

    private class CampaignStep(private val model: PromoCampaignModel) : ComposableContentComponent {

        @Composable
        override fun Content(modifier: Modifier) {
            val state by model.uiState.collectAsStateWithLifecycle()
            PromoCampaignContent(state = state, modifier = modifier)
        }
    }

    data class Params(
        val campaign: PromoCampaign,
    )
}