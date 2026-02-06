package com.tangem.core.ui.ds.row.token.internal

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
internal fun TokenRowPromoBanner(promoBannerUM: TangemTokenRowUM.PromoBannerUM, modifier: Modifier = Modifier) {
    when (promoBannerUM) {
        is TangemTokenRowUM.PromoBannerUM.Content -> TokenRowPromoBanner(
            promoBannerUM = promoBannerUM,
            modifier = modifier,
        )
        TangemTokenRowUM.PromoBannerUM.Empty -> Unit
    }
}

@Composable
internal fun TokenRowPromoBanner(promoBannerUM: TangemTokenRowUM.PromoBannerUM.Content, modifier: Modifier = Modifier) {
    LaunchedEffect(promoBannerUM) {
        promoBannerUM.onPromoShown()
    }
    val bgColor = TangemTheme.colors.control.default
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .background(color = bgColor, shape = RoundedCornerShape(TangemTheme.dimens2.x4))
                .clickable(onClick = promoBannerUM.onPromoBannerClick)
                .padding(horizontal = TangemTheme.dimens2.x3, vertical = TangemTheme.dimens2.x2)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_analytics_up_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.accent,
                modifier = Modifier
                    .padding(end = TangemTheme.dimens2.x2)
                    .size(TangemTheme.dimens2.x4),
            )
            Text(
                text = promoBannerUM.title.resolveReference(),
                style = TangemTheme.typography2.captionSemibold12,
                color = TangemTheme.colors2.text.neutral.secondary,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = TangemTheme.dimens2.x2),
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_close_24),
                contentDescription = null,
                tint = TangemTheme.colors2.text.neutral.secondary,
                modifier = Modifier
                    .size(TangemTheme.dimens2.x4)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false),
                        onClick = { promoBannerUM.onCloseClick() },
                    ),
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_rectangle_bottom),
                contentDescription = null,
                tint = bgColor,
                modifier = Modifier
                    .size(width = TangemTheme.dimens2.x3, height = TangemTheme.dimens2.x2),
            )
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TokenRowPromoBanner() {
    TangemThemePreviewRedesign {
        TokenRowPromoBanner(
            promoBannerUM = TangemTokenRowPreviewData.promoBannerUM,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}