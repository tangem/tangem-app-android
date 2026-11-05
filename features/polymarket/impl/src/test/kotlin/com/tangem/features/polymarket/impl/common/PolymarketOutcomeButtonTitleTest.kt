package com.tangem.features.polymarket.impl.common

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketTeam
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource

internal class PolymarketOutcomeButtonTitleTest {

    private val team = PolymarketTeam("São Paulo", " sao1 ", "Tricolor", null, null)
    private val away = PolymarketTeam("Away", "AWY", null, null, null)

    @ParameterizedTest
    @ValueSource(strings = ["São Paulo", " SAO PAULO ", "tricolor", " TRIColor "])
    fun `GIVEN team names or aliases WHEN all match THEN the whole row uses abbreviations`(title: String) {
        assertThat(PolymarketOutcomeButtonTitle.convert(listOf(title, "Away"), listOf(team, away), true))
            .containsExactly("SAO1", "AWY").inOrder()
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = [" ", "\t"])
    fun `GIVEN one missing abbreviation WHEN formatted THEN all original labels are preserved`(abbreviation: String?) {
        val titles = listOf(" São Paulo ", "Away")
        assertThat(PolymarketOutcomeButtonTitle.convert(titles, listOf(team, away.copy(abbreviation = abbreviation)), true))
            .isEqualTo(titles)
    }

    @Test
    fun `GIVEN matching aliases WHEN formatted THEN the first team in provider order wins as on iOS`() {
        val other = team.copy(name = "Other", abbreviation = "OTH")
        assertThat(PolymarketOutcomeButtonTitle.convert(listOf("Tricolor"), listOf(team, other), true))
            .containsExactly("SAO1")
    }

    @ParameterizedTest
    @ValueSource(strings = ["Yes", "No", "Draw", "Tie", "Other", "sao1"])
    fun `GIVEN an unmatched outcome WHEN formatted THEN the whole row keeps its original labels`(title: String) {
        val titles = listOf("São Paulo", title)
        assertThat(PolymarketOutcomeButtonTitle.convert(titles, listOf(team, away), true)).isEqualTo(titles)
    }

    @Test
    fun `GIVEN disabled feature WHEN every outcome matches THEN the original labels are preserved`() {
        val titles = listOf("São Paulo", "Away")
        assertThat(PolymarketOutcomeButtonTitle.convert(titles, listOf(team, away), false)).isEqualTo(titles)
    }
}