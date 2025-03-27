package com.tangem.features.onboarding.v2.multiwallet.impl.common.ui

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onboarding.v2.common.ui.OnboardingDialogUM
import com.tangem.features.onboarding.v2.impl.R

internal fun resetCardDialog(onConfirm: () -> Unit, dismiss: () -> Unit, onDismissButtonClick: () -> Unit) =
    OnboardingDialogUM(
        title = resourceReference(R.string.onboarding_activation_error_title),
        message = resourceReference(R.string.onboarding_activation_error_message),
        confirmButtonText = resourceReference(R.string.common_support),
        dismissButtonText = resourceReference(R.string.common_ok),
        dismissWarningColor = true,
        onDismiss = dismiss,
        onConfirmClick = {
            onConfirm()
            dismiss()
        },
        onDismissButtonClick = {
            onDismissButtonClick()
            dismiss()
        },
    )