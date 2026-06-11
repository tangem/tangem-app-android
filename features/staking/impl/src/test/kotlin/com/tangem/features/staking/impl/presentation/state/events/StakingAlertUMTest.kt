package com.tangem.features.staking.impl.presentation.state.events

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.staking.impl.R
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class StakingAlertUMTest {

    @Test
    fun `noTargets dialog uses no validators string and has no title`() {
        val message = StakingAlertUM.stakeMoreClickUnavailableNoTargets()

        assertThat(message.title).isNull()
        assertThat((message.message as TextReference.Res).id)
            .isEqualTo(R.string.staking_no_validators_error_message)
    }

    @Test
    fun `default stake more dialog uses stake more unavailability string`() {
        val currency: CryptoCurrency = mockk(relaxed = true)

        val message = StakingAlertUM.stakeMoreClickUnavailable(currency)

        assertThat(message.title).isNull()
        assertThat((message.message as TextReference.Res).id)
            .isEqualTo(R.string.staking_stake_more_button_unavailability_reason)
    }
}