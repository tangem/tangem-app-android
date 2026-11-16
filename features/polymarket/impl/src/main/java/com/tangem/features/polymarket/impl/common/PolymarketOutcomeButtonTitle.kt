package com.tangem.features.polymarket.impl.common

import com.tangem.domain.polymarket.model.PolymarketTeam
import java.util.Locale

internal object PolymarketOutcomeButtonTitle {

    fun convert(titles: List<String>, teams: List<PolymarketTeam>?, isEnabled: Boolean): List<String> {
        if (!isEnabled) return titles
        return titles.map { title ->
            val abbreviation = PolymarketTeamMatcher.find(title, teams)?.abbreviation
                ?.trim()?.takeIf(String::isNotEmpty) ?: return titles
            abbreviation.uppercase(Locale.ROOT)
        }
    }
}