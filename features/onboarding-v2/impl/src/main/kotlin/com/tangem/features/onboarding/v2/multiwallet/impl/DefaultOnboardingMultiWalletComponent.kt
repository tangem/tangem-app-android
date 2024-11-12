package com.tangem.features.onboarding.v2.multiwallet.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerNavigation
import com.tangem.core.decompose.navigation.inner.InnerNavigationState
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletModel
import com.tangem.features.onboarding.v2.multiwallet.impl.ui.OnboardingMultiWallet
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow

internal class DefaultOnboardingMultiWalletComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: OnboardingMultiWalletComponent.Params,
) : OnboardingMultiWalletComponent, AppComponentContext by context {

    private val model: OnboardingMultiWalletModel = getOrCreateModel(params)

    override val innerNavigation: InnerNavigation = object : InnerNavigation {
        override val state = MutableStateFlow(
            Wallet12InnerNavigationState(1, 5), // TODO
        )

        override fun pop(onComplete: (Boolean) -> Unit) {
            // TODO
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val uiState by model.uiState.collectAsStateWithLifecycle()

        OnboardingMultiWallet(
            modifier = modifier,
            state = uiState,
        )
    }

    @AssistedFactory
    interface Factory : OnboardingMultiWalletComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnboardingMultiWalletComponent.Params,
        ): DefaultOnboardingMultiWalletComponent
    }
}

data class Wallet12InnerNavigationState(
    override val stackSize: Int,
    override val stackMaxSize: Int?,
) : InnerNavigationState