package com.tangem.features.hotwallet.stepper.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.tangem.features.hotwallet.impl.R
import com.tangem.features.hotwallet.stepper.api.HotWalletStepperComponent

@Composable
internal fun HotWalletStepper(
    state: HotWalletStepperComponent.StepperUM,
    onBackClick: () -> Unit,
    onSkipClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = state.currentStep.toFloat() / state.steps.coerceAtLeast(1)
    val animatedIndicatorFraction by TangemAnimations.horizontalIndicatorAsState(targetFraction = fraction)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TangemTopAppBar(
            startButton = if (state.showBackButton) {
                TopAppBarButtonUM.Back { onBackClick() }
            } else {
                null
            },
            endButton = when {
                state.showSkipButton -> TopAppBarButtonUM.Text(
                    text = resourceReference(R.string.common_skip),
                    onClicked = { onSkipClick() },
                )
                state.showFeedbackButton -> TopAppBarButtonUM.Icon(
                    iconRes = R.drawable.ic_chat_24,
                    onClicked = { onFeedbackClick() },
                )
                else -> null
            },
            title = state.title,
            containerColor = TangemTheme.colors.background.primary,
            modifier = modifier,
            titleAlignment = Alignment.CenterHorizontally,
        )

        TangemLinearProgressIndicator(
            modifier = Modifier
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
private fun HotWalletStepper_Preview() {
    TangemThemePreview {
        Box(
            Modifier
                .fillMaxSize()
                .background(TangemTheme.colors.background.primary),
        ) {
            HotWalletStepper(
                modifier = Modifier.align(Alignment.TopCenter),
                state = HotWalletStepperComponent.StepperUM(
                    currentStep = 2,
                    steps = 3,
                    title = resourceReference(R.string.common_done),
                    showBackButton = true,
                    showSkipButton = false,
                    showFeedbackButton = true,
                ),
                onBackClick = {},
                onSkipClick = {},
                onFeedbackClick = {},
            )
        }
    }
}