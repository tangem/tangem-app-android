package com.tangem.datasource.local.txhistory.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapColumn
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressOnrampEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressProviderEntity
import com.tangem.datasource.local.txhistory.db.entity.express.OnrampCountryEntity
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
    suspend fun upsertCountries(items: List<OnrampCountryEntity>)

    /**
     * All persisted providers keyed by [ExpressProviderEntity.id]
     */
    @Query("SELECT * FROM express_provider")
    fun getProvidersById(): Flow<Map<@MapColumn(columnName = "id") String, ExpressProviderEntity>>

    /** All persisted onramp countries keyed by [OnrampCountryEntity.code]. */
    @Query("SELECT * FROM onramp_country")
    fun getCountriesByCode(): Flow<Map<@MapColumn(columnName = "code") String, OnrampCountryEntity>>

    /**
     * Outgoing swaps: the viewed currency is the swap's `from` side, so the row is looked up by its `from_address`.
     * Join to on-chain by `payin_hash`.
     *

     * loading the whole table; [activeStatuses] keeps in-progress deals visible even outside the window.
     */
    @Query(
        """
        SELECT * FROM express_exchange
        WHERE from_address = :fromAddress
          AND from_network = :network
          AND from_contract_address = :contract
          AND (created_at >= :fromCreatedAtIso OR status IN (:activeStatuses))
        ORDER BY created_at DESC
        """,
    )
    fun observeOutgoingSwaps(
        fromAddress: String,
        network: String,
        contract: String,
        fromCreatedAtIso: String,
        activeStatuses: List<String>,
    ): Flow<List<ExpressExchangeEntity>>

    /**
     * Incoming swaps: the viewed currency is the swap's `to` side, so the row is looked up by its `payout_address`
     * (where the target assets landed = this currency's address). Join to on-chain by `payout_hash`.
     */
    @Query(
        """
        SELECT * FROM express_exchange
        WHERE payout_address = :payoutAddress
          AND to_network = :network
          AND to_contract_address = :contract
          AND (created_at >= :fromCreatedAtIso OR status IN (:activeStatuses))
        ORDER BY created_at DESC
        """,
    )
    fun observeIncomingSwaps(
        payoutAddress: String,
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
        WHERE payout_address = :payoutAddress
          AND to_network = :network
          AND to_contract_address = :contract
          AND (created_at >= :fromCreatedAtIso OR status IN (:activeStatuses))
        ORDER BY created_at DESC
        """,
    )
    fun observeIncomingOnramps(
        payoutAddress: String,
        network: String,
        contract: String,
        fromCreatedAtIso: String,
        activeStatuses: List<String>,
    ): Flow<List<ExpressOnrampEntity>>
}