package com.tangem.features.onboarding.v2.visa.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.navigation.inner.InnerNavigation
import com.tangem.core.decompose.navigation.inner.InnerNavigationState
import com.tangem.features.onboarding.v2.visa.api.OnboardingVisaComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Suppress("UnusedPrivateMember")
class DefaultOnboardingVisaComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: OnboardingVisaComponent.Params,
) : OnboardingVisaComponent, AppComponentContext by appComponentContext {

    private val innerNavigationState = MutableStateFlow(
        OnboardingVisaInnerNavigationState(
            stackSize = 1,
            stackMaxSize = 7,
        ),
    )

    private val currentChildBackEventHandle = MutableSharedFlow<Unit>(onBufferOverflow = BufferOverflow.DROP_LATEST)

    override val innerNavigation: InnerNavigation = object : InnerNavigation {
        override val state: StateFlow<InnerNavigationState> = innerNavigationState

        override fun pop(onComplete: (Boolean) -> Unit) {
            // TODO add stack handling
            componentScope.launch { currentChildBackEventHandle.emit(Unit) }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        TODO("Not yet implemented")
    }

    @AssistedFactory
    interface Factory : OnboardingVisaComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnboardingVisaComponent.Params,
        ): DefaultOnboardingVisaComponent
    }
}

data class OnboardingVisaInnerNavigationState(
    override val stackSize: Int,
    override val stackMaxSize: Int?,
) : InnerNavigationState