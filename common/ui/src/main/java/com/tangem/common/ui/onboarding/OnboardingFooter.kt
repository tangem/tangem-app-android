package com.tangem.common.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.fade.TangemFade

/**
 * Pinned footer of a feature-onboarding screen: a blurring scrim carrying the legal line and the CTA.
 *
 * Belongs in the scaffold's overlay slot rather than its content slot — the content slot is the haze
 * source, so a blurring child of it would sample itself back as a ghost.
 *
 * The frame's background is [TangemFade.Variant.Hard] stretched over the whole footer: its gradient band
 * runs up above the legal line with the content faintly showing through, everything below it — the CTA
 * included — sits on solid.
 *
 * @param contentPadding safe-area padding from the scaffold; its bottom inset keeps the CTA off the
 *   navigation bar and its horizontal insets are added to the design's own 24dp.
 * @param modifier applied to the footer root; the caller aligns it to the bottom and measures its height.
 * @param content legal line and CTA, in that order — consent has to be visible before the action it
 *   covers is taken, so it is never gated on scroll position.
 */
@Composable
fun OnboardingFooter(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current

    Box(modifier = modifier.fillMaxWidth()) {
        TangemFade(
            modifier = Modifier.matchParentSize(),
            position = TangemFade.Position.Bottom,
            variant = TangemFade.Variant.Hard,
            blur = true,
        )
        Column(
            modifier = Modifier.padding(
                start = 24.dp + contentPadding.calculateStartPadding(layoutDirection),
                end = 24.dp + contentPadding.calculateEndPadding(layoutDirection),
                top = 28.dp,
                bottom = contentPadding.calculateBottomPadding() + 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}