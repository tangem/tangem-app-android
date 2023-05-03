package com.tangem.feature.onboarding.api

import androidx.compose.runtime.Composable
import com.tangem.feature.onboarding.presentation.wallet2.viewmodel.SeedPhraseViewModel

/**
[REDACTED_AUTHOR]
 */
interface OnboardingSeedPhraseApi {
    @Suppress("TopLevelComposableFunctions")
    @Composable
    fun ScreenContent(viewModel: SeedPhraseViewModel, maxProgress: Int)
}