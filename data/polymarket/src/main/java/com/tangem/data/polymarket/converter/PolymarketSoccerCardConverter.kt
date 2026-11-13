package com.tangem.data.polymarket.converter

import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketSoccerCard
import com.tangem.domain.polymarket.model.PolymarketSoccerCard.Button
import com.tangem.domain.polymarket.model.PolymarketSoccerCard.ButtonSide
import com.tangem.domain.polymarket.model.PolymarketSoccerCard.Content
import com.tangem.domain.polymarket.model.PolymarketSoccerCard.Position
import com.tangem.grow.datasource.polymarket.models.PolymarketSoccerButtonDto
import com.tangem.grow.datasource.polymarket.models.PolymarketSoccerCardDto
import com.tangem.grow.datasource.polymarket.models.PolymarketSoccerPickerPositionDto
import java.math.BigDecimal

internal object PolymarketSoccerCardConverter {

    private val SUPPORTED_TEMPLATES = setOf("three_way", "yes_no", "list", "side_picker", "over_under_picker")
    private val THREE_WAY_SIDES = listOf(ButtonSide.LEFT, ButtonSide.MIDDLE, ButtonSide.RIGHT)
    private val YES_NO_SIDES = listOf(ButtonSide.YES, ButtonSide.NO)
    private val TEAM_SIDES = listOf(ButtonSide.LEFT, ButtonSide.RIGHT)
    private val OVER_UNDER_SIDES = listOf(ButtonSide.OVER, ButtonSide.UNDER)

    fun convert(dto: PolymarketSoccerCardDto): PolymarketSoccerCard? {
        val key = PolymarketSoccerCard.Key.entries.find { it.value == dto.key } ?: return null
        if (dto.template !in SUPPORTED_TEMPLATES) return null
        val markets = dto.markets.map(PolymarketMarketConverter::convert)
        val marketsById = markets.associateBy { it.id }
        if (marketsById.size != markets.size) return null
        val content = when (dto.template) {
            "three_way" -> buttons(dto.buttons, marketsById, THREE_WAY_SIDES)?.let(Content::ThreeWay)
            "yes_no" -> buttons(dto.buttons, marketsById, YES_NO_SIDES)?.let(Content::YesNo)
            "list" -> buttons(dto.rows, marketsById)?.takeIf { rows -> rows.all { it.side == ButtonSide.YES } }
                ?.let(Content::Rows)
            "side_picker" -> positions(dto.positions, marketsById, TEAM_SIDES)?.let(Content::SidePicker)
            "over_under_picker" -> overUnder(dto, key, marketsById)
            else -> null
        } ?: return null
        return PolymarketSoccerCard(
            key = key,
            sportsMarketType = dto.sportsMarketType,
            markets = markets,
            content = content,
        )
    }

    private fun overUnder(
        dto: PolymarketSoccerCardDto,
        key: PolymarketSoccerCard.Key,
        markets: Map<String, PolymarketMarket>,
    ): Content.OverUnderPicker? {
        val teamSide = when (dto.teamSide) {
            "left" -> PolymarketSoccerCard.TeamSide.LEFT
            "right" -> PolymarketSoccerCard.TeamSide.RIGHT
            null -> null
            else -> return null
        }
        if (key == PolymarketSoccerCard.Key.TEAM_TOTALS && teamSide == null) return null
        return positions(dto.positions, markets, OVER_UNDER_SIDES)?.let { Content.OverUnderPicker(it, teamSide) }
    }

    private fun buttons(
        values: List<PolymarketSoccerButtonDto>?,
        markets: Map<String, PolymarketMarket>,
        expectedSides: List<ButtonSide>? = null,
    ): List<Button>? {
        if (values.isNullOrEmpty()) return null
        val buttons = values.map { button ->
            val side = ButtonSide.entries.find { it.value == button.side } ?: return null
            val market = markets[button.marketId] ?: return null
            if (market.outcomes.none { it.assetId == button.assetId }) return null
            Button(side = side, label = button.label, marketId = button.marketId, assetId = button.assetId)
        }
        if (expectedSides != null && buttons.map { it.side } != expectedSides) return null
        return buttons
    }

    private fun positions(
        values: List<PolymarketSoccerPickerPositionDto>?,
        markets: Map<String, PolymarketMarket>,
        sides: List<ButtonSide>,
    ): List<Position>? {
        if (values.isNullOrEmpty() || values.count { it.isDefault } != 1) return null
        if (values.distinctBy { it.marketId }.size != values.size) return null
        return values.map { position ->
            if (!position.line.isFinite()) return null
            val buttons = buttons(position.buttons, markets, sides) ?: return null
            if (buttons.any { it.marketId != position.marketId }) return null
            Position(
                marketId = position.marketId,
                pickerLabel = position.pickerLabel,
                line = BigDecimal.valueOf(position.line),
                isDefault = position.isDefault,
                buttons = buttons,
            )
        }
    }
}