package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.Yield
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.usecase.IsFeeApproximateUseCase
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.InnerFeeState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.Provider
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal

internal class SetConfirmStateDataStateTransformer(
    private val yield: Yield,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val isFeeApproximateUseCase: IsFeeApproximateUseCase,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        // TODO staking fill with real data
        val fee = Fee.Common(
            Amount(value = BigDecimal(1), blockchain = Blockchain.Solana),
        )
        return prevState.copy(
            confirmStakingState = StakingStates.ConfirmStakingState.Data(
                isPrimaryButtonEnabled = true,
                feeState = StakingStates.ConfirmStakingState.FeeState(
                    innerFeeState = InnerFeeState.Content(TransactionFee.Single(fee)),
                    fee = fee,
                    rate = null,
                    isFeeConvertibleToFiat = true,
                    appCurrency = appCurrencyProvider.invoke(),
                    isFeeApproximate = isFeeApproximate(fee),
                ),
                validatorState = StakingStates.ConfirmStakingState.ValidatorState(
                    validatorState = InnerValidatorState.Content(
                        chosenValidator = yield.validators.first(),
                    ),
                    availableValidators = yield.validators,
                ),
                isStaking = false,
                isSuccess = false,
            ),
        )
    }

    private fun isFeeApproximate(fee: Fee): Boolean {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        return isFeeApproximateUseCase(
            networkId = cryptoCurrencyStatus.currency.network.id,
            amountType = fee.amount.type,
        )
    }
}
