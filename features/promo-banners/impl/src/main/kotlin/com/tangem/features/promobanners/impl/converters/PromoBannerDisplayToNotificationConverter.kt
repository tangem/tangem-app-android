package com.tangem.features.promobanners.impl.converters

import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.promobanners.impl.model.PromoBannerDisplay
import com.tangem.features.promobanners.impl.model.PromoBannerNotificationUM

internal class PromoBannerDisplayToNotificationConverter {

    fun convert(
        banner: PromoBannerDisplay,
        onDeeplinkClick: (String?) -> Unit,
        onDismiss: (Int) -> Unit,
    ): PromoBannerNotificationUM {
        return PromoBannerNotificationUM(
            displayId = banner.id,
            config = NotificationConfig(
                title = TextReference.Str(banner.title),
                subtitle = TextReference.Str(banner.subtitle),
                iconResId = com.tangem.core.ui.R.drawable.ic_alert_circle_24,
                iconUrl = banner.iconUrl,
                onCloseClick = if (banner.isDismissable) {
                    { onDismiss(banner.id) }
                } else {
                    null
                },
                buttonsState = banner.buttonText?.takeIf { banner.isButtonEnabled }?.let { text ->
                    NotificationConfig.ButtonsState.SecondaryButtonConfig(
                        text = TextReference.Str(text),
                        onClick = { onDeeplinkClick(banner.deeplink) },
                    )
                },
            ),
        )
    }
}