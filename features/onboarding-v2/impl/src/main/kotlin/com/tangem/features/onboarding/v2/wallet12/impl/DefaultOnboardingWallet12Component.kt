package com.tangem.features.onboarding.v2.wallet12.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.navigation.inner.InnerNavigation
import com.tangem.core.decompose.navigation.inner.InnerNavigationState
import com.tangem.features.onboarding.v2.wallet12.api.OnboardingWallet12Component
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow

@Suppress("UnusedPrivateMember")
class DefaultOnboardingWallet12Component @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: OnboardingWallet12Component.Params,
) : OnboardingWallet12Component, AppComponentContext by context {

    override val innerNavigation: InnerNavigation = object : InnerNavigation {
        override val state = MutableStateFlow(
            Wallet12InnerNavigationState(0, null), // TODO
        )

        override fun pop(onComplete: (Boolean) -> Unit) {
            // TODO
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
    }

    @AssistedFactory
    interface Factory : OnboardingWallet12Component.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnboardingWallet12Component.Params,
        ): DefaultOnboardingWallet12Component
    }
}

data class Wallet12InnerNavigationState(
    override val stackSize: Int,
    override val stackMaxSize: Int?,
) : InnerNavigationState
