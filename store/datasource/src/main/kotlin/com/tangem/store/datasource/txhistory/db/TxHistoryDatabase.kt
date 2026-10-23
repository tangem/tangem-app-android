package com.tangem.store.datasource.txhistory.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameTable
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import com.tangem.store.datasource.txhistory.db.dao.ExpressHistoryDao
import com.tangem.store.datasource.txhistory.db.dao.HistoryIndexDao
import com.tangem.store.datasource.txhistory.db.dao.HistorySyncStateDao
import com.tangem.store.datasource.txhistory.db.dao.JointOperationDao
import com.tangem.store.datasource.txhistory.db.dao.P2PEthPoolVaultDao
import com.tangem.store.datasource.txhistory.db.dao.StakingValidatorDao
import com.tangem.store.datasource.txhistory.db.dao.TokenInfoDao
import com.tangem.store.datasource.txhistory.db.entity.HistoryIndexEntity
import com.tangem.store.datasource.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.store.datasource.txhistory.db.entity.express.ExpressOnrampEntity
import com.tangem.store.datasource.txhistory.db.entity.express.ExpressProviderEntity
import com.tangem.store.datasource.txhistory.db.entity.express.OnrampCurrencyEntity
import com.tangem.store.datasource.txhistory.db.entity.express.TokenInfoEntity
import com.tangem.store.datasource.txhistory.db.entity.joint.JointOperationEntity
import com.tangem.store.datasource.txhistory.db.entity.joint.JointOperationVoteEntity
import com.tangem.store.datasource.txhistory.db.entity.staking.P2PEthPoolVaultEntity
import com.tangem.store.datasource.txhistory.db.entity.staking.StakingValidatorEntity
import com.tangem.store.datasource.txhistory.db.entity.sync.HistorySyncStateEntity

@Database(
    version = 3,
    entities = [
        ExpressProviderEntity::class,
        ExpressExchangeEntity::class,
        ExpressOnrampEntity::class,
        HistorySyncStateEntity::class,
        OnrampCurrencyEntity::class,
        TokenInfoEntity::class,
        HistoryIndexEntity::class,
        StakingValidatorEntity::class,
        P2PEthPoolVaultEntity::class,
        JointOperationEntity::class,
        JointOperationVoteEntity::class,
    ],
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = TxHistoryDatabase.Migration2To3::class),
    ],
)
abstract class TxHistoryDatabase : RoomDatabase() {

    abstract fun expressHistoryDao(): ExpressHistoryDao

    abstract fun syncStateDao(): HistorySyncStateDao

    abstract fun tokenInfoDao(): TokenInfoDao

    abstract fun historyIndexDao(): HistoryIndexDao

    abstract fun stakingValidatorDao(): StakingValidatorDao

    abstract fun p2pEthPoolVaultDao(): P2PEthPoolVaultDao

    abstract fun jointOperationDao(): JointOperationDao

    @RenameTable(fromTableName = "express_sync_state", toTableName = "history_sync_state")
    class Migration2To3 : AutoMigrationSpec
}