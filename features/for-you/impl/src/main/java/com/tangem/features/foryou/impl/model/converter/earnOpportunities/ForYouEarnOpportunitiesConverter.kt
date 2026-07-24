package com.tangem.features.foryou.impl.model.converter.earnOpportunities

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.models.earn.EarnRewardType
import com.tangem.domain.models.earn.EarnTopToken
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.model.common.RewardInfo
import com.tangem.domain.staking.model.common.RewardType
import com.tangem.domain.staking.model.optionOrNull
import com.tangem.features.foryou.impl.entity.EarnOpportunitiesUM
import com.tangem.features.foryou.impl.entity.ForYouEarnOpportunitiesType
import com.tangem.features.foryou.impl.entity.ForYouWalletHeaderUM
import com.tangem.features.foryou.impl.model.ForYouSelectedPortfolio
import com.tangem.features.foryou.impl.model.converter.EarnApyInfo
import com.tangem.features.foryou.impl.model.converter.EarnOpportunities
import com.tangem.features.foryou.impl.model.converter.PERCENT_BASE
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.orZero
import com.tangem.utils.isNullOrZero
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Builds the For You earn-opportunities section. For every portfolio currency it resolves an earn rate
 * (see [resolveEarnApy]) and keeps only tokens that could earn (positive fiat balance) or already do
 * (active yield supply / stake).
 *
 * The section state is then picked from the result:
 * - nothing is earn-eligible → [ForYouEarnOpportunitiesNoTokensConverter] (suggests [topEarnTokens]);
 * - every eligible token already earns → [ForYouEarnOpportunitiesTokensActiveConverter]
 *   (suggests [topEarnTokens] the user is not earning on yet);
 * - otherwise → [ForYouEarnOpportunitiesPotentialRewardsConverter] (per-account potential rewards,
 *   accounts sorted by reward descending).
 */
@Suppress("LongParameterList")
internal class ForYouEarnOpportunitiesConverter(
    private val appCurrency: AppCurrency,
    private val isAccountsModeEnabled: Boolean,
    private val expandClick: (assetId: String) -> Unit,
    private val yieldSupplyAvailability: Map<String, BigDecimal>,
    private val yieldStakingAvailability: Map<CryptoCurrency, StakingAvailability>,
    private val topEarnTokens: EarnTopToken?,
    private val onTokenClick: (UserWalletId?, CryptoCurrency, ForYouEarnOpportunitiesType) -> Unit,
    private val onAllEarnTokensClick: () -> Unit,
    private val walletHeaders: Map<UserWalletId, ForYouWalletHeaderUM>,
    private val isBalanceHidden: Boolean = false,
) : Converter<ForYouSelectedPortfolio, EarnOpportunitiesUM> {

    override fun convert(value: ForYouSelectedPortfolio): EarnOpportunitiesUM {
        val data = value.accountCryptoCurrencyStatuses
            .groupBy { it.account }
            .mapNotNull { (account, accountStatuses) ->
                val tokenList = accountStatuses.mapNotNull { accountCryptoCurrencyStatus ->
                    val cryptoCurrencyStatus = accountCryptoCurrencyStatus.status
                    val earn = resolveEarnApy(
                        cryptoCurrencyStatus = cryptoCurrencyStatus,
                        yieldModuleApyMap = yieldSupplyAvailability,
                        stakingApyMap = yieldStakingAvailability,
                    )

                    if (earn == null || cryptoCurrencyStatus.value.fiatAmount.isNullOrZero() && !earn.isActive) {
                        return@mapNotNull null
                    }

                    cryptoCurrencyStatus to earn
                }

                if (tokenList.isEmpty()) return@mapNotNull null

                val accountPotentialReward = tokenList.sumOf { (_, earn) ->
                    earn.potentialRewards.orZero()
                }

                EarnOpportunities(
                    userWalletId = account.userWalletId,
                    account = account,
                    earnCurrencies = tokenList.toMap(),
                    accountPotentialReward = accountPotentialReward,
                )
            }
            .sortedByDescending { it.accountPotentialReward }

        return when {
            data.isEmpty() -> {
                ForYouEarnOpportunitiesNoTokensConverter(
                    topEarnTokens = topEarnTokens,
                    onTokenClick = onTokenClick,
                    onAllEarnTokensClick = onAllEarnTokensClick,
                ).convert(data)
            }
            data.all { earn -> earn.earnCurrencies.all { entry -> entry.value.isActive } } -> {
                ForYouEarnOpportunitiesTokensActiveConverter(
                    topEarnTokens = topEarnTokens,
                    onTokenClick = onTokenClick,
                    onAllEarnTokensClick = onAllEarnTokensClick,
                ).convert(data)
            }
            else -> {
                ForYouEarnOpportunitiesPotentialRewardsConverter(
                    appCurrency = appCurrency,
                    isAccountsModeEnabled = isAccountsModeEnabled,
                    expandClick = expandClick,
                    onTokenClick = onTokenClick,
                    onAllEarnTokensClick = onAllEarnTokensClick,
                    walletHeaders = walletHeaders,
                    isBalanceHidden = isBalanceHidden,
                ).convert(data)
            }
        }
    }

    /**
     * Resolves the earn rate for a currency; yield supply takes precedence over staking. The returned
     * [EarnApyInfo.apy] is a fraction (0.05 = 5%): yield APY arrives from the backend in percent and is
     * scaled down by [PERCENT_BASE]. Returns `null` when the token cannot earn at all.
     */
    private fun resolveEarnApy(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        yieldModuleApyMap: Map<String, BigDecimal>,
        stakingApyMap: Map<CryptoCurrency, StakingAvailability>,
    ): EarnApyInfo? {
        val token = cryptoCurrencyStatus.currency as? CryptoCurrency.Token
        if (token != null && yieldModuleApyMap.isNotEmpty()) {
            val yieldSupplyApy = yieldModuleApyMap.entries.firstOrNull { (tokenId, _) ->
                tokenId.equals(
                    other = token.yieldSupplyKey(),
                    ignoreCase = BlockchainUtils.isCaseInsensitiveContractAddress(token.network.rawId),
                )
            }?.value
            if (yieldSupplyApy != null) {
                val isActive = cryptoCurrencyStatus.value.yieldSupplyStatus?.isActive == true
                val apy = yieldSupplyApy.divide(PERCENT_BASE, RoundingMode.HALF_UP)
                return EarnApyInfo(
                    isActive = isActive,
                    potentialRewards = cryptoCurrencyStatus.value.fiatAmount?.multiply(apy),
                    apy = apy,
                    type = ForYouEarnOpportunitiesType.YieldSupply(
                        apy = yieldSupplyApy.toPlainString(),
                        rewardType = EarnRewardType.APY,
                    ),
                )
            }
        }

        if (stakingApyMap.isNotEmpty()) {
            val stakingInfo = findStakingRate(
                currencyStatus = cryptoCurrencyStatus,
                stakingApyMap = stakingApyMap,
            )
            if (stakingInfo != null) {
                return EarnApyInfo(
                    isActive = stakingInfo.isActive,
                    apy = stakingInfo.rate,
                    potentialRewards = cryptoCurrencyStatus.value.fiatAmount?.multiply(stakingInfo.rate),
                    type = ForYouEarnOpportunitiesType.Staking(
                        integrationID = stakingInfo.integrationId,
                        rewardType = when (stakingInfo.rewardType) {
                            RewardType.APY -> EarnRewardType.APY
                            RewardType.APR -> EarnRewardType.APR
                            RewardType.UNKNOWN -> EarnRewardType.APY
                        },
                    ),
                )
            }
        }

        return null
    }

    /**
     * Picks the staking rate to display: for an active StakeKit stake — the rate of the validator the user
     * actually stakes with (falling back to the best preferred one), otherwise the best preferred
     * validator's rate. [StakingAvailability.Full] pools are surfaced only for tokens already staked.
     */
    private fun findStakingRate(
        currencyStatus: CryptoCurrencyStatus,
        stakingApyMap: Map<CryptoCurrency, StakingAvailability>,
    ): StakingLocalInfo? {
        val availability = stakingApyMap[currencyStatus.currency]
        val option = availability?.optionOrNull ?: return null

        val stakingBalance = currencyStatus.value.stakingBalance as? StakingBalance.Data
        val stakeKitBalance = stakingBalance as? StakingBalance.Data.StakeKit
        val p2pEthPoolBalance = stakingBalance as? StakingBalance.Data.P2PEthPool
        val isActive = stakeKitBalance != null || p2pEthPoolBalance != null

        // Full = no free capacity: show the badge only for tokens that already have a stake.
        if (availability is StakingAvailability.Full && !isActive) {
            return null
        }

        val rateInfo = when (option) {
            is StakingOption.P2PEthPool -> {
                RewardInfo(
                    rate = option.apy,
                    type = RewardType.APY,
                )
            }
            is StakingOption.StakeKit -> if (stakeKitBalance != null) {
                val validatorsByAddress = option.yield.validators.associateBy { it.address }
                stakeKitBalance.balance.items
                    .mapNotNull { it.validatorAddress }
                    .firstNotNullOfOrNull { address ->
                        validatorsByAddress[address]?.rewardInfo
                    } ?: option.yield.validators
                    .filter { it.preferred }
                    .mapNotNull { validator ->
                        validator.rewardInfo
                    }
                    .maxByOrNull { it.rate }
            } else {
                option.yield.validators
                    .filter { it.preferred }
                    .mapNotNull { validator ->
                        validator.rewardInfo
                    }
                    .maxByOrNull { it.rate }
            }
        } ?: return null

        return StakingLocalInfo(
            rate = rateInfo.rate,
            isActive = isActive,
            rewardType = rateInfo.type,
            integrationId = option.integrationId,
        )
    }

    private data class StakingLocalInfo(
        val rate: BigDecimal,
        val isActive: Boolean,
        val rewardType: RewardType,
        val integrationId: StakingIntegrationID,
    )
}