package com.tangem.data.txhistory.repository

import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.store.datasource.jointaccount.JointAccountApi
import com.tangem.store.datasource.jointaccount.models.JointOperationHistoryDeltaResponse
import com.tangem.store.datasource.jointaccount.models.JointOperationHistoryResponse
import com.tangem.store.datasource.txhistory.db.dao.HistorySyncStateDao
import com.tangem.store.datasource.txhistory.db.dao.JointOperationDao
import com.tangem.store.datasource.txhistory.db.entity.sync.HistorySyncStateEntity
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class JointOperationHistorySyncer @Inject constructor(
    private val jointAccountApi: JointAccountApi,
    private val jointOperationDao: JointOperationDao,
    private val historySyncStateDao: HistorySyncStateDao,
) {

    suspend fun fetchHistory(userWalletId: UserWalletId, limit: Int = DEFAULT_LIMIT): JointOperationHistoryResponse {
        val state = syncState(userWalletId)

        val response = jointAccountApi.getJointOperationHistory(
            walletId = userWalletId.stringValue,
            afterCursor = state?.afterCursor,
            limit = limit,
        ).getOrResetSyncState(userWalletId)

        jointOperationDao.upsertFinished(userWalletId, response.items)
        persistHistoryState(userWalletId = userWalletId, previous = state, pagination = response.pagination)
        return response
    }

    suspend fun fetchHistoryDelta(
        userWalletId: UserWalletId,
        limit: Int = DEFAULT_LIMIT,
    ): JointOperationHistoryDeltaResponse? {
        val state = syncState(userWalletId) ?: return null

        val apiResponse = jointAccountApi.getJointOperationHistoryDelta(
            walletId = userWalletId.stringValue,
            beforeCursor = state.deltaCursor,
            limit = limit,
        )
        apiResponse.refusedCursorCause()?.let { cause ->
            resetSyncState(userWalletId, cause)
            return null
        }
        val response = apiResponse.getOrThrow()

        jointOperationDao.upsertFinished(userWalletId, response.items)
        response.pagination.startCursor?.let { cursor ->
            historySyncStateDao.updateDeltaCursor(
                type = SYNC_TYPE.name,
                userWalletId = userWalletId.stringValue,
                deltaCursor = cursor,
            )
        }
        return response
    }

    suspend fun syncState(userWalletId: UserWalletId): HistorySyncStateEntity? {
        return historySyncStateDao.observe(type = SYNC_TYPE.name, userWalletId = userWalletId.stringValue).first()
    }

    /**
     * The backend answers a cursor it no longer accepts with 400. Dropping the sync state makes the next sync walk
     * the history from the start instead of failing on that cursor forever.
     */
    private suspend fun <T : Any> ApiResponse<T>.getOrResetSyncState(userWalletId: UserWalletId): T {
        refusedCursorCause()?.let { cause -> resetSyncState(userWalletId, cause) }
        return getOrThrow()
    }

    private fun ApiResponse<*>.refusedCursorCause(): ApiResponseError? {
        val cause = (this as? ApiResponse.Error)?.cause
        val isBadRequest = (cause as? ApiResponseError.HttpException)?.code ==
            ApiResponseError.HttpException.Code.BAD_REQUEST
        return cause.takeIf { isBadRequest }
    }

    private suspend fun resetSyncState(userWalletId: UserWalletId, cause: ApiResponseError) {
        TangemLogger.e("Joint operation history refused the cursor, the sync starts over", cause)
        historySyncStateDao.delete(type = SYNC_TYPE.name, userWalletId = userWalletId.stringValue)
    }

    private suspend fun persistHistoryState(
        userWalletId: UserWalletId,
        previous: HistorySyncStateEntity?,
        pagination: JointOperationHistoryResponse.Pagination,
    ) {
        if (previous == null) {
            historySyncStateDao.upsert(
                HistorySyncStateEntity(
                    type = SYNC_TYPE.name,
                    userWalletId = userWalletId.stringValue,
                    isInitialCompleted = !pagination.hasMore,
                    afterCursor = pagination.endCursor,
                    deltaCursor = pagination.startDeltaCursor,
                ),
            )
        } else {
            historySyncStateDao.updateHistoryCursor(
                type = SYNC_TYPE.name,
                userWalletId = userWalletId.stringValue,
                afterCursor = pagination.endCursor,
                isInitialCompleted = !pagination.hasMore,
            )
        }
    }

    private companion object {
        const val DEFAULT_LIMIT = 100
        val SYNC_TYPE = HistorySyncStateEntity.Type.JOINT_OPERATION
    }
}