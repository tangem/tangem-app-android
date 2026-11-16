package com.tangem.features.earn.product.api.session

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.earn.product.api.analytics.EarnTxAnalytics
import com.tangem.features.earn.product.api.product.EarnRow
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.math.BigDecimal

data class EarnResult(
    val title: TextReference,
    val subtitle: TextReference?,
    val chip: TextReference?,
    val rows: List<EarnRow>,
    val primary: EarnResultButton?,
    val secondary: EarnResultButton?,
)

data class EarnResultButton(val text: TextReference, val exit: EarnFlowExit)

enum class EarnFlowExit { Done, ToYield, ToToken }

sealed interface EarnSessionError {

    data class Fee(val error: GetFeeError) : EarnSessionError

    data class BelowMinimum(val min: BigDecimal) : EarnSessionError

    data class ExceedsLimit(val max: BigDecimal) : EarnSessionError

    data object InsufficientBalance : EarnSessionError

    data class Rejected(val reason: TextReference) : EarnSessionError

    data class Failed(val cause: Throwable? = null) : EarnSessionError
}

/**
 * Persisted reference to a sent operation, [EarnSession.track] follows it. [version] guards the stored format.
 * There is no address on purpose: the pending state comes from the network status.
 */
@Serializable
data class EarnTxRef(
    val version: Int,
    val productId: EarnProductId,
    val id: String,
    val userWalletId: UserWalletId,
    val currencyId: CryptoCurrency.ID,
    val createdAt: Instant,
    val analytics: EarnTxAnalytics,
)