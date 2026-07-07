package com.tangem.features.promobanners.impl.campaigns.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.LocalBottomSheetContentScrollable
import com.tangem.core.ui.components.bottomsheets.LocalTangemBottomSheetContentBottomInset
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.extensions.rememberLastNonNull
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenComponent
import com.tangem.features.promobanners.api.swapcashback.CampaignsComponent
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignsBottomSheetConfig
import com.tangem.features.promobanners.impl.campaigns.model.ActivateCampaignsModel
import com.tangem.features.promobanners.impl.campaigns.model.CampaignAlreadyActivatedModel
import com.tangem.features.promobanners.impl.campaigns.model.CampaignsModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultCampaignsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Unit,
    private val chooseTokenComponentFactory: ChooseTokenComponent.Factory,
) : CampaignsComponent, AppComponentContext by appComponentContext {

    private val model: CampaignsModel = getOrCreateModel()

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val bottomSheet by bottomSheetSlot.subscribeAsState()
        val activeChild = bottomSheet.child?.instance
        val displayedChild = rememberLastNonNull(activeChild)
        val bottomSheetState = remember { mutableStateOf(BottomSheetState.EXPANDED) }

        TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
            config = TangemBottomSheetConfig(
                isShown = activeChild != null,
                onDismissRequest = model::onDismiss,
                content = TangemBottomSheetConfigContent.Empty,
            ),
            onBack = model::onDismiss,
            title = {
                displayedChild?.Title(bottomSheetState)
            },
            content = {
                val bottomInset = LocalTangemBottomSheetContentBottomInset.current
                val scrollableSignal = LocalBottomSheetContentScrollable.current

                if (scrollableSignal != null) {
                    LaunchedEffect(Unit) {
                        scrollableSignal.value = false
                    }
                    DisposableEffect(scrollableSignal) {
                        onDispose { scrollableSignal.value = true }
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(bottom = bottomInset)
                        .animateContentSize(),
                ) {
                    displayedChild?.Content(
                        bottomSheetState = bottomSheetState,
                        contentPadding = PaddingValues(),
                        modifier = Modifier,
                    )
                }
            },
            footer = {
                Box(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(12.dp),
                ) {
                    displayedChild?.Footer()
                }
            },
        )
    }

    private fun bottomSheetChild(
        config: CampaignsBottomSheetConfig,
        componentContext: ComponentContext,
    ): CampaignsModularComponent {
        val context = childByContext(componentContext)
        return when (config) {
            CampaignsBottomSheetConfig.NotActive -> NotActiveCampaignBottomSheetComponent(
                onDismissRequest = model::onDismiss,
            )
            is CampaignsBottomSheetConfig.Enrolled -> CampaignEnrolledBottomSheetComponent(
                campaignType = config.campaignType,
                onDismissRequest = model::onDismiss,
            )
            is CampaignsBottomSheetConfig.Activate -> ActivateCampaignBottomSheetComponent(
                appComponentContext = context,
                chooseTokenComponentFactory = chooseTokenComponentFactory,
                params = ActivateCampaignsModel.Params(
                    campaignType = config.campaignType,
                    onDismiss = model::onDismiss,
                    onActivated = model::onActivated,
                    onAlreadyActivated = { campaignType, appCurrency, account, currency ->
                        model.onAlreadyActivated(
                            campaignType = campaignType,
                            appCurrency = appCurrency,
                            account = account,
                            currency = currency,
                        )
                    },
                ),
            )
            is CampaignsBottomSheetConfig.AlreadyActivated -> CampaignAlreadyActivatedBottomSheetComponent(
                appComponentContext = context,
                params = CampaignAlreadyActivatedModel.Params(
                    campaignType = config.campaignType,
                    appCurrency = config.appCurrency,
                    account = config.account,
                    currency = config.currency,
                    onDismiss = model::onDismiss,
                ),
            )
        }
    }

    @AssistedFactory
    interface Factory : CampaignsComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultCampaignsComponent
    }
}