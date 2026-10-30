package com.tangem.datasource.local.token.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.staking.PendingAction
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.grow.datasource.stakekit.models.response.model.BalanceDTO
import com.tangem.grow.datasource.stakekit.models.response.model.action.StakingActionTypeDTO
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class PendingActionConverterTest {

    // Pins current behaviour: args is never null after conversion
    @Test
    fun `GIVEN pending action without args WHEN convert THEN args object with all nulls`() {
        // Arrange
        val dto = BalanceDTO.PendingAction(type = StakingActionTypeDTO.CLAIM_REWARDS, passthrough = "p1", args = null)

        // Act
        val actual = PendingActionConverter.convert(dto)

        // Assert
        val expected = PendingAction(
            type = StakingActionType.CLAIM_REWARDS,
            passthrough = "p1",
            args = PendingAction.PendingActionArgs(
                amount = null,
                duration = null,
                hasValidatorAddress = null,
                hasValidatorAddresses = null,
                tronResource = null,
                hasSignatureVerification = null,
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN full args WHEN convert THEN every arg mapped`() {
        // Arrange
        val args = BalanceDTO.PendingAction.PendingActionArgs(
            amount = BalanceDTO.PendingAction.PendingActionArgs.Amount(true, BigDecimal("1"), BigDecimal("10")),
            duration = BalanceDTO.PendingAction.PendingActionArgs.Duration(false, 1, 30),
            validatorAddress = BalanceDTO.Required(true),
            validatorAddresses = BalanceDTO.Required(false),
            nfts = listOf(BalanceDTO.PendingAction.PendingActionArgs.Nft(null, null, null)),
            tronResource = BalanceDTO.PendingAction.PendingActionArgs.TronResource(true, listOf("ENERGY", "BANDWIDTH")),
            signatureVerification = BalanceDTO.Required(true),
        )
        val dto = BalanceDTO.PendingAction(type = StakingActionTypeDTO.VOTE_LOCKED, passthrough = "p1", args = args)

        // Act
        val actual = PendingActionConverter.convert(dto)

        // Assert
        val expected = PendingAction(
            type = StakingActionType.VOTE_LOCKED,
            passthrough = "p1",
            args = PendingAction.PendingActionArgs(
                amount = PendingAction.PendingActionArgs.Amount(true, BigDecimal("1"), BigDecimal("10")),
                duration = PendingAction.PendingActionArgs.Duration(false, 1, 30),
                hasValidatorAddress = true,
                hasValidatorAddresses = false,
                tronResource = PendingAction.PendingActionArgs.TronResource(true, listOf("ENERGY", "BANDWIDTH")),
                hasSignatureVerification = true,
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }
}