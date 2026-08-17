package com.tangem.datasource.local.txhistory.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapColumn
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressOnrampEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressProviderEntity
import com.tangem.datasource.local.txhistory.db.entity.express.OnrampCurrencyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpressHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProviders(items: List<ExpressProviderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExchanges(items: List<ExpressExchangeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOnramps(items: List<ExpressOnrampEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCurrencies(items: List<OnrampCurrencyEntity>)

    /**
     * All persisted providers keyed by [ExpressProviderEntity.id]
     */
    @Query("SELECT * FROM express_provider")
    fun getProvidersById(): Flow<Map<@MapColumn(columnName = "id") String, ExpressProviderEntity>>

    /** All persisted onramp fiat currencies keyed by [OnrampCurrencyEntity.code]. */
    @Query("SELECT * FROM onramp_currency")
    fun getCurrenciesByCode(): Flow<Map<@MapColumn(columnName = "code") String, OnrampCurrencyEntity>>

    /**
     * Outgoing swaps: the viewed currency is the swap's `from` side, so the row is looked up by its `from_address`.
     * Join to on-chain by `payin_hash`.
     *
     * [fromAddresses] holds every address the currency is watched under — the default one plus the dynamic (UTXO)
     * addresses already used, so a swap paid from a non-base address is still found.
     *

     * loading the whole table; [activeStatuses] keeps in-progress deals visible even outside the window.
     */
    @Query(
        """
        SELECT * FROM express_exchange
        WHERE from_address IN (:fromAddresses)
          AND from_network = :network
          AND from_contract_address = :contract
          AND (created_at >= :fromCreatedAtIso OR status IN (:activeStatuses))
        ORDER BY created_at DESC
        """,
    )
    fun observeOutgoingSwaps(
        fromAddresses: List<String>,
        network: String,
        contract: String,
        fromCreatedAtIso: String,
        activeStatuses: List<String>,
    ): Flow<List<ExpressExchangeEntity>>

    /**
     * Incoming swaps: the viewed currency is the swap's `to` side, so the row is looked up by its `payout_address`
     * (where the target assets landed = one of this currency's addresses). Join to on-chain by `payout_hash`.
     */
    @Query(
        """
        SELECT * FROM express_exchange
        WHERE payout_address IN (:payoutAddresses)
          AND to_network = :network
          AND to_contract_address = :contract
          AND (created_at >= :fromCreatedAtIso OR status IN (:activeStatuses))
        ORDER BY created_at DESC
        """,
    )
    fun observeIncomingSwaps(
        payoutAddresses: List<String>,
        network: String,
        contract: String,
        fromCreatedAtIso: String,
        activeStatuses: List<String>,
    ): Flow<List<ExpressExchangeEntity>>

    /**
     * Onramp is always incoming, looked up by its `payout_address`. Join by `payout_hash`.
     */
    @Query(
        """
        SELECT * FROM express_onramp
        WHERE payout_address IN (:payoutAddresses)
          AND to_network = :network
          AND to_contract_address = :contract
          AND (created_at >= :fromCreatedAtIso OR status IN (:activeStatuses))
        ORDER BY created_at DESC
        """,
    )
    fun observeIncomingOnramps(
        payoutAddresses: List<String>,
        network: String,
        contract: String,
        fromCreatedAtIso: String,
        activeStatuses: List<String>,
    ): Flow<List<ExpressOnrampEntity>>
}