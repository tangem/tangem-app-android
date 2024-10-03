package com.tangem.feature.swap.models

import com.tangem.common.ui.alerts.models.AlertUM
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference

sealed class SwapAlertUM : AlertUM {

    data class GenericError(
        override val onConfirmClick: (() -> Unit),
        override val message: TextReference = resourceReference(R.string.common_unknown_error),
    ) : SwapAlertUM() {
        override val title: TextReference? = null
        override val confirmButtonText: TextReference =
            resourceReference(id = R.string.common_support)
    }

    data class ExpressErrorAlert(
        override val message: TextReference = resourceReference(R.string.common_unknown_error),
        override val onConfirmClick: (() -> Unit),
    ) : SwapAlertUM() {
        override val title: TextReference? = null
        override val confirmButtonText: TextReference =
            resourceReference(id = R.string.common_support)
    }

    data class FeesAlert(
        override val message: TextReference,
        override val onConfirmClick: (() -> Unit),
    ) : SwapAlertUM() {
        override val title: TextReference = resourceReference(
            com.tangem.feature.swap.presentation.R.string.swapping_alert_title,
        )
        override val confirmButtonText: TextReference =
            resourceReference(id = R.string.common_ok)
    }
}