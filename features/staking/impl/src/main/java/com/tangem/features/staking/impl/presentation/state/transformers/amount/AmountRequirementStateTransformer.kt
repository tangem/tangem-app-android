package com.tangem.features.staking.impl.presentation.state.transformers.amount

import androidx.compose.ui.text.input.ImeAction
import com.tangem.common.extensions.isZero
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.isNullOrEmpty
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.staking.model.stakekit.AddressArgument
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.R
import com.tangem.lib.crypto.BlockchainUtils.isTron
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal
import java.math.RoundingMode

internal class AmountRequirementStateTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val yield: Yield,
    private val actionType: StakingActionCommonType,
) : Transformer<AmountState> {
    override fun transform(prevState: AmountState): AmountState {
        val amountRequirements = yield.args.enter.args[Yield.Args.ArgType.AMOUNT]

        return if (prevState is AmountState.Data && amountRequirements != null) {
            updateWithError(
                prevState,
                actionType,
                amountRequirements,
            )
        } else {
            prevState
        }
    }

    private fun updateWithError(
        amountState: AmountState.Data,
        actionType: StakingActionCommonType,
        amountRequirements: AddressArgument,
    ): AmountState {
        val isRequirementError = isRequirementError(amountState, amountRequirements)
        val isIntegerOnlyError = isIntegerOnlyError(amountState, actionType)

        val cryptoAmount = amountState.amountTextField.cryptoAmount
        val roundedDownCrypto = cryptoAmount.value?.setScale(0, RoundingMode.DOWN)
        val value = roundedDownCrypto?.parseBigDecimal(0).orEmpty()

        val errorText = when {
            amountState.amountTextField.isError -> amountState.amountTextField.error
            isRequirementError -> resourceReference(
                R.string.staking_amount_requirement_error,
                wrappedList(
                    BigDecimalFormatter.formatCryptoAmount(
                        amountRequirements.minimum,
                        cryptoCurrencyStatus.currency.symbol,
                        cryptoCurrencyStatus.currency.decimals,
                    ),
                ),
            )
            isIntegerOnlyError -> resourceReference(
                R.string.staking_amount_tron_integer_error,
                wrappedList(value),
            )
            else -> TextReference.EMPTY
        }
        val isError = amountState.amountTextField.isError || isRequirementError
        return if (!errorText.isNullOrEmpty()) {
            amountState.copy(
                isPrimaryButtonEnabled = !isError,
                amountTextField = amountState.amountTextField.copy(
                    isError = isError,
                    isWarning = isIntegerOnlyError,
                    error = errorText,
                    keyboardOptions = amountState.amountTextField.keyboardOptions.copy(
                        imeAction = ImeAction.None,
                    ),
                ),
            )
        } else {
            amountState
        }
    }

    private fun isRequirementError(prevState: AmountState.Data, amountRequirements: AddressArgument): Boolean {
        val amountDecimal = prevState.amountTextField.cryptoAmount.value ?: return false

        val isAlreadyErrorState = prevState.amountTextField.isError
        val isAmountRequired = amountRequirements.required
        val isAmountZero = amountDecimal.isZero()
        val isExceedsRequirements =
            amountRequirements.maximum?.compareTo(amountDecimal) == -1 ||
                amountRequirements.minimum?.compareTo(amountDecimal) == 1

        return !isAmountZero && isAmountRequired && isExceedsRequirements && !isAlreadyErrorState
    }

    private fun isIntegerOnlyError(amountState: AmountState.Data, actionType: StakingActionCommonType): Boolean {
        val cryptoAmountValue = amountState.amountTextField.cryptoAmount.value ?: return false

        val isEnter = actionType == StakingActionCommonType.Enter
        val isTron = isTron(cryptoCurrencyStatus.currency.network.id.value)

        val isIntegerOnly = cryptoAmountValue.isZero() || cryptoAmountValue.remainder(BigDecimal.ONE).isZero()

        return isEnter && isTron && !isIntegerOnly
    }

    data class Data(
        val amountState: AmountState,
        val actionType: StakingActionCommonType,
    )
}
