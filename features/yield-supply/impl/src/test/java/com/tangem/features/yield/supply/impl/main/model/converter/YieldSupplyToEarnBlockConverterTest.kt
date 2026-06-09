package com.tangem.features.yield.supply.impl.main.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.earn.EarnBlockUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.yield.supply.impl.main.entity.YieldSupplyUM
import org.junit.jupiter.api.Test

internal class YieldSupplyToEarnBlockConverterTest {

    private val converter = YieldSupplyToEarnBlockConverter()

    @Test
    fun `GIVEN Initial WHEN convert THEN null`() {
        val result = converter.convert(YieldSupplyUM.Initial)

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN Unavailable WHEN convert THEN null`() {
        val result = converter.convert(YieldSupplyUM.Unavailable)

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN Loading WHEN convert THEN EarnBlockUM Loading`() {
        val result = converter.convert(YieldSupplyUM.Loading)

        assertThat(result).isEqualTo(EarnBlockUM.Loading)
    }

    @Test
    fun `GIVEN Content WHEN convert THEN Content`() {
        var clicked = false
        val content = YieldSupplyUM.Content(
            apy = "5.1",
            title = stringReference("Yield Mode"),
            subtitle = stringReference("Interest accrues automatically"),
            rewardsApy = stringReference("APY 5.1%"),
            onClick = { clicked = true },
            showWarningIcon = false,
            showInfoIcon = false,
        )

        val result = converter.convert(content)

        assertThat(result).isInstanceOf(EarnBlockUM.Content::class.java)
        val earnBlock = result as EarnBlockUM.Content
        assertThat(earnBlock.type).isEqualTo(EarnBlockUM.Type.YieldSupply)
        assertThat(earnBlock.backgroundUM).isEqualTo(EarnBlockUM.BackgroundUM.Surface)
        assertThat(earnBlock.iconUM).isInstanceOf(EarnBlockUM.IconUM.Glowing::class.java)
        assertThat(earnBlock.titleUM.tone).isEqualTo(EarnBlockUM.TitleUM.Tone.Primary)
        assertThat((earnBlock.subtitleUM as EarnBlockUM.SubtitleUM.Text).tone)
            .isEqualTo(EarnBlockUM.SubtitleUM.Tone.Accent)
        assertThat(earnBlock.trailingUM).isInstanceOf(EarnBlockUM.TrailingUM.Button::class.java)
        val button = earnBlock.trailingUM as EarnBlockUM.TrailingUM.Button
        assertThat(button.isEnabled).isTrue()
        assertThat(earnBlock.onClick).isNotNull()
        earnBlock.onClick?.invoke()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `GIVEN Processing Enter WHEN convert THEN Surface Content`() {
        val result = converter.convert(YieldSupplyUM.Processing.Enter)

        assertThat(result).isInstanceOf(EarnBlockUM.Content::class.java)
        val earnBlock = result as EarnBlockUM.Content
        assertThat(earnBlock.type).isEqualTo(EarnBlockUM.Type.YieldSupply)
        assertThat(earnBlock.backgroundUM).isEqualTo(EarnBlockUM.BackgroundUM.Surface)
        assertThat(earnBlock.iconUM).isInstanceOf(EarnBlockUM.IconUM.Glowing::class.java)
        assertThat(earnBlock.trailingUM).isNull()
    }

    @Test
    fun `GIVEN Processing Exit WHEN convert THEN Surface Content`() {
        val result = converter.convert(YieldSupplyUM.Processing.Exit)

        assertThat(result).isInstanceOf(EarnBlockUM.Content::class.java)
        val earnBlock = result as EarnBlockUM.Content
        assertThat(earnBlock.type).isEqualTo(EarnBlockUM.Type.YieldSupply)
        assertThat(earnBlock.backgroundUM).isEqualTo(EarnBlockUM.BackgroundUM.Surface)
        assertThat(earnBlock.iconUM).isInstanceOf(EarnBlockUM.IconUM.Plain::class.java)
        assertThat(earnBlock.trailingUM).isNull()
    }

    @Test
    fun `GIVEN Content with showWarningIcon WHEN convert THEN title Warning Icon`() {
        val content = YieldSupplyUM.Content(
            apy = "5.1",
            title = stringReference("Yield Mode"),
            subtitle = stringReference("Interest accrues automatically"),
            rewardsApy = stringReference("APY 5.1%"),
            onClick = {},
            showWarningIcon = true,
            showInfoIcon = false,
        )

        val result = converter.convert(content)

        assertThat(result).isInstanceOf(EarnBlockUM.Content::class.java)
        val earnBlock = result as EarnBlockUM.Content
        assertThat(earnBlock.titleUM.iconUM).isNotNull()
        assertThat(earnBlock.titleUM.iconUM?.tone).isEqualTo(EarnBlockUM.TitleUM.IconTone.Warning)
        assertThat(earnBlock.trailingUM).isInstanceOf(EarnBlockUM.TrailingUM.Button::class.java)
    }

    @Test
    fun `GIVEN Content with showInfoIcon WHEN convert THEN title Info Icon`() {
        val content = YieldSupplyUM.Content(
            apy = "5.1",
            title = stringReference("Yield Mode"),
            subtitle = stringReference("Interest accrues automatically"),
            rewardsApy = stringReference("APY 5.1%"),
            onClick = {},
            showWarningIcon = false,
            showInfoIcon = true,
        )

        val result = converter.convert(content)

        assertThat(result).isInstanceOf(EarnBlockUM.Content::class.java)
        val earnBlock = result as EarnBlockUM.Content
        assertThat(earnBlock.titleUM.iconUM).isNotNull()
        assertThat(earnBlock.titleUM.iconUM?.tone).isEqualTo(EarnBlockUM.TitleUM.IconTone.Info)
        assertThat(earnBlock.trailingUM).isInstanceOf(EarnBlockUM.TrailingUM.Button::class.java)
    }

    @Test
    fun `GIVEN Content with both icons WHEN convert THEN Warning takes precedence`() {
        val content = YieldSupplyUM.Content(
            apy = "5.1",
            title = stringReference("Yield Mode"),
            subtitle = stringReference("Interest accrues automatically"),
            rewardsApy = stringReference("APY 5.1%"),
            onClick = {},
            showWarningIcon = true,
            showInfoIcon = true,
        )

        val result = converter.convert(content)

        val earnBlock = result as EarnBlockUM.Content
        assertThat(earnBlock.titleUM.iconUM).isNotNull()
        assertThat(earnBlock.titleUM.iconUM?.tone).isEqualTo(EarnBlockUM.TitleUM.IconTone.Warning)
    }

    @Test
    fun `GIVEN Available WHEN convert THEN Content with expected structure`() {
        var clicked = false
        val available = YieldSupplyUM.Available(
            apy = "5.1",
            apyText = stringReference("5.1 % APY"),
            title = stringReference("Yield Mode"),
            onClick = { clicked = true },
            onLearnMoreClick = {},
        )

        val result = converter.convert(available)

        assertThat(result).isInstanceOf(EarnBlockUM.Content::class.java)
        val content = result as EarnBlockUM.Content

        assertThat(content.type).isEqualTo(EarnBlockUM.Type.YieldSupply)
        assertThat(content.backgroundUM).isEqualTo(EarnBlockUM.BackgroundUM.AccentSoft)
        assertThat(content.iconUM).isInstanceOf(EarnBlockUM.IconUM.Glowing::class.java)

        assertThat(content.titleUM.style).isEqualTo(EarnBlockUM.TitleUM.Style.Large)
        assertThat(content.titleUM.tone).isEqualTo(EarnBlockUM.TitleUM.Tone.Primary)

        assertThat(content.subtitleUM).isInstanceOf(EarnBlockUM.SubtitleUM.Text::class.java)
        val subtitle = content.subtitleUM as EarnBlockUM.SubtitleUM.Text
        assertThat(subtitle.style).isEqualTo(EarnBlockUM.SubtitleUM.Style.Small)
        assertThat(subtitle.tone).isEqualTo(EarnBlockUM.SubtitleUM.Tone.Accent)

        assertThat(content.trailingUM).isInstanceOf(EarnBlockUM.TrailingUM.Button::class.java)
        val button = content.trailingUM as EarnBlockUM.TrailingUM.Button
        assertThat(button.isEnabled).isTrue()

        assertThat(content.onClick).isNotNull()
        content.onClick?.invoke()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `GIVEN Available isBoostAvailable WHEN convert THEN Promo with both button callbacks`() {
        var activateClicked = false
        var learnMoreClicked = false
        val available = YieldSupplyUM.Available(
            apy = "5.1",
            apyText = stringReference("APY 5.1% x3 → 15.3%"),
            title = stringReference("Special offer for Yield mode"),
            onClick = { activateClicked = true },
            onLearnMoreClick = { learnMoreClicked = true },
            isBoostAvailable = true,
        )

        val result = converter.convert(available)

        assertThat(result).isInstanceOf(EarnBlockUM.Promo::class.java)
        val promo = result as EarnBlockUM.Promo
        assertThat(promo.type).isEqualTo(EarnBlockUM.Type.YieldSupply)
        assertThat(promo.backgroundUM).isEqualTo(EarnBlockUM.BackgroundUM.AccentSoft)
        assertThat(promo.iconUM).isInstanceOf(EarnBlockUM.IconUM.Glowing::class.java)

        promo.onSecondaryClick()
        assertThat(learnMoreClicked).isTrue()

        promo.onPrimaryClick()
        assertThat(activateClicked).isTrue()
    }
}