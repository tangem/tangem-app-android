package com.tangem.features.onboarding.v2.twin.impl

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerNavigation
import com.tangem.core.decompose.navigation.inner.InnerNavigationHolder
import com.tangem.core.decompose.navigation.inner.InnerNavigationState
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.twin.api.OnboardingTwinComponent
import com.tangem.features.onboarding.v2.twin.impl.model.OnboardingTwinModel
import com.tangem.features.onboarding.v2.twin.impl.ui.OnboardingTwin
import com.tangem.features.tokenreceive.TokenReceiveComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow

internal class DefaultOnboardingTwinComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: OnboardingTwinComponent.Params,
    private val tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
) : OnboardingTwinComponent, AppComponentContext by appComponentContext, InnerNavigationHolder {

    private val model: OnboardingTwinModel = getOrCreateModel(params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = TokenReceiveConfig.serializer(),
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    init {
        params.titleProvider.changeTitle(resourceReference(R.string.twins_recreate_toolbar))
    }

    override val innerNavigation: InnerNavigation = object : InnerNavigation {
        override val state: StateFlow<TwinInnerNavigationState> = model.innerNavigationState

        override fun pop(onComplete: (Boolean) -> Unit) {
            model.onBack()
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler { model.onBack() }

        val bottomSheet by bottomSheetSlot.subscribeAsState()
        val state by model.uiState.collectAsStateWithLifecycle()

        OnboardingTwin(
            state = state,
            modifier = modifier,
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

    data class TwinInnerNavigationState(
        override val stackSize: Int,
    ) : InnerNavigationState {
        override val stackMaxSize: Int? = 5
    }

    @AssistedFactory
    interface Factory : OnboardingTwinComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnboardingTwinComponent.Params,
        ): DefaultOnboardingTwinComponent
    }
}