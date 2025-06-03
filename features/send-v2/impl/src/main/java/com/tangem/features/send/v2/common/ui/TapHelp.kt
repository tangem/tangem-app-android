package com.tangem.features.send.v2.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.v2.impl.R
import kotlinx.coroutines.delay

private const val TAP_HELP_KEY = "TAP_HELP_KEY"
private const val TAP_HELP_ANIMATION_DELAY = 500L

internal fun LazyListScope.tapHelp(isDisplay: Boolean, modifier: Modifier = Modifier) {
    item(key = TAP_HELP_KEY) {
        var wrappedIsDisplay by remember { mutableStateOf(false) }

        LaunchedEffect(key1 = isDisplay) {
            delay(TAP_HELP_ANIMATION_DELAY)
            wrappedIsDisplay = isDisplay
        }

        if (wrappedIsDisplay) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier
                    .fillMaxWidth()
                    .animateItem()
                    .padding(top = TangemTheme.dimens.spacing20),
            ) {
                val background = TangemTheme.colors.button.secondary
                Icon(
                    painter = painterResource(id = R.drawable.ic_send_hint_shape_12),
                    tint = TangemTheme.colors.button.secondary,
                    contentDescription = null,
                    modifier = Modifier,
                )
                Text(
                    text = stringResourceSafe(id = R.string.send_summary_tap_hint),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.secondary,
                    modifier = Modifier
                        .clip(TangemTheme.shapes.roundedCornersXMedium)
                        .background(background)
                        .padding(
                            horizontal = TangemTheme.dimens.spacing14,
                            vertical = TangemTheme.dimens.spacing12,
                        ),
                )
            }
        }
    }
}