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
 * the session's `maxAmount`: the fee is not part of it, the flow deducts it on submit. Every function returns `null`
 * while the card has nothing to change.
 */
internal class EarnAmountInput(
    private val clickIntents: AmountScreenClickIntents,
    private val accountTitle: AccountTitleUM,
) {

    private val iconStateConverter by lazy(::CryptoCurrencyToIconStateConverter)

    fun initial(state: EarnFlowState): AmountState? {
        if (state.amountState !is AmountState.Empty) return null
        val status = state.status?.takeIf { it.value !is CryptoCurrencyStatus.Loading } ?: return null
        val appCurrency = state.appCurrency ?: return null
        val max = state.maxAmount ?: return null

        return AmountStateConverter(
            clickIntents = clickIntents,
            appCurrency = appCurrency,
            cryptoCurrencyStatus = status,
            maxEnterAmount = EnterAmountBoundary(amount = max, fiatRate = status.value.fiatRate),
            iconStateConverter = iconStateConverter,
            isBalanceHidden = false,
            accountTitleUM = accountTitle,
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
        return change(state, data.amountTextField.value)
    }

    fun percent(state: EarnFlowState, percent: Int): AmountState? {
        val status = state.status ?: return null
        val share = state.maxAmount
            ?.multiply(BigDecimal(percent))
            ?.divide(ONE_HUNDRED)
            ?.setScale(status.currency.decimals, RoundingMode.DOWN)
            ?: return null
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

    fun withInitial(state: EarnFlowState): EarnFlowState = initial(state)?.let { state.copy(amountState = it) } ?: state

    /** The minimum joins the field validation from now on, so the field shows the error right away. */
    fun withMinimum(state: EarnFlowState, min: BigDecimal): EarnFlowState {
        val withMinimum = state.copy(minAmount = min)
        return withMinimum.copy(amountState = revalidate(withMinimum) ?: state.amountState)
    }

    private fun EarnFlowState.minBoundary(): EnterAmountBoundary? =
        minAmount?.let { EnterAmountBoundary(amount = it, fiatAmount = null, fiatRate = null) }

    private fun EarnFlowState.maxBoundary(): EnterAmountBoundary? =
        maxAmount?.let { EnterAmountBoundary(amount = it, fiatRate = status?.value?.fiatRate) }

    private companion object {
        val ONE_HUNDRED = BigDecimal(100)
    }
}