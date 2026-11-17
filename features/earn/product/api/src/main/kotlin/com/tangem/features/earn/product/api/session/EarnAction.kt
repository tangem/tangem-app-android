package com.tangem.features.earn.product.api.session

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.transaction.models.TransactionFeeExtended
import java.math.BigDecimal

interface EarnAction {

    val id: String

    val label: TextReference
}

interface EarnFlowAction : EarnAction {

    val input: EarnActionInput

    val isGaslessAllowed: Boolean

    /** Shown under the amount from the first screen on, before the session prepares a plan with its own notice. */
    val notice: TextReference?
        get() = null

    fun title(productTitle: TextReference): TextReference

    fun confirmTitle(productTitle: TextReference): TextReference = productTitle
}

sealed interface EarnActionInput {

    data object None : EarnActionInput

    data class Amount(val source: Source) : EarnActionInput {

        enum class Source { Wallet, Position }
    }
}

data class Deposit(override val isGaslessAllowed: Boolean = false) : EarnFlowAction {

    override val id: String = ID

    override val input: EarnActionInput = EarnActionInput.Amount(EarnActionInput.Amount.Source.Wallet)

    override val label: TextReference = resourceReference(R.string.lending_deposit_button)

    override fun title(productTitle: TextReference): TextReference =
        resourceReference(R.string.earn_deposit_title, wrappedList(productTitle))

    companion object {
        const val ID = "deposit"
    }
}

data class Withdraw(
    override val isGaslessAllowed: Boolean = false,
    override val notice: TextReference? = null,
) : EarnFlowAction {

    override val id: String = ID

    override val input: EarnActionInput = EarnActionInput.Amount(EarnActionInput.Amount.Source.Position)

    override val label: TextReference = resourceReference(R.string.lending_withdraw_button)

    override fun title(productTitle: TextReference): TextReference = resourceReference(R.string.common_withdrawal)

    override fun confirmTitle(productTitle: TextReference): TextReference =
        resourceReference(R.string.common_withdrawal)

    companion object {
        const val ID = "withdraw"
    }
}

sealed interface EarnAmount {

    data class Exact(val value: BigDecimal) : EarnAmount

    /** The whole balance of the action's source. The session does not subtract the fee: the flow does. */
    data object Entire : EarnAmount
}

data class EarnFeeSelection(
    val fee: Fee,
    val extended: TransactionFeeExtended?,
    val feeType: AnalyticsParam.FeeType = AnalyticsParam.FeeType.Normal,
)