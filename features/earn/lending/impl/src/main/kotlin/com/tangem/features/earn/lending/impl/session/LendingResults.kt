package com.tangem.features.earn.lending.impl.session

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fee
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.earn.product.api.analytics.EarnTxAnalytics
import com.tangem.features.earn.product.api.product.EarnRow
import com.tangem.features.earn.product.api.product.EarnValue
import com.tangem.features.earn.product.api.session.EarnFlowAction
import com.tangem.features.earn.product.api.session.EarnFlowExit
import com.tangem.features.earn.product.api.session.EarnResult
import com.tangem.features.earn.product.api.session.EarnResultAsset
import com.tangem.features.earn.product.api.session.EarnResultButton
import java.math.BigDecimal

internal fun lendingTxAnalytics(action: EarnFlowAction): EarnTxAnalytics =
    EarnTxAnalytics(params = mapOf("action" to action.id))

// Temporary plain text in this file: replace with localized string resources.
internal fun lendingDepositResult(amount: BigDecimal, currency: CryptoCurrency, provider: TextReference) = EarnResult(
    title = stringReference("Deposited ${amount.format { crypto(currency) }}"),
    subtitle = combinedReference(stringReference("Your ${currency.symbol} is now earning in "), provider),
    chip = null,
    rows = emptyList(),
    primary = EarnResultButton(stringReference("Go to yield"), EarnFlowExit.ToYield),
    secondary = EarnResultButton(resourceReference(R.string.common_go_to_token), EarnFlowExit.ToToken),
)

internal fun lendingWithdrawResult(amount: BigDecimal, currency: CryptoCurrency, fee: Fee) = EarnResult(
    title = stringReference("Withdrawn ${amount.format { crypto(currency) }}"),
    subtitle = null,
    chip = null,
    rows = listOf(
        EarnRow(
            label = stringReference("Withdrawal period"),
            value = EarnValue.Text(stringReference("Usually instant")),
        ),
        EarnRow(
            label = resourceReference(R.string.common_network_fee_title),
            value = EarnValue.Text(stringReference(fee.amount.value.format { crypto(currency).fee() })),
        ),
    ),
    primary = EarnResultButton(resourceReference(R.string.common_done), EarnFlowExit.Done),
    secondary = null,
    asset = EarnResultAsset.Status(TangemStatusIcon.Kind.Checkmark),
    isExplorerShown = true,
)