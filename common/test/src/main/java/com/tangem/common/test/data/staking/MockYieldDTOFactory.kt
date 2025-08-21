package com.tangem.common.test.data.staking

import com.tangem.datasource.api.stakekit.models.response.model.AddressArgumentDTO
import com.tangem.datasource.api.stakekit.models.response.model.NetworkTypeDTO
import com.tangem.datasource.api.stakekit.models.response.model.TokenDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldDTO
import com.tangem.domain.models.staking.StakingID
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
object MockYieldDTOFactory {

    val defaultStakingID = StakingID(
        integrationId = "ton-ton-chorus-one-pools-staking",
        address = "0x1",
    )

    fun create(stakingID: StakingID = defaultStakingID): YieldDTO {
        return YieldDTO(
            id = stakingID.integrationId,
            token = TokenDTO(
                name = "Miguel Estes",
                network = NetworkTypeDTO.POLYGON,
                symbol = "splendide",
                decimals = 1323,
                address = null,
                coinGeckoId = null,
                logoURI = null,
                isPoints = null,
            ),
            tokens = listOf(),
            args = YieldDTO.ArgsDTO(
                enter = YieldDTO.ArgsDTO.Enter(
                    addresses = YieldDTO.ArgsDTO.Enter.Addresses(
                        address = AddressArgumentDTO(required = false),
                    ),
                    args = mapOf(),
                ),
                exit = null,
            ),
            status = YieldDTO.StatusDTO(enter = true, exit = true),
            apy = BigDecimal.ONE,
            rewardRate = 1.0,
            rewardType = YieldDTO.RewardTypeDTO.UNKNOWN,
            metadata = YieldDTO.MetadataDTO(
                name = "name",
                logoUri = "logoUri",
                description = "description",
                documentation = null,
                gasFeeTokenDTO = TokenDTO(
                    name = "Johnnie Mullen",
                    network = NetworkTypeDTO.POLYGON,
                    symbol = "fuisset",
                    decimals = 2957,
                    address = null,
                    coinGeckoId = null,
                    logoURI = null,
                    isPoints = null,
                ),
                tokenDTO = TokenDTO(
                    name = "Lazaro Wood",
                    network = NetworkTypeDTO.POLYGON,
                    symbol = "vocent",
                    decimals = 1602,
                    address = null,
                    coinGeckoId = null,
                    logoURI = null,
                    isPoints = null,
                ),
                tokensDTO = listOf(),
                type = "type",
                rewardSchedule = YieldDTO.MetadataDTO.RewardScheduleDTO.DAY,
                cooldownPeriod = null,
                warmupPeriod = YieldDTO.MetadataDTO.PeriodDTO(1),
                rewardClaiming = YieldDTO.MetadataDTO.RewardClaimingDTO.AUTO,
                defaultValidator = null,
                minimumStake = null,
                supportsMultipleValidators = null,
                revshare = YieldDTO.MetadataDTO.EnabledDTO(enabled = true),
                fee = YieldDTO.MetadataDTO.EnabledDTO(enabled = true),
            ),
            validators = listOf(),
            isAvailable = true,
        )
    }
}