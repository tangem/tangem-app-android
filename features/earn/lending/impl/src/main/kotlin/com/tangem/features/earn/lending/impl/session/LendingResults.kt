package com.tangem.features.earn.lending.impl.session

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fee
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.earn.product.api.analytics.EarnTxAnalytics
import com.tangem.features.earn.product.api.product.EarnRow
import com.tangem.features.earn.product.api.product.EarnValue
import com.tangem.features.earn.product.api.session.EarnFlowExit
import com.tangem.features.earn.product.api.session.EarnResult
import com.tangem.features.earn.product.api.session.EarnResultAsset
import com.tangem.features.earn.product.api.session.EarnResultButton
import java.math.BigDecimal

internal fun lendingTxAnalytics(plan: LendingPlan, amountBucket: String?): EarnTxAnalytics {
    val operation = plan.operation
    val transactionType = when (operation) {
        LendingPlannedOperation.Deposit -> TYPE_DEPOSIT
        is LendingPlannedOperation.Withdraw -> TYPE_WITHDRAW
    }
    return EarnTxAnalytics(
        params = mapOf(
            TRANSACTION_TYPE_KEY to transactionType,
            WITH_UPGRADE_KEY to plan.isWithUpgrade.toString(),
        ),
        confirmedParams = buildMap {
            amountBucket?.let { put(AMOUNT_BUCKET_KEY, it) }
            if (operation is LendingPlannedOperation.Withdraw) {
                put(WITHDRAW_TYPE_KEY, if (operation.isEntire) WITHDRAW_FULL else WITHDRAW_PARTIAL)
            }
        },
    )
}

internal fun lendingDepositResult(amount: BigDecimal, currency: CryptoCurrency, accountName: TextReference) =
    EarnResult(
        title = resourceReference(R.string.lending_deposited_title, wrappedList(amount.format { crypto(currency) })),
        subtitle = resourceReference(R.string.lending_deposited_subtitle, wrappedList(currency.symbol, accountName)),
        chip = null,
        rows = emptyList(),
        primary = EarnResultButton(
            resourceReference(R.string.yield_module_claim_go_to_yield_button),
            EarnFlowExit.ToYield,
        ),
        secondary = EarnResultButton(resourceReference(R.string.common_go_to_token), EarnFlowExit.ToToken),
    )

internal fun lendingWithdrawResult(amount: BigDecimal, currency: CryptoCurrency, fee: Fee) = EarnResult(
    title = resourceReference(
        id = R.string.earn_withdrawal_result_withdrawn,
        formatArgs = wrappedList(amount.format { crypto(currency) }),
    ),
    subtitle = null,
    chip = null,
    rows = listOf(
        EarnRow(
            label = resourceReference(R.string.earn_withdrawal_result_period),
            value = EarnValue.Text(resourceReference(R.string.earn_product_details_usually_instant)),
        ),
        EarnRow(
            label = resourceReference(R.string.common_network_fee_title),
            value = EarnValue.Text(
                stringReference(
                    fee.amount.value.format { crypto(fee.amount.currencySymbol, fee.amount.decimals).fee() },
                ),
            ),
        ),
    ),
    primary = EarnResultButton(resourceReference(R.string.common_done), EarnFlowExit.Done),
    secondary = null,
    asset = EarnResultAsset.Status(TangemStatusIcon.Kind.Checkmark),
    isExplorerShown = true,
)

private const val TRANSACTION_TYPE_KEY = "Transaction Type"
private const val WITH_UPGRADE_KEY = "With Upgrade"
private const val AMOUNT_BUCKET_KEY = "Amount Bucket"
private const val WITHDRAW_TYPE_KEY = "Withdraw Type"
private const val TYPE_DEPOSIT = "Deposit"
private const val TYPE_WITHDRAW = "Withdraw"
private const val WITHDRAW_FULL = "Full"
private const val WITHDRAW_PARTIAL = "Partial"