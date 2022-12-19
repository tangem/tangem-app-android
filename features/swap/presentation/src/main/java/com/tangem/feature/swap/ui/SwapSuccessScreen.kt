package com.tangem.feature.swap.ui

import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.components.SuccessScreenContent
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.models.SwapSuccessStateHolder
import com.tangem.feature.swap.presentation.R

@Composable
fun SwapSuccessScreen(state: SwapSuccessStateHolder, onBack: () -> Unit) {
    TangemTheme {
        Scaffold(
            content = {
                SuccessScreenContent(
                    successMessage = state.message,
                    onButtonClick = onBack,
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