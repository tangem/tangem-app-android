package com.tangem.features.onboarding.v2.common.ui

import com.tangem.common.ui.alerts.models.AlertUM
import com.tangem.core.ui.extensions.TextReference

internal data class OnboardingDialogUM(
    override val title: TextReference,
    override val message: TextReference,
    val dismissButtonText: TextReference,
    override val confirmButtonText: TextReference,
    val dismissWarningColor: Boolean = false,
    override val onConfirmClick: () -> Unit,
    val onDismissButtonClick: () -> Unit,
    val onDismiss: () -> Unit,
) : AlertUM