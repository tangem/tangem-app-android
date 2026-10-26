@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.DsVersion
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.res.TangemTheme

/**
 * Version of the DS component the opened story showcases, pinned to the top-right corner.
 *
 * @param version version to show. `null` — the story is not a versioned DS component, nothing is drawn.
 * @param withStatusBarPadding pass `true` for edge-to-edge stories, whose content is not inset.
 */
@Composable
internal fun BoxScope.DsVersionBadge(version: DsVersion?, withStatusBarPadding: Boolean) {
    if (version == null) return

    Text(
        text = "v$version",
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .conditional(withStatusBarPadding) { statusBarsPadding() }
            .padding(all = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(TangemTheme.colors3.bg.opaque.secondary)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}