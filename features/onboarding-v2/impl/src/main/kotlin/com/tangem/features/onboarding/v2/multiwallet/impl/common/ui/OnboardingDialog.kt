package com.tangem.features.onboarding.v2.multiwallet.impl.common.ui

import com.tangem.core.ui.extensions.TextReference

internal data class OnboardingDialog(
    val title: TextReference,
    val description: TextReference,
    val dismissButtonText: TextReference,
    val confirmButtonText: TextReference,
    val dismissWarningColor: Boolean = false,
    val onConfirm: () -> Unit,
    val onDismissButtonClick: () -> Unit,
    val onDismiss: () -> Unit,
)
