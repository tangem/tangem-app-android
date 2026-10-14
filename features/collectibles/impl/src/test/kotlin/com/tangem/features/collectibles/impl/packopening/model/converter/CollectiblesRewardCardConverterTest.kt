package com.tangem.features.collectibles.impl.packopening.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.models.CollectibleRarity
import com.tangem.features.collectibles.impl.createOpenedPack
import com.tangem.features.collectibles.impl.packopening.ui.state.RewardCardUM
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CollectiblesRewardCardConverterTest {

    @Test
    fun `GIVEN an opened pack WHEN convert THEN the card reads its fields`() {
        // Arrange
        val pack = createOpenedPack()

        // Act
        val actual = CollectiblesRewardCardConverter.convert(pack = pack)

        // Assert
        assertThat(actual).isEqualTo(
            RewardCardUM(
                rarity = "Uncommon pull",
                accent = RewardCardUM.Accent.Green,
                title = "2024 Series One",
                editionNumber = "#216",
                marketValue = "$62",
                sellBackValue = "$55.80",
                frontImageUrl = "https://tangem.com/front.png",
                backImageUrl = "https://tangem.com/back.png",
            ),
        )
    }

    @Test
    fun `GIVEN a pack without edition details WHEN convert THEN those fields are empty`() {
        // Arrange
        val pack = createOpenedPack(serialNumber = null, year = null)

        // Act
        val actual = CollectiblesRewardCardConverter.convert(pack = pack)

        // Assert
        assertThat(listOf(actual.editionNumber, actual.title))
            .containsExactly("", "Series One")
            .inOrder()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun rarity(model: RarityModel) {
        // Arrange
        val pack = createOpenedPack(rarity = model.rarity)

        // Act
        val actual = CollectiblesRewardCardConverter.convert(pack = pack)

        // Assert
        assertThat(actual.rarity to actual.accent).isEqualTo(model.expected to model.expectedAccent)
    }

    private fun provideTestModels() = listOf(
        RarityModel(CollectibleRarity.COMMON, expected = "Common pull", expectedAccent = RewardCardUM.Accent.Neutral),
        RarityModel(CollectibleRarity.UNCOMMON, expected = "Uncommon pull", expectedAccent = RewardCardUM.Accent.Green),
        RarityModel(CollectibleRarity.RARE, expected = "Rare pull", expectedAccent = RewardCardUM.Accent.Blue),
        RarityModel(CollectibleRarity.EPIC, expected = "Epic pull", expectedAccent = RewardCardUM.Accent.Violet),
        RarityModel(rarity = null, expected = "Your pull", expectedAccent = RewardCardUM.Accent.Neutral),
    )

    internal data class RarityModel(
        val rarity: CollectibleRarity?,
        val expected: String,
        val expectedAccent: RewardCardUM.Accent,
    )
}