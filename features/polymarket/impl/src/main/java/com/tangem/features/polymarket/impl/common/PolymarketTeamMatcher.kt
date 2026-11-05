package com.tangem.features.polymarket.impl.common

import com.tangem.domain.polymarket.model.PolymarketTeam
import java.text.Normalizer
import java.util.Locale

internal object PolymarketTeamMatcher {

    private val DIACRITICS = Regex("\\p{M}+")

    fun find(title: String, teams: List<PolymarketTeam>?): PolymarketTeam? {
        val label = title.normalized()
        if (label.isEmpty()) return null
        return teams?.firstOrNull { team ->
            listOfNotNull(team.name, team.alias).any { it.normalized() == label }
        }
    }

    private fun String.normalized(): String = Normalizer.normalize(trim(), Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .lowercase(Locale.ROOT)
}