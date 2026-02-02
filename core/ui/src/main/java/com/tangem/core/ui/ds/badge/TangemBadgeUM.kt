package com.tangem.core.ui.ds.badge

import androidx.annotation.DrawableRes
import com.tangem.core.ui.ds.badge.TangemBadgeSize.X9
import com.tangem.core.ui.extensions.TextReference

/**
 * UI model for [TangemBadge] component
 *
 * @param text          TextReference for the badge label.
 * @param modifier      Modifier to be applied to the badge.
 * @param iconRes       Drawable resource ID for the icon to be displayed in the badge.
 * @param size          [TangemBadgeSize] defining the size of the badge.
 * @param shape         [TangemBadgeShape] defining the shape of the badge.
 * @param color         [TangemBadgeColor] defining the color scheme of the badge.
 * @param type          [TangemBadgeType] defining the style of the badge.
 * @param iconPosition  [TangemBadgeIconPosition] defining icon position of the badge.
 */
class TangemBadgeUM(
    val text: TextReference,
    @DrawableRes val iconRes: Int? = null,
    val size: TangemBadgeSize = X9,
    val shape: TangemBadgeShape = TangemBadgeShape.Default,
    val color: TangemBadgeColor = TangemBadgeColor.Gray,
    val type: TangemBadgeType = TangemBadgeType.Solid,
    val iconPosition: TangemBadgeIconPosition = TangemBadgeIconPosition.Start,
)