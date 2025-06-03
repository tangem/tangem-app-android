package com.tangem.common.test.data.staking

import com.tangem.datasource.api.stakekit.models.request.Address
import com.tangem.datasource.api.stakekit.models.response.model.BalanceDTO
import com.tangem.datasource.api.stakekit.models.response.model.NetworkTypeDTO
import com.tangem.datasource.api.stakekit.models.response.model.TokenDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.domain.staking.model.StakingID
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
object MockYieldBalanceWrapperDTOFactory {

    val defaultStakingId = StakingID(
        integrationId = "ton-ton-chorus-one-pools-staking",
        address = "0x1",
    )

    fun createWithBalance(stakingId: StakingID = defaultStakingId): YieldBalanceWrapperDTO {
        return YieldBalanceWrapperDTO(
            addresses = Address(address = stakingId.address),
            balances = listOf(
                BalanceDTO(
                    groupId = "groupId",
                    type = BalanceDTO.BalanceTypeDTO.UNKNOWN,
                    amount = BigDecimal.ONE,
                    date = null,
                    pricePerShare = BigDecimal.ZERO,
                    pendingActions = listOf(),
                    pendingActionConstraints = null,
                    tokenDTO = TokenDTO(
                        name = "The-Open-Network",
                        network = NetworkTypeDTO.TON,
                        symbol = "TON",
                        decimals = 8,
                        address = null,
                        coinGeckoId = null,
                        logoURI = null,
                        isPoints = null,
                    ),
                    validatorAddress = null,
                    validatorAddresses = null,
                    providerId = null,
                ),
            ),
            integrationId = stakingId.integrationId,
        )
    }

    fun createWithEmptyBalance(stakingId: StakingID = defaultStakingId): YieldBalanceWrapperDTO {
        return YieldBalanceWrapperDTO(
            addresses = Address(address = stakingId.address),
            balances = emptyList(),
            integrationId = stakingId.integrationId,
        )
    }
}