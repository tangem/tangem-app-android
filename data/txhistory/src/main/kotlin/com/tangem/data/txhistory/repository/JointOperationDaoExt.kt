package com.tangem.data.txhistory.repository

import com.tangem.data.txhistory.repository.converter.FinishedJointOperationConverter
import com.tangem.data.txhistory.repository.converter.JointOperationEntityConverter
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.store.datasource.jointaccount.models.JointOperationHistoryItemDto
import com.tangem.store.datasource.txhistory.db.dao.JointOperationDao

private val entityConverter = JointOperationEntityConverter()

internal suspend fun JointOperationDao.upsertFinished(
    userWalletId: UserWalletId,
    items: List<JointOperationHistoryItemDto>,
) {
    val finished = items.filter { it.status in FinishedJointOperationConverter.FINISHED_STATUSES }
    if (finished.isEmpty()) return
    val entities = finished.map { item ->
        entityConverter.convert(JointOperationEntityConverter.Input(userWalletId = userWalletId, dto = item))
    }
    upsert(entities)
}