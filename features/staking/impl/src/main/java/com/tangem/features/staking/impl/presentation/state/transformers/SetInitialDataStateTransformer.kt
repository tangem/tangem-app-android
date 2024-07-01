package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.common.ui.amountScreen.converters.AmountStateConverter
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.components.currency.tokenicon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.BalanceType
import com.tangem.domain.staking.model.Yield
import com.tangem.domain.staking.model.YieldBalance
import com.tangem.domain.staking.model.YieldBalanceItem
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.previewdata.ConfirmStakingStatePreviewData
import com.tangem.features.staking.impl.presentation.viewmodel.StakingClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.extensions.orZero
import com.tangem.utils.isNullOrZero
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal

internal class SetInitialDataStateTransformer(
    private val clickIntents: StakingClickIntents,
    private val yield: Yield,
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

    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(
            clickIntents = clickIntents,
            currentStep = StakingStep.InitialInfo,
            initialInfoState = createInitialInfoState(),
            amountState = createInitialAmountState(),
            confirmStakingState = createInitialConfirmationState(),
        )
    }

    private fun createInitialInfoState(): StakingStates.InitialInfoState.Data {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val yieldBalance = cryptoCurrencyStatus.value.yieldBalance

        return StakingStates.InitialInfoState.Data(
            isPrimaryButtonEnabled = true,
            available = BigDecimalFormatter.formatCryptoAmount(
                cryptoAmount = cryptoCurrencyStatus.value.amount,
                cryptoCurrency = cryptoCurrencyStatus.currency.symbol,
                decimals = cryptoCurrencyStatus.currency.decimals,
            ),
            onStake = BigDecimalFormatter.formatCryptoAmount(
                cryptoAmount = (yieldBalance as? YieldBalance.Data)?.getTotalStakingBalance().orZero(),
                cryptoCurrency = cryptoCurrencyStatus.currency.symbol,
                decimals = cryptoCurrencyStatus.currency.decimals,
            ),
            aprRange = getAprRange(),
            unbondingPeriod = yield.metadata.cooldownPeriod.days.toString(),
            minimumRequirement = yield.metadata.minimumStake.toString(),
            rewardClaiming = yield.metadata.rewardClaiming,
            warmupPeriod = yield.metadata.warmupPeriod.days.toString(),
            rewardSchedule = yield.metadata.rewardSchedule,
            onInfoClick = clickIntents::onInfoClick,
            yieldBalance = getStakedBalances(cryptoCurrencyStatus),
        )
    }

    private fun createInitialAmountState(): AmountState {
        return amountStateConverter.convert("")
    }

    private fun createInitialConfirmationState(): StakingStates.ConfirmStakingState {
        return ConfirmStakingStatePreviewData.confirmStakingState.copy(
            validatorState = ValidatorState.Content(
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
        )
        val formattedMaxApr = BigDecimalFormatter.formatPercent(
            percent = maxApr,
            useAbsoluteValue = true,
        )

        if (maxApr - minApr < EQUALITY_THRESHOLD) {
            return stringReference("$formattedMinApr%")
        }
        return resourceReference(R.string.common_range, wrappedList(formattedMinApr, formattedMaxApr))
    }

    private fun getStakedBalances(cryptoCurrencyStatus: CryptoCurrencyStatus): InnerYieldBalanceState {
        val yieldBalance = cryptoCurrencyStatus.value.yieldBalance
        return if (yieldBalance is YieldBalance.Data) {
            val appCurrency = appCurrencyProvider()
            val cryptoCurrency = cryptoCurrencyStatus.currency
            val cryptoRewardsValue = yieldBalance.getRewardStakingBalance()
            val fiatRewardsValue = cryptoCurrencyStatus.value.fiatRate?.times(cryptoRewardsValue)
            val groupedBalances = getGroupedBalance(yieldBalance.balance, cryptoCurrencyStatus, appCurrency)

            InnerYieldBalanceState.Data(
                rewardsCrypto = BigDecimalFormatter.formatCryptoAmount(
                    cryptoAmount = cryptoRewardsValue,
                    cryptoCurrency = cryptoCurrency,
                ),
                rewardsFiat = BigDecimalFormatter.formatFiatAmount(
                    fiatAmount = fiatRewardsValue,
                    fiatCurrencyCode = appCurrency.code,
                    fiatCurrencySymbol = appCurrency.symbol,
                ),
                isRewardsToClaim = !cryptoRewardsValue.isNullOrZero(),
                balance = groupedBalances,
            )
        } else {
            InnerYieldBalanceState.Empty
        }
    }

    private fun getGroupedBalance(
        balance: YieldBalanceItem,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        appCurrency: AppCurrency,
    ) = balance.items
        .sortedByDescending { it.type }
        .groupBy { it.type }
        .mapNotNull { item ->
            val (title, footer) = getValidatorBalanceInto(item.key)

            val balances = item.value.mapNotNull { balance ->
                val validator = yield.validators.firstOrNull {
                    balance.validatorAddress?.contains(it.address, ignoreCase = true) == true
                }
                val cryptoAmount = balance.amount * balance.pricePerShare
                val fiatAmount = cryptoCurrencyStatus.value.fiatRate?.times(cryptoAmount)

                validator?.let {
                    BalanceState(
                        validator = validator,
                        cryptoAmount = stringReference(
                            BigDecimalFormatter.formatCryptoAmount(
                                cryptoAmount = balance.amount,
                                cryptoCurrency = cryptoCurrencyStatus.currency,
                            ),
                        ),
                        fiatAmount = stringReference(
                            BigDecimalFormatter.formatFiatAmount(
                                fiatAmount = fiatAmount,
                                fiatCurrencyCode = appCurrency.code,
                                fiatCurrencySymbol = appCurrency.symbol,
                            ),
                        ),
                        rawCurrencyId = balance.rawCurrencyId,
                    )
                }
            }

            title?.let {
                BalanceGroupedState(
                    items = balances,
                    type = item.key,
                    footer = footer,
                    title = it,
                )
            }
        }

    private fun getValidatorBalanceInto(type: BalanceType) = when (type) {
        BalanceType.PREPARING,
        BalanceType.STAKED,
        -> resourceReference(R.string.staking_active) to resourceReference(R.string.staking_active_footer)
        BalanceType.UNSTAKING,
        BalanceType.UNLOCKING,
        BalanceType.UNSTAKED,
        -> resourceReference(R.string.staking_unstaked) to resourceReference(R.string.staking_unstaked_footer)
        BalanceType.AVAILABLE,
        BalanceType.LOCKED,
        BalanceType.UNKNOWN,
        BalanceType.REWARDS,
        -> null to null
    }

    companion object {
        private val EQUALITY_THRESHOLD = BigDecimal(1E-10)
    }
}
