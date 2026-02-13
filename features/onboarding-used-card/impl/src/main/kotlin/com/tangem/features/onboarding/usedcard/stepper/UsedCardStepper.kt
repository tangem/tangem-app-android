package com.tangem.features.onboarding.usedcard.stepper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.progressbar.TangemLinearProgressIndicator
import com.tangem.core.ui.res.TangemAnimations
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun UsedCardStepper(state: UsedCardStepperUM, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val fraction = state.currentStep.toFloat() / state.steps.coerceAtLeast(1)
    val animatedIndicatorFraction by TangemAnimations.horizontalIndicatorAsState(targetFraction = fraction)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TangemTopAppBar(
            startButton = if (state.shouldShowBackButton) {
                TopAppBarButtonUM.Back(onBackClick)
            } else {
                null
            },
            title = state.title,
            containerColor = TangemTheme.colors.background.primary,
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