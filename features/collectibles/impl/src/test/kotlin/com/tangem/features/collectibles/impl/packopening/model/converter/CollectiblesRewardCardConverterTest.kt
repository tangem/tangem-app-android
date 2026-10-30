package com.tangem.features.collectibles.impl.packopening.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
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
                rarity = resourceReference(R.string.collectibles_open_pack_pull_uncommon),
                accent = RewardCardUM.Accent.Green,
                title = "2024 Series One",
                editionNumber = "#216",
                marketValue = "$62",
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
        RarityModel(
            rarity = CollectibleRarity.COMMON,
            expected = resourceReference(R.string.collectibles_open_pack_pull_common),
            expectedAccent = RewardCardUM.Accent.Neutral,
        ),
        RarityModel(
            rarity = CollectibleRarity.UNCOMMON,
            expected = resourceReference(R.string.collectibles_open_pack_pull_uncommon),
            expectedAccent = RewardCardUM.Accent.Green,
        ),
        RarityModel(
            rarity = CollectibleRarity.RARE,
            expected = resourceReference(R.string.collectibles_open_pack_pull_rare),
            expectedAccent = RewardCardUM.Accent.Blue,
        ),
        RarityModel(
            rarity = CollectibleRarity.EPIC,
            expected = resourceReference(R.string.collectibles_open_pack_pull_epic),
            expectedAccent = RewardCardUM.Accent.Violet,
        ),
        RarityModel(
            rarity = null,
            expected = resourceReference(R.string.collectibles_open_pack_pull_common),
            expectedAccent = RewardCardUM.Accent.Neutral,
        ),
    )

    internal data class RarityModel(
        val rarity: CollectibleRarity?,
        val expected: TextReference,
        val expectedAccent: RewardCardUM.Accent,
    )
}