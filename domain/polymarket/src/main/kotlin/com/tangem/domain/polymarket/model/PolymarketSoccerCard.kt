package com.tangem.domain.polymarket.model

import java.math.BigDecimal

/** Card and market order belongs to the BFF. Team totals share a key and differ by teamSide. */
data class PolymarketSoccerCard(
    val key: Key,
    val sportsMarketType: String?,
    val markets: List<PolymarketMarket>,
    val content: Content,
) {
    enum class Key(val value: String) {
        MONEYLINE("moneyline"), RESULT("result"), SPREADS("spreads"), TOTALS("totals"),
        BOTH_TEAMS_TO_SCORE("both_teams_to_score"), FIRST_TO_SCORE("first_to_score"),
        TEAM_TOTALS("team_totals"), EXACT_SCORE("exact_score"),
    }

    sealed interface Content {
        data class ThreeWay(val buttons: List<Button>) : Content
        data class SidePicker(val positions: List<Position>) : Content
        data class OverUnderPicker(val positions: List<Position>, val teamSide: TeamSide?) : Content
        data class YesNo(val buttons: List<Button>) : Content
        data class Rows(val rows: List<Button>) : Content
    }

    enum class TeamSide { LEFT, RIGHT }

    enum class ButtonSide(val value: String) {
        LEFT("left"), MIDDLE("middle"), RIGHT("right"), YES("yes"), NO("no"), OVER("over"), UNDER("under"),
    }

    /** Prices and trading metadata come from the referenced market/outcome, never the display label. */
    data class Button(val side: ButtonSide, val label: String, val marketId: String, val assetId: String)

    /** marketId identifies the position. line is the signed left-team handicap or the over/under threshold. */
    data class Position(
        val marketId: String,
        val pickerLabel: String,
        val line: BigDecimal,
        val isDefault: Boolean,
        val buttons: List<Button>,
    )
}