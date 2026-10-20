package com.tangem.domain.staking.model

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StakingAvailabilityTest {

    private val option = StakingOption.StakeKit(StakingIntegrationID.StakeKit.Coin.Solana, MockStaking.solanaYield)

    @Test
    fun `GIVEN Available WHEN optionOrNull read THEN option returned`() {
        // Arrange
        val availability = StakingAvailability.Available(option)

        // Act
        val actual = availability.optionOrNull

        // Assert
        assertThat(actual).isEqualTo(option)
    }

    @Test
    fun `GIVEN Full WHEN optionOrNull read THEN option returned`() {
        // Arrange
        val availability = StakingAvailability.Full(option)

        // Act
        val actual = availability.optionOrNull

        // Assert
        assertThat(actual).isEqualTo(option)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN availability without option WHEN optionOrNull read THEN null`(availability: StakingAvailability) {
        // Act
        val actual = availability.optionOrNull

        // Assert
        assertThat(actual).isNull()
    }

    private fun provideTestModels() = listOf<StakingAvailability>(
        StakingAvailability.Unavailable,
        StakingAvailability.TemporaryUnavailable,
        StakingAvailability.RegionUnavailable,
    )
}