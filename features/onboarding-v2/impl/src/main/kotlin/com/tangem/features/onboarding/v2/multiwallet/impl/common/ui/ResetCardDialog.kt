package com.tangem.features.onboarding.v2.multiwallet.impl.common.ui

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onboarding.v2.impl.R

internal fun resetCardDialog(onConfirm: () -> Unit, dismiss: () -> Unit, onDismissButtonClick: () -> Unit) =
    OnboardingDialog(
        title = resourceReference(R.string.onboarding_activation_error_title),
        description = resourceReference(R.string.onboarding_activation_error_message),
        confirmButtonText = resourceReference(R.string.common_support),
        dismissButtonText = resourceReference(R.string.common_ok),
        dismissWarningColor = true,
        onDismiss = dismiss,
        onConfirm = {
            onConfirm()
            dismiss()
        },
        onDismissButtonClick = {
            onDismissButtonClick()
            dismiss()
        },
    )
