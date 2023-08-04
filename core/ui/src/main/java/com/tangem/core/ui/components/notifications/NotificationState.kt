package com.tangem.core.ui.components.notifications

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.extensions.TextReference

/**
 * Notification component state
 *
 * @property title     title
 * @property subtitle  subtitle
 * @property iconResId icon resource id
 * @property tint      icon tint
 *
* [REDACTED_AUTHOR]
 */
sealed class NotificationState(
    open val title: TextReference,
    open val subtitle: TextReference? = null,
    @DrawableRes open val iconResId: Int,
    open val tint: Color? = null,
) {

    /**
     * Simple notification state. Non clickable.
     *
     * @property title     title
     * @property subtitle  subtitle
     * @property iconResId icon resource id
     * @property tint      icon tint
     */
    data class Simple(
        override val title: TextReference,
        override val subtitle: TextReference? = null,
        @DrawableRes override val iconResId: Int,
        override val tint: Color? = null,
    ) : NotificationState(title, subtitle, iconResId, tint)

    /**
     * Clickable notification state
     *
     * @property title     title
     * @property subtitle  subtitle
     * @property iconResId icon resource id
     * @property tint      icon tint
     * @property onClick   lambda be invoked when notification component is clicked
     */
    data class Clickable(
        override val title: TextReference,
        override val subtitle: TextReference? = null,
        @DrawableRes override val iconResId: Int,
        override val tint: Color? = null,
        val onClick: () -> Unit,
    ) : NotificationState(title, subtitle, iconResId, tint)

    /**
     * Closable notification state
     *
     * @property title        title
     * @property subtitle     subtitle
     * @property iconResId    icon resource id
     * @property tint         icon tint
     * @property onCloseClick lambda be invoked when close button is clicked
     */
    data class Closable(
        override val title: TextReference,
        override val subtitle: TextReference? = null,
        @DrawableRes override val iconResId: Int,
        override val tint: Color? = null,
        val onCloseClick: (() -> Unit)? = null,
    ) : NotificationState(title, subtitle, iconResId, tint)
}
