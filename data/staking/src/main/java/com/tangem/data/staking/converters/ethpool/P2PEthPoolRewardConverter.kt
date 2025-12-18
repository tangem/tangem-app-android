package com.tangem.data.staking.converters.ethpool

import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolRewardDTO
import com.tangem.domain.staking.model.ethpool.P2PEthPoolReward
import com.tangem.utils.converter.Converter

/**
 * Converter from P2PEthPool Reward Entry DTO to Domain model
 */
internal object P2PEthPoolRewardConverter : Converter<P2PEthPoolRewardDTO, P2PEthPoolReward> {

    override fun convert(value: P2PEthPoolRewardDTO): P2PEthPoolReward {
        return P2PEthPoolReward(
            date = value.date,
            apy = value.apy.toBigDecimal(),
            balance = value.balance,
            rewards = value.rewards,
        )
    }
}