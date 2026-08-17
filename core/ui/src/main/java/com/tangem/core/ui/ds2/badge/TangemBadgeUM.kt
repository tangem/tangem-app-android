package com.tangem.core.ui.ds2.badge

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference

/**
 * UI model for [TangemBadge]. Lets callers keep badge content and styling inside their own UI models.
 *
 * @param text Badge label.
 * @param variant Visual style. See [TangemBadge.Variant].
 * @param status Status color scheme. See [TangemBadge.Status].
 * @param size Size preset. See [TangemBadge.Size].
 * @param iconStart Optional leading icon.
 * @param iconEnd Optional trailing icon.
 * @param contentDescription Accessibility label; overrides [text] for screen readers when non-null.
 */
data class TangemBadgeUM(
    val text: TextReference,
    val variant: TangemBadge.Variant = TangemBadge.Variant.Tinted,
    val status: TangemBadge.Status = TangemBadge.Status.Neutral,
    val size: TangemBadge.Size = TangemBadge.Size.X9,
    val iconStart: TangemIconUM? = null,
    val iconEnd: TangemIconUM? = null,
    val contentDescription: String? = null,
)