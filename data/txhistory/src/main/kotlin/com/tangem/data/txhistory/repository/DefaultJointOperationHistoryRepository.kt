package com.tangem.data.txhistory.repository

import arrow.core.Either
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ETAG_HEADER
import com.tangem.data.txhistory.repository.converter.FinishedJointOperationConverter
import com.tangem.domain.jointaccount.model.FinishedJointOperation
import com.tangem.domain.jointaccount.model.JointOperationFetchResult
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.repository.JointOperationHistoryRepository
import com.tangem.store.datasource.jointaccount.JointAccountApi
import com.tangem.store.datasource.jointaccount.models.JointOperationHistoryItemDto
import com.tangem.store.datasource.txhistory.db.dao.JointOperationDao
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.ISODateTimeFormat
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

internal class DefaultJointOperationHistoryRepository @Inject constructor(
    private val jointAccountApi: JointAccountApi,
    private val jointOperationDao: JointOperationDao,
) : JointOperationHistoryRepository {

    private val finishedConverter = FinishedJointOperationConverter()
    private val operationETags = ConcurrentHashMap<OperationKey, String>()

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

    override fun observeOperation(userWalletId: UserWalletId, operationId: String): Flow<FinishedJointOperation?> =
        jointOperationDao.observeById(userWalletId = userWalletId.stringValue, operationId = operationId)
            .distinctUntilChanged()
            .map { entity ->
                entity?.let { stored ->
                    runCatching { finishedConverter.convert(stored) }
                        .onFailure { TangemLogger.e("Dropped the joint operation $operationId", it) }
                        .getOrNull()
                }
            }

    override suspend fun fetchOperation(
        userWalletId: UserWalletId,
        operationId: String,
    ): Either<Throwable, JointOperationFetchResult> = Either.catch {
        val key = OperationKey(userWalletId = userWalletId, operationId = operationId)
        val response = jointAccountApi.getJointOperation(
            walletId = userWalletId.stringValue,
            operationId = operationId,
            eTag = operationETags[key],
        )

        when (response) {
            is ApiResponse.Success -> when {
                response.data.isFinished() -> {
                    jointOperationDao.upsertFinished(userWalletId, listOf(response.data))
                    operationETags.remove(key)
                    JointOperationFetchResult.Updated
                }
                response.data.status in IN_FLIGHT_STATUSES -> {
                    val eTag = response.headers[ETAG_HEADER]?.firstOrNull()
                    if (eTag != null) operationETags[key] = eTag else operationETags.remove(key)
                    JointOperationFetchResult.InFlight
                }
                else -> {
                    TangemLogger.e("Joint operation $operationId came back unreadable: status ${response.data.status}")
                    operationETags.remove(key)
                    JointOperationFetchResult.Gone
                }
            }
            is ApiResponse.Error -> when ((response.cause as? ApiResponseError.HttpException)?.code) {
                ApiResponseError.HttpException.Code.NOT_MODIFIED -> JointOperationFetchResult.NotModified
                ApiResponseError.HttpException.Code.NOT_FOUND,
                ApiResponseError.HttpException.Code.FORBIDDEN,
                -> {
                    operationETags.remove(key)
                    JointOperationFetchResult.Gone
                }
                else -> throw response.cause
            }
        }
    }

    private fun JointOperationHistoryItemDto.isFinished(): Boolean {
        return status in FinishedJointOperationConverter.FINISHED_STATUSES && finishedAt != null
    }

    /** Same ISO bound the express window uses: lexicographic order of the stored ISO strings is chronological */
    private fun Long.toIsoBound(): String = DateTime(this, DateTimeZone.UTC)
        .withMillisOfSecond(0)
        .toString(ISODateTimeFormat.dateTime())

    private companion object {
        val IN_FLIGHT_STATUSES = setOf("awaiting_signatures", "pending")
    }
}

private data class OperationKey(val userWalletId: UserWalletId, val operationId: String)