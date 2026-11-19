package com.tangem.features.earn.flow.impl.model

import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.common.ui.amountScreen.converters.AmountCurrencyTransformer
import com.tangem.common.ui.amountScreen.converters.AmountPastedTriggerDismissTransformer
import com.tangem.common.ui.amountScreen.converters.AmountStateConverter
import com.tangem.common.ui.amountScreen.converters.field.AmountFieldChangeTransformer
import com.tangem.common.ui.amountScreen.converters.field.AmountFieldSetMaxAmountTransformer
import com.tangem.common.ui.amountScreen.models.AmountParameters
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * The amount card of the flow on the shared transformers of the Send and Staking amount screens. The input boundary is
 * [EarnFlowState.availableMax]: the session's `maxAmount` less the fee paid from the same balance, so Max, the percent
 * presets and the field limit leave room for the fee. Every function returns `null` while the card has nothing to
 * change.
 */
internal class EarnAmountInput(private val clickIntents: AmountScreenClickIntents) {

    private val iconStateConverter by lazy(::CryptoCurrencyToIconStateConverter)

    fun initial(state: EarnFlowState): AmountState? {
        if (state.amountState !is AmountState.Empty) return null
        val status = state.status?.takeIf { it.value !is CryptoCurrencyStatus.Loading } ?: return null
        val appCurrency = state.appCurrency ?: return null
        val max = state.availableMax ?: return null

        return AmountStateConverter(
            clickIntents = clickIntents,
            appCurrency = appCurrency,
            cryptoCurrencyStatus = status,
            maxEnterAmount = EnterAmountBoundary(amount = max, fiatRate = status.value.fiatRate),
            iconStateConverter = iconStateConverter,
            isBalanceHidden = false,
            accountTitleUM = state.source ?: AccountTitleUM.Text(TextReference.EMPTY),
        ).convert(AmountParameters(title = TextReference.EMPTY, value = ""))
    }

    fun change(state: EarnFlowState, value: String): AmountState? {
        val status = state.status ?: return null
        return AmountFieldChangeTransformer(
            cryptoCurrencyStatus = status,
            maxEnterAmount = state.maxBoundary() ?: return null,
            minimumTransactionAmount = state.minBoundary(),
            value = value,
        ).transform(state.amountState)
    }

    fun max(state: EarnFlowState): AmountState? = set(state, state.maxBoundary())

    /** Revalidates the entered value, e.g. after the session reported a minimum. */
    fun revalidate(state: EarnFlowState): AmountState? {
        val data = state.amountState as? AmountState.Data ?: return null
        val field = data.amountTextField
        return change(state, if (field.isFiatValue) field.fiatValue else field.value)
    }

    /**
     * 100% is the Max: cut to the currency decimals it would fall short of a position and leave dust behind. A share
     * that rounds to zero clears the field, as the shared transformer skips a zero and would keep the previous amount.
     */
    fun percent(state: EarnFlowState, percent: Int): AmountState? {
        if (percent == FULL_SHARE_PERCENT) return max(state)
        val status = state.status ?: return null
        val share = state.availableMax
            ?.multiply(BigDecimal(percent))
            ?.divide(ONE_HUNDRED)
            ?.setScale(status.currency.decimals, RoundingMode.DOWN)
            ?: return null
        if (share.signum() == 0) return change(state, "")
        return set(state, EnterAmountBoundary(amount = share, fiatRate = status.value.fiatRate))
    }

    fun currency(state: EarnFlowState, isFiat: Boolean): AmountState? {
        val status = state.status ?: return null
        return AmountCurrencyTransformer(status, isFiat).transform(state.amountState)
    }

    fun dismissPasteTrigger(state: EarnFlowState): AmountState =
        AmountPastedTriggerDismissTransformer.transform(state.amountState)

    private fun set(state: EarnFlowState, boundary: EnterAmountBoundary?): AmountState? {
        val status = state.status ?: return null
        return AmountFieldSetMaxAmountTransformer(
            cryptoCurrencyStatus = status,
            maxAmount = boundary ?: return null,
            minAmount = state.minBoundary(),
        ).transform(state.amountState)
    }

    /**
     * Applies a fee estimate to the amount: a Max or percent tap waiting for it is set once the card is there too; an
     * amount that fitted with the previous fee but not with this one is capped to the new Max; an amount above the new
     * Max, or one already in error, is checked against it. Any other amount is left as is: rechecking a fiat input
     * re-reads its rounded text and can move the crypto amount past the limit.
     *
     * The shared transformers skip a zero Max, so with nothing left after the fee an amount from before is checked
     * against it and shows the error instead.
     */
    fun withFeeDeduction(state: EarnFlowState, deduction: BigDecimal): EarnFlowState {
        val updated = state.copy(feeDeduction = deduction)
        if (updated.pendingSharePercent != null) return withPendingShare(updated)
        val newMax = updated.availableMax ?: return updated
        val entered = state.enteredAmount
        val amountState = when {
            newMax.signum() == 0 -> entered?.let { revalidate(updated) }
            isNoLongerAffordable(entered, previousMax = state.availableMax, newMax = newMax) -> max(updated)
            needsCheck(state, entered, newMax) -> revalidate(updated)
            else -> null
        }
        return amountState?.let { updated.copy(amountState = it) } ?: updated
    }

    /** A Max or percent tap can be set once the fee, the Max and the card are all there */
    fun isShareReady(state: EarnFlowState): Boolean =
        state.feeDeduction != null && state.availableMax != null && state.amountState is AmountState.Data

    /** Sets a waiting Max or percent tap once it can be set; until then it keeps waiting */
    private fun withPendingShare(state: EarnFlowState): EarnFlowState {
        val pending = state.pendingSharePercent ?: return state
        if (!isShareReady(state)) return state
        return state.copy(pendingSharePercent = null, amountState = percent(state, pending) ?: state.amountState)
    }

    private fun isNoLongerAffordable(entered: BigDecimal?, previousMax: BigDecimal?, newMax: BigDecimal): Boolean =
        entered != null && previousMax != null && entered <= previousMax && entered > newMax

    private fun needsCheck(state: EarnFlowState, entered: BigDecimal?, newMax: BigDecimal): Boolean =
        entered != null && (entered > newMax || state.isAmountInvalid)

    fun withInitial(state: EarnFlowState): EarnFlowState =
        initial(state)?.let { withPendingShare(state.copy(amountState = it)) } ?: state

    /** The minimum joins the field validation from now on, so the field shows the error right away. */
    fun withMinimum(state: EarnFlowState, min: BigDecimal): EarnFlowState {
        val withMinimum = state.copy(minAmount = min)
        return withMinimum.copy(amountState = revalidate(withMinimum) ?: state.amountState)
    }

    private fun EarnFlowState.minBoundary(): EnterAmountBoundary? =
        minAmount?.let { EnterAmountBoundary(amount = it, fiatAmount = null, fiatRate = null) }

    private fun EarnFlowState.maxBoundary(): EnterAmountBoundary? =
        availableMax?.let { EnterAmountBoundary(amount = it, fiatRate = status?.value?.fiatRate) }

    companion object {
        private val ONE_HUNDRED = BigDecimal(100)
        const val FULL_SHARE_PERCENT = 100
    }
}