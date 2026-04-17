package com.tangem.common.ui.earn

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.ds.button.TangemButton
import com.tangem.core.ui.ds.button.TangemButtonShape
import com.tangem.core.ui.ds.button.TangemButtonSize
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.res.R as CoreResR

private const val TINTED_BACKGROUND_ALPHA = 0.1f
private const val TINTED_BORDER_ALPHA = 0.1f
private const val TINTED_INNER_SHADOW_ALPHA = 0.3f
private val BorderWidth = 1.dp
private val InnerShadowBlur = 20.dp
private val ShimmerSubtitleWidth = 78.dp

@Composable
fun EarnBlock(state: EarnBlockUM, modifier: Modifier = Modifier) {
    when (state) {
        is EarnBlockUM.Loading -> EarnBlockLoading(modifier)
        is EarnBlockUM.Content -> EarnBlockContent(state, modifier)
    }
}

@Composable
private fun EarnBlockLoading(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(TangemTheme.dimens2.x4)
    TangemRowContainer(
        modifier = modifier
            .clip(shape)
            .background(TangemTheme.colors2.surface.level3)
            .border(width = BorderWidth, color = TangemTheme.colors2.border.neutral.primary, shape = shape),
        contentPadding = PaddingValues(all = TangemTheme.dimens2.x3),
        content = {
            CircleShimmer(
                modifier = Modifier
                    .layoutId(TangemRowLayoutId.HEAD)
                    .padding(end = TangemTheme.dimens2.x3)
                    .size(TangemTheme.dimens2.x10),
            )
            RectangleShimmer(
                modifier = Modifier
                    .layoutId(TangemRowLayoutId.START_TOP)
                    .size(width = TangemTheme.dimens2.x16, height = TangemTheme.dimens2.x5),
                radius = TangemTheme.dimens2.x2,
            )
            RectangleShimmer(
                modifier = Modifier
                    .layoutId(TangemRowLayoutId.START_BOTTOM)
                    .size(width = ShimmerSubtitleWidth, height = TangemTheme.dimens2.x4),
                radius = TangemTheme.dimens2.x2,
            )
        },
    )
}

@Composable
private fun EarnBlockContent(state: EarnBlockUM.Content, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(TangemTheme.dimens2.x4)

    val backgroundModifier = resolveBackgroundModifier(state.backgroundUM, shape)

    val clickModifier = when (val trailing = state.trailingUM) {
        is EarnBlockUM.TrailingUM.Balance -> Modifier.clickable(onClick = trailing.onClick)
        else -> Modifier
    }

    TangemRowContainer(
        modifier = modifier
            .clip(shape)
            .then(backgroundModifier)
            .then(clickModifier),
        contentPadding = PaddingValues(all = TangemTheme.dimens2.x3),
        content = {
            // Icon (HEAD)
            EarnBlockIcon(
                iconUM = state.iconUM,
                modifier = Modifier
                    .layoutId(TangemRowLayoutId.HEAD)
                    .padding(end = TangemTheme.dimens2.x3),
            )

            // Title (START_TOP)
            Text(
                text = state.titleUM.text.resolveReference(),
                style = when (state.titleUM.style) {
                    EarnBlockUM.TitleUM.Style.Large -> TangemTheme.typography2.bodySemibold16
                    EarnBlockUM.TitleUM.Style.Small -> TangemTheme.typography2.captionMedium12
                },
                color = state.titleUM.color(),
                modifier = Modifier
                    .layoutId(TangemRowLayoutId.START_TOP)
                    .padding(end = TangemTheme.dimens2.x2),
            )

            // Subtitle (START_BOTTOM)
            val subtitle = state.subtitleUM
            if (subtitle is EarnBlockUM.SubtitleUM.Text) {
                Text(
                    text = subtitle.text.resolveReference(),
                    style = when (subtitle.style) {
                        EarnBlockUM.SubtitleUM.Style.Large -> TangemTheme.typography2.bodySemibold16
                        EarnBlockUM.SubtitleUM.Style.Small -> TangemTheme.typography2.captionMedium12
                    },
                    color = subtitle.color(),
                    modifier = Modifier
                        .layoutId(TangemRowLayoutId.START_BOTTOM)
                        .padding(end = TangemTheme.dimens2.x2),
                )
            }

            // Trailing
            EarnBlockTrailing(trailingUM = state.trailingUM)
        },
    )
}

@Composable
private fun resolveBackgroundModifier(backgroundUM: EarnBlockUM.BackgroundUM, shape: RoundedCornerShape): Modifier {
    return when (backgroundUM) {
        is EarnBlockUM.BackgroundUM.Surface -> Modifier
            .background(TangemTheme.colors2.surface.level3)
            .border(width = BorderWidth, color = TangemTheme.colors2.border.neutral.primary, shape = shape)
        is EarnBlockUM.BackgroundUM.Tinted -> {
            val tintColor = backgroundUM.color()
            Modifier
                .background(tintColor.copy(alpha = TINTED_BACKGROUND_ALPHA))
                .border(width = BorderWidth, color = tintColor.copy(alpha = TINTED_BORDER_ALPHA), shape = shape)
                .innerShadow(
                    shape = shape,
                    shadow = Shadow(
                        radius = InnerShadowBlur,
                        color = tintColor.copy(alpha = TINTED_INNER_SHADOW_ALPHA),
                        offset = DpOffset.Zero,
                    ),
                )
        }
    }
}

@Composable
private fun EarnBlockTrailing(trailingUM: EarnBlockUM.TrailingUM?) {
    when (trailingUM) {
        is EarnBlockUM.TrailingUM.Button -> {
            TangemButton(
                buttonUM = trailingUM.buttonUM,
                modifier = Modifier.layoutId(TangemRowLayoutId.TAIL),
            )
        }
        is EarnBlockUM.TrailingUM.Balance -> {
            if (!trailingUM.isBalanceHidden) {
                Text(
                    text = trailingUM.fiatValue.resolveAnnotatedReference(),
                    style = TangemTheme.typography2.bodySemibold16,
                    color = TangemTheme.colors2.text.neutral.primary,
                    modifier = Modifier.layoutId(TangemRowLayoutId.END_TOP),
                )
                Text(
                    text = trailingUM.cryptoValue.resolveReference(),
                    style = TangemTheme.typography2.captionMedium12,
                    color = TangemTheme.colors2.text.neutral.secondary,
                    modifier = Modifier.layoutId(TangemRowLayoutId.END_BOTTOM),
                )
            }
        }
        null -> Unit
    }
}

@Composable
private fun EarnBlockIcon(iconUM: EarnBlockUM.IconUM, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(TangemTheme.dimens2.x10),
    ) {
        if (iconUM is EarnBlockUM.IconUM.Glowing) {
            val glowShape = RoundedCornerShape(percent = 50)
            Box(
                modifier = Modifier
                    .size(TangemTheme.dimens2.x6)
                    .blur(radius = TangemTheme.dimens2.x4, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(color = iconUM.glowColor().copy(alpha = 0.7f), shape = glowShape),
            )
        }
        val iconRes = when (iconUM) {
            is EarnBlockUM.IconUM.Glowing -> iconUM.iconRes
            is EarnBlockUM.IconUM.Plain -> iconUM.iconRes
        }
        TangemIcon(
            tangemIconUM = TangemIconUM.Image(imageRes = iconRes),
            modifier = Modifier.size(TangemTheme.dimens2.x10),
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnBlock_Preview(@PreviewParameter(EarnBlockPreviewProvider::class) state: EarnBlockUM) {
    TangemThemePreviewRedesign {
        EarnBlock(
            state = state,
            modifier = Modifier.padding(TangemTheme.dimens2.x4),
        )
    }
}

private class EarnBlockPreviewProvider : CollectionPreviewParameterProvider<EarnBlockUM>(
    collection = listOf(
        EarnBlockUM.Loading,
        EarnBlockUM.Content(
            backgroundUM = EarnBlockUM.BackgroundUM.Surface,
            iconUM = EarnBlockUM.IconUM.Plain(iconRes = R.drawable.ic_staking_disable_40),
            titleUM = EarnBlockUM.TitleUM(
                text = resourceReference(CoreResR.string.staking_native),
                style = EarnBlockUM.TitleUM.Style.Large,
                color = { TangemTheme.colors2.text.neutral.tertiary },
            ),
            subtitleUM = EarnBlockUM.SubtitleUM.Text(
                text = resourceReference(CoreResR.string.staking_notification_network_error_text),
                style = EarnBlockUM.SubtitleUM.Style.Small,
                color = { TangemTheme.colors2.text.neutral.tertiary },
            ),
            trailingUM = null,
        ),
        EarnBlockUM.Content(
            backgroundUM = EarnBlockUM.BackgroundUM.Tinted { TangemTheme.colors2.border.status.accent },
            iconUM = EarnBlockUM.IconUM.Glowing(
                iconRes = R.drawable.ic_staking_40,
                glowColor = { TangemTheme.colors2.border.status.accent },
            ),
            titleUM = EarnBlockUM.TitleUM(
                text = resourceReference(CoreResR.string.token_details_staking_block_title),
                style = EarnBlockUM.TitleUM.Style.Small,
                color = { TangemTheme.colors2.text.status.accent },
            ),
            subtitleUM = EarnBlockUM.SubtitleUM.Text(
                text = stringReference("Average APR 5.24%"),
                style = EarnBlockUM.SubtitleUM.Style.Large,
                color = { TangemTheme.colors2.text.neutral.primary },
            ),
            trailingUM = EarnBlockUM.TrailingUM.Button(
                buttonUM = TangemButtonUM(
                    text = stringReference("Stake"),
                    type = TangemButtonType.Accent,
                    size = TangemButtonSize.X9,
                    shape = TangemButtonShape.Rounded,
                    onClick = {},
                ),
            ),
        ),
        EarnBlockUM.Content(
            backgroundUM = EarnBlockUM.BackgroundUM.Surface,
            iconUM = EarnBlockUM.IconUM.Glowing(
                iconRes = R.drawable.ic_staking_40,
                glowColor = { TangemTheme.colors2.text.status.accent },
            ),
            titleUM = EarnBlockUM.TitleUM(
                text = stringReference("Native staking"),
                style = EarnBlockUM.TitleUM.Style.Large,
                color = { TangemTheme.colors2.text.neutral.primary },
            ),
            subtitleUM = EarnBlockUM.SubtitleUM.Text(
                text = stringReference("$ 12.34 rewards"),
                style = EarnBlockUM.SubtitleUM.Style.Small,
                color = { TangemTheme.colors2.text.status.accent },
            ),
            trailingUM = EarnBlockUM.TrailingUM.Balance(
                fiatValue = stringReference("$ 500.17"),
                cryptoValue = stringReference("500.00 SOL"),
                isBalanceHidden = false,
                onClick = {},
            ),
        ),
    ),
)
// endregion