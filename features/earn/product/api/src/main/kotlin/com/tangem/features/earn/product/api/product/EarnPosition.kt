package com.tangem.features.earn.product.api.product

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.network.TxInfo
import com.tangem.features.earn.product.api.session.EarnAction
import kotlinx.datetime.Instant
import java.math.BigDecimal

sealed interface EarnPositionState {

    /** The product answered: the user has no position. */
    data object None : EarnPositionState

    data class Known(val position: EarnPosition) : EarnPositionState

    /** The position was not read. "Start earning" must not be offered over a possible balance. */
    data object Unknown : EarnPositionState
}

/** @property cryptoAmount without pending withdrawals */
data class EarnPosition(
    val cryptoAmount: BigDecimal,
    val earned: BigDecimal?,
    val description: TextReference?,
    val rows: List<EarnRow>,
    val rewards: EarnRewards?,
    val freshness: EarnFreshness?,
)

/** @property claimAction a claim without an amount: the action input is `None` */
data class EarnRewards(
    val amount: BigDecimal,
    val claimAction: EarnAction,
)

data class EarnFreshness(val asOf: Instant, val isStale: Boolean)

/** @property until when the block lifts, for a time-based block */
data class EarnBlock(
    val reason: TextReference,
    val action: EarnBlockAction? = null,
    val until: Instant? = null,
)

enum class EarnBlockAction { Retry, UpdateApp }

/** An outgoing transaction without confirmation is in flight for the currency. */
fun NetworkStatus.Value.hasOutgoingPendingTransaction(currencyId: CryptoCurrency.ID): Boolean =
    (this as? NetworkStatus.Verified)?.pendingTransactions?.get(currencyId).orEmpty().any(TxInfo::isOutgoing)