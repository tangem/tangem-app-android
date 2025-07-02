package com.tangem.core.ui.components.badge.entity

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference

/**
 * Badge component UI model
 *
 * @param iconRes   icon drawable resource
 * @param text      text reference
 */
data class BadgeUM(
    val text: TextReference,
    @DrawableRes val iconRes: Int,
)