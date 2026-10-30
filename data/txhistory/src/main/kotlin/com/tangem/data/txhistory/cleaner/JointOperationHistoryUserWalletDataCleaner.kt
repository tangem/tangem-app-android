package com.tangem.data.txhistory.cleaner

import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.store.datasource.txhistory.db.dao.HistorySyncStateDao
import com.tangem.store.datasource.txhistory.db.dao.JointOperationDao
import com.tangem.store.datasource.txhistory.db.entity.sync.HistorySyncStateEntity
import javax.inject.Inject

internal class JointOperationHistoryUserWalletDataCleaner @Inject constructor(
    private val jointOperationDao: JointOperationDao,
    private val historySyncStateDao: HistorySyncStateDao,
) : UserWalletDataCleaner {

    override suspend fun clear(userWalletIds: List<UserWalletId>) {
        val ids = userWalletIds.map(UserWalletId::stringValue)
        jointOperationDao.delete(ids)
        historySyncStateDao.delete(type = HistorySyncStateEntity.Type.JOINT_OPERATION.name, userWalletIds = ids)
    }
}