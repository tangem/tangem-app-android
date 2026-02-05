package com.tangem.core.ui.ds.row.token.internal

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.ds.badge.TangemBadge
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
fun TokenRowTitle(titleUM: TangemTokenRowUM.TitleUM, modifier: Modifier = Modifier) {
    when (titleUM) {
        is TangemTokenRowUM.TitleUM.Content -> ContentTitle(titleUM = titleUM, modifier = modifier)
        TangemTokenRowUM.TitleUM.Loading -> TextShimmer(
            style = TangemTheme.typography2.bodySemibold16,
            modifier = modifier.width(TangemTheme.dimens2.x18),
            radius = TangemTheme.dimens2.x25,
        )
        TangemTokenRowUM.TitleUM.Empty -> Unit
    }
}

@Composable
private fun ContentTitle(titleUM: TangemTokenRowUM.TitleUM.Content, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens2.x1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        /*
         * If currency name has a long width, then it will completely displace the image.
         * So we need to use [weight] to avoid displacement.
         */
        Text(
            text = titleUM.text.resolveAnnotatedReference(),
            modifier = Modifier.weight(weight = 1f, fill = false),
            color = if (titleUM.isAvailable) {
                TangemTheme.colors2.text.neutral.primary
            } else {
                TangemTheme.colors2.text.status.disabled
            },
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = TangemTheme.typography2.bodySemibold16,
        )

        AnimatedVisibility(
            visible = titleUM.hasPending,
            modifier = Modifier.align(alignment = Alignment.CenterVertically),
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_loader_15),
                contentDescription = null,
            )
        }

        AnimatedVisibility(
            visible = titleUM.badge != null,
            modifier = Modifier.conditional(
                condition = titleUM.onBadgeClick != null,
                modifier = { clickable(onClick = requireNotNull(titleUM.onBadgeClick)) },
            ),
        ) {
            val wrappedBadge = remember(this) { requireNotNull(titleUM.badge) }
            TangemBadge(wrappedBadge)
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TokenRowTitle_Preview() {
    TangemThemePreviewRedesign {
        TokenRowTitle(
            titleUM = TangemTokenRowPreviewData.titleUM,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// endregion