package com.tangem.features.onboarding.v2.multiwallet.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.FaultyDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetpack.stack.Children
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.router.slot.navigate
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.getOrCreateSimple
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerNavigation
import com.tangem.core.decompose.navigation.inner.InnerNavigationState
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode.MultiWalletAccessCodeComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.MultiWalletBackupComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.chooseoption.Wallet1ChooseOptionComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.createwallet.MultiWalletCreateWalletComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.MultiWalletSeedPhraseComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletModel
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState.Step.*
import com.tangem.features.onboarding.v2.multiwallet.impl.ui.OnboardingMultiWallet
import com.tangem.features.onboarding.v2.multiwallet.impl.ui.WalletArtworksState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

internal class DefaultOnboardingMultiWalletComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: OnboardingMultiWalletComponent.Params,
) : OnboardingMultiWalletComponent, AppComponentContext by context {

    private val model: OnboardingMultiWalletModel = getOrCreateModel(params)
    private val stackNavigation = StackNavigation<OnboardingMultiWalletState.Step>()
    private val artworksState = instanceKeeper.getOrCreateSimple {
        MutableStateFlow(
            when (model.state.value.currentStep) {
                CreateWallet -> WalletArtworksState.Folded
                ChooseBackupOption -> WalletArtworksState.Fan
                SeedPhrase -> WalletArtworksState.Folded
                AddBackupDevice -> WalletArtworksState.Unfold(WalletArtworksState.Unfold.Step.First)
                FinishBackup -> WalletArtworksState.Stack()
                Done -> error("Done state should not be used as starting state")
            },
        )
    }

    private val childParams = MultiWalletChildParams(
        multiWalletState = model.state,
        parentParams = params,
    )

    override val innerNavigation: InnerNavigation = object : InnerNavigation {
        override val state = MutableStateFlow(
            MultiWalletInnerNavigationState(1, 5),
        )

        override fun pop(onComplete: (Boolean) -> Unit) {
            // TODO add warning dialog
            stackNavigation.pop(onComplete)
        }
    }

    private val childStack: Value<ChildStack<OnboardingMultiWalletState.Step, MultiWalletChildComponent>> =
        childStack(
            key = "innerStack",
            source = stackNavigation,
            serializer = null,
            initialConfiguration = model.state.value.currentStep,
            handleBackButton = true,
            childFactory = { configuration, factoryContext ->
                createChild(
                    configuration,
                    childByContext(factoryContext),
                )
            },
        )

    private val bottomSheetNavigation = SlotNavigation<Unit>()
    private val accessCodeBottomSheetSlot = childSlot(
        source = bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = { configuration, componentContext ->
            MultiWalletAccessCodeComponent(
                context = childByContext(componentContext),
                params = childParams,
                onDismiss = { bottomSheetNavigation.dismiss() },
            )
        },
    )

    init {
        bottomSheetNavigation.navigate {}
    }

    private fun createChild(
        step: OnboardingMultiWalletState.Step,
        childContext: AppComponentContext,
    ): MultiWalletChildComponent {
        return when (step) {
            CreateWallet -> MultiWalletCreateWalletComponent(
                context = childContext,
                params = MultiWalletChildParams(
                    multiWalletState = model.state,
                    parentParams = params,
                ),
                onNextStep = ::handleNavigationEvent,
            )
            ChooseBackupOption -> Wallet1ChooseOptionComponent(
                context = childContext,
                onNextStep = ::handleNavigationEvent,
            )
            SeedPhrase -> MultiWalletSeedPhraseComponent(
                context = childContext,
                params = MultiWalletChildParams(
                    multiWalletState = model.state,
                    parentParams = params,
                ),
                onNextStep = ::handleNavigationEvent,
                backButtonClickFlow = MutableSharedFlow(),
            )
            AddBackupDevice -> MultiWalletBackupComponent(
                context = childContext,
                params = MultiWalletChildParams(
                    multiWalletState = model.state,
                    parentParams = params,
                ),
                onEvent = ::handleBackupComponentEvent,
            )
            FinishBackup -> TODO()
            Done -> TODO()
        }
    }

    private fun handleNavigationEvent(nextStep: OnboardingMultiWalletState.Step) {
        when (nextStep) {
            ChooseBackupOption -> {
                artworksState.value = WalletArtworksState.Fan
            }
            SeedPhrase -> {
            }
            AddBackupDevice -> {
                artworksState.value = WalletArtworksState.Unfold(WalletArtworksState.Unfold.Step.First)
            }
            FinishBackup -> {
            }
            Done -> {
                // TODO
            }
            else -> return
        }
        stackNavigation.push(nextStep)
    }

    private fun handleBackupComponentEvent(event: MultiWalletBackupComponent.Event) {
        when (event) {
            MultiWalletBackupComponent.Event.Done -> {
                // TODO navigate to finalize
            }
            MultiWalletBackupComponent.Event.OneDeviceAdded -> {
                artworksState.value = WalletArtworksState.Unfold(WalletArtworksState.Unfold.Step.Second)
            }
            MultiWalletBackupComponent.Event.TwoDeviceAdded -> {
                artworksState.value = WalletArtworksState.Unfold(WalletArtworksState.Unfold.Step.Third)
            }
        }
    }

    @OptIn(FaultyDecomposeApi::class)
    @Composable
    override fun Content(modifier: Modifier) {
        val stackState by childStack.subscribeAsState()
        val state by model.uiState.collectAsStateWithLifecycle()
        val artworksState by artworksState.collectAsStateWithLifecycle()

        OnboardingMultiWallet(
            state = state,
            modifier = modifier,
            artworksState = artworksState,
            isSeedPhraseState = stackState.active.configuration == SeedPhrase,
            childContent = { mdfr ->
                Children(
                    stack = stackState,
                    animation = stackAnimation(
                        selector = { one, two, direction ->
                            if (one.configuration == SeedPhrase || two.configuration == SeedPhrase) {
                                fade()
                            } else {
                                slide()
                            }
                        },
                    ),
                ) {
                    it.instance.Content(mdfr)
                }
            },
        )

        val bottomSheetState by accessCodeBottomSheetSlot.subscribeAsState()
        bottomSheetState.child?.instance?.BottomSheet()
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