package com.tangem.features.earn.flow.impl.model

import androidx.annotation.DrawableRes
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.earn.product.api.session.EarnFeeSelection
import com.tangem.features.earn.product.api.session.EarnPlan
import com.tangem.features.earn.product.api.session.EarnResult
import com.tangem.features.earn.product.api.session.EarnTxStatus
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.api.subcomponents.feeSelector.utils.FeeCalculationUtils
import java.math.BigDecimal

internal enum class EarnFlowStep { Amount, Confirm, Result }

/**
 * [amountState] holds the amount the user entered, before the fee is deducted; [plan] carries the amount that goes
 * into the protocol. [planFee] is the fee [plan] was prepared with: a plan built for another fee is never verified.
 * [minAmount] comes from the session's `BelowMinimum` and joins the amount field validation from then on.
 * [sentAmount] keeps the plan's amount for the result: the plan itself is dropped once the transaction is sent.
 */
internal data class EarnFlowState(
    val step: EarnFlowStep,
    val status: CryptoCurrencyStatus? = null,
    val feeStatus: CryptoCurrencyStatus? = null,
    val appCurrency: AppCurrency? = null,
    val maxAmount: BigDecimal? = null,
    val minAmount: BigDecimal? = null,
    val amountState: AmountState = AmountState.Empty,
    val fee: FeeSelectorUM = FeeSelectorUM.Loading,
    val isAmountSubtractAvailable: Boolean = false,
    val plan: EarnPlan? = null,
    val planFee: EarnFeeSelection? = null,
    val isPreparing: Boolean = false,
    val signing: EarnSigningState = EarnSigningState.Idle,
    val result: EarnResult? = null,
    val txStatus: EarnTxStatus? = null,
    val explorerUrl: String? = null,
    val resultTimestamp: Long? = null,
    val sentAmount: BigDecimal? = null,
    @DrawableRes val walletIcon: Int? = null,
    val source: AccountTitleUM? = null,
) {

    val feeSelection: EarnFeeSelection?
        get() = (fee as? FeeSelectorUM.Content)
            ?.takeIf { it.isPrimaryButtonEnabled }
            ?.let { EarnFeeSelection(fee = it.selectedFeeItem.fee, extended = it.feeExtraInfo.transactionFeeExtended) }

    val isPlanCurrent: Boolean
        get() = plan != null && planFee != null && planFee == feeSelection

    val enteredAmount: BigDecimal?
        get() = (amountState as? AmountState.Data)
            ?.amountTextField
            ?.cryptoAmount
            ?.value
            ?.takeIf { it.signum() > 0 }

    /** [enteredAmount] reduced by the fee when both do not fit into the balance, as Send and Staking do it. */
    val sendingAmount: BigDecimal?
        get() {
            val entered = enteredAmount ?: return null
            val status = status ?: return entered
            return FeeCalculationUtils.checkAndCalculateSubtractedAmount(
                isAmountSubtractAvailable = isAmountSubtractAvailable,
                cryptoCurrencyStatus = status,
                amountValue = entered,
                feeValue = feeSelection?.fee?.amount?.value,
                reduceAmountBy = BigDecimal.ZERO,
            )
        }
}

internal fun EarnFlowState.stack(hasAmountStep: Boolean): List<EarnFlowStep> =
    if (step == EarnFlowStep.Confirm && hasAmountStep) listOf(EarnFlowStep.Amount, step) else listOf(step)

internal data class EarnSigningState(val stepLabel: TextReference?, val isExecuting: Boolean) {

    companion object {
        val Idle = EarnSigningState(stepLabel = null, isExecuting = false)
    }
}