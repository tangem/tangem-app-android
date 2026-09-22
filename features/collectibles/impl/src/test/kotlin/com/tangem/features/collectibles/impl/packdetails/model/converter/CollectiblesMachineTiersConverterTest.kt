package com.tangem.features.collectibles.impl.packdetails.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.features.collectibles.impl.createMachine
import com.tangem.features.collectibles.impl.createOdds
import com.tangem.features.collectibles.impl.createTierRanges
import com.tangem.features.collectibles.impl.machinedetails.model.converter.CollectiblesMachineTiersConverter
import com.tangem.features.collectibles.impl.machinedetails.ui.state.CollectiblesMachineDetailsUM
import org.junit.jupiter.api.Test

internal class CollectiblesMachineTiersConverterTest {

    @Test
    fun `GIVEN every tier WHEN convert THEN the labels are read from resources in rarity order`() {
        // Arrange
        val machine = createMachine()

        // Act
        val actual = CollectiblesMachineTiersConverter.convert(machine)

        // Assert
        assertThat(actual.map { it.name })
            .containsExactly(
                resourceReference(R.string.collectibles_machine_tier_common),
                resourceReference(R.string.collectibles_machine_tier_uncommon),
                resourceReference(R.string.collectibles_machine_tier_rare),
                resourceReference(R.string.collectibles_machine_tier_epic),
            )
            .inOrder()
    }

    @Test
    fun `GIVEN every tier WHEN convert THEN each tier carries its own accent`() {
        // Arrange
        val machine = createMachine()

        // Act
        val actual = CollectiblesMachineTiersConverter.convert(machine)

        // Assert
        assertThat(actual.map { it.accent })
            .containsExactly(
                CollectiblesMachineDetailsUM.TierUM.Accent.Neutral,
                CollectiblesMachineDetailsUM.TierUM.Accent.Green,
                CollectiblesMachineDetailsUM.TierUM.Accent.Blue,
                CollectiblesMachineDetailsUM.TierUM.Accent.Violet,
            )
            .inOrder()
    }

    @Test
    fun `GIVEN no epic tier WHEN convert THEN only three cards are built`() {
        // Arrange
        val machine = createMachine(
            odds = createOdds(epic = null),
            tierRanges = createTierRanges(epic = null),
        )

        // Act
        val actual = CollectiblesMachineTiersConverter.convert(machine)

        // Assert
        assertThat(actual.map { it.id }).containsExactly("common", "uncommon", "rare").inOrder()
    }

    @Test
    fun `GIVEN epic odds without a range WHEN convert THEN the tier is left out`() {
        // Arrange
        val machine = createMachine(odds = createOdds(epic = 1), tierRanges = createTierRanges(epic = null))

        // Act
        val actual = CollectiblesMachineTiersConverter.convert(machine)

        // Assert
        assertThat(actual.map { it.id }).doesNotContain("epic")
    }
}