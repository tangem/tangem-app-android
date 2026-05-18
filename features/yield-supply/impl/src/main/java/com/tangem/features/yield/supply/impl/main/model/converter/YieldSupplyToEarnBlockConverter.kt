package com.tangem.features.yield.supply.impl.main.model.converter

import com.tangem.common.ui.earn.EarnBlockUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.yield.supply.impl.main.entity.YieldSupplyUM
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

    private fun buildAvailable(value: YieldSupplyUM.Available): EarnBlockUM.Content {
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
                iconUM = buildTitleIcon(value),
            ),
            subtitleUM = EarnBlockUM.SubtitleUM.Text(
                text = resourceReference(
                    id = CoreResR.string.yield_module_average_apy,
                    formatArgs = wrappedList(value.apy),
                ),
                style = EarnBlockUM.SubtitleUM.Style.Small,
                tone = EarnBlockUM.SubtitleUM.Tone.Accent,
            ),
            trailingUM = EarnBlockUM.TrailingUM.Button(
                text = resourceReference(CoreResR.string.details_title),
                style = EarnBlockUM.TrailingUM.Button.Style.Secondary,
            ),
            onClick = value.onClick,
        )
    }

    private fun buildTitleIcon(value: YieldSupplyUM.Content): EarnBlockUM.TitleUM.IconUM? = when {
        value.showWarningIcon -> EarnBlockUM.TitleUM.IconUM(tone = EarnBlockUM.TitleUM.IconTone.Warning)
        value.showInfoIcon -> EarnBlockUM.TitleUM.IconUM(tone = EarnBlockUM.TitleUM.IconTone.Info)
        else -> null
    }

    private fun buildProcessingEnter(): EarnBlockUM.Content {
        return EarnBlockUM.Content(
            type = EarnBlockUM.Type.YieldSupply,
            backgroundUM = EarnBlockUM.BackgroundUM.Surface,
            iconUM = EarnBlockUM.IconUM.Glowing(iconRes = CoreUiR.drawable.ic_yield_40),
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
        )
    }

    private fun buildProcessingExit(): EarnBlockUM.Content {
        return EarnBlockUM.Content(
            type = EarnBlockUM.Type.YieldSupply,
            backgroundUM = EarnBlockUM.BackgroundUM.Surface,
            iconUM = EarnBlockUM.IconUM.Plain(iconRes = CoreUiR.drawable.ic_yield_disabling_40),
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
        )
    }
}