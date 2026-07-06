package com.tangem.features.txhistory.converter

import androidx.annotation.StringRes
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Status
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.express.models.ExpressTransactionAsset
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.AmountType
import com.tangem.features.txhistory.impl.R

// region Status helpers

/** Maps the domain [TxInfo.TransactionStatus] to the UI [Status] bucket that drives row title/icon/amount colors. */
internal fun TxInfo.TransactionStatus.toUiStatus(): Status = when (this) {
    TxInfo.TransactionStatus.Confirmed -> Status.Confirmed
    TxInfo.TransactionStatus.Failed -> Status.Failed
    TxInfo.TransactionStatus.Unconfirmed -> Status.Unconfirmed
}

/**
 * Status-aware action title: the [confirmed] label once settled, the [pending] label while in flight, and the
 * "{pending} failed" template on failure.
 */
internal fun Status.statusAwareTitle(@StringRes pending: Int, @StringRes confirmed: Int): TextReference = when (this) {
    is Status.Failed -> resourceReference(R.string.common_action_failed, wrappedList(resourceReference(pending)))
    is Status.Unconfirmed -> resourceReference(pending)
    is Status.Confirmed -> resourceReference(confirmed)
}

/** [statusAwareTitle] keyed off an on-chain [TxInfo]'s status. */
internal fun TxInfo.statusAwareTitle(@StringRes pending: Int, @StringRes confirmed: Int): TextReference =
    status.toUiStatus().statusAwareTitle(pending, confirmed)

// endregion

// region Express asset helpers

/** Ticker shown for an express leg: the resolved currency symbol, falling back to the network id while unresolved. */
internal val ExpressTransactionAsset.displaySymbol: String
    get() = cryptoCurrency?.symbol ?: id.networkId

/** Fiat currency code of an [Amount], falling back to its symbol when the amount is not a fiat type. */
internal val Amount.fiatCode: String
    get() = (type as? AmountType.FiatType)?.code ?: currencySymbol

// endregion