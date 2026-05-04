package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.model.common.RewardInfo
import com.tangem.domain.staking.model.common.RewardType
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class EarnApyConverter(
    val yieldModuleApyMap: Map<String, BigDecimal>,
    val stakingApyMap: Map<CryptoCurrency, StakingAvailability>,
) : Converter<CryptoCurrencyStatus, EarnApyConverter.EarnApyInfo?> {

    override fun convert(value: CryptoCurrencyStatus): EarnApyInfo? {
        val token = value.currency as? CryptoCurrency.Token
        if (token != null && yieldModuleApyMap.isNotEmpty()) {
            val yieldSupplyApy = yieldModuleApyMap.entries.firstOrNull { apy ->
                apy.key.equals(
                    other = token.yieldSupplyKey(),
                    ignoreCase = BlockchainUtils.isCaseInsensitiveContractAddress(token.network.rawId),
                )
            }?.value
            if (yieldSupplyApy != null) {
                val isActive = value.value.yieldSupplyStatus?.isActive == false
                return EarnApyInfo(
                    text = resourceReference(
                        R.string.yield_module_earn_badge,
                        wrappedList(yieldSupplyApy),
                    ),
                    isActive = isActive,
                    apy = yieldSupplyApy.toString(),
                )
            }
        }

        if (stakingApyMap.isNotEmpty()) {
            val stakingInfo = findStakingRate(
                currencyStatus = value,
                stakingApyMap = stakingApyMap,
            )
            val rewardTypeRes = when (stakingInfo.rewardType) {
                RewardType.APR -> R.string.staking_apr_earn_badge
                RewardType.UNKNOWN,
                RewardType.APY,
                null,
                -> R.string.yield_module_earn_badge
            }
            if (stakingInfo.rate != null) {
                val apyString = stakingInfo.rate.format { percent(withPercentSign = false) }
                return EarnApyInfo(
                    text = resourceReference(
                        rewardTypeRes,
                        wrappedList(apyString),
                    ),
                    isActive = stakingInfo.isActive,
                    apy = apyString,
                )
            }
        }

        return null
    }

    private fun findStakingRate(
        currencyStatus: CryptoCurrencyStatus,
        stakingApyMap: Map<CryptoCurrency, StakingAvailability>,
    ): StakingLocalInfo {
        val stakingAvailability = stakingApyMap[currencyStatus.currency] as? StakingAvailability.Available
            ?: return StakingLocalInfo(rate = null, isActive = false, rewardType = null)

        val stakingBalance = currencyStatus.value.stakingBalance as? StakingBalance.Data
        val stakeKitBalance = stakingBalance as? StakingBalance.Data.StakeKit
        val p2pEthPoolBalance = stakingBalance as? StakingBalance.Data.P2PEthPool

        val rateInfo = when (val stakingOptions = stakingAvailability.option) {
            is StakingOption.P2PEthPool -> {
                RewardInfo(
                    rate = stakingOptions.apy,
                    type = RewardType.APY,
                )
            }
            is StakingOption.StakeKit -> if (stakeKitBalance != null) {
                val validatorsByAddress = stakingOptions.yield.validators.associateBy { it.address }
                stakeKitBalance.balance.items
                    .mapNotNull { it.validatorAddress }
                    .firstNotNullOfOrNull { address ->
                        validatorsByAddress[address]?.rewardInfo
                    } ?: stakingOptions.yield.validators
                    .filter { it.preferred }
                    .mapNotNull { validator ->
                        validator.rewardInfo
                    }
                    .maxByOrNull { it.rate }
            } else {
                stakingOptions.yield.validators
                    .filter { it.preferred }
                    .mapNotNull { validator ->
                        validator.rewardInfo
                    }
                    .maxByOrNull { it.rate }
            }
        }

        return StakingLocalInfo(
            rate = rateInfo?.rate,
            isActive = stakeKitBalance != null || p2pEthPoolBalance != null,
            rewardType = rateInfo?.type,
        )
    }

    data class StakingLocalInfo(
        val rate: BigDecimal?,
        val isActive: Boolean,
        val rewardType: RewardType?,
    )

    data class EarnApyInfo(
        val text: TextReference?,
        val isActive: Boolean,
        val apy: String?,
    )
}