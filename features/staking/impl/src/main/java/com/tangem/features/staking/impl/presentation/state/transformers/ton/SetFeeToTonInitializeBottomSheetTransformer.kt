package com.tangem.features.staking.impl.presentation.state.transformers.ton

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.bottomsheet.TonInitializeAccountBottomSheetConfig
import com.tangem.utils.Provider
import com.tangem.utils.transformer.Transformer

internal class SetFeeToTonInitializeBottomSheetTransformer(
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val feeCryptoCurrencyStatus: CryptoCurrencyStatus?,
    private val fee: Fee,
    private val isFeeApproximate: Boolean,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val prevBottomSheetConfig = prevState.bottomSheetConfig ?: return prevState
        val prevBottomSheetConfigContent =
            prevBottomSheetConfig.content as? TonInitializeAccountBottomSheetConfig ?: return prevState

        val isFeeConvertibleToFiat = feeCryptoCurrencyStatus?.currency?.network?.hasFiatFeeRate == true
        return prevState.copy(
            bottomSheetConfig = prevBottomSheetConfig.copy(
                content = prevBottomSheetConfigContent.copy(
                    isButtonEnabled = true,
                    isButtonLoading = false,
                    feeState = FeeState.Content(
                        fee = fee,
                        rate = feeCryptoCurrencyStatus?.value?.fiatRate,
                        isFeeConvertibleToFiat = isFeeConvertibleToFiat,
                        appCurrency = appCurrencyProvider(),
                        isFeeApproximate = isFeeApproximate,
                    ),
                ),
            ),
        )
    }
}