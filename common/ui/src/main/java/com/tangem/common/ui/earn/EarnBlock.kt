package com.tangem.common.ui.earn

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.earn.EarnBlockUM.Type
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
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.res.R as CoreResR

private const val TINTED_BACKGROUND_ALPHA = 0.1f
private const val TINTED_BORDER_ALPHA = 0.1f
private const val TINTED_INNER_SHADOW_ALPHA = 0.3f
private const val GLOW_ALPHA = 0.7f
private val BorderWidth = 1.dp
private val InnerShadowBlur = 20.dp
private val ShimmerSubtitleWidth = 78.dp
private val LoaderSize = 12.dp
private val LoaderStrokeWidth = 1.5.dp

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

    val clickModifier = state.onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier

    TangemRowContainer(
        modifier = modifier
            .clip(shape)
            .then(clickModifier.backgroundModifier(state.type, state.backgroundUM, shape)),
        contentPadding = PaddingValues(all = TangemTheme.dimens2.x3),
        content = {
            EarnBlockIcon(
                type = state.type,
                iconUM = state.iconUM,
                modifier = Modifier
                    .layoutId(TangemRowLayoutId.HEAD)
                    .padding(end = TangemTheme.dimens2.x3),
            )

            EarnBlockTitle(
                titleUM = state.titleUM,
                type = state.type,
                modifier = Modifier
                    .layoutId(TangemRowLayoutId.START_TOP)
                    .padding(end = TangemTheme.dimens2.x2),
            )

            val subtitle = state.subtitleUM
            if (subtitle is EarnBlockUM.SubtitleUM.Text) {
                EarnBlockSubtitle(
                    subtitle = subtitle,
                    type = state.type,
                    modifier = Modifier
                        .layoutId(TangemRowLayoutId.START_BOTTOM)
                        .padding(end = TangemTheme.dimens2.x2),
                )
            }

            EarnBlockTrailing(type = state.type, trailingUM = state.trailingUM, onClick = state.onClick)
        },
    )
}

@Composable
private fun Modifier.backgroundModifier(
    type: Type,
    backgroundUM: EarnBlockUM.BackgroundUM,
    shape: RoundedCornerShape,
): Modifier {
    return when (backgroundUM) {
        is EarnBlockUM.BackgroundUM.Surface -> this
            .background(TangemTheme.colors2.surface.level3)
            .border(width = BorderWidth, color = TangemTheme.colors2.border.neutral.primary, shape = shape)
        is EarnBlockUM.BackgroundUM.AccentSoft -> tintedBackground(type.accentSoftTint(), shape)
        is EarnBlockUM.BackgroundUM.AccentStrong -> tintedBackground(type.accentStrongTint(), shape)
    }
}

private fun Modifier.tintedBackground(tintColor: Color, shape: RoundedCornerShape): Modifier = this
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

@Composable
private fun EarnBlockTrailing(type: Type, trailingUM: EarnBlockUM.TrailingUM?, onClick: (() -> Unit)?) {
    when (trailingUM) {
        is EarnBlockUM.TrailingUM.Button -> {
            TangemButton(
                buttonUM = TangemButtonUM(
                    text = trailingUM.text,
                    type = trailingUM.style.buttonType(type),
                    size = TangemButtonSize.X9,
                    shape = TangemButtonShape.Rounded,
                    isEnabled = trailingUM.isEnabled,
                    onClick = onClick ?: {},
                ),
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
private fun EarnBlockTitle(titleUM: EarnBlockUM.TitleUM, type: Type, modifier: Modifier = Modifier) {
    val titleText: @Composable () -> Unit = {
        Text(
            text = titleUM.text.resolveReference(),
            style = titleUM.style.textStyle,
            color = titleUM.tone.color(type),
        )
    }
    val icon = titleUM.iconUM
    if (icon == null) {
        Box(modifier = modifier) { titleText() }
        return
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        titleText()
        Spacer(modifier = Modifier.width(TangemTheme.dimens2.x1))
        TangemIcon(
            tangemIconUM = TangemIconUM.Icon(
                iconRes = icon.tone.iconRes(),
                tintReference = { icon.tone.tint() },
            ),
            modifier = Modifier.size(TangemTheme.dimens2.x4),
        )
    }
}

@Composable
private fun EarnBlockSubtitle(subtitle: EarnBlockUM.SubtitleUM.Text, type: Type, modifier: Modifier = Modifier) {
    val textStyle = subtitle.style.textStyle
    val textColor = subtitle.tone.color(type)
    if (subtitle.loader == null) {
        Text(text = subtitle.text.resolveReference(), style = textStyle, color = textColor, modifier = modifier)
        return
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = subtitle.text.resolveReference(), style = textStyle, color = textColor)
        Spacer(modifier = Modifier.width(3.dp))
        CircularProgressIndicator(
            color = subtitle.loader.tone.color(),
            strokeWidth = LoaderStrokeWidth,
            strokeCap = StrokeCap.Round,
            modifier = Modifier.size(LoaderSize),
        )
    }
}

@Composable
private fun EarnBlockIcon(type: Type, iconUM: EarnBlockUM.IconUM, modifier: Modifier = Modifier) {
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
                    .background(color = type.accentGlow().copy(alpha = GLOW_ALPHA), shape = glowShape),
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

// region Type → theme mapping
@Composable
@ReadOnlyComposable
private fun Type.accentText(): Color = when (this) {
    Type.Staking -> TangemTheme.colors2.text.status.accent
    Type.YieldSupply -> TangemTheme.colors2.text.status.positive
}

@Composable
@ReadOnlyComposable
private fun Type.accentGlow(): Color = when (this) {
    Type.Staking -> TangemTheme.colors2.border.status.accent
    Type.YieldSupply -> TangemTheme.colors2.text.status.positive
}

@Composable
@ReadOnlyComposable
private fun Type.accentSoftTint(): Color = when (this) {
    Type.Staking -> TangemTheme.colors2.markers.backgroundTintedBlue
    Type.YieldSupply -> TangemTheme.colors2.markers.backgroundTintedGreen
}

@Composable
@ReadOnlyComposable
private fun Type.accentStrongTint(): Color = when (this) {
    Type.Staking -> TangemTheme.colors2.text.status.accent
    Type.YieldSupply -> TangemTheme.colors2.text.status.positive
}

private fun EarnBlockUM.TrailingUM.Button.Style.buttonType(type: Type): TangemButtonType = when (this) {
    EarnBlockUM.TrailingUM.Button.Style.Default -> when (type) {
        Type.Staking -> TangemButtonType.Accent
        Type.YieldSupply -> TangemButtonType.Positive
    }
    EarnBlockUM.TrailingUM.Button.Style.Secondary -> TangemButtonType.Secondary
}

@Composable
@ReadOnlyComposable
private fun EarnBlockUM.TitleUM.Tone.color(type: Type): Color = when (this) {
    EarnBlockUM.TitleUM.Tone.Primary -> TangemTheme.colors2.text.neutral.primary
    EarnBlockUM.TitleUM.Tone.Secondary -> TangemTheme.colors2.text.neutral.secondary
    EarnBlockUM.TitleUM.Tone.Disabled -> TangemTheme.colors2.text.neutral.tertiary
    EarnBlockUM.TitleUM.Tone.Accent -> type.accentText()
}

@Composable
@ReadOnlyComposable
private fun EarnBlockUM.SubtitleUM.Tone.color(type: Type): Color = when (this) {
    EarnBlockUM.SubtitleUM.Tone.Primary -> TangemTheme.colors2.text.neutral.primary
    EarnBlockUM.SubtitleUM.Tone.Disabled -> TangemTheme.colors2.text.neutral.tertiary
    EarnBlockUM.SubtitleUM.Tone.Accent -> type.accentText()
}

private fun EarnBlockUM.TitleUM.IconTone.iconRes(): Int = when (this) {
    EarnBlockUM.TitleUM.IconTone.Warning -> R.drawable.ic_attention_default_24
    EarnBlockUM.TitleUM.IconTone.Info -> R.drawable.ic_alert_circle_24
}

@Composable
@ReadOnlyComposable
private fun EarnBlockUM.TitleUM.IconTone.tint(): Color = when (this) {
    EarnBlockUM.TitleUM.IconTone.Warning -> TangemTheme.colors2.graphic.status.attention
    EarnBlockUM.TitleUM.IconTone.Info -> TangemTheme.colors2.graphic.neutral.secondary
}

@Composable
@ReadOnlyComposable
private fun EarnBlockUM.SubtitleUM.LoaderTone.color(): Color = when (this) {
    EarnBlockUM.SubtitleUM.LoaderTone.Positive -> TangemTheme.colors2.text.status.positive
    EarnBlockUM.SubtitleUM.LoaderTone.Muted -> TangemTheme.colors2.graphic.neutral.tertiaryConstant
}

private val EarnBlockUM.TitleUM.Style.textStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() = when (this) {
        EarnBlockUM.TitleUM.Style.Large -> TangemTheme.typography2.bodySemibold16
        EarnBlockUM.TitleUM.Style.Small -> TangemTheme.typography2.captionMedium12
    }

private val EarnBlockUM.SubtitleUM.Style.textStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() = when (this) {
        EarnBlockUM.SubtitleUM.Style.Large -> TangemTheme.typography2.bodySemibold16
        EarnBlockUM.SubtitleUM.Style.Small -> TangemTheme.typography2.captionMedium12
    }
// endregion

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnBlock_Staking_Preview(@PreviewParameter(EarnBlockStakingPreviewProvider::class) state: EarnBlockUM) {
    TangemThemePreviewRedesign {
        EarnBlock(
            state = state,
            modifier = Modifier.padding(TangemTheme.dimens2.x4),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnBlock_YieldSupply_Preview(
    @PreviewParameter(EarnBlockYieldSupplyPreviewProvider::class) state: EarnBlockUM,
) {
    TangemThemePreviewRedesign {
        EarnBlock(
            state = state,
            modifier = Modifier.padding(TangemTheme.dimens2.x4),
        )
    }
}

private class EarnBlockStakingPreviewProvider : CollectionPreviewParameterProvider<EarnBlockUM>(
    collection = listOf(
        EarnBlockUM.Loading,
        EarnBlockUM.Content(
            type = Type.Staking,
            backgroundUM = EarnBlockUM.BackgroundUM.Surface,
            iconUM = EarnBlockUM.IconUM.Plain(iconRes = R.drawable.ic_staking_disable_40),
            titleUM = EarnBlockUM.TitleUM(
                text = resourceReference(CoreResR.string.common_stake),
                style = EarnBlockUM.TitleUM.Style.Large,
                tone = EarnBlockUM.TitleUM.Tone.Disabled,
            ),
            subtitleUM = EarnBlockUM.SubtitleUM.Text(
                text = resourceReference(CoreResR.string.staking_notification_network_error_text),
                style = EarnBlockUM.SubtitleUM.Style.Small,
                tone = EarnBlockUM.SubtitleUM.Tone.Disabled,
            ),
            trailingUM = null,
        ),
        EarnBlockUM.Content(
            type = Type.Staking,
            backgroundUM = EarnBlockUM.BackgroundUM.AccentSoft,
            iconUM = EarnBlockUM.IconUM.Glowing(iconRes = R.drawable.ic_staking_40),
            titleUM = EarnBlockUM.TitleUM(
                text = resourceReference(CoreResR.string.common_staking),
                style = EarnBlockUM.TitleUM.Style.Large,
                tone = EarnBlockUM.TitleUM.Tone.Primary,
            ),
            subtitleUM = EarnBlockUM.SubtitleUM.Text(
                text = stringReference("Average APR 5.24%"),
                style = EarnBlockUM.SubtitleUM.Style.Small,
                tone = EarnBlockUM.SubtitleUM.Tone.Disabled,
            ),
            trailingUM = EarnBlockUM.TrailingUM.Button(
                text = resourceReference(CoreResR.string.common_stake),
            ),
            onClick = {},
        ),
        EarnBlockUM.Content(
            type = Type.Staking,
            backgroundUM = EarnBlockUM.BackgroundUM.Surface,
            iconUM = EarnBlockUM.IconUM.Glowing(iconRes = R.drawable.ic_staking_40),
            titleUM = EarnBlockUM.TitleUM(
                text = resourceReference(CoreResR.string.staking_enabled),
                style = EarnBlockUM.TitleUM.Style.Large,
                tone = EarnBlockUM.TitleUM.Tone.Primary,
            ),
            subtitleUM = EarnBlockUM.SubtitleUM.Text(
                text = stringReference("$ 12.34 rewards"),
                style = EarnBlockUM.SubtitleUM.Style.Small,
                tone = EarnBlockUM.SubtitleUM.Tone.Accent,
            ),
            trailingUM = EarnBlockUM.TrailingUM.Balance(
                fiatValue = stringReference("$ 500.17"),
                cryptoValue = stringReference("500.00 SOL"),
                isBalanceHidden = false,
            ),
            onClick = {},
        ),
    ),
)

private class EarnBlockYieldSupplyPreviewProvider : CollectionPreviewParameterProvider<EarnBlockUM>(
    collection = listOf(
        // Available — promo entry: AccentSoft background, "More" button
        EarnBlockUM.Content(
            type = Type.YieldSupply,
            backgroundUM = EarnBlockUM.BackgroundUM.AccentSoft,
            iconUM = EarnBlockUM.IconUM.Glowing(iconRes = R.drawable.ic_yield_40),
            titleUM = EarnBlockUM.TitleUM(
                text = resourceReference(
                    id = CoreResR.string.yield_module_token_details_earn_notification_subtitle,
                    formatArgs = wrappedList("5.24"),
                ),
                style = EarnBlockUM.TitleUM.Style.Large,
                tone = EarnBlockUM.TitleUM.Tone.Primary,
            ),
            subtitleUM = EarnBlockUM.SubtitleUM.Text(
                text = resourceReference(
                    CoreResR.string.yield_module_token_details_earn_notification_description,
                ),
                style = EarnBlockUM.SubtitleUM.Style.Small,
                tone = EarnBlockUM.SubtitleUM.Tone.Accent,
            ),
            trailingUM = EarnBlockUM.TrailingUM.Button(
                text = resourceReference(CoreResR.string.common_more),
            ),
            onClick = {},
        ),
        // Content — yield enabled, "Details" button
        EarnBlockUM.Content(
            type = Type.YieldSupply,
            backgroundUM = EarnBlockUM.BackgroundUM.Surface,
            iconUM = EarnBlockUM.IconUM.Glowing(iconRes = R.drawable.ic_yield_40),
            titleUM = EarnBlockUM.TitleUM(
                text = resourceReference(CoreResR.string.yield_module_transaction_enter),
                style = EarnBlockUM.TitleUM.Style.Large,
                tone = EarnBlockUM.TitleUM.Tone.Primary,
            ),
            subtitleUM = EarnBlockUM.SubtitleUM.Text(
                text = resourceReference(
                    id = CoreResR.string.yield_module_average_apy,
                    formatArgs = wrappedList("5.24"),
                ),
                style = EarnBlockUM.SubtitleUM.Style.Small,
                tone = EarnBlockUM.SubtitleUM.Tone.Accent,
            ),
            trailingUM = EarnBlockUM.TrailingUM.Button(
                text = resourceReference(CoreResR.string.details_title),
                style = EarnBlockUM.TrailingUM.Button.Style.Secondary,
            ),
            onClick = {},
        ),
        // Content with Warning title icon
        EarnBlockUM.Content(
            type = Type.YieldSupply,
            backgroundUM = EarnBlockUM.BackgroundUM.Surface,
            iconUM = EarnBlockUM.IconUM.Glowing(iconRes = R.drawable.ic_yield_40),
            titleUM = EarnBlockUM.TitleUM(
                text = resourceReference(CoreResR.string.common_yield_mode),
                style = EarnBlockUM.TitleUM.Style.Large,
                tone = EarnBlockUM.TitleUM.Tone.Primary,
                iconUM = EarnBlockUM.TitleUM.IconUM(tone = EarnBlockUM.TitleUM.IconTone.Warning),
            ),
            subtitleUM = EarnBlockUM.SubtitleUM.Text(
                text = resourceReference(
                    id = CoreResR.string.yield_module_average_apy,
                    formatArgs = wrappedList("5.24"),
                ),
                style = EarnBlockUM.SubtitleUM.Style.Small,
                tone = EarnBlockUM.SubtitleUM.Tone.Accent,
            ),
            trailingUM = EarnBlockUM.TrailingUM.Button(
                text = resourceReference(CoreResR.string.details_title),
                style = EarnBlockUM.TrailingUM.Button.Style.Secondary,
            ),
            onClick = {},
        ),
        // Content with Info title icon
        EarnBlockUM.Content(
            type = Type.YieldSupply,
            backgroundUM = EarnBlockUM.BackgroundUM.Surface,
            iconUM = EarnBlockUM.IconUM.Glowing(iconRes = R.drawable.ic_yield_40),
            titleUM = EarnBlockUM.TitleUM(
                text = resourceReference(CoreResR.string.common_yield_mode),
                style = EarnBlockUM.TitleUM.Style.Large,
                tone = EarnBlockUM.TitleUM.Tone.Primary,
                iconUM = EarnBlockUM.TitleUM.IconUM(tone = EarnBlockUM.TitleUM.IconTone.Info),
            ),
            subtitleUM = EarnBlockUM.SubtitleUM.Text(
                text = resourceReference(
                    id = CoreResR.string.yield_module_average_apy,
                    formatArgs = wrappedList("5.24"),
                ),
                style = EarnBlockUM.SubtitleUM.Style.Small,
                tone = EarnBlockUM.SubtitleUM.Tone.Accent,
            ),
            trailingUM = EarnBlockUM.TrailingUM.Button(
                text = resourceReference(CoreResR.string.details_title),
                style = EarnBlockUM.TrailingUM.Button.Style.Secondary,
            ),
            onClick = {},
        ),
        // Processing.Enter — enabling, no trailing
        EarnBlockUM.Content(
            type = Type.YieldSupply,
            backgroundUM = EarnBlockUM.BackgroundUM.Surface,
            iconUM = EarnBlockUM.IconUM.Glowing(iconRes = R.drawable.ic_yield_40),
            titleUM = EarnBlockUM.TitleUM(
                text = resourceReference(CoreResR.string.common_yield_mode),
                style = EarnBlockUM.TitleUM.Style.Large,
                tone = EarnBlockUM.TitleUM.Tone.Primary,
            ),
            subtitleUM = EarnBlockUM.SubtitleUM.Text(
                text = resourceReference(CoreResR.string.common_enabling),
                style = EarnBlockUM.SubtitleUM.Style.Small,
                tone = EarnBlockUM.SubtitleUM.Tone.Accent,
                loader = EarnBlockUM.SubtitleUM.Loader(tone = EarnBlockUM.SubtitleUM.LoaderTone.Positive),
            ),
            trailingUM = null,
        ),
        // Processing.Exit — disabling, no trailing, plain icon
        EarnBlockUM.Content(
            type = Type.YieldSupply,
            backgroundUM = EarnBlockUM.BackgroundUM.Surface,
            iconUM = EarnBlockUM.IconUM.Plain(iconRes = R.drawable.ic_yield_disabling_40),
            titleUM = EarnBlockUM.TitleUM(
                text = resourceReference(CoreResR.string.common_yield_mode),
                style = EarnBlockUM.TitleUM.Style.Large,
                tone = EarnBlockUM.TitleUM.Tone.Primary,
            ),
            subtitleUM = EarnBlockUM.SubtitleUM.Text(
                text = resourceReference(CoreResR.string.common_disabling),
                style = EarnBlockUM.SubtitleUM.Style.Small,
                tone = EarnBlockUM.SubtitleUM.Tone.Disabled,
                loader = EarnBlockUM.SubtitleUM.Loader(tone = EarnBlockUM.SubtitleUM.LoaderTone.Muted),
            ),
            trailingUM = null,
        ),
    ),
)
// endregion