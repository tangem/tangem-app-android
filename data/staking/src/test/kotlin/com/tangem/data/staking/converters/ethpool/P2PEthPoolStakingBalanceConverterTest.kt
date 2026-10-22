package com.tangem.data.staking.converters.ethpool

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.data.staking.MockP2PEthPoolAccountResponseFactory.createWithBalance
import com.tangem.common.test.data.staking.MockP2PEthPoolAccountResponseFactory.createWithEmptyBalance
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolExitQueueDTO
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolExitRequestDTO
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolStakeDTO
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class P2PEthPoolStakingBalanceConverterTest {

    @Test
    fun `GIVEN staked account WHEN convertAll THEN P2PEthPool data balance`() {
        // Arrange
        val response = createWithBalance()

        // Act
        val actual = P2PEthPoolStakingBalanceConverter.convertAll(setOf(response), StatusSource.ACTUAL)

        // Assert
        val expected = StakingBalance.Data.P2PEthPool(
            stakingId = stakingId(),
            source = StatusSource.ACTUAL,
            accounts = listOf(P2PEthPoolStakingAccountConverter.convert(response)),
        )
        assertThat(actual).containsExactly(expected)
    }

    @Test
    fun `GIVEN zero account WHEN convertAll THEN Empty balance`() {
        // Act
        val actual = P2PEthPoolStakingBalanceConverter.convertAll(setOf(createWithEmptyBalance()), StatusSource.CACHE)

        // Assert
        assertThat(actual).containsExactly(StakingBalance.Empty(stakingId(), StatusSource.CACHE))
    }

    @Test
    fun `GIVEN only exit queue WHEN convertAll THEN data balance`() {
        // Arrange
        val response = createWithEmptyBalance().copy(
            exitQueue = P2PEthPoolExitQueueDTO(total = BigDecimal("0.4"), requests = listOf(exitRequestDTO())),
        )

        // Act
        val actual = P2PEthPoolStakingBalanceConverter.convertAll(setOf(response), StatusSource.ACTUAL)

        // Assert
        assertThat(actual).containsExactly(dataBalance(response))
    }

    @Test
    fun `GIVEN only withdrawable amount WHEN convertAll THEN data balance`() {
        // Arrange
        val response = createWithEmptyBalance().copy(availableToWithdraw = BigDecimal("0.2"))

        // Act
        val actual = P2PEthPoolStakingBalanceConverter.convertAll(setOf(response), StatusSource.ACTUAL)

        // Assert
        assertThat(actual).containsExactly(dataBalance(response))
    }

    // Pins current behaviour: rewards and availableToUnstake are not an active position
    @Test
    fun `GIVEN only earned rewards WHEN convertAll THEN Empty balance`() {
        // Arrange
        val response = createWithEmptyBalance().copy(
            stake = P2PEthPoolStakeDTO(assets = BigDecimal.ZERO, totalEarnedAssets = BigDecimal("0.05")),
            availableToUnstake = BigDecimal("0.05"),
        )

        // Act
        val actual = P2PEthPoolStakingBalanceConverter.convertAll(setOf(response), StatusSource.ACTUAL)

        // Assert
        assertThat(actual.single()).isEqualTo(StakingBalance.Empty(stakingId(), StatusSource.ACTUAL))
    }

    @Test
    fun `GIVEN two vaults of one delegator WHEN convertAll THEN one balance with both accounts`() {
        // Arrange
        val vaultA = createWithBalance(vaultAddress = "0xvaultA")
        val vaultB = createWithEmptyBalance(vaultAddress = "0xvaultB")

        // Act
        val actual = P2PEthPoolStakingBalanceConverter.convertAll(setOf(vaultA, vaultB), StatusSource.ACTUAL)

        // Assert
        assertThat(actual).containsExactly(dataBalance(vaultA, vaultB))
    }

    @Test
    fun `GIVEN two delegators WHEN convertAll THEN one balance per delegator`() {
        // Arrange
        val staked = createWithBalance(stakingId = StakingID(P2P_ID, "0xA"))
        val empty = createWithEmptyBalance(stakingId = StakingID(P2P_ID, "0xB"))

        // Act
        val actual = P2PEthPoolStakingBalanceConverter.convertAll(setOf(staked, empty), StatusSource.ACTUAL)

        // Assert
        assertThat(actual).containsExactly(
            dataBalance(staked, address = "0xA"),
            StakingBalance.Empty(stakingId("0xB"), StatusSource.ACTUAL),
        )
    }

    private fun stakingId(address: String = DELEGATOR) = StakingID(P2P_ID, address)

    private fun dataBalance(vararg responses: P2PEthPoolAccountResponse, address: String = DELEGATOR) =
        StakingBalance.Data.P2PEthPool(
            stakingId = stakingId(address),
            source = StatusSource.ACTUAL,
            accounts = responses.map(P2PEthPoolStakingAccountConverter::convert),
        )

    private fun exitRequestDTO(
        ticket: String = "t1",
        totalAssets: BigDecimal = BigDecimal("0.4"),
        timestamp: Long = 1_700_000_000_000L,
        withdrawalTimestamp: Long? = null,
        isClaimable: Boolean = false,
    ) = P2PEthPoolExitRequestDTO(
        ticket = ticket,
        totalAssets = totalAssets,
        timestamp = timestamp,
        withdrawalTimestamp = withdrawalTimestamp,
        isClaimable = isClaimable,
    )

    private companion object {
        const val DELEGATOR = "0x5aa711F440Eb6d4361148bBD89d03464628ace84"
        const val P2P_ID = "p2p-ethereum-pooled"
    }
}