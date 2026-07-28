package com.tangem.features.txhistory.component

import com.tangem.domain.txhistory.model.TxHistoryInfo
import kotlinx.coroutines.flow.Flow

/**
 * Activation config for the transaction-details child slot owned by the host (e.g. token-details).
 *

 * with `serializer = null` — the details sheet is intentionally not restored after process death.
 */
data class TxHistoryDetailsSlotConfig(val txHistoryInfo: Flow<TxHistoryInfo>)