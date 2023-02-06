package com.tangem.feature.swap.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.ResultScreenContent
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.models.SwapSuccessStateHolder
import com.tangem.feature.swap.presentation.R

@Composable
fun SwapSuccessScreen(state: SwapSuccessStateHolder, onBack: () -> Unit) {
    TangemTheme {
        Scaffold(
            content = { padding ->
                ResultScreenContent(
                    resultMessage = makeSuccessMessage(
                        fromTokenAmount = state.fromTokenAmount,
                        toTokenAmount = state.toTokenAmount,
                    ),
                    resultColor = TangemTheme.colors.icon.attention,
                    onButtonClick = onBack,
                    icon = R.drawable.ic_clock_24,
                    secondaryButtonIcon = R.drawable.ic_arrow_top_right_24,
                    onSecondaryButtonClick = state.onSecondaryButtonClick,
                    secondaryButtonText = R.string.swapping_success_view_explorer_button_title,
                    title = R.string.swapping_success_view_title,
                    modifier = Modifier.padding(padding),
                )
            },
            topBar = {
                AppBarWithBackButton(
                    text = stringResource(R.string.swapping_swap),
                    onBackClick = onBack,
                    iconRes = R.drawable.ic_close_24,
                )
            },
        )
    }
}

@Composable
private fun makeSuccessMessage(fromTokenAmount: String, toTokenAmount: String): AnnotatedString {
    val message = stringResource(id = R.string.swapping_swap_of_to, fromTokenAmount, toTokenAmount)
    return buildAnnotatedString {
        append(message)
        addStyle(
            style = SpanStyle(color = TangemTheme.colors.text.accent),
            start = message.indexOf(toTokenAmount),
            end = message.length,
        )
    }
}

// region preview

private val state = SwapSuccessStateHolder(
    fromTokenAmount = "1 000 DAI",
    toTokenAmount = "1 131,46 MATIC",
) {}

@Preview(showBackground = true)
@Composable
private fun Preview_Success_InLightTheme() {
    TangemTheme(isDark = false) {
        SwapSuccessScreen(state) {}
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_Success_InDarkTheme() {
    TangemTheme(isDark = true) {
        SwapSuccessScreen(state) {}
    }
}

// endregion preview
