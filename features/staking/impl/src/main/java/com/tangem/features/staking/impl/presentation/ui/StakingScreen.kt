package com.tangem.features.staking.impl.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tangem.common.ui.amountScreen.AmountScreenContent
import com.tangem.core.ui.components.appbar.AppBarWithBackButtonAndIcon
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingStep
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.bottomsheet.StakingInfoBottomSheetConfig
import com.tangem.features.staking.impl.presentation.ui.bottomsheet.StakingInfoBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex

@Composable
internal fun StakingScreen(uiState: StakingUiState) {
    BackHandler(onBack = uiState.clickIntents::onBackClick)
    Column(
        modifier = Modifier
            .background(color = TangemTheme.colors.background.tertiary)
            .fillMaxSize()
            .imePadding()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SendAppBar(
            uiState = uiState,
        )
        StakingScreenContent(
            uiState = uiState,
            modifier = Modifier.weight(1f),
        )
        StakingNavigationButtons(
            uiState = uiState,
        )
        StakingBottomSheet(bottomSheetConfig = uiState.bottomSheetConfig)
    }
}

@Composable
fun StakingBottomSheet(bottomSheetConfig: TangemBottomSheetConfig?) {
    if (bottomSheetConfig == null) return
    when (bottomSheetConfig.content) {
        is StakingInfoBottomSheetConfig -> StakingInfoBottomSheet(bottomSheetConfig)
    }
}

@Composable
private fun SendAppBar(uiState: StakingUiState) {
    val titleRes = when (uiState.currentStep) {
        StakingStep.Amount -> stringResource(id = R.string.send_amount_label)
        StakingStep.InitialInfo,
        StakingStep.RewardsValidators,
        StakingStep.Validators,
        StakingStep.Confirmation,
        -> stringResource(id = R.string.common_stake)
    }
    val backIcon = when (uiState.currentStep) {
        StakingStep.Amount,
        StakingStep.Validators,
        StakingStep.Confirmation,
        -> {
            R.drawable.ic_close_24
        }
        StakingStep.RewardsValidators,
        StakingStep.InitialInfo,
        -> {
            R.drawable.ic_back_24
        }
    }
    AppBarWithBackButtonAndIcon(
        text = titleRes,
        backIconRes = backIcon,
        onBackClick = uiState.clickIntents::onBackClick,
        backgroundColor = TangemTheme.colors.background.tertiary,
        modifier = Modifier.height(TangemTheme.dimens.size56),
    )
}

@OptIn(ExperimentalAnimationApi::class)
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
                if (running && index != 0) {
                    delay(timeMillis = 200)
                }
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
                    clickIntents = uiState.clickIntents,
                )
                StakingStep.RewardsValidators -> {
                    StakingClaimRewardsValidatorContent(
                        state = uiState.rewardsValidatorsState,
                        clickIntents = uiState.clickIntents,
                    )
                }
                StakingStep.Amount -> AmountScreenContent(
                    amountState = uiState.amountState,
                    isBalanceHiding = uiState.isBalanceHidden,
                    clickIntents = uiState.clickIntents,
                )
                StakingStep.Confirmation -> StakingConfirmationContent(
                    amountState = uiState.amountState,
                    state = uiState.confirmationState,
                    clickIntents = uiState.clickIntents,
                    type = uiState.routeType,
                )
                StakingStep.Validators -> {
                    val confirmState = uiState.confirmationState
                    if (confirmState !is StakingStates.ConfirmationState.Data) return@AnimatedContent
                    StakingValidatorListContent(
                        state = confirmState.validatorState,
                        clickIntents = uiState.clickIntents,
                    )
                }
            }
        }
    }
}
