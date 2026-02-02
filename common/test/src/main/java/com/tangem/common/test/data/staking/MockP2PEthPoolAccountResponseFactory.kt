package com.tangem.common.test.data.staking

import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolExitQueueDTO
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolStakeDTO
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import java.math.BigDecimal

/**
 * Factory for creating mock P2PEthPool account responses for testing
 */
object MockP2PEthPoolAccountResponseFactory {

    private val defaultStakingId = StakingID(
        integrationId = "p2p-ethereum-pooled",
        address = "0x5aa711F440Eb6d4361148bBD89d03464628ace84",
    )

    const val defaultVaultAddress = "0x7f39C581F595B53c5cb19bD0b3f8dA6c935E2Ca0"

    fun createWithBalance(
        stakingId: StakingID = defaultStakingId,
        vaultAddress: String = defaultVaultAddress,
        stakedAmount: BigDecimal = BigDecimal("1.5"),
        earnedAmount: BigDecimal = BigDecimal("0.05"),
    ): P2PEthPoolAccountResponse {
        return P2PEthPoolAccountResponse(
            delegatorAddress = stakingId.address,
            vaultAddress = vaultAddress,
            stake = P2PEthPoolStakeDTO(
                assets = stakedAmount,
                totalEarnedAssets = earnedAmount,
            ),
            availableToUnstake = stakedAmount,
            availableToWithdraw = BigDecimal.ZERO,
            exitQueue = P2PEthPoolExitQueueDTO(
                total = BigDecimal.ZERO,
                requests = emptyList(),
            ),
        )
    }

    fun createWithEmptyBalance(
        stakingId: StakingID = defaultStakingId,
        vaultAddress: String = defaultVaultAddress,
    ): P2PEthPoolAccountResponse {
        return P2PEthPoolAccountResponse(
            delegatorAddress = stakingId.address,
            vaultAddress = vaultAddress,
            stake = P2PEthPoolStakeDTO(
                assets = BigDecimal.ZERO,
                totalEarnedAssets = BigDecimal.ZERO,
            ),
            availableToUnstake = BigDecimal.ZERO,
            availableToWithdraw = BigDecimal.ZERO,
            exitQueue = P2PEthPoolExitQueueDTO(
                total = BigDecimal.ZERO,
                requests = emptyList(),
            ),
        )
    }

    fun createMockVault(vaultAddress: String = defaultVaultAddress): P2PEthPoolVault {
        return P2PEthPoolVault(
            vaultAddress = vaultAddress,
            displayName = "Test Vault",
            apy = BigDecimal("3.5"),
            baseApy = BigDecimal("3.0"),
            capacity = BigDecimal("10000"),
            totalAssets = BigDecimal("5000"),
            feePercent = BigDecimal("10"),
            isPrivate = false,
            isGenesis = false,
            isSmoothingPool = false,
            isErc20 = false,
            tokenName = "Test Token",
            tokenSymbol = "TT",
            createdAt = 0L,
        )
    }
}