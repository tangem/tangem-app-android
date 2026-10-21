package com.tangem.data.txhistory.repository

import arrow.core.Either
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ETAG_HEADER
import com.tangem.data.txhistory.repository.converter.FinishedJointOperationConverter
import com.tangem.data.txhistory.repository.converter.JointOperationEntityConverter
import com.tangem.data.txhistory.repository.converter.JointOperationRecordConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.domain.jointaccount.model.FinishedJointOperation
import com.tangem.domain.jointaccount.model.JointOperationRecord
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.repository.JointOperationHistoryRepository
import com.tangem.domain.txhistory.repository.JointOperationHistoryRepository.FetchResult
import com.tangem.store.datasource.jointaccount.JointAccountApi
import com.tangem.store.datasource.jointaccount.models.JointOperationHistoryDeltaResponse
import com.tangem.store.datasource.jointaccount.models.JointOperationHistoryItemDto
import com.tangem.store.datasource.jointaccount.models.JointOperationHistoryResponse
import com.tangem.store.datasource.txhistory.db.dao.HistorySyncStateDao
import com.tangem.store.datasource.txhistory.db.dao.JointOperationDao
import com.tangem.store.datasource.txhistory.db.entity.sync.HistorySyncStateEntity
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.ISODateTimeFormat
import javax.inject.Inject

/** Fetches the joint account operation history from the API and persists it into the local database */
internal class DefaultJointOperationHistoryRepository @Inject constructor(
    private val jointAccountApi: JointAccountApi,
    private val jointOperationDao: JointOperationDao,
    private val historySyncStateDao: HistorySyncStateDao,
) : JointOperationHistoryRepository {

    private val entityConverter = JointOperationEntityConverter()
    private val finishedConverter = FinishedJointOperationConverter()
    private val recordConverter = JointOperationRecordConverter(finishedConverter)

    suspend fun fetchHistory(userWalletId: UserWalletId, limit: Int = DEFAULT_LIMIT): JointOperationHistoryResponse {
        val state = syncState(userWalletId)

        val response = jointAccountApi.getJointOperationHistory(
            walletId = userWalletId.stringValue,
            afterCursor = state?.afterCursor,
            limit = limit,
        ).getOrResetSyncState(userWalletId)

        store(userWalletId, response.items)
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

        store(userWalletId, response.items)
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

    override fun getFinishedOperations(
        accountId: AccountId,
        chainId: Long,
        fromFinishedAtMillis: Long,
    ): Flow<List<FinishedJointOperation>> = jointOperationDao.observe(
        userWalletId = accountId.userWalletId.stringValue,
        cryptoAccountId = accountId.value,
        chainId = chainId,
        statuses = FinishedJointOperationConverter.FINISHED_STATUSES,
        fromFinishedAtIso = fromFinishedAtMillis.toIsoBound(),
    )
        .distinctUntilChanged()
        .map { entities ->
            entities.mapNotNull { entity ->
                runCatching { finishedConverter.convert(entity) }
                    .onFailure { TangemLogger.e("Dropped the joint operation ${entity.operation.operationId}", it) }
                    .getOrNull()
            }
        }

    override fun getFinishedOperationIds(userWalletId: UserWalletId): Flow<Set<String>> = jointOperationDao
        .observeIds(
            userWalletId = userWalletId.stringValue,
            statuses = FinishedJointOperationConverter.FINISHED_STATUSES,
        )
        .map { it.toSet() }
        .distinctUntilChanged()

    override fun observeOperation(userWalletId: UserWalletId, operationId: String): Flow<JointOperationRecord?> =
        jointOperationDao.observeById(userWalletId = userWalletId.stringValue, operationId = operationId)
            .distinctUntilChanged()
            .map { entity ->
                entity?.let { stored ->
                    runCatching { recordConverter.convert(stored) }
                        .onFailure { TangemLogger.e("Dropped the joint operation $operationId", it) }
                        .getOrNull()
                }
            }

    override suspend fun fetchOperation(
        userWalletId: UserWalletId,
        operationId: String,
        eTag: String?,
    ): Either<Throwable, FetchResult> = Either.catch {
        val response = jointAccountApi.getJointOperation(
            walletId = userWalletId.stringValue,
            operationId = operationId,
            eTag = eTag,
        )

        when (response) {
            is ApiResponse.Success -> {
                store(userWalletId, listOf(response.data))
                FetchResult.Updated(eTag = response.headers[ETAG_HEADER]?.firstOrNull())
            }
            is ApiResponse.Error -> when ((response.cause as? ApiResponseError.HttpException)?.code) {
                ApiResponseError.HttpException.Code.NOT_MODIFIED -> FetchResult.NotModified
                ApiResponseError.HttpException.Code.BAD_REQUEST,
                ApiResponseError.HttpException.Code.NOT_FOUND,
                ApiResponseError.HttpException.Code.FORBIDDEN,
                -> FetchResult.Gone
                else -> throw response.cause
            }
        }
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

    private suspend fun store(userWalletId: UserWalletId, items: List<JointOperationHistoryItemDto>) {
        if (items.isEmpty()) return
        val entities = items.map { item ->
            entityConverter.convert(JointOperationEntityConverter.Input(userWalletId = userWalletId, dto = item))
        }
        jointOperationDao.upsert(entities)
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

    /** Same ISO bound the express window uses: lexicographic order of the stored ISO strings is chronological */
    private fun Long.toIsoBound(): String = DateTime(this, DateTimeZone.UTC)
        .withMillisOfSecond(0)
        .toString(ISODateTimeFormat.dateTime())

    private companion object {
        const val DEFAULT_LIMIT = 100
        val SYNC_TYPE = HistorySyncStateEntity.Type.JOINT_OPERATION
    }
}