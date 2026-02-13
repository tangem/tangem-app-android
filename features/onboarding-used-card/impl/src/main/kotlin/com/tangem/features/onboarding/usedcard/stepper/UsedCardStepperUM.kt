package com.tangem.features.onboarding.usedcard.stepper

import androidx.annotation.IntRange
import com.tangem.core.ui.extensions.TextReference

internal data class UsedCardStepperUM(
    @IntRange(from = 0) val currentStep: Int,
    @IntRange(from = 0) val steps: Int,
    val title: TextReference,
    val shouldShowBackButton: Boolean,
)