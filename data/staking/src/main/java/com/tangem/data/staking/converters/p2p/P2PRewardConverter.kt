package com.tangem.data.staking.converters.p2p

import com.tangem.datasource.api.p2p.models.response.P2PRewardEntryDTO
import com.tangem.domain.staking.model.p2p.P2PReward
import com.tangem.utils.converter.Converter

/**
 * Converter from P2P Reward Entry DTO to Domain model
 */
internal object P2PRewardConverter : Converter<P2PRewardEntryDTO, P2PReward> {

    override fun convert(value: P2PRewardEntryDTO): P2PReward {
        return P2PReward(
            date = value.date,
            apy = value.apy.toBigDecimal(),
            balance = value.balance,
            rewards = value.rewards,
        )
    }
}