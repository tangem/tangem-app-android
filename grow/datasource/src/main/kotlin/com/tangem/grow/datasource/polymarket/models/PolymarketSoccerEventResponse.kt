package com.tangem.grow.datasource.polymarket.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** The event is always the main match, even when requested by a child event ID. */
@JsonClass(generateAdapter = true)
data class PolymarketSoccerEventResponse(
    @Json(name = "event") val event: PolymarketSoccerEventDto,
)

@JsonClass(generateAdapter = true)
data class PolymarketSoccerEventDto(
    @Json(name = "eventId") val eventId: String,
    @Json(name = "slug") val slug: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "polymarketRulesUrl") val polymarketRulesUrl: String,
    @Json(name = "image") val image: String?,
    @Json(name = "icon") val icon: String?,
    @Json(name = "status") val status: String,
    @Json(name = "live") val isLive: Boolean,
    @Json(name = "startTime") val startTime: String?,
    @Json(name = "startDate") val startDate: String?,
    @Json(name = "endDate") val endDate: String?,
    @Json(name = "volume") val volume: Double?,
    @Json(name = "volume24hr") val volume24hr: Double?,
    @Json(name = "liquidity") val liquidity: Double?,
    @Json(name = "negRisk") val isNegRisk: Boolean,
    @Json(name = "gameId") val gameId: String?,
    @Json(name = "score") val score: String?,
    @Json(name = "period") val period: String?,
    @Json(name = "elapsed") val elapsed: String?,
    @Json(name = "ended") val isEnded: Boolean?,
    @Json(name = "dataFetchedAt") val dataFetchedAt: String?,
    @Json(name = "league") val league: PolymarketSoccerLeagueDto,
    @Json(name = "resolutionSource") val resolutionSource: String?,
    @Json(name = "sportOrdering") val sportOrdering: String?,
    @Json(name = "teams") val teams: List<PolymarketTeamDto>,
    @Json(name = "tabs") val tabs: List<PolymarketSoccerTabDto>,
)

@JsonClass(generateAdapter = true)
data class PolymarketSoccerLeagueDto(
    @Json(name = "slug") val slug: String,
    @Json(name = "name") val name: String,
    @Json(name = "image") val image: String?,
)

@JsonClass(generateAdapter = true)
data class PolymarketSoccerTabDto(
    @Json(name = "key") val key: String,
    @Json(name = "cards") val cards: List<PolymarketSoccerCardDto>,
)

/**
 * Raw keys/templates and optional template payloads allow a consumer to skip unsupported cards.
 * Arrays retain BFF display order. A key alone is not unique: team totals have left and right cards.
 */
@JsonClass(generateAdapter = true)
data class PolymarketSoccerCardDto(
    @Json(name = "key") val key: String,
    @Json(name = "template") val template: String,
    @Json(name = "sportsMarketType") val sportsMarketType: String? = null,
    @Json(name = "markets") val markets: List<PolymarketMarketDto> = emptyList(),
    @Json(name = "teamSide") val teamSide: String? = null,
    @Json(name = "buttons") val buttons: List<PolymarketSoccerButtonDto>? = null,
    @Json(name = "positions") val positions: List<PolymarketSoccerPickerPositionDto>? = null,
    @Json(name = "rows") val rows: List<PolymarketSoccerButtonDto>? = null,
)

/** [marketId] identifies a position; [pickerLabel] can repeat for opposite signed handicaps. */
@JsonClass(generateAdapter = true)
data class PolymarketSoccerPickerPositionDto(
    @Json(name = "pickerLabel") val pickerLabel: String,
    @Json(name = "line") val line: Double,
    @Json(name = "isDefault") val isDefault: Boolean,
    @Json(name = "marketId") val marketId: String,
    @Json(name = "buttons") val buttons: List<PolymarketSoccerButtonDto>,
)

/** Price belongs to the referenced market outcome; Market WS updates it by [assetId]. */
@JsonClass(generateAdapter = true)
data class PolymarketSoccerButtonDto(
    @Json(name = "side") val side: String,
    @Json(name = "label") val label: String,
    @Json(name = "marketId") val marketId: String,
    @Json(name = "assetId") val assetId: String,
)