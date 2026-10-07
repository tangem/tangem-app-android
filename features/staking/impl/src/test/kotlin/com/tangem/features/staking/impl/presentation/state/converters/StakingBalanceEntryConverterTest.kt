package com.tangem.features.staking.impl.presentation.state.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.staking.StakingBalanceEntry
import com.tangem.domain.models.staking.StakingEntryActions
import com.tangem.domain.models.staking.StakingEntryType
import com.tangem.domain.models.staking.ValidatorInfo
import com.tangem.domain.staking.model.StakeKitIntegration
import com.tangem.domain.staking.model.StakingTarget
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.utils.Provider
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class StakingBalanceEntryConverterTest {

    private val jailedTarget = StakingTarget.Validator(
        Yield.Validator(
            address = JAILED_ADDRESS,
            status = Yield.Validator.ValidatorStatus.JAILED,
            name = "Jailed validator",
            preferred = false,
            isStrategicPartner = false,
        ),
    )

    private val integration: StakeKitIntegration = mockk {
        every { allTargets } returns listOf(jailedTarget)
    }

    private val converter = StakingBalanceEntryConverter(
        cryptoCurrencyStatus = CryptoCurrencyStatus(
            currency = mockk(relaxed = true),
            value = mockk(relaxed = true) {
                every { this@mockk.fiatRate } returns null
                every { this@mockk.stakingBalance } returns null
                every { this@mockk.contributions } returns emptyList()
            },
        ),
        appCurrencyProvider = Provider { AppCurrency.Default },
        integration = integration,
    )

    @Test
    fun `GIVEN stake at a jailed validator WHEN convert THEN row is kept with the validator name`() {
        // Arrange
        val entry = createStakedEntry(validatorAddress = JAILED_ADDRESS)

        // Act
        val actual = converter.convert(entry)

        // Assert
        assertThat(actual?.title).isEqualTo(stringReference("Jailed validator"))
        assertThat(actual?.target).isEqualTo(jailedTarget)
    }

    @Test
    fun `GIVEN stake at a validator StakeKit no longer lists WHEN convert THEN row keeps that validator as target`() {
        // Arrange
        val entry = createStakedEntry(validatorAddress = UNKNOWN_ADDRESS)

        // Act
        val actual = converter.convert(entry)

        // Assert
        assertThat(actual?.title).isEqualTo(stringReference("cosmos...wxyz"))
        assertThat(actual?.target?.address).isEqualTo(UNKNOWN_ADDRESS)
        assertThat(actual?.target?.isPreferred).isFalse()
    }

    private fun createStakedEntry(validatorAddress: String) = StakingBalanceEntry(
        id = "group",
        type = StakingEntryType.STAKED,
        amount = BigDecimal.ONE,
        validator = ValidatorInfo(address = validatorAddress, name = null),
        date = null,
        actions = StakingEntryActions.StakeKit(pendingActions = emptyList(), pendingActionsConstraints = emptyList()),
        isPending = false,
        rawCurrencyId = null,
    )

    private companion object {
        const val JAILED_ADDRESS = "cosmosvaloper1jailed"
        const val UNKNOWN_ADDRESS = "cosmosvaloper1unknownwxyz"
    }
}