package com.tangem.feature.onboarding.presentation.wallet2.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.res.TangemTheme

/**
[REDACTED_AUTHOR]
 */
@Composable
fun OnboardingActionBlock(
    modifier: Modifier = Modifier,
    firstActionContent: @Composable (() -> Unit)? = null,
    secondActionContent: @Composable (() -> Unit)? = null,
) {
    if (firstActionContent == null && secondActionContent == null) return

    Box(
        modifier = modifier
            .padding(horizontal = TangemTheme.dimens.size16)
            .padding(
                top = TangemTheme.dimens.size8,
                bottom = TangemTheme.dimens.size32,
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