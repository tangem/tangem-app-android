package com.tangem.features.onboarding.v2.common.ui

import com.tangem.core.ui.extensions.TextReference

internal data class OnboardingDialogUM(
    val title: TextReference,
    val message: TextReference,
    val dismissButtonText: TextReference,
    val confirmButtonText: TextReference,
    val isDismissWarningColor: Boolean = false,
    val onConfirmClick: () -> Unit,
    val onDismissButtonClick: () -> Unit,
    val onDismiss: () -> Unit,
)