package com.tangem.features.onboarding.v2.multiwallet.impl

import androidx.activity.compose.BackHandler
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
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerNavigation
import com.tangem.core.decompose.navigation.inner.InnerNavigationState
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.analytics.OnboardingEvent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode.MultiWalletAccessCodeComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.MultiWalletBackupComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.chooseoption.Wallet1ChooseOptionComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.createwallet.MultiWalletCreateWalletComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.finalize.MultiWalletFinalizeComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.MultiWalletSeedPhraseComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletModel
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState.Step.*
import com.tangem.features.onboarding.v2.multiwallet.impl.ui.OnboardingMultiWallet
import com.tangem.features.onboarding.v2.multiwallet.impl.ui.WalletArtworksState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal class DefaultOnboardingMultiWalletComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: OnboardingMultiWalletComponent.Params,
    private val analyticsHandler: AnalyticsEventHandler,
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
                Finalize -> WalletArtworksState.Stack()
                Done -> error("Done state should not be used as starting state")
            },
        )
    }

    private val innerNavigationStateFlow = MutableStateFlow(
        MultiWalletInnerNavigationState(1, 5),
    )

    private val childParams = MultiWalletChildParams(
        multiWalletState = model.state,
        parentParams = params,
        innerNavigation = innerNavigationStateFlow,
        backups = model.backups,
    )

    private val childStack: Value<ChildStack<OnboardingMultiWalletState.Step, ComposableContentComponent>> =
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

    val backButtonClickFlow = MutableSharedFlow<Unit>()
    override val innerNavigation: InnerNavigation = object : InnerNavigation {
        override val state = innerNavigationStateFlow

        override fun pop(onComplete: (Boolean) -> Unit) {
            if (childStack.active.configuration == SeedPhrase) {
                componentScope.launch { backButtonClickFlow.emit(Unit) }
            } else {
                model.onBack()
            }
        }
    }

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
        componentScope.launch {
            // access code bs result
            childParams.multiWalletState
                .map { it.accessCode }
                .distinctUntilChanged()
                .filter { it != null }
                .collectLatest { handleNavigationEvent(Finalize) }
        }
    }

    private fun createChild(
        step: OnboardingMultiWalletState.Step,
        childContext: AppComponentContext,
    ): ComposableContentComponent {
        return when (step) {
            CreateWallet -> MultiWalletCreateWalletComponent(
                context = childContext,
                params = childParams,
                onNextStep = ::handleNavigationEvent,
            )
            ChooseBackupOption -> Wallet1ChooseOptionComponent(
                context = childContext,
                params = childParams,
                onNextStep = ::handleNavigationEvent,
            )
            SeedPhrase -> MultiWalletSeedPhraseComponent(
                context = childContext,
                params = childParams,
                onNextStep = ::handleNavigationEvent,
                backButtonClickFlow = backButtonClickFlow,
                onBack = { stackNavigation.pop() },
            )
            AddBackupDevice -> MultiWalletBackupComponent(
                context = childContext,
                params = childParams,
                onEvent = ::handleBackupComponentEvent,
            )
            Finalize -> MultiWalletFinalizeComponent(
                context = childContext,
                params = childParams,
                onEvent = ::handleFinalizeComponentEvent,
            )
            Done -> error("Unexpected Done state")
        }
    }

    private fun handleNavigationEvent(nextStep: OnboardingMultiWalletState.Step) {
        when (nextStep) {
            ChooseBackupOption -> {
                artworksState.value = WalletArtworksState.Fan
                stackNavigation.navigate { listOf(nextStep) }
            }
            AddBackupDevice -> {
                artworksState.value = WalletArtworksState.Unfold(WalletArtworksState.Unfold.Step.First)
                stackNavigation.navigate { listOf(nextStep) }
            }
            Finalize -> {
                artworksState.value = WalletArtworksState.Stack(threeCards = model.state.value.isThreeCards)
                stackNavigation.navigate { listOf(nextStep) }
            }
            SeedPhrase -> {
                stackNavigation.push(nextStep)
            }
            Done -> {
                // final step - navigate to parent
                analyticsHandler.send(OnboardingEvent.Finished)
                val userWallet = childParams.multiWalletState.value.resultUserWallet ?: return
                params.onDone(userWallet)
            }
            else -> return
        }
    }

    private fun handleBackupComponentEvent(event: MultiWalletBackupComponent.Event) {
        when (event) {
            MultiWalletBackupComponent.Event.OneDeviceAdded -> {
                artworksState.value = WalletArtworksState.Unfold(WalletArtworksState.Unfold.Step.Second)
            }
            MultiWalletBackupComponent.Event.TwoDeviceAdded -> {
                artworksState.value = WalletArtworksState.Unfold(WalletArtworksState.Unfold.Step.Third)
            }
            MultiWalletBackupComponent.Event.Done -> {
                openAccessCodeBottomSheet()
            }
        }
    }

    private fun handleFinalizeComponentEvent(event: MultiWalletFinalizeComponent.Event) {
        when (event) {
            MultiWalletFinalizeComponent.Event.OneBackupCardAdded -> {
                artworksState.value = WalletArtworksState.Leapfrog(
                    threeCards = model.state.value.isThreeCards,
                    step = WalletArtworksState.Leapfrog.Step.Second,
                )
            }
            MultiWalletFinalizeComponent.Event.TwoBackupCardsAdded -> {
                if (model.state.value.isThreeCards) {
                    artworksState.value = WalletArtworksState.Leapfrog(
                        threeCards = model.state.value.isThreeCards,
                        step = WalletArtworksState.Leapfrog.Step.Third,
                    )
                } else {
                    handleNavigationEvent(Done)
                }
            }
            MultiWalletFinalizeComponent.Event.ThreeBackupCardsAdded -> {
                handleNavigationEvent(Done)
            }
        }
    }

    private fun openAccessCodeBottomSheet() { bottomSheetNavigation.navigate { } }

    @OptIn(FaultyDecomposeApi::class)
    @Composable
    override fun Content(modifier: Modifier) {
        val stackState by childStack.subscribeAsState()
        val state by model.uiState.collectAsStateWithLifecycle()
        val artworksState by artworksState.collectAsStateWithLifecycle()

        BackHandler { model.onBack() }

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