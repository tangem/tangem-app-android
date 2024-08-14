package com.tangem.features.staking.impl.presentation.state.transformers.approval

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.bottomsheet.permission.state.*
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.ValidatorState
import com.tangem.utils.Provider
import com.tangem.utils.transformer.Transformer

internal class ShowApprovalBottomSheetTransformer(
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val onDismiss: () -> Unit,
) : Transformer<StakingUiState> {
    override fun transform(prevState: StakingUiState): StakingUiState {
        val cryptoCurrency = cryptoCurrencyStatusProvider().currency
        val cryptoCurrencyValue = cryptoCurrencyStatusProvider().value

        val amountState = prevState.amountState as? AmountState.Data ?: return prevState
        val confirmationState = prevState.confirmationState as? StakingStates.ConfirmationState.Data ?: return prevState
        val validatorState = confirmationState.validatorState as? ValidatorState.Content ?: return prevState
        val feeState = confirmationState.feeState as? FeeState.Content ?: return prevState

        val walletAddress = cryptoCurrencyValue.networkAddress?.defaultAddress?.value.orEmpty()
        val validatorAddress = validatorState.chosenValidator.address
        val feeCryptoValue = BigDecimalFormatter.formatCryptoAmount(
            cryptoAmount = feeState.fee?.amount?.value,
            cryptoCurrency = cryptoCurrency,
        )
        val feeFiatValue = BigDecimalFormatter.formatFiatAmount(
            fiatAmount = feeState.fee?.amount?.maxValue,
            fiatCurrencyCode = appCurrencyProvider().code,
            fiatCurrencySymbol = appCurrencyProvider().symbol,
        )
        return prevState.copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShow = true,
                onDismissRequest = onDismiss,
                content = GiveTxPermissionBottomSheetConfig(
                    data = GiveTxPermissionState.ReadyForRequest(
                        currency = cryptoCurrency.symbol,
                        amount = amountState.amountTextField.value,
                        approveType = ApproveType.LIMITED,
                        walletAddress = walletAddress,
                        spenderAddress = validatorAddress,
                        fee = resourceReference(
                            R.string.common_crypto_fiat_format,
                            wrappedList(feeCryptoValue, feeFiatValue),
                        ),
                        approveButton = ApprovePermissionButton(
                            enabled = true,
                            loading = false,
                            onClick = prevState.clickIntents::onApprovalClick,
                        ),
                        cancelButton = CancelPermissionButton(
                            enabled = true,
                        ),
                        subtitle = resourceReference(
                            id = R.string.give_permission_staking_subtitle,
                            formatArgs = wrappedList(cryptoCurrency.symbol),
                        ),
                        dialogText = resourceReference(R.string.give_permission_staking_footer),
                    ),
                    onCancel = onDismiss,
                ),
            ),
        )
    }
}