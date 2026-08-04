package com.tangem.feature.wallet.presentation.wallet.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.feature.wallet.impl.R

/**
 * Hint above the markets bottom sheet suggesting to pull it up to add tokens.
 *
 * The hint renders only when [isVisible] and its own on-screen bounds do not intersect
 * [obstacleBounds] (e.g. the "Add & Manage" button resting nearby), so it never draws
 * over other content.
 */
@Composable
internal fun MarketsHint(isVisible: Boolean, modifier: Modifier = Modifier, obstacleBounds: () -> Rect? = { null }) {
    var hintBounds by remember { mutableStateOf<Rect?>(null) }
    val isObstacleInTheWay by remember(obstacleBounds) {
        derivedStateOf {
            val hint = hintBounds
            val obstacle = obstacleBounds()
            hint != null && obstacle != null && hint.overlaps(obstacle)
        }
    }

    val isShown = isVisible && !isObstacleInTheWay
    val alpha by animateFloatAsState(
        targetValue = if (isShown) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "marketsHintAlpha",
    )

    Column(
        modifier = modifier
            .onGloballyPositioned { hintBounds = it.boundsInRoot() }
            .graphicsLayer { this.alpha = alpha }
            .conditional(!isShown) { clearAndSetSemantics { } },
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResourceSafe(R.string.markets_hint),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )
        Icon(
            modifier = Modifier.size(size = 24.dp),
            painter = painterResource(id = R.drawable.ic_chevron_24),
            tint = TangemTheme.colors3.icon.secondary,
            contentDescription = null,
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun MarketsHint_Preview() {
    TangemThemePreviewRedesign {
        MarketsHint(
            isVisible = true,
        )
    }
}
// endregion