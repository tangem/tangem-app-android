package com.tangem.feature.onboarding.presentation.wallet2.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH12

/**
* [REDACTED_AUTHOR]
 */
@Composable
fun OnboardingActionBlock(
    firstActionContent: @Composable (() -> Unit)? = null,
    secondActionContent: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .wrapContentSize()
            .padding(
                top = 8.dp,
                bottom = 32.dp,
            ),
    ) {
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            firstActionContent?.invoke()
            if (firstActionContent != null && secondActionContent != null) SpacerH12()
            secondActionContent?.invoke()
        }
    }
}
