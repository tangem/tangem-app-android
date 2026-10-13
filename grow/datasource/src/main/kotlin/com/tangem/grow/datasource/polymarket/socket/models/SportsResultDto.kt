package com.tangem.grow.datasource.polymarket.socket.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @property gameId the same id as the Gamma event `gameId`, which comes as a string over REST
 * @property score provider score, e.g. `000-000|1-1|Bo3` for esports or `7-10` for team sports
 * @property period provider period, e.g. `3/3`, `Q4` or `FT`
 */
@Serializable
data class SportsResultDto(
    @SerialName("gameId") val gameId: Long,
    @SerialName("leagueAbbreviation") val leagueAbbreviation: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("score") val score: String? = null,
    @SerialName("period") val period: String? = null,
    @SerialName("live") val isLive: Boolean? = null,
    @SerialName("ended") val isEnded: Boolean? = null,
)