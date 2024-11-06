package com.tangem.features.onboarding.v2.multiwallet.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.jetpack.stack.Children
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerNavigation
import com.tangem.core.decompose.navigation.inner.InnerNavigationState
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.MultiWalletBackupComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.createwallet.MultiWalletCreateWalletComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletModel
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow

internal class DefaultOnboardingMultiWalletComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: OnboardingMultiWalletComponent.Params,
) : OnboardingMultiWalletComponent, AppComponentContext by context {

    private val model: OnboardingMultiWalletModel = getOrCreateModel(params)
    private val stackNavigation = StackNavigation<OnboardingMultiWalletState.Step>()

    override val innerNavigation: InnerNavigation = object : InnerNavigation {
        override val state = MutableStateFlow(
            MultiWalletInnerNavigationState(1, 5),
        )

        override fun pop(onComplete: (Boolean) -> Unit) {
            // TODO add warning dialog
            stackNavigation.pop(onComplete)
        }
    }

    private val childStack: Value<ChildStack<OnboardingMultiWalletState.Step, MultiWalletChildComponent>> = childStack(
        key = "innerStack",
        source = stackNavigation,
        serializer = null,
        initialConfiguration = model.state.value.currentStep,
        handleBackButton = true,
        childFactory = { configuration, factoryContext -> createChild(configuration, childByContext(factoryContext)) },
    )

    private fun createChild(
        step: OnboardingMultiWalletState.Step,
        childContext: AppComponentContext,
    ): MultiWalletChildComponent {
        return when (step) {
            OnboardingMultiWalletState.Step.CreateWallet -> MultiWalletCreateWalletComponent(
                context = childContext,
                params = MultiWalletChildParams(
                    multiWalletState = model.state,
                    parentParams = params,
                ),
                onDone = ::onStepDone,
            )
            OnboardingMultiWalletState.Step.AddBackupDevice -> MultiWalletBackupComponent(
                context = childContext,
                params = MultiWalletChildParams(
                    multiWalletState = model.state,
                    parentParams = params,
                ),
                onDone = ::onStepDone,
            )
            OnboardingMultiWalletState.Step.FinishBackup -> TODO()
            OnboardingMultiWalletState.Step.Done -> TODO()
        }
    }

    private fun onStepDone() {
        when (model.state.value.currentStep) {
            OnboardingMultiWalletState.Step.CreateWallet -> {
                stackNavigation.push(OnboardingMultiWalletState.Step.AddBackupDevice)
                // innerNavigation.state.value TODO
            }
            OnboardingMultiWalletState.Step.AddBackupDevice -> TODO()
            OnboardingMultiWalletState.Step.FinishBackup -> TODO()
            OnboardingMultiWalletState.Step.Done -> TODO()
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val stackState by childStack.subscribeAsState()

        Children(
            stack = stackState,
            animation = stackAnimation(slide()),
        ) {
            it.instance.Content(Modifier)
        }
    }

    @AssistedFactory
    interface Factory : OnboardingMultiWalletComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnboardingMultiWalletComponent.Params,
        ): DefaultOnboardingMultiWalletComponent
    }
}

data class MultiWalletInnerNavigationState(
    override val stackSize: Int,
    override val stackMaxSize: Int?,
) : InnerNavigationState
