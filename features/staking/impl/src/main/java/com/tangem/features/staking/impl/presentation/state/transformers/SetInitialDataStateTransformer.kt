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
import com.tangem.domain.staking.model.Yield
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingStep
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.ValidatorState
import com.tangem.features.staking.impl.presentation.state.previewdata.ConfirmStakingStatePreviewData
import com.tangem.features.staking.impl.presentation.viewmodel.StakingClickIntents
import com.tangem.utils.Provider
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

        return StakingStates.InitialInfoState.Data(
            isPrimaryButtonEnabled = true,
            available = BigDecimalFormatter.formatCryptoAmount(
                cryptoCurrencyStatus.value.amount,
                cryptoCurrency = cryptoCurrencyStatus.currency.symbol,
                decimals = cryptoCurrencyStatus.currency.decimals,
            ),
            onStake = "0 SOL", // TODO staking add after adding /balances request
            aprRange = getAprRange(),
            unbondingPeriod = yield.metadata.cooldownPeriod.days.toString(),
            minimumRequirement = yield.metadata.minimumStake.toString(),
            rewardClaiming = yield.metadata.rewardClaiming,
            warmupPeriod = yield.metadata.warmupPeriod.days.toString(),
            rewardSchedule = yield.metadata.rewardSchedule,

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

    companion object {
        private val EQUALITY_THRESHOLD = BigDecimal(1E-10)
    }
}