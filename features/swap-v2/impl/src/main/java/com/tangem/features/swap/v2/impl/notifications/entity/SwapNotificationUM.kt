package com.tangem.features.swap.v2.impl.notifications.entity

import com.tangem.common.ui.R
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.express.models.ExpressError
import com.tangem.features.swap.v2.impl.common.SwapUtils.getExpressErrorMessage
import com.tangem.features.swap.v2.impl.common.SwapUtils.getExpressErrorTitle

internal object SwapNotificationUM {

    sealed class Error(
        title: TextReference,
        subtitle: TextReference,
        iconResId: Int = R.drawable.ic_alert_24,
        buttonState: NotificationConfig.ButtonsState? = null,
        onCloseClick: (() -> Unit)? = null,
    ) : NotificationUM.Error(
        title = title,
        subtitle = subtitle,
        iconResId = iconResId,
        buttonState = buttonState,
        onCloseClick = onCloseClick,
    ) {
        data class MinimalAmountError(
            val amount: String,
        ) : Error(
            title = resourceReference(
                id = R.string.warning_express_too_minimal_amount_title,
                formatArgs = wrappedList(amount),
            ),
            subtitle = resourceReference(R.string.warning_express_wrong_amount_description),
        )

        data class MaximumAmountError(
            val amount: String,
        ) : Error(
            title = resourceReference(
                id = R.string.warning_express_too_maximum_amount_title,
                formatArgs = wrappedList(amount),
            ),
            subtitle = resourceReference(R.string.warning_express_wrong_amount_description),
            iconResId = R.drawable.ic_alert_circle_24,
        )

        data object FCAWarningList : Error(
            title = resourceReference(R.string.warning_express_providers_fca_warning_title),
            subtitle = resourceReference(R.string.warning_express_providers_fca_warning_description),
            iconResId = R.drawable.ic_alert_circle_24,
        )
    }

    sealed class Warning(
        title: TextReference,
        subtitle: TextReference,
        iconResId: Int = R.drawable.img_attention_20,
        buttonsState: NotificationConfig.ButtonsState? = null,
        onCloseClick: (() -> Unit)? = null,
    ) : NotificationUM.Warning(
        title = title,
        subtitle = subtitle,
        iconResId = iconResId,
        buttonsState = buttonsState,
        onCloseClick = onCloseClick,
    ) {
        data class ExpressGeneralError(
            val expressError: ExpressError,
            val onConfirmClick: () -> Unit,
        ) : Warning(
            title = getExpressErrorTitle(expressError),
            subtitle = getExpressErrorMessage(expressError),
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(R.string.warning_button_refresh),
                onClick = onConfirmClick,
            ),
        )
    }
}