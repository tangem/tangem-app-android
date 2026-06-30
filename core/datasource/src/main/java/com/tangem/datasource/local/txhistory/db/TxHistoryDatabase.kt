package com.tangem.datasource.local.txhistory.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tangem.datasource.local.txhistory.db.dao.ExpressHistoryDao
import com.tangem.datasource.local.txhistory.db.dao.ExpressSyncStateDao
import com.tangem.datasource.local.txhistory.db.dao.TokenInfoDao
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressSyncStateEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressExchangeEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressOnrampEntity
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressProviderEntity
import com.tangem.datasource.local.txhistory.db.entity.express.OnrampCountryEntity
import com.tangem.datasource.local.txhistory.db.entity.express.TokenInfoEntity

@Database(
    version = 1,
    entities = [
        ExpressProviderEntity::class,
        ExpressExchangeEntity::class,
        ExpressOnrampEntity::class,
        ExpressSyncStateEntity::class,
        OnrampCountryEntity::class,
        TokenInfoEntity::class,
    ],
)
abstract class TxHistoryDatabase : RoomDatabase() {

    abstract fun expressHistoryDao(): ExpressHistoryDao

    abstract fun syncStateDao(): ExpressSyncStateDao

    abstract fun tokenInfoDao(): TokenInfoDao
}