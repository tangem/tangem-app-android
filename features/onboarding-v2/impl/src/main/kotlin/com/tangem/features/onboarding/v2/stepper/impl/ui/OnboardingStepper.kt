package com.tangem.features.onboarding.v2.stepper.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.TextButton
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.core.ui.components.progressbar.LinearProgressIndicator
import com.tangem.core.ui.extensions.resolveReference
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

    Column(
        modifier = modifier.background(color = TangemTheme.colors.background.primary),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TangemTopAppBar(
            modifier = modifier,
            title = state.title.resolveReference(),
            startButton = TopAppBarButtonUM(
                iconRes = R.drawable.ic_back_24,
                onIconClicked = onBackClick,
            ),
            endContentFixedHeight = false,
            endContent = {
                if (state.steps != state.currentStep) {
                    TextButton(
                        text = stringResource(R.string.common_support),
                        colors = TangemButtonsDefaults.defaultTextButtonColors.copy(
                            contentColor = TangemTheme.colors.text.primary1,
                        ),
                        onClick = onSupportButtonClick,
                    )
                }
            },
        )

        LinearProgressIndicator(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(4.dp)
                .fillMaxWidth(),
            progress = { animatedIndicatorFraction },
            color = TangemTheme.colors.icon.primary1,
            backgroundColor = TangemTheme.colors.background.tertiary,
            strokeCap = StrokeCap.Round,
        )
    }
}

@Preview
@Composable
private fun Preview() {
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
                ),
                onBackClick = {},
                onSupportButtonClick = {},
            )
        }
    }
}