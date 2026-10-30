package com.tangem.features.staking.impl.presentation.state.events

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.staking.impl.R
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class StakingAlertUMTest {

    @Test
    fun `GIVEN no targets WHEN stakeMoreClickUnavailableNoTargets THEN no title and no validators message`() {
        // Act
        val message = StakingAlertUM.stakeMoreClickUnavailableNoTargets()

        // Assert
        assertThat(message.title).isNull()
        assertThat((message.message as TextReference.Res).id)
            .isEqualTo(R.string.staking_no_validators_error_message)
    }

    @Test
    fun `GIVEN currency WHEN stakeMoreClickUnavailable THEN no title and stake more unavailability message`() {
        // Arrange
        val currency: CryptoCurrency = mockk(relaxed = true)

        // Act
        val message = StakingAlertUM.stakeMoreClickUnavailable(currency)

        // Assert
        assertThat(message.title).isNull()
        assertThat((message.message as TextReference.Res).id)
            .isEqualTo(R.string.staking_stake_more_button_unavailability_reason)
    }
}