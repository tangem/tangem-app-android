package com.tangem.features.send.impl.presentation.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.alerts.models.AlertUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.send.impl.R

@Immutable
internal sealed class SendAlertUM : AlertUM {

    data class GenericError(
        override val title: TextReference? = resourceReference(id = R.string.send_alert_transaction_failed_title),
        override val onConfirmClick: (() -> Unit),
    ) : SendAlertUM() {
        override val message: TextReference = resourceReference(R.string.common_unknown_error)
        override val confirmButtonText: TextReference =
            resourceReference(id = R.string.common_support)
    }

    data class FeeIncreased(
        override val onConfirmClick: () -> Unit,
    ) : SendAlertUM() {
        override val title: TextReference? = null
        override val message: TextReference = resourceReference(id = R.string.send_notification_high_fee_title)
        override val confirmButtonText: TextReference = resourceReference(id = R.string.common_ok)
    }

    data class FeeTooLow(
        override val onConfirmClick: () -> Unit,
    ) : SendAlertUM() {
        override val title: TextReference? = null
        override val message: TextReference = resourceReference(id = R.string.send_alert_fee_too_low_text)
        override val confirmButtonText: TextReference = resourceReference(R.string.common_continue)
    }

    data class FeeTooHigh(
        val times: String,
        override val onConfirmClick: () -> Unit,
    ) : SendAlertUM() {
        override val title: TextReference? = null
        override val message: TextReference =
            resourceReference(id = R.string.send_alert_fee_too_high_text, wrappedList(times))
        override val confirmButtonText: TextReference = resourceReference(R.string.common_continue)
    }

    data class FeeUnreachableError(
        override val onConfirmClick: (() -> Unit),
    ) : SendAlertUM() {
        override val title: TextReference = resourceReference(R.string.send_fee_unreachable_error_title)
        override val message: TextReference = resourceReference(R.string.send_fee_unreachable_error_text)
        override val confirmButtonText = resourceReference(R.string.warning_button_refresh)
    }
}