package com.tangem.features.staking.impl.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.tangem.common.ui.amountScreen.AmountScreenContent
import com.tangem.common.ui.bottomsheet.permission.GiveTxPermissionBottomSheet
import com.tangem.common.ui.bottomsheet.permission.state.GiveTxPermissionBottomSheetConfig
import com.tangem.common.ui.navigationButtons.NavigationButtonsBlock
import com.tangem.common.ui.navigationButtons.NavigationButtonsState
import com.tangem.core.ui.components.appbar.AppBarWithBackButtonAndIcon
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.SendScreenTestTags
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingStep
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.bottomsheet.StakingActionSelectionBottomSheetConfig
import com.tangem.features.staking.impl.presentation.state.bottomsheet.StakingInfoBottomSheetConfig
import com.tangem.features.staking.impl.presentation.ui.bottomsheet.StakingActionSelectorBottomSheet
import com.tangem.features.staking.impl.presentation.ui.bottomsheet.StakingInfoBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex

@Composable
internal fun StakingScreen(uiState: StakingUiState) {
    val snackbarHostState = remember { SnackbarHostState() }
    val confirmationState = uiState.confirmationState as? StakingStates.ConfirmationState.Data

    BackHandler(onBack = uiState.clickIntents::onPrevClick)
    Column(
        modifier = Modifier
            .background(color = TangemTheme.colors.background.secondary)
            .fillMaxSize()
            .imePadding()
            .systemBarsPadding()
            .testTag(SendScreenTestTags.SCREEN_CONTAINER),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StakingAppBar(
            uiState = uiState,
        )
        StakingScreenContent(
            uiState = uiState,
            modifier = Modifier.weight(1f),
        )
        NavigationButtonsBlock(
            buttonState = uiState.buttonsState.takeUnless { uiState.currentStep == StakingStep.InitialInfo }
                ?: NavigationButtonsState.Empty,
            footerText = confirmationState?.footerText.takeIf { uiState.currentStep == StakingStep.Confirmation },
            modifier = Modifier.padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16,
            ),
        )
        StakingBottomSheet(bottomSheetConfig = uiState.bottomSheetConfig)
    }

    StakingEventEffect(
        event = uiState.event,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun StakingBottomSheet(bottomSheetConfig: TangemBottomSheetConfig?) {
    if (bottomSheetConfig == null) return
    when (bottomSheetConfig.content) {
        is StakingInfoBottomSheetConfig -> StakingInfoBottomSheet(bottomSheetConfig)
        is GiveTxPermissionBottomSheetConfig -> GiveTxPermissionBottomSheet(bottomSheetConfig)
        is StakingActionSelectionBottomSheetConfig -> StakingActionSelectorBottomSheet(bottomSheetConfig)
    }
}

@Composable
private fun StakingAppBar(uiState: StakingUiState) {
    val (backIcon, click) = when (uiState.currentStep) {
        StakingStep.Amount,
        StakingStep.Confirmation,
        -> R.drawable.ic_close_24 to uiState.clickIntents::onBackClick
        StakingStep.Validators,
        StakingStep.RewardsValidators,
        StakingStep.RestakeValidator,
        StakingStep.InitialInfo,
        -> R.drawable.ic_back_24 to uiState.clickIntents::onPrevClick
    }
    AppBarWithBackButtonAndIcon(
        text = uiState.title.resolveReference(),
        subtitle = uiState.subtitle?.resolveReference(),
        backIconRes = backIcon,
        onBackClick = click,
        backgroundColor = TangemTheme.colors.background.secondary,
        modifier = Modifier.height(TangemTheme.dimens.size56),
    )
}

@Composable
private fun StakingScreenContent(uiState: StakingUiState, modifier: Modifier = Modifier) {
    val currentScreen = uiState.currentStep
    var currentStateProxy by remember { mutableStateOf(currentScreen) }
    var isTransitionAnimationRunning by remember { mutableStateOf(false) }

    // Prevent quick screen changes to avoid some of the transition animation distortions
    LaunchedEffect(currentScreen) {
        snapshotFlow { isTransitionAnimationRunning }
            .withIndex()
            .map { (index, running) ->
                if (running && index != 0) delay(timeMillis = 200)
                running
            }
            .first { !it }

        currentStateProxy = currentScreen
    }
    // Restrict pressing the back button while screen transition is running to avoid most of the animation distortions
    BackHandler(enabled = isTransitionAnimationRunning) {}

    // Box is needed to fix animation with resizing of AnimatedContent
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentStateProxy,
            contentAlignment = Alignment.TopCenter,
            label = "Staking Screen Navigation",
            transitionSpec = {
                val direction = if (initialState.ordinal < targetState.ordinal) {
                    AnimatedContentTransitionScope.SlideDirection.Start
                } else {
                    AnimatedContentTransitionScope.SlideDirection.End
                }

                slideIntoContainer(towards = direction, animationSpec = tween())
                    .togetherWith(slideOutOfContainer(towards = direction, animationSpec = tween()))
            },
        ) { state ->
            isTransitionAnimationRunning = transition.targetState != transition.currentState

            when (state) {
                StakingStep.InitialInfo -> StakingInitialInfoContent(
                    state = uiState.initialInfoState,
                    buttonState = uiState.buttonsState,
                    clickIntents = uiState.clickIntents,
                    isBalanceHidden = uiState.isBalanceHidden,
                )
                StakingStep.RewardsValidators -> {
                    StakingClaimRewardsValidatorContent(
                        state = uiState.rewardsValidatorsState,
                        clickIntents = uiState.clickIntents,
                    )
                }
                StakingStep.Amount -> AmountScreenContent(
                    amountState = uiState.amountState,
                    isBalanceHidden = uiState.isBalanceHidden,
                    clickIntents = uiState.clickIntents,
                    modifier = Modifier.background(TangemTheme.colors.background.secondary),
                )
                StakingStep.Confirmation -> StakingConfirmationContent(
                    amountState = uiState.amountState,
                    state = uiState.confirmationState,
                    validatorState = uiState.validatorState,
                    clickIntents = uiState.clickIntents,
                )
                StakingStep.RestakeValidator,
                StakingStep.Validators,
                -> StakingValidatorListContent(
                    state = uiState.validatorState,
                    clickIntents = uiState.clickIntents,
                )
            }
        }
    }
}