package com.tangem.features.feed.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme

/**
 * Chrome shared by every tab of the search screen, so the tabs cannot drift apart visually.
 * Each tab still owns its own results — only the framing around them lives here.
 */

/** Title above one group of results, e.g. "My portfolio" or "Earn assets". */
@Composable
fun FeedSearchSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = TangemTheme.typography3.heading.small,
        color = TangemTheme.colors3.text.primary,
        maxLines = 1,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
    )
}

/**
 * @param shouldFillParent `true` when this is the only thing on the page, which centres it further
 * down instead of leaving it hanging under a section that did match
 */
@Composable
fun FeedSearchNoResults(shouldFillParent: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = stringResourceSafe(R.string.common_no_results),
        style = TangemTheme.typography3.body.medium,
        color = TangemTheme.colors3.text.secondary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (shouldFillParent) 64.dp else 24.dp),
    )
}