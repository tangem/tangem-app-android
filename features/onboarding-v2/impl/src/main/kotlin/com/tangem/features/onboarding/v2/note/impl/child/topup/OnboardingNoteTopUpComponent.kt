package com.tangem.features.onboarding.v2.note.impl.child.topup

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.features.onboarding.v2.note.impl.DefaultOnboardingNoteComponent
import com.tangem.features.onboarding.v2.note.impl.child.topup.model.OnboardingNoteTopUpModel
import com.tangem.features.onboarding.v2.note.impl.child.topup.ui.OnboardingNoteTopUp
import com.tangem.features.tokenreceive.TokenReceiveComponent

internal class OnboardingNoteTopUpComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
    private val tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: OnboardingNoteTopUpModel = getOrCreateModel(params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = TokenReceiveConfig.serializer(),
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        BackHandler(onBack = remember(this) { { params.childParams.onBack() } })

        OnboardingNoteTopUp(
            modifier = modifier,
            state = state,
        )
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: TokenReceiveConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = tokenReceiveComponentFactory.create(
        context = childByContext(componentContext),
        params = TokenReceiveComponent.Params(
            config = config,
            onDismiss = model.bottomSheetNavigation::dismiss,
        ),
    )

    data class Params(
        val childParams: DefaultOnboardingNoteComponent.ChildParams,
        val onDone: () -> Unit,
    )
}