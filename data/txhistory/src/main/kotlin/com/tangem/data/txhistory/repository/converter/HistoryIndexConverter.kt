package com.tangem.data.txhistory.repository.converter

import com.tangem.datasource.local.txhistory.db.entity.HistoryIndexEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressOnrampEntity
import org.joda.time.DateTime

/**
 * Index rows for a swap: it shows on the from-token screen (outgoing, under `from_address`) and, when the payout lands
 * on another owned token, on that screen too (incoming, under `payout_address`). The set de-duplicates the addresses so
 * a swap whose from and payout addresses coincide is indexed once.
 */
internal fun ExpressExchangeEntity.toHistoryIndexEntities(): List<HistoryIndexEntity> {
    val sortTimeMillis = DateTime.parse(createdAt).millis
    return setOf(fromAddress, payoutAddress).map { address ->
        HistoryIndexEntity(
            type = HistoryIndexEntity.Type.EXCHANGE.value,
            entityId = txId,
            address = address,
            sortTimeMillis = sortTimeMillis,
        )
    }
}

/** Onramp is always incoming: a single index row under the payout address. */
internal fun ExpressOnrampEntity.toHistoryIndexEntity(): HistoryIndexEntity = HistoryIndexEntity(
    type = HistoryIndexEntity.Type.ONRAMP.value,
    entityId = txId,
    address = payoutAddress,
    sortTimeMillis = DateTime.parse(createdAt).millis,
)