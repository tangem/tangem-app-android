package com.tangem.features.onboarding.v2.multiwallet.impl.model

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.ui.state.OnboardingMultiWalletUM

internal fun resetCardDialog(onConfirm: () -> Unit, onDismiss: () -> Unit, onDismissButtonClick: () -> Unit) =
    OnboardingMultiWalletUM.Dialog(
        title = resourceReference(R.string.onboarding_activation_error_title),
        description = resourceReference(R.string.onboarding_activation_error_message),
        confirmButtonText = resourceReference(R.string.common_ok),
        dismissButtonText = resourceReference(R.string.common_support),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        onDismissButtonClick = onDismissButtonClick,
    )