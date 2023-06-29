package com.tangem.core.ui.components.notifications

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color

/**
 * Notification component state
 *
 * @property title     title
 * @property subtitle  subtitle
 * @property iconResId icon resource id
 * @property tint      icon tint
 *
[REDACTED_AUTHOR]
 */
sealed class NotificationState(
    open val title: String,
    open val subtitle: String? = null,
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
        override val title: String,
        override val subtitle: String? = null,
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
     * @param onClick      lambda be invoked when notification component is clicked
     */
    data class Action(
        override val title: String,
        override val subtitle: String? = null,
        @DrawableRes override val iconResId: Int,
        override val tint: Color? = null,
        val onClick: () -> Unit,
    ) : NotificationState(title, subtitle, iconResId, tint)
}