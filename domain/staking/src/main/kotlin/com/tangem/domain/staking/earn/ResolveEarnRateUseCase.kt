package com.tangem.domain.staking.earn

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.model.common.RewardInfo
import com.tangem.domain.staking.model.common.RewardType
import com.tangem.domain.staking.model.optionOrNull
import com.tangem.domain.staking.model.stakingBalanceData
import com.tangem.lib.crypto.BlockchainUtils
import java.math.BigDecimal

/**
 * Resolves the earn rate a portfolio currency can offer, and whether the user is already earning it.
 *
 * Yield supply takes precedence over staking: a token the yield module lists is shown at the module's
 * rate even when it could also be staked. Returns `null` when the currency cannot earn at all.
 */
class ResolveEarnRateUseCase {

    /**
     * @param yieldSupplyApy APY in **percent** per yield-supply key, as the backend reports it
     * @param stakingAvailability staking availability per currency
     */
    operator fun invoke(
        currencyStatus: CryptoCurrencyStatus,
        yieldSupplyApy: Map<String, BigDecimal>,
        stakingAvailability: Map<CryptoCurrency, StakingAvailability>,
    ): EarnRate? = resolveYieldSupplyRate(currencyStatus, yieldSupplyApy)
        ?: resolveStakingRate(currencyStatus, stakingAvailability)

    private fun resolveYieldSupplyRate(
        currencyStatus: CryptoCurrencyStatus,
        yieldSupplyApy: Map<String, BigDecimal>,
    ): EarnRate? {
        val token = currencyStatus.currency as? CryptoCurrency.Token ?: return null
        if (yieldSupplyApy.isEmpty()) return null

        val apyPercent = yieldSupplyApy.entries
            .firstOrNull { (tokenId, _) ->
                tokenId.equals(
                    other = token.yieldSupplyKey(),
                    ignoreCase = BlockchainUtils.isCaseInsensitiveContractAddress(token.network.rawId),
                )
            }
            ?.value
            ?: return null

        return EarnRate(
            // Percent to fraction by shifting the point, not dividing: the shift is exact, so the rate
            // reaches the formatter with every digit the backend sent and is rounded only there.
            apy = apyPercent.movePointLeft(PERCENT_POINT_SHIFT),
            isActive = currencyStatus.value.yieldSupplyStatus?.isActive == true,
            source = EarnRate.Source.YieldSupply(apyPercent = apyPercent),
        )
    }

    /**
     * Picks the staking rate to show: for an active StakeKit stake — the rate of the validator the user
     * actually stakes with (falling back to the best preferred one), otherwise the best preferred
     * validator's rate. [StakingAvailability.Full] pools are surfaced only for already-staked tokens.
     */
    private fun resolveStakingRate(
        currencyStatus: CryptoCurrencyStatus,
        stakingAvailability: Map<CryptoCurrency, StakingAvailability>,
    ): EarnRate? {
        if (stakingAvailability.isEmpty()) return null

        val availability = stakingAvailability[currencyStatus.currency]
        val option = availability?.optionOrNull ?: return null

        val stakingBalance = currencyStatus.value.stakingBalanceData
        val stakeKitBalance = stakingBalance as? StakingBalance.Data.StakeKit
        val isActive = stakeKitBalance != null || stakingBalance is StakingBalance.Data.P2PEthPool

        // Full = no free capacity: offer it only to tokens that already have a stake.
        if (availability is StakingAvailability.Full && !isActive) return null

        val rewardInfo = when (option) {
            is StakingOption.P2PEthPool -> RewardInfo(rate = option.apy, type = RewardType.APY)
            is StakingOption.StakeKit -> option.stakedValidatorRewardInfo(stakeKitBalance)
                ?: option.preferredValidatorRewardInfo()
        } ?: return null

        return EarnRate(
            apy = rewardInfo.rate,
            isActive = isActive,
            source = EarnRate.Source.Staking(integrationId = option.integrationId, rewardType = rewardInfo.type),
        )
    }

    /** Rate of a validator the user actually stakes with, or `null` when they stake with none. */
    private fun StakingOption.StakeKit.stakedValidatorRewardInfo(balance: StakingBalance.Data.StakeKit?): RewardInfo? {
        val validatorsByAddress = yield.validators.associateBy { it.address }
        return balance?.balance
            ?.items
            ?.mapNotNull { it.validatorAddress }
            ?.firstNotNullOfOrNull { address -> validatorsByAddress[address]?.rewardInfo }
    }

    private fun StakingOption.StakeKit.preferredValidatorRewardInfo(): RewardInfo? = yield.validators
        .filter { it.preferred }
        .mapNotNull { it.rewardInfo }
        .maxByOrNull { it.rate }

    private companion object {

        /** Decimal places between a backend percent (5.5) and the fraction it means (0.055). */
        const val PERCENT_POINT_SHIFT = 2
    }
}

/**
 * Earn rate resolved for a currency.
 *
 * @property apy rate as a fraction — `0.05` is 5%
 * @property isActive whether the user already earns on the currency (active yield supply or stake)
 * @property source which earn product the rate belongs to, with the details its entry point needs
 */
data class EarnRate(
    val apy: BigDecimal,
    val isActive: Boolean,
    val source: Source,
) {

    sealed interface Source {

        /** @property apyPercent rate in percent as received from the backend, for the yield-supply route */
        data class YieldSupply(val apyPercent: BigDecimal) : Source

        data class Staking(val integrationId: StakingIntegrationID, val rewardType: RewardType) : Source
    }
}