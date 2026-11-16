package com.tangem.domain.polymarket.sports

import com.tangem.domain.polymarket.model.PolymarketSportsState.Team
import com.tangem.domain.polymarket.model.PolymarketTeam

internal object PolymarketTeamSides {

    fun resolve(teams: List<PolymarketTeam>, homeName: String?, awayName: String?): Pair<Team?, Team?> {
        var home = teams.indices.firstOrNull { teams[it].ordering.equals("home", ignoreCase = true) }
        var away = teams.indices.firstOrNull { it != home && teams[it].ordering.equals("away", ignoreCase = true) }
        home = home ?: findName(teams, homeName, except = away)
        away = away ?: findName(teams, awayName, except = home)
        home = home ?: teams.indices.firstOrNull { it != away }
        away = away ?: teams.indices.firstOrNull { it != home }
        return team(teams = teams, index = home, name = homeName, ordering = "home") to
            team(teams = teams, index = away, name = awayName, ordering = "away")
    }

    private fun findName(teams: List<PolymarketTeam>, name: String?, except: Int?): Int? =
        name?.takeIf(String::isNotBlank)?.let { wanted ->
            teams.indices.firstOrNull { it != except && teams[it].name.trim().equals(wanted.trim(), ignoreCase = true) }
        }

    private fun team(teams: List<PolymarketTeam>, index: Int?, name: String?, ordering: String): Team? = when {
        index != null -> Team(teams[index], index)
        !name.isNullOrBlank() -> Team(
            value = PolymarketTeam(name = name, abbreviation = null, alias = null, logoUrl = null, ordering = ordering),
            restIndex = null,
        )
        else -> null
    }
}