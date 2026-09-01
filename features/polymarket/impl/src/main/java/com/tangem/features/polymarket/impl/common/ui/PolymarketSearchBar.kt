package com.tangem.features.polymarket.impl.common.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_search_20

/**
 * How much bottom padding a list scrolling under [PolymarketSearchBar] needs so that, scrolled to its
 * end, the last item clears the bar.
 */
internal val PolymarketSearchBarClearance = 76.dp

/**
 * The search pill floating over the feed, on the same material glass — blur, gradient rim, shadow — as
 * the field of the search screen. Not a text field: a tap hands over to the search screen.
 *
 * The glass samples a haze source, so three rules hold (learned the hard way): the screen's content must
 * be marked as the source with its background painted inside it; the feature sits in a modal, so the
 * source and this pill need a window-local `ProvideHaze`; and the pill must stay OUTSIDE the sourced
 * node — glass inside its own source has nothing to sample.
 */
@Composable
internal fun PolymarketSearchBar(placeholder: TextReference, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemSurface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        isMaterial = true,
        shape = CircleShape,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = rememberVectorPainter(Icons.ic_search_20),
                tint = TangemTheme.colors3.icon.secondary,
                contentDescription = null,
            )

            Spacer(modifier = Modifier.size(4.dp))

            Text(
                text = placeholder.resolveReference(),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}