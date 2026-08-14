package com.tangem.datasource.local.txhistory.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tangem.datasource.local.txhistory.db.dao.ExpressHistoryDao
import com.tangem.datasource.local.txhistory.db.dao.ExpressSyncStateDao
import com.tangem.datasource.local.txhistory.db.dao.HistoryIndexDao
import com.tangem.datasource.local.txhistory.db.dao.P2PEthPoolVaultDao
import com.tangem.datasource.local.txhistory.db.dao.StakingValidatorDao
import com.tangem.datasource.local.txhistory.db.dao.TokenInfoDao
import com.tangem.datasource.local.txhistory.db.entity.HistoryIndexEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressSyncStateEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressOnrampEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressProviderEntity
import com.tangem.datasource.local.txhistory.db.entity.express.OnrampCurrencyEntity
import com.tangem.datasource.local.txhistory.db.entity.express.TokenInfoEntity
import com.tangem.datasource.local.txhistory.db.entity.staking.P2PEthPoolVaultEntity
import com.tangem.datasource.local.txhistory.db.entity.staking.StakingValidatorEntity

@Database(
    version = 1,
    entities = [
        ExpressProviderEntity::class,
        ExpressExchangeEntity::class,
        ExpressOnrampEntity::class,
        ExpressSyncStateEntity::class,
        OnrampCurrencyEntity::class,
        TokenInfoEntity::class,
        HistoryIndexEntity::class,
        StakingValidatorEntity::class,
        P2PEthPoolVaultEntity::class,
    ],
)
abstract class TxHistoryDatabase : RoomDatabase() {

    abstract fun expressHistoryDao(): ExpressHistoryDao

    abstract fun syncStateDao(): ExpressSyncStateDao

    abstract fun tokenInfoDao(): TokenInfoDao

    abstract fun historyIndexDao(): HistoryIndexDao

    abstract fun stakingValidatorDao(): StakingValidatorDao

    abstract fun p2pEthPoolVaultDao(): P2PEthPoolVaultDao
}