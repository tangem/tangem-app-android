package com.tangem.features.foryou.impl.model.converter.earnOpportunities

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.earn.EarnRewardType
import com.tangem.domain.models.earn.EarnTopToken
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.earn.EarnRate
import com.tangem.domain.staking.earn.ResolveEarnRateUseCase
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.common.RewardType
import com.tangem.features.foryou.impl.entity.EarnOpportunitiesUM
import com.tangem.features.foryou.impl.entity.ForYouEarnOpportunitiesType
import com.tangem.features.foryou.impl.entity.ForYouWalletHeaderUM
import com.tangem.features.foryou.impl.model.ForYouSelectedPortfolio
import com.tangem.features.foryou.impl.model.converter.EarnApyInfo
import com.tangem.features.foryou.impl.model.converter.EarnOpportunities
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.orZero
import com.tangem.utils.isNullOrZero
import java.math.BigDecimal

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

    private val resolveEarnRateUseCase = ResolveEarnRateUseCase()

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
     * Maps the resolved [EarnRate] onto the section's own [EarnApyInfo], adding the potential reward the
     * rate would yield on the currency's fiat balance.
     */
    private fun resolveEarnApy(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        yieldModuleApyMap: Map<String, BigDecimal>,
        stakingApyMap: Map<CryptoCurrency, StakingAvailability>,
    ): EarnApyInfo? {
        val earnRate = resolveEarnRateUseCase(
            currencyStatus = cryptoCurrencyStatus,
            yieldSupplyApy = yieldModuleApyMap,
            stakingAvailability = stakingApyMap,
        ) ?: return null

        return EarnApyInfo(
            isActive = earnRate.isActive,
            apy = earnRate.apy,
            potentialRewards = cryptoCurrencyStatus.value.fiatAmount?.multiply(earnRate.apy),
            type = when (val source = earnRate.source) {
                is EarnRate.Source.YieldSupply -> ForYouEarnOpportunitiesType.YieldSupply(
                    apy = source.apyPercent.toPlainString(),
                    rewardType = EarnRewardType.APY,
                )
                is EarnRate.Source.Staking -> ForYouEarnOpportunitiesType.Staking(
                    integrationID = source.integrationId,
                    rewardType = when (source.rewardType) {
                        RewardType.APY -> EarnRewardType.APY
                        RewardType.APR -> EarnRewardType.APR
                        RewardType.UNKNOWN -> EarnRewardType.APY
                    },
                )
            },
        )
    }
}