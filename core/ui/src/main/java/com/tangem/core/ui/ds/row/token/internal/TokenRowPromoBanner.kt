package com.tangem.core.ui.ds.row.token.internal

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.badge.*
import com.tangem.core.ui.ds.image.TangemIconUM
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
    val bgColor = TangemTheme.colors2.markers.backgroundTintedGreen
    Column(
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.shape_triangular),
            contentDescription = null,
            tint = bgColor,
            modifier = Modifier.padding(start = TangemTheme.dimens2.x5),
        )
        Row(
            modifier = Modifier
                .background(color = bgColor, shape = RoundedCornerShape(TangemTheme.dimens2.x4))
                .clickable(onClick = promoBannerUM.onPromoBannerClick)
                .padding(
                    start = TangemTheme.dimens2.x2_5,
                    end = TangemTheme.dimens2.x0_5,
                    top = TangemTheme.dimens2.x0_5,
                    bottom = TangemTheme.dimens2.x0_5,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_analytics_up_24),
                contentDescription = null,
                tint = TangemTheme.colors2.markers.textGreen,
                modifier = Modifier
                    .padding(vertical = TangemTheme.dimens2.x0_5)
                    .size(TangemTheme.dimens2.x3),
            )
            Text(
                text = promoBannerUM.title.resolveReference(),
                style = TangemTheme.typography2.captionSemibold11,
                color = TangemTheme.colors2.markers.textGreen,
                modifier = Modifier
                    .padding(vertical = TangemTheme.dimens2.x0_5),
            )
            TangemBadge(
                size = TangemBadgeSize.X4,
                shape = TangemBadgeShape.Rounded,
                color = TangemBadgeColor.Green,
                type = TangemBadgeType.Tinted,
                tangemIconUM = TangemIconUM.Icon(R.drawable.ic_close_24),
                iconPosition = TangemBadgeIconPosition.None,
                onClick = promoBannerUM.onCloseClick,
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