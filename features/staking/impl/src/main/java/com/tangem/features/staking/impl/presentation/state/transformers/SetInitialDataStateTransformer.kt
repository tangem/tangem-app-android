package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.common.extensions.remove
import com.tangem.common.ui.amountScreen.converters.AmountStateConverter
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.list.RoundedListWithDividersItemData
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.pullToRefresh.PullToRefreshConfig
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.stakekit.BalanceItem
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.bottomsheet.InfoType
import com.tangem.features.staking.impl.presentation.state.converters.RewardsValidatorStateConverter
import com.tangem.features.staking.impl.presentation.state.converters.YieldBalancesConverter
import com.tangem.features.staking.impl.presentation.state.utils.getRewardSchedule
import com.tangem.features.staking.impl.presentation.viewmodel.StakingClickIntents
import com.tangem.lib.crypto.BlockchainUtils.isPolkadot
import com.tangem.utils.Provider
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
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val userWalletProvider: Provider<UserWallet>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val balancesToShowProvider: Provider<List<BalanceItem>>,
) : Transformer<StakingUiState> {

    private val iconStateConverter by lazy(::CryptoCurrencyToIconStateConverter)

    private val amountStateConverter by lazy(LazyThreadSafetyMode.NONE) {
        AmountStateConverter(
            clickIntents = clickIntents,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
            appCurrencyProvider = appCurrencyProvider,
            userWalletProvider = userWalletProvider,
            iconStateConverter = iconStateConverter,
        )
    }

    private val rewardsValidatorStateConverter by lazy(LazyThreadSafetyMode.NONE) {
        RewardsValidatorStateConverter(cryptoCurrencyStatusProvider, appCurrencyProvider, yield)
    }

    private val yieldBalancesConverter by lazy(LazyThreadSafetyMode.NONE) {
        YieldBalancesConverter(
            cryptoCurrencyStatusProvider,
            appCurrencyProvider,
            balancesToShowProvider,
            yield,
        )
    }

    override fun transform(prevState: StakingUiState): StakingUiState {
        val cryptoCurrency = cryptoCurrencyStatusProvider().currency
        return prevState.copy(
            title = TextReference.EMPTY,
            cryptoCurrencyName = cryptoCurrency.name,
            cryptoCurrencySymbol = cryptoCurrency.symbol,
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
        return StakingStates.InitialInfoState.Data(
            isPrimaryButtonEnabled = !cryptoCurrencyStatusProvider().value.amount.isNullOrZero(),
            showBanner = !isAnyTokenStaked && yieldBalance == InnerYieldBalanceState.Empty,
            aprRange = getAprRange(yield.preferredValidators),
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
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()

        return listOfNotNull(
            createAnnualPercentageRateItem(),
            createAvailableItem(cryptoCurrencyStatus),
            createUnbondingPeriodItem(),
            createMinimumRequirementItem(cryptoCurrencyStatus),
            createRewardClaimingItem(),
            createWarmupPeriodItem(),
            createRewardScheduleItem(),
        ).toPersistentList()
    }

    private fun createAnnualPercentageRateItem(): RoundedListWithDividersItemData {
        val validators = yield.preferredValidators
        return RoundedListWithDividersItemData(
            id = R.string.staking_details_annual_percentage_rate,
            startText = TextReference.Res(R.string.staking_details_annual_percentage_rate),
            endText = getAprRange(validators),
            iconClick = { clickIntents.onInfoClick(InfoType.ANNUAL_PERCENTAGE_RATE) },
            isEndTextHighlighted = true,
        )
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
        if (!isPolkadot(cryptoCurrencyStatus.currency.network.id.value)) return null

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
        val endTextReference = getRewardScheduleText() ?: return null

        return RoundedListWithDividersItemData(
            id = R.string.staking_details_reward_schedule,
            startText = resourceReference(R.string.staking_details_reward_schedule),
            endText = endTextReference,
            iconClick = { clickIntents.onInfoClick(InfoType.REWARD_SCHEDULE) },
        )
    }

    private fun createInitialAmountState(): AmountState {
        return amountStateConverter.convert("")
    }

    private fun getAprRange(validators: List<Yield.Validator>): TextReference {
        val aprValues = validators.mapNotNull { it.apr }

        val minApr = aprValues.min()
        val maxApr = aprValues.max()

        val formattedMinApr = minApr.format { percent() }.remove("%")
        val formattedMaxApr = maxApr.format { percent() }

        if (maxApr - minApr < EQUALITY_THRESHOLD) {
            return stringReference("$formattedMinApr%")
        }
        return resourceReference(R.string.common_range, wrappedList(formattedMinApr, formattedMaxApr))
    }

    private fun getRewardScheduleText(): TextReference? {
        val rewardSchedule = getRewardSchedule(
            yield.metadata.rewardSchedule,
            cryptoCurrencyStatusProvider().currency.network.id.value,
        ) ?: return null

        return resourceReference(
            R.string.staking_reward_schedule_each,
            wrappedList(rewardSchedule),
        )
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
