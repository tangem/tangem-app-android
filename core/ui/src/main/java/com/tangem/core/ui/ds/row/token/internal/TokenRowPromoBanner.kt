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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.badge.*
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.ds2.messagebubble.TangemMessageBubble
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_cross_circle_16_filled

@Composable
internal fun TokenRowPromoBanner(promoBannerUM: TangemTokenRowUM.PromoBannerUM, modifier: Modifier = Modifier) {
    when (promoBannerUM) {
        is TangemTokenRowUM.PromoBannerUM.Content -> TokenRowPromoBanner(
            promoBannerUM = promoBannerUM,
            modifier = modifier,
        )
        is TangemTokenRowUM.PromoBannerUM.Warning -> Box(modifier = modifier) {
            TangemMessageBubble(
                text = promoBannerUM.text,
                variant = TangemMessageBubble.Variant.Warning,
                icon = promoBannerUM.icon,
                onClick = promoBannerUM.onBubbleClick,
                onClose = promoBannerUM.onCloseClick,
                trailingIcon = promoBannerUM.trailingIcon ?: Icons.ic_cross_circle_16_filled,
                closeContentDescription = promoBannerUM.closeContentDescription?.resolveReference(),
            )
        }
        TangemTokenRowUM.PromoBannerUM.Empty -> Unit
    }
}

@Composable
internal fun TokenRowPromoBanner(promoBannerUM: TangemTokenRowUM.PromoBannerUM.Content, modifier: Modifier = Modifier) {
    LaunchedEffect(promoBannerUM) {
        promoBannerUM.onPromoShown()
    }
    val bgColor = when (promoBannerUM.type) {
        TangemTokenRowUM.PromoBannerUM.Content.Type.Yield -> TangemTheme.colors2.markers.backgroundTintedGreenAlt
        TangemTokenRowUM.PromoBannerUM.Content.Type.Staking -> TangemTheme.colors2.markers.backgroundTintedBlue
    }
    val contentColor = when (promoBannerUM.type) {
        TangemTokenRowUM.PromoBannerUM.Content.Type.Yield -> TangemTheme.colors2.markers.textGreenAlt
        TangemTokenRowUM.PromoBannerUM.Content.Type.Staking -> TangemTheme.colors2.markers.textBlue
    }
    val badgeColor = when (promoBannerUM.type) {
        TangemTokenRowUM.PromoBannerUM.Content.Type.Yield -> TangemBadgeColor.GreenAlt
        TangemTokenRowUM.PromoBannerUM.Content.Type.Staking -> TangemBadgeColor.Blue
    }
    Column(
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.shape_triangular),
            contentDescription = null,
            tint = bgColor,
            modifier = Modifier.padding(start = 20.dp),
        )
        Row(
            modifier = Modifier
                .background(color = bgColor, shape = RoundedCornerShape(16.dp))
                .clickable(onClick = promoBannerUM.onPromoBannerClick)
                .padding(
                    start = 10.dp,
                    end = 2.dp,
                    top = 2.dp,
                    bottom = 2.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = promoBannerUM.iconRes),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .size(12.dp),
            )
            Text(
                text = promoBannerUM.title.resolveReference(),
                style = TangemTheme.typography2.captionSemibold11,
                color = contentColor,
                modifier = Modifier
                    .padding(vertical = 2.dp),
            )
            TangemBadge(
                size = TangemBadgeSize.X4,
                shape = TangemBadgeShape.Rounded,
                color = badgeColor,
                type = TangemBadgeType.Tinted,
                tangemIconUM = TangemIconUM.Icon(R.drawable.ic_close_24),
                iconPosition = TangemBadgeIconPosition.None,
                onClick = promoBannerUM.onCloseClick,
            )
        }
    }
}

// region Preview
@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TokenRowPromoBanner(
    @PreviewParameter(TangemTokenRowPromoBannerPreviewProvider::class)
    promoType: TangemTokenRowUM.PromoBannerUM.Content.Type,
) {
    TangemThemePreviewRedesign {
        TokenRowPromoBanner(
            promoBannerUM = TangemTokenRowPreviewData.promoBannerUM.copy(type = promoType),
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors2.surface.level1),
        )
    }
}

internal class TangemTokenRowPromoBannerPreviewProvider :
    PreviewParameterProvider<TangemTokenRowUM.PromoBannerUM.Content.Type> {
    override val values: Sequence<TangemTokenRowUM.PromoBannerUM.Content.Type>
        get() = sequenceOf(
            TangemTokenRowUM.PromoBannerUM.Content.Type.Yield,
            TangemTokenRowUM.PromoBannerUM.Content.Type.Staking,
        )
}
// endregion