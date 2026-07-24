package com.tangem.features.yield.supply.impl.main.model.converter

import com.tangem.common.ui.earn.EarnBlockUM
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.yield.supply.impl.main.entity.YieldSupplyUM
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.R as CoreUiR

internal class YieldSupplyToEarnBlockConverter : Converter<YieldSupplyUM, EarnBlockUM?> {

    override fun convert(value: YieldSupplyUM): EarnBlockUM? = when (value) {
        is YieldSupplyUM.Initial,
        is YieldSupplyUM.Unavailable,
        -> null
        is YieldSupplyUM.Available -> buildAvailable(value)
        is YieldSupplyUM.Content -> buildContent(value)
        is YieldSupplyUM.Processing.Enter -> buildProcessingEnter()
        is YieldSupplyUM.Processing.Exit -> buildProcessingExit()
        is YieldSupplyUM.Loading -> EarnBlockUM.Loading
    }

    private fun buildAvailable(value: YieldSupplyUM.Available): EarnBlockUM = if (value.isBoostAvailable) {
        buildBoostedPromo(value)
    } else {
        buildAvailableContent(value)
    }

    private fun buildBoostedPromo(value: YieldSupplyUM.Available): EarnBlockUM.Promo {
        return EarnBlockUM.Promo(
            type = EarnBlockUM.Type.YieldSupply,
            backgroundUM = EarnBlockUM.BackgroundUM.AccentSoft,
            iconUM = EarnBlockUM.IconUM.Glowing(iconRes = CoreUiR.drawable.ic_yield_40),
            title = combinedReference(value.title, stringReference("\n"), value.apyText),
            subtitle = resourceReference(CoreResR.string.yield_apy_boost_banner_subtitle),
            onPrimaryClick = value.onClick,
            onSecondaryClick = value.onLearnMoreClick,
        )
    }

    private fun buildAvailableContent(value: YieldSupplyUM.Available): EarnBlockUM.Content {
        return EarnBlockUM.Content(
            type = EarnBlockUM.Type.YieldSupply,
            backgroundUM = EarnBlockUM.BackgroundUM.AccentSoft,
            iconUM = EarnBlockUM.IconUM.Glowing(iconRes = CoreUiR.drawable.ic_yield_40),
            titleUM = EarnBlockUM.TitleUM(
                text = resourceReference(
                    id = CoreResR.string.yield_module_token_details_earn_notification_subtitle,
                    formatArgs = wrappedList(value.apy),
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
            onClick = value.onClick,
        )
    }

    private fun buildContent(value: YieldSupplyUM.Content): EarnBlockUM.Content {
        return EarnBlockUM.Content(
            type = EarnBlockUM.Type.YieldSupply,
            backgroundUM = EarnBlockUM.BackgroundUM.Surface,
            iconUM = EarnBlockUM.IconUM.Glowing(iconRes = CoreUiR.drawable.ic_yield_40),
            titleUM = EarnBlockUM.TitleUM(
                text = resourceReference(CoreResR.string.yield_module_transaction_enter),
                style = EarnBlockUM.TitleUM.Style.Large,
                tone = EarnBlockUM.TitleUM.Tone.Primary,
            ),
            subtitleUM = EarnBlockUM.SubtitleUM.Text(
                text = combinedReference(
                    resourceReference(CoreResR.string.yield_module_earn_sheet_current_apy_title),
                    stringReference(StringsSigns.WHITE_SPACE),
                    stringReference(value.apy + StringsSigns.PERCENT),
                ),
                style = EarnBlockUM.SubtitleUM.Style.Small,
                tone = EarnBlockUM.SubtitleUM.Tone.Accent,
            ),
            trailingUM = buildTrailing(value),
            onClick = value.onClick,
        )
    }

    private fun buildTrailing(value: YieldSupplyUM.Content): EarnBlockUM.TrailingUM = when {
        value.shouldShowWarningIcon -> EarnBlockUM.TrailingUM.StatusIcon(
            tone = EarnBlockUM.TrailingUM.StatusIcon.Tone.Warning,
        )
        value.shouldShowInfoIcon -> EarnBlockUM.TrailingUM.StatusIcon(
            tone = EarnBlockUM.TrailingUM.StatusIcon.Tone.Info,
        )
        else -> EarnBlockUM.TrailingUM.Chevron
    }

    private fun buildProcessingEnter(): EarnBlockUM.Content {
        return EarnBlockUM.Content(
            type = EarnBlockUM.Type.YieldSupply,
            backgroundUM = EarnBlockUM.BackgroundUM.Surface,
            iconUM = EarnBlockUM.IconUM.Glowing(iconRes = CoreUiR.drawable.ic_yield_40),
            titleUM = EarnBlockUM.TitleUM(
                text = combinedReference(
                    resourceReference(CoreResR.string.common_yield_mode),
                    stringReference(StringsSigns.WHITE_SPACE),
                    resourceReference(CoreResR.string.common_enabling),
                ),
                style = EarnBlockUM.TitleUM.Style.Large,
                tone = EarnBlockUM.TitleUM.Tone.Primary,
            ),
            subtitleUM = null,
            trailingUM = EarnBlockUM.TrailingUM.Loader(
                tone = EarnBlockUM.TrailingUM.Loader.LoaderTone.Positive,
            ),
        )
    }

    private fun buildProcessingExit(): EarnBlockUM.Content {
        return EarnBlockUM.Content(
            type = EarnBlockUM.Type.YieldSupply,
            backgroundUM = EarnBlockUM.BackgroundUM.Surface,
            iconUM = EarnBlockUM.IconUM.Glowing(
                iconRes = CoreUiR.drawable.ic_yield_40,
                tone = EarnBlockUM.IconUM.Tone.Warning,
            ),
            titleUM = EarnBlockUM.TitleUM(
                text = combinedReference(
                    resourceReference(CoreResR.string.common_yield_mode),
                    stringReference(StringsSigns.WHITE_SPACE),
                    resourceReference(CoreResR.string.common_disabling),
                ),
                style = EarnBlockUM.TitleUM.Style.Large,
                tone = EarnBlockUM.TitleUM.Tone.Primary,
            ),
            subtitleUM = null,
            trailingUM = EarnBlockUM.TrailingUM.Loader(
                tone = EarnBlockUM.TrailingUM.Loader.LoaderTone.Muted,
            ),
        )
    }
}