package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.common.extensions.remove
import com.tangem.common.ui.amountScreen.converters.AmountStateConverter
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.list.RoundedListWithDividersItemData
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingStep
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.ValidatorState
import com.tangem.features.staking.impl.presentation.state.converters.RewardsValidatorStateConverter
import com.tangem.features.staking.impl.presentation.state.converters.YieldBalancesConverter
import com.tangem.features.staking.impl.presentation.state.previewdata.ConfirmationStatePreviewData
import com.tangem.features.staking.impl.presentation.viewmodel.StakingClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.extensions.orZero
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

internal class SetInitialDataStateTransformer(
    private val clickIntents: StakingClickIntents,
    private val yield: Yield,
    private val isStakeMoreAvailable: Boolean,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val userWalletProvider: Provider<UserWallet>,
    private val appCurrencyProvider: Provider<AppCurrency>,
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
        YieldBalancesConverter(cryptoCurrencyStatusProvider, appCurrencyProvider, yield)
    }

    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(
            clickIntents = clickIntents,
            currentStep = StakingStep.InitialInfo,
            initialInfoState = createInitialInfoState(),
            amountState = createInitialAmountState(),
            confirmationState = createInitialConfirmationState(),
            rewardsValidatorsState = rewardsValidatorStateConverter.convert(Unit),
            bottomSheetConfig = null,
        )
    }

    private fun createInitialInfoState(): StakingStates.InitialInfoState.Data {
        return StakingStates.InitialInfoState.Data(
            isPrimaryButtonEnabled = true,
            aprRange = getAprRange(),
            infoItems = getInfoItems(),
            onInfoClick = clickIntents::onInfoClick,
            yieldBalance = yieldBalancesConverter.convert(Unit),
            isStakeMoreAvailable = isStakeMoreAvailable,
        )
    }

    private fun getInfoItems(): PersistentList<RoundedListWithDividersItemData> {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val yieldBalance = cryptoCurrencyStatus.value.yieldBalance

        return persistentListOf(
            RoundedListWithDividersItemData(
                id = R.string.staking_details_available,
                startText = TextReference.Res(R.string.staking_details_available),
                endText = TextReference.Str(
                    value = BigDecimalFormatter.formatCryptoAmount(
                        cryptoAmount = cryptoCurrencyStatus.value.amount,
                        cryptoCurrency = cryptoCurrencyStatus.currency.symbol,
                        decimals = cryptoCurrencyStatus.currency.decimals,
                    ),
                ),
            ),
            RoundedListWithDividersItemData(
                id = R.string.staking_details_apy,
                startText = TextReference.Res(R.string.staking_details_apy),
                endText = getAprRange(),
                iconClick = { clickIntents.onInfoClick(InfoType.APY) },
            ),
            RoundedListWithDividersItemData(
                id = R.string.staking_details_on_stake,
                startText = TextReference.Res(R.string.staking_details_on_stake),
                endText = TextReference.Str(
                    value = BigDecimalFormatter.formatCryptoAmount(
                        cryptoAmount = (yieldBalance as? YieldBalance.Data)?.getTotalStakingBalance().orZero(),
                        cryptoCurrency = cryptoCurrencyStatus.currency.symbol,
                        decimals = cryptoCurrencyStatus.currency.decimals,
                    ),
                ),
            ),
            RoundedListWithDividersItemData(
                id = R.string.staking_details_unbonding_period,
                startText = TextReference.Res(R.string.staking_details_unbonding_period),
                endText = TextReference.Str(yield.metadata.cooldownPeriod.days.toString()),
                iconClick = { clickIntents.onInfoClick(InfoType.UNBOUNDING_PERIOD) },
            ),
            RoundedListWithDividersItemData(
                id = R.string.staking_details_minimum_requirement,
                startText = TextReference.Res(R.string.staking_details_minimum_requirement),
                endText = TextReference.Str(
                    value = BigDecimalFormatter.formatCryptoAmount(
                        cryptoAmount = yield.args.enter.args[Yield.Args.ArgType.AMOUNT]?.minimum,
                        cryptoCurrency = cryptoCurrencyStatus.currency.symbol,
                        decimals = cryptoCurrencyStatus.currency.decimals,
                    ),
                ),
            ),
            RoundedListWithDividersItemData(
                id = R.string.staking_details_reward_claiming,
                startText = TextReference.Res(R.string.staking_details_reward_claiming),
                endText = TextReference.Str(yield.metadata.rewardClaiming),
                iconClick = { clickIntents.onInfoClick(InfoType.REWARD_CLAIMING) },
            ),
            RoundedListWithDividersItemData(
                id = R.string.staking_details_warmup_period,
                startText = TextReference.Res(R.string.staking_details_warmup_period),
                endText = TextReference.Str(yield.metadata.warmupPeriod.days.toString()),
                iconClick = { clickIntents.onInfoClick(InfoType.WARMUP_PERIOD) },
            ),
            RoundedListWithDividersItemData(
                id = R.string.staking_details_reward_schedule,
                startText = TextReference.Res(R.string.staking_details_reward_schedule),
                endText = TextReference.Str(yield.metadata.rewardSchedule),
                iconClick = { clickIntents.onInfoClick(InfoType.REWARD_SCHEDULE) },
            ),
        )
    }

    private fun createInitialAmountState(): AmountState {
        return amountStateConverter.convert("")
    }

    private fun createInitialConfirmationState(): StakingStates.ConfirmationState {
        return ConfirmationStatePreviewData.assentStakingState.copy(
            validatorState = ValidatorState.Content(
                isClickable = true,
                chosenValidator = yield.validators.first(),
                availableValidators = yield.validators,
            ),
        )
    }

    private fun getAprRange(): TextReference {
        val aprValues = yield.validators.mapNotNull { it.apr }

        val minApr = aprValues.min()
        val maxApr = aprValues.max()

        val formattedMinApr = BigDecimalFormatter.formatPercent(
            percent = minApr,
            useAbsoluteValue = true,
        ).remove("%")
        val formattedMaxApr = BigDecimalFormatter.formatPercent(
            percent = maxApr,
            useAbsoluteValue = true,
        )

        if (maxApr - minApr < EQUALITY_THRESHOLD) {
            return stringReference("$formattedMinApr%")
        }
        return resourceReference(R.string.common_range, wrappedList(formattedMinApr, formattedMaxApr))
    }

    companion object {
        private val EQUALITY_THRESHOLD = BigDecimal(1E-10)
    }
}