package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.Yield
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.usecase.IsFeeApproximateUseCase
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.previewdata.ConfirmStakingStatePreviewData
import com.tangem.utils.Provider
import com.tangem.utils.transformer.Transformer

@Suppress("UnusedPrivateMember")
internal class SetConfirmStateDataStateTransformer(
    private val yield: Yield,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val isFeeApproximateUseCase: IsFeeApproximateUseCase,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        // TODO staking fill with real data
        return prevState.copy(
            confirmStakingState = ConfirmStakingStatePreviewData.confirmStakingState,
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