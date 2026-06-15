package com.tangem.datasource.local.txhistory.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tangem.datasource.local.txhistory.db.dao.ExpressHistoryDao
import com.tangem.datasource.local.txhistory.db.dao.ExpressSyncStateDao
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressSyncStateEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressOnrampEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressProviderEntity

@Database(
    version = 1,
    entities = [
        ExpressProviderEntity::class,
        ExpressExchangeEntity::class,
        ExpressOnrampEntity::class,
        ExpressSyncStateEntity::class,
    ],
)
abstract class TxHistoryDatabase : RoomDatabase() {

    abstract fun expressHistoryDao(): ExpressHistoryDao

    abstract fun syncStateDao(): ExpressSyncStateDao
}