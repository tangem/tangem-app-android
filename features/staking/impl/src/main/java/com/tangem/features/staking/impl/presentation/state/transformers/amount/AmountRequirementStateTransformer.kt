package com.tangem.features.staking.impl.presentation.state.transformers.amount

import androidx.annotation.StringRes
import androidx.compose.ui.text.input.ImeAction
import com.tangem.common.extensions.isZero
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
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
        return if (prevState is AmountState.Data) {
            updateWithError(
                prevState,
                actionType,
            )
        } else {
            prevState
        }
    }

    private fun updateWithError(amountState: AmountState.Data, actionType: StakingActionCommonType): AmountState {
        val requirementError = getRequirementError(amountState)
        val isIntegerOnlyError = isIntegerOnlyError(amountState, actionType)

        val cryptoAmount = amountState.amountTextField.cryptoAmount
        val roundedDownCrypto = cryptoAmount.value?.setScale(0, RoundingMode.DOWN)
        val value = roundedDownCrypto?.parseBigDecimal(0).orEmpty()

        val errorText = when {
            amountState.amountTextField.isError -> amountState.amountTextField.error
            requirementError != null -> requirementError
            isIntegerOnlyError -> when (actionType) {
                StakingActionCommonType.Enter -> resourceReference(
                    R.string.staking_amount_tron_integer_error,
                    wrappedList(value),
                )
                StakingActionCommonType.Exit -> resourceReference(
                    R.string.staking_amount_tron_integer_error_unstaking,
                    wrappedList(value),
                )
                else -> null
            }
            else -> null
        }
        val isError = amountState.amountTextField.isError || requirementError != null
        return amountState.copy(
            isPrimaryButtonEnabled = !isError,
            amountTextField = amountState.amountTextField.copy(
                isError = isError,
                isWarning = isIntegerOnlyError,
                error = errorText ?: amountState.amountTextField.error,
                keyboardOptions = amountState.amountTextField.keyboardOptions.copy(
                    imeAction = ImeAction.None,
                ),
            ),
        )
    }

    private fun getRequirementError(prevState: AmountState.Data): TextReference? {
        val amountDecimal = prevState.amountTextField.cryptoAmount.value ?: return null

        val isAlreadyErrorState = prevState.amountTextField.isError
        val isAmountZero = amountDecimal.isZero()

        if (isAlreadyErrorState || isAmountZero) return null

        return when (actionType) {
            StakingActionCommonType.Enter -> {
                val enterRequirements = yield.args.enter.args[Yield.Args.ArgType.AMOUNT]
                enterRequirements?.getError(amountDecimal, R.string.staking_amount_requirement_error)
            }
            StakingActionCommonType.Exit -> {
                val exitRequirements = yield.args.exit?.args?.get(Yield.Args.ArgType.AMOUNT)
                exitRequirements?.getError(amountDecimal, R.string.staking_unstake_amount_requirement_error)
            }
            else -> null
        }
    }

    private fun isIntegerOnlyError(amountState: AmountState.Data, actionType: StakingActionCommonType): Boolean {
        val cryptoAmountValue = amountState.amountTextField.cryptoAmount.value ?: return false

        val isEnterOrExit = actionType == StakingActionCommonType.Enter || actionType == StakingActionCommonType.Exit
        val isTron = isTron(cryptoCurrencyStatus.currency.network.id.value)

        val isIntegerOnly = cryptoAmountValue.isZero() || cryptoAmountValue.remainder(BigDecimal.ONE).isZero()

        return isEnterOrExit && isTron && !isIntegerOnly
    }

    private fun AddressArgument.getError(amount: BigDecimal, @StringRes errorTextRes: Int): TextReference? {
        val isExceedsRequirements = maximum?.compareTo(amount) == -1 ||
            minimum?.compareTo(amount) == 1

        return resourceReference(
            errorTextRes,
            wrappedList(
                minimum.format {
                    crypto(cryptoCurrencyStatus.currency)
                },
            ),
        ).takeIf { required && isExceedsRequirements }
    }

    data class Data(
        val amountState: AmountState,
        val actionType: StakingActionCommonType,
    )
}
