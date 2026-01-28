package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.remove
import com.tangem.common.ui.amountScreen.converters.AmountAccountConverter
import com.tangem.common.ui.amountScreen.converters.AmountStateConverter
import com.tangem.common.ui.amountScreen.models.AmountParameters
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.list.RoundedListWithDividersItemData
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.staking.BalanceItem
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.model.StakingClickIntents
import com.tangem.features.staking.impl.presentation.state.InnerYieldBalanceState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingStep
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.bottomsheet.InfoType
import com.tangem.features.staking.impl.presentation.state.converters.RewardsValidatorStateConverter
import com.tangem.features.staking.impl.presentation.state.converters.YieldBalancesConverter
import com.tangem.features.staking.impl.presentation.state.utils.getRewardScheduleText
import com.tangem.utils.Provider
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.isNullOrZero
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

@Suppress("LongParameterList")
internal class SetInitialDataStateTransformer(
    private val clickIntents: StakingClickIntents,
    private val yield: Yield,
    private val isAnyTokenStaked: Boolean,
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val userWalletProvider: Provider<UserWallet>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val balancesToShowProvider: Provider<List<BalanceItem>>,
    private val isAccountsModeEnabled: Boolean,
    private val account: Account.CryptoPortfolio?,
    private val isBalanceHidden: Boolean,
) : Transformer<StakingUiState> {

    private val iconStateConverter by lazy(::CryptoCurrencyToIconStateConverter)

    private val rewardsValidatorStateConverter by lazy(LazyThreadSafetyMode.NONE) {
        RewardsValidatorStateConverter(cryptoCurrencyStatus, appCurrencyProvider, yield)
    }

    private val yieldBalancesConverter by lazy(LazyThreadSafetyMode.NONE) {
        YieldBalancesConverter(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            appCurrencyProvider = appCurrencyProvider,
            balancesToShowProvider = balancesToShowProvider,
            yield = yield,
        )
    }

    override fun transform(prevState: StakingUiState): StakingUiState {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        return prevState.copy(
            title = TextReference.EMPTY,
            cryptoCurrencyName = cryptoCurrency.name,
            cryptoCurrencySymbol = cryptoCurrency.symbol,
            cryptoCurrencyBlockchainId = cryptoCurrency.network.rawId,
            clickIntents = clickIntents,
            currentStep = StakingStep.InitialInfo,
            initialInfoState = createInitialInfoState(),
            amountState = createInitialAmountState(),
            confirmationState = StakingStates.ConfirmationState.Empty(),
            rewardsValidatorsState = rewardsValidatorStateConverter.convert(Unit),
            bottomSheetConfig = null,
        )
    }

    private fun createInitialInfoState(): StakingStates.InitialInfoState.Data {
        val yieldBalance = yieldBalancesConverter.convert(Unit)

        val status = cryptoCurrencyStatus.value
        return StakingStates.InitialInfoState.Data(
            isPrimaryButtonEnabled = with(status) {
                !amount.isNullOrZero() && sources.stakingBalanceSource.isActual() && sources.networkSource.isActual()
            },
            showBanner = !isAnyTokenStaked && yieldBalance == InnerYieldBalanceState.Empty,
            infoItems = getInfoItems(),
            onInfoClick = clickIntents::onInfoClick,
            yieldBalance = yieldBalance,
            pullToRefreshConfig = PullToRefreshConfig(
                onRefresh = { clickIntents.onRefreshSwipe(it.value) },
                isRefreshing = false,
            ),
        )
    }

    private fun getInfoItems(): PersistentList<RoundedListWithDividersItemData> {
        return listOfNotNull(
            createAnnualPercentageItem(),
            createAvailableItem(cryptoCurrencyStatus),
            createUnbondingPeriodItem(),
            createMinimumRequirementItem(cryptoCurrencyStatus),
            createRewardClaimingItem(),
            createWarmupPeriodItem(),
            createRewardScheduleItem(),
        ).toPersistentList()
    }

    private fun createAnnualPercentageItem(): RoundedListWithDividersItemData {
        val validators = yield.preferredValidators
        val rateRangeInfo = getPercentageRange(validators)
        return RoundedListWithDividersItemData(
            id = R.string.staking_details_annual_percentage_rate,
            startText = getRateStartText(rateRangeInfo.first),
            endText = rateRangeInfo.second,
            iconClick = {
                when (rateRangeInfo.first) {
                    Yield.RewardType.APR -> clickIntents.onInfoClick(InfoType.ANNUAL_PERCENTAGE_RATE)
                    Yield.RewardType.APY -> clickIntents.onInfoClick(InfoType.ANNUAL_PERCENTAGE_YIELD)
                    Yield.RewardType.UNKNOWN -> {}
                }
            },
            isEndTextHighlighted = false,
        )
    }

    private fun getRateStartText(rewardType: Yield.RewardType): TextReference {
        return when (rewardType) {
            Yield.RewardType.APR -> TextReference.Res(R.string.staking_details_annual_percentage_rate)
            Yield.RewardType.APY -> TextReference.Res(R.string.staking_details_annual_percentage_yield)
            else -> TextReference.EMPTY
        }
    }

    private fun createAvailableItem(cryptoCurrencyStatus: CryptoCurrencyStatus): RoundedListWithDividersItemData {
        return RoundedListWithDividersItemData(
            id = R.string.staking_details_available,
            startText = TextReference.Res(R.string.staking_details_available),
            endText = TextReference.Str(
                value = cryptoCurrencyStatus.value.amount.format { crypto(cryptoCurrencyStatus.currency) },
            ),
            isEndTextHideable = true,
        )
    }

    private fun createUnbondingPeriodItem(): RoundedListWithDividersItemData? {
        val cooldownPeriodDays = yield.metadata.cooldownPeriod?.days ?: return null
        return RoundedListWithDividersItemData(
            id = R.string.staking_details_unbonding_period,
            startText = TextReference.Res(R.string.staking_details_unbonding_period),
            endText = pluralReference(
                id = R.plurals.common_days,
                count = cooldownPeriodDays,
                formatArgs = wrappedList(cooldownPeriodDays),
            ),
            iconClick = { clickIntents.onInfoClick(InfoType.UNBONDING_PERIOD) },
        )
    }

    private fun createMinimumRequirementItem(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): RoundedListWithDividersItemData? {
        val minimumCryptoAmount = yield.args.enter.args[Yield.Args.ArgType.AMOUNT]?.minimum ?: return null
        val blockchainId = cryptoCurrencyStatus.currency.network.rawId
        if (!showMinimumRequirementInfo(blockchainId)) return null

        val formattedAmount = minimumCryptoAmount.format { crypto(cryptoCurrencyStatus.currency) }

        return RoundedListWithDividersItemData(
            id = R.string.staking_details_minimum_requirement,
            startText = TextReference.Res(R.string.staking_details_minimum_requirement),
            endText = TextReference.Str(formattedAmount),
        )
    }

    private fun createRewardClaimingItem(): RoundedListWithDividersItemData? {
        val rewardClaiming = yield.metadata.rewardClaiming
        val endTextId = rewardClaimingResources[rewardClaiming] ?: return null

        return RoundedListWithDividersItemData(
            id = R.string.staking_details_reward_claiming,
            startText = TextReference.Res(R.string.staking_details_reward_claiming),
            endText = TextReference.Res(endTextId),
            iconClick = { clickIntents.onInfoClick(InfoType.REWARD_CLAIMING) },
        )
    }

    private fun createWarmupPeriodItem(): RoundedListWithDividersItemData? {
        val warmupPeriodDays = yield.metadata.warmupPeriod.days
        if (warmupPeriodDays == 0) return null

        return RoundedListWithDividersItemData(
            id = R.string.staking_details_warmup_period,
            startText = TextReference.Res(R.string.staking_details_warmup_period),
            endText = pluralReference(
                id = R.plurals.common_days,
                count = warmupPeriodDays,
                formatArgs = wrappedList(warmupPeriodDays),
            ),
            iconClick = { clickIntents.onInfoClick(InfoType.WARMUP_PERIOD) },
        )
    }

    private fun createRewardScheduleItem(): RoundedListWithDividersItemData? {
        val endTextReference = getRewardScheduleText(
            rewardSchedule = yield.metadata.rewardSchedule,
            networkId = cryptoCurrencyStatus.currency.network.rawId,
            decapitalize = false,
        ) ?: return null

        return RoundedListWithDividersItemData(
            id = R.string.staking_details_reward_schedule,
            startText = resourceReference(R.string.staking_details_reward_schedule),
            endText = endTextReference,
            iconClick = { clickIntents.onInfoClick(InfoType.REWARD_SCHEDULE) },
        )
    }

    private fun createInitialAmountState(): AmountState {
        val cryptoBalanceValue = cryptoCurrencyStatus.value
        val maxEnterAmount = EnterAmountBoundary(
            amount = cryptoBalanceValue.amount,
            fiatAmount = cryptoBalanceValue.fiatAmount,
            fiatRate = cryptoBalanceValue.fiatRate,
        )
        return AmountStateConverter(
            clickIntents = clickIntents,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            appCurrency = appCurrencyProvider(),
            iconStateConverter = iconStateConverter,
            maxEnterAmount = maxEnterAmount,
            isBalanceHidden = isBalanceHidden,
            accountTitleUM = AmountAccountConverter(
                isAccountsMode = isAccountsModeEnabled,
                walletTitle = stringReference(userWalletProvider().name),
                prefixText = resourceReference(R.string.common_from),
            ).convert(account),
        ).convert(
            AmountParameters(
                title = stringReference(userWalletProvider().name),
                value = "",
            ),
        )
    }

    private fun getPercentageRange(validators: List<Yield.Validator>): Pair<Yield.RewardType, TextReference> {
        if (validators.isEmpty()) {
            return Yield.RewardType.APR to stringReference(DASH_SIGN)
        }
        val rewardInfos = validators
            .filter { it.preferred }
            .takeIf { it.isNotEmpty() }
            ?.mapNotNull { it.rewardInfo }
            ?: validators.mapNotNull { it.rewardInfo }

        val infoWithMinRate = rewardInfos.minBy { it.rate }
        val infoWithMaxRate = rewardInfos.maxBy { it.rate }

        val formattedMinRate = infoWithMinRate.rate.format { percent() }.remove("%")
        val formattedMaxRate = infoWithMaxRate.rate.format { percent() }

        if (infoWithMaxRate.rate - infoWithMinRate.rate < EQUALITY_THRESHOLD) {
            return infoWithMaxRate.type to stringReference("$formattedMinRate%")
        }
        return infoWithMaxRate.type to
            resourceReference(
                id = R.string.common_range,
                formatArgs = wrappedList(formattedMinRate, formattedMaxRate),
            )
    }

    private fun showMinimumRequirementInfo(blockchainId: String): Boolean {
        return blockchainId == Blockchain.Polkadot.id || blockchainId == Blockchain.Cardano.id
    }

    private companion object {
        val EQUALITY_THRESHOLD = BigDecimal(1E-10)

        val rewardClaimingResources = mapOf(
            Yield.Metadata.RewardClaiming.MANUAL to R.string.staking_reward_claiming_manual,
            Yield.Metadata.RewardClaiming.AUTO to R.string.staking_reward_claiming_auto,
            Yield.Metadata.RewardSchedule.UNKNOWN to null,
        )
    }
}