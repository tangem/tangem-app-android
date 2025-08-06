package com.tangem.features.onboarding.v2.stepper.impl.ui

import android.content.res.Configuration
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.progressbar.TangemLinearProgressIndicator
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemAnimations
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.stepper.api.OnboardingStepperComponent

@Composable
internal fun OnboardingStepper(
    state: OnboardingStepperComponent.StepperState,
    onBackClick: () -> Unit,
    onSupportButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = state.currentStep.toFloat() / state.steps.coerceAtLeast(1)
    val animatedIndicatorFraction by TangemAnimations.horizontalIndicatorAsState(targetFraction = fraction)

    val progressAlpha by animateFloatAsState(
        targetValue = if (state.showProgress) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            easing = LinearEasing,
        ),
        label = "progressAlpha",
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TangemTopAppBar(
            startButton = TopAppBarButtonUM.Back(onBackClick),
            endButton = TopAppBarButtonUM.Icon(iconRes = R.drawable.ic_chat_24, onClicked = onSupportButtonClick)
                .takeIf { state.steps != state.currentStep },
            title = if (state.steps == state.currentStep) {
                resourceReference(R.string.common_done)
            } else {
                state.title
            },
            containerColor = TangemTheme.colors.background.primary,
            modifier = modifier,
        )

        TangemLinearProgressIndicator(
            modifier = Modifier
                .alpha(progressAlpha)
                .padding(horizontal = 16.dp)
                .height(4.dp)
                .fillMaxWidth(),
            progress = { animatedIndicatorFraction },
            color = TangemTheme.colors.icon.primary1,
            backgroundColor = TangemTheme.colors.icon.primary1.copy(alpha = 0.4f),
            strokeCap = StrokeCap.Round,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingStepper_Preview() {
    TangemThemePreview {
        Box(
            Modifier
                .fillMaxSize()
                .background(TangemTheme.colors.background.primary),
        ) {
            OnboardingStepper(
                modifier = Modifier.align(Alignment.TopCenter),
                state = OnboardingStepperComponent.StepperState(
                    currentStep = 2,
                    steps = 3,
                    title = resourceReference(R.string.common_done),
                    showProgress = true,
                ),
                onBackClick = {},
                onSupportButtonClick = {},
            )
        }
    }
}