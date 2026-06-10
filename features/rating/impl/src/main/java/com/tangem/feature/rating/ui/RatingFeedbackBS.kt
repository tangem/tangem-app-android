package com.tangem.feature.rating.ui

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent

internal data class RatingFeedbackBS(
    val feedbackText: String,
    val isSubmitting: Boolean,
    val onFeedbackChanged: (String) -> Unit,
    val onDismiss: () -> Unit,
    val onSubmit: () -> Unit,
) : TangemBottomSheetConfigContent