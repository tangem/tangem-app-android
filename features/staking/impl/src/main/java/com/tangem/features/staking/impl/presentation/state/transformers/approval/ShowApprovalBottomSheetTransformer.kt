package com.tangem.features.staking.impl.presentation.state.transformers.approval

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.bottomsheet.permission.state.*
import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.Provider
import com.tangem.utils.transformer.Transformer

internal class ShowApprovalBottomSheetTransformer(
    private val userWallet: UserWallet,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val feeCryptoCurrencyStatus: CryptoCurrencyStatus?,
    private val onDismiss: () -> Unit,
) : Transformer<StakingUiState> {
    override fun transform(prevState: StakingUiState): StakingUiState {
        val cryptoCurrency = cryptoCurrencyStatusProvider().currency
        val cryptoCurrencyValue = cryptoCurrencyStatusProvider().value

        val amountState = prevState.amountState as? AmountState.Data ?: return prevState
        val confirmationState = prevState.confirmationState as? StakingStates.ConfirmationState.Data ?: return prevState
        val validatorState = prevState.validatorState as? StakingStates.ValidatorState.Data ?: return prevState
        val feeState = confirmationState.feeState as? FeeState.Content ?: return prevState
        val fee = feeState.fee ?: return prevState

        val walletAddress = cryptoCurrencyValue.networkAddress?.defaultAddress?.value.orEmpty()
        val validatorAddress = validatorState.chosenValidator.address
        val feeCryptoValue = fee.amount.value.format {
            crypto(fee.amount.currencySymbol, fee.amount.decimals)
        }
        val feeFiatValue = feeCryptoCurrencyStatus?.value?.fiatRate?.multiply(fee.amount.value).format {
            fiat(
                fiatCurrencyCode = appCurrencyProvider().code,
                fiatCurrencySymbol = appCurrencyProvider().symbol,
            )
        }
        return prevState.copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = onDismiss,
                content = GiveTxPermissionBottomSheetConfig(
                    data = GiveTxPermissionState.ReadyForRequest(
                        currency = cryptoCurrency.symbol,
                        amount = amountState.amountTextField.value,
                        approveType = ApproveType.UNLIMITED,
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
                        footerText = resourceReference(R.string.staking_give_permission_fee_footer),
                        onChangeApproveType = prevState.clickIntents::onApproveTypeChange,
                    ),
                    walletInteractionIcon = walletInterationIcon(userWallet),
                    onCancel = onDismiss,
                ),
            ),
        )
    }
}