package com.tangem.feature.onboarding.api

import androidx.compose.runtime.Composable
import com.tangem.feature.onboarding.presentation.wallet2.model.OnboardingSeedPhraseState
import com.tangem.feature.onboarding.presentation.wallet2.viewmodel.SeedPhraseScreen

/**
[REDACTED_AUTHOR]
 */
interface OnboardingSeedPhraseApi {
    @Suppress("TopLevelComposableFunctions")
    @Composable
    fun ScreenContent(uiState: OnboardingSeedPhraseState, subScreen: SeedPhraseScreen, progress: Float)
}