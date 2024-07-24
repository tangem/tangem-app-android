package com.tangem.features.staking.impl.presentation.state.transformers.amount

import com.tangem.common.extensions.isZero
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.R
import com.tangem.utils.transformer.Transformer

internal class AmountRequirementStateTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val yield: Yield,
    private val value: String,
) : Transformer<AmountState> {
    override fun transform(prevState: AmountState): AmountState {
        val amountRequirements = yield.args.enter.args[Yield.Args.ArgType.AMOUNT]
        val amountDecimal = value.parseToBigDecimal(cryptoCurrencyStatus.currency.decimals)

        return if (prevState !is AmountState.Data || amountRequirements == null) {
            prevState
        } else {
            val isAlreadyErrorState = prevState.amountTextField.isError
            val isAmountRequired = amountRequirements.required
            val isAmountZero = amountDecimal.isZero()
            val isExceedsRequirements =
                amountRequirements.maximum?.compareTo(amountDecimal) == -1 ||
                    amountRequirements.minimum?.compareTo(amountDecimal) == 1

            val isRequirementError = !isAmountZero && isAmountRequired && isExceedsRequirements && !isAlreadyErrorState
            if (isRequirementError) {
                prevState.copy(
                    amountTextField = prevState.amountTextField.copy(
                        isError = true,
                        error = resourceReference(
                            R.string.staking_amount_requirement_error,
                            wrappedList(
                                BigDecimalFormatter.formatCryptoAmount(
                                    amountRequirements.minimum,
                                    cryptoCurrencyStatus.currency.symbol,
                                    cryptoCurrencyStatus.currency.decimals,
                                ),
                            ),
                        ),
                    ),
                )
            } else {
                prevState
            }
        }
    }
}
