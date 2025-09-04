package com.tangem.features.onramp.mainv2.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.onramp.mainv2.entity.OnrampOffersBlockUM

@Composable
internal fun OnrampOffersContent(state: OnrampOffersBlockUM) {
    AnimatedVisibility(
        visible = state.isBlockVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 300),
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 300),
        ),
        label = "Offers block animation",
    ) {
        Text(
            text = "Some offers",
            style = TangemTheme.typography.head,
        )
        // TODO in [REDACTED_TASK_KEY] to be continued
    }
}