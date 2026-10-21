package com.tangem.data.txhistory.repository

import arrow.core.right
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ETAG_HEADER
import com.tangem.domain.jointaccount.model.JointOperationRecord
import com.tangem.domain.txhistory.repository.JointOperationHistoryRepository.FetchResult
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.params.ParameterizedTest
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.data.txhistory.repository.converter.FinishedJointOperationConverter
import com.tangem.data.txhistory.repository.converter.JointOperationEntityConverter
import com.tangem.domain.jointaccount.model.FinishedJointOperation
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.store.datasource.jointaccount.JointAccountApi
import com.tangem.store.datasource.jointaccount.models.JointOperationDto
import com.tangem.store.datasource.jointaccount.models.JointOperationHistoryDeltaResponse
import com.tangem.store.datasource.jointaccount.models.JointOperationHistoryItemDto
import com.tangem.store.datasource.jointaccount.models.JointOperationHistoryResponse
import com.tangem.store.datasource.txhistory.db.dao.HistorySyncStateDao
import com.tangem.store.datasource.txhistory.db.dao.JointOperationDao
import com.tangem.store.datasource.txhistory.db.entity.sync.HistorySyncStateEntity
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigInteger

internal class DefaultJointOperationHistoryRepositoryTest {

    private val jointAccountApi: JointAccountApi = mockk()
    private val jointOperationDao: JointOperationDao = mockk(relaxUnitFun = true)
    private val historySyncStateDao: HistorySyncStateDao = mockk(relaxUnitFun = true)
    private val converter = JointOperationEntityConverter()

    private val repository = DefaultJointOperationHistoryRepository(
        jointAccountApi = jointAccountApi,
        jointOperationDao = jointOperationDao,
        historySyncStateDao = historySyncStateDao,
    )

    private val expectedSafeTx = SafeTx(
        to = "0xrecipient",
        value = BigInteger.TEN,
        data = "0x",
        operation = 0,
        safeTxGas = BigInteger.ONE,
        baseGas = BigInteger.ONE,
        gasPrice = BigInteger.ONE,
        gasToken = "0x0",
        refundReceiver = "0xrelayer",
        nonce = BigInteger.TWO,
    )

    private val expectedSignatures = listOf(
        JointOperation.MemberSignature(address = "0xalice", state = JointOperation.MemberSignature.State.SIGNED, at = CREATED_AT),
        JointOperation.MemberSignature(address = "0xbob", state = JointOperation.MemberSignature.State.DECLINED, at = FINISHED_AT),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(jointAccountApi, jointOperationDao, historySyncStateDao)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FetchHistory {

        @Test
        fun `GIVEN no sync state WHEN fetchHistory THEN asks the first page and stores items and both cursors`() =
            runTest {
                // Arrange
                val item = createItem(status = "executed")
                stubSyncState(state = null)
                coEvery {
                    jointAccountApi.getJointOperationHistory(WALLET_ID.stringValue, afterCursor = null, limit = 100)
                } returns ApiResponse.Success(
                    JointOperationHistoryResponse(
                        items = listOf(item),
                        pagination = JointOperationHistoryResponse.Pagination(
                            endCursor = "end",
                            startDeltaCursor = "delta",
                            hasMore = false,
                        ),
                    ),
                )

                // Act
                repository.fetchHistory(WALLET_ID)

                // Assert
                coVerify(exactly = 1) { jointOperationDao.upsert(listOf(toEntity(item))) }
                coVerify(exactly = 1) {
                    historySyncStateDao.upsert(
                        syncState(isInitialCompleted = true, afterCursor = "end", deltaCursor = "delta"),
                    )
                }
            }

        @Test
        fun `GIVEN sync state WHEN fetchHistory THEN continues after the cursor and keeps the delta cursor`() =
            runTest {
                // Arrange
                stubSyncState(syncState(isInitialCompleted = false, afterCursor = "after", deltaCursor = "delta"))
                coEvery {
                    jointAccountApi.getJointOperationHistory(WALLET_ID.stringValue, afterCursor = "after", limit = 100)
                } returns ApiResponse.Success(
                    JointOperationHistoryResponse(
                        items = emptyList(),
                        pagination = JointOperationHistoryResponse.Pagination(
                            endCursor = "next",
                            startDeltaCursor = null,
                            hasMore = true,
                        ),
                    ),
                )

                // Act
                repository.fetchHistory(WALLET_ID)

                // Assert
                coVerify(exactly = 0) { jointOperationDao.upsert(any()) }
                coVerify(exactly = 1) {
                    historySyncStateDao.updateHistoryCursor(
                        type = HistorySyncStateEntity.Type.JOINT_OPERATION.name,
                        userWalletId = WALLET_ID.stringValue,
                        afterCursor = "next",
                        isInitialCompleted = false,
                    )
                }
                coVerify(exactly = 0) { historySyncStateDao.upsert(any()) }
            }

        @Test
        fun `GIVEN refused cursor WHEN fetchHistory THEN drops the sync state and fails`() = runTest {
            // Arrange
            stubSyncState(syncState(isInitialCompleted = false, afterCursor = "stale", deltaCursor = "delta"))
            coEvery {
                jointAccountApi.getJointOperationHistory(WALLET_ID.stringValue, afterCursor = "stale", limit = 100)
            } returns badRequest()

            // Act
            val actual = runCatching { repository.fetchHistory(WALLET_ID) }.exceptionOrNull()

            // Assert
            assertThat(actual).isNotNull()
            coVerify(exactly = 1) { historySyncStateDao.delete(SYNC_TYPE, WALLET_ID.stringValue) }
            coVerify(exactly = 0) { historySyncStateDao.updateHistoryCursor(any(), any(), any(), any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FetchHistoryDelta {

        @Test
        fun `GIVEN delta cursor WHEN fetchHistoryDelta THEN asks before it and saves the new cursor`() = runTest {
            // Arrange
            stubSyncState(syncState(isInitialCompleted = true, afterCursor = "end", deltaCursor = "delta"))
            stubDelta(beforeCursor = "delta", startCursor = "next")

            // Act
            repository.fetchHistoryDelta(WALLET_ID)

            // Assert
            coVerify(exactly = 1) {
                historySyncStateDao.updateDeltaCursor(
                    type = HistorySyncStateEntity.Type.JOINT_OPERATION.name,
                    userWalletId = WALLET_ID.stringValue,
                    deltaCursor = "next",
                )
            }
        }

        @Test
        fun `GIVEN empty delta without cursor WHEN fetchHistoryDelta THEN keeps the old cursor`() = runTest {
            // Arrange
            stubSyncState(syncState(isInitialCompleted = true, afterCursor = "end", deltaCursor = null))
            stubDelta(beforeCursor = null, startCursor = null)

            // Act
            repository.fetchHistoryDelta(WALLET_ID)

            // Assert
            coVerify(exactly = 0) { historySyncStateDao.updateDeltaCursor(any(), any(), any()) }
        }

        @Test
        fun `GIVEN refused cursor WHEN fetchHistoryDelta THEN drops the sync state and returns null`() = runTest {
            // Arrange
            stubSyncState(syncState(isInitialCompleted = true, afterCursor = "end", deltaCursor = "stale"))
            coEvery {
                jointAccountApi.getJointOperationHistoryDelta(WALLET_ID.stringValue, "stale", limit = 100)
            } returns badRequest()

            // Act
            val actual = repository.fetchHistoryDelta(WALLET_ID)

            // Assert
            assertThat(actual).isNull()
            coVerify(exactly = 1) { historySyncStateDao.delete(SYNC_TYPE, WALLET_ID.stringValue) }
            coVerify(exactly = 0) { historySyncStateDao.updateDeltaCursor(any(), any(), any()) }
        }

        @Test
        fun `GIVEN no sync state WHEN fetchHistoryDelta THEN returns null without asking the backend`() = runTest {
            // Arrange
            stubSyncState(state = null)

            // Act
            val actual = repository.fetchHistoryDelta(WALLET_ID)

            // Assert
            assertThat(actual).isNull()
            coVerify(exactly = 0) { jointAccountApi.getJointOperationHistoryDelta(any(), any(), any()) }
        }

        private fun stubDelta(beforeCursor: String?, startCursor: String?) {
            coEvery {
                jointAccountApi.getJointOperationHistoryDelta(WALLET_ID.stringValue, beforeCursor, limit = 100)
            } returns ApiResponse.Success(
                JointOperationHistoryDeltaResponse(
                    items = emptyList(),
                    pagination = JointOperationHistoryDeltaResponse.Pagination(
                        startCursor = startCursor,
                        hasMore = false,
                    ),
                ),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetFinishedOperations {

        @Test
        fun `GIVEN stored operations WHEN getFinishedOperations THEN maps finished ones and drops the broken`() =
            runTest {
                // Arrange
                val executed = toEntity(createItem(operationId = "1", status = "executed"))
                val broken = toEntity(createItem(operationId = "2", status = "cancelled"))
                    .let { it.copy(operation = it.operation.copy(safeTx = it.operation.safeTx.copy(value = "nan"))) }
                val inFlight = toEntity(createItem(operationId = "3", status = "pending"))
                every {
                    jointOperationDao.observe(
                        userWalletId = WALLET_ID.stringValue,
                        cryptoAccountId = ACCOUNT_ID.value,
                        chainId = CHAIN_ID,
                        statuses = listOf("executed", "failed", "cancelled"),
                        fromFinishedAtIso = "1970-01-01T00:00:00.000Z",
                    )
                } returns flowOf(listOf(executed, broken, inFlight))

                // Act
                val actual = repository.getFinishedOperations(ACCOUNT_ID, CHAIN_ID, fromFinishedAtMillis = 0).first()

                // Assert
                assertThat(actual).containsExactly(
                    FinishedJointOperation(
                        operationId = "1",
                        chainId = CHAIN_ID,
                        safeAddress = "0xsafe",
                        safeTx = SafeTx(
                            to = "0xrecipient",
                            value = BigInteger.TEN,
                            data = "0x",
                            operation = 0,
                            safeTxGas = BigInteger.ONE,
                            baseGas = BigInteger.ONE,
                            gasPrice = BigInteger.ONE,
                            gasToken = "0x0",
                            refundReceiver = "0xrelayer",
                            nonce = BigInteger.TWO,
                        ),
                        status = FinishedJointOperation.Status.EXECUTED,
                        initiator = "0xalice",
                        signatures = listOf(
                            JointOperation.MemberSignature(
                                address = "0xalice",
                                state = JointOperation.MemberSignature.State.SIGNED,
                                at = CREATED_AT,
                            ),
                            JointOperation.MemberSignature(
                                address = "0xbob",
                                state = JointOperation.MemberSignature.State.DECLINED,
                                at = FINISHED_AT,
                            ),
                        ),
                        createdAt = CREATED_AT,
                        finishedAtMillis = 1788859442000L,
                        txHash = "0xhash",
                    ),
                )
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ObserveOperation {

        @Test
        fun `GIVEN a pending operation stored WHEN observeOperation THEN it is in flight`() = runTest {
            // Arrange
            val entity = toEntity(createItem(operationId = "1", status = "pending"))
            every { jointOperationDao.observeById(WALLET_ID.stringValue, "1") } returns flowOf(entity)

            // Act
            val actual = repository.observeOperation(WALLET_ID, operationId = "1").first()

            // Assert
            assertThat(actual).isEqualTo(
                JointOperationRecord.InFlight(
                    JointOperation(
                        operationId = "1",
                        chainId = CHAIN_ID,
                        safeTx = expectedSafeTx,
                        status = JointOperation.Status.PENDING,
                        initiator = "0xalice",
                        signatures = expectedSignatures,
                        createdAt = CREATED_AT,
                    ),
                ),
            )
        }

        @Test
        fun `GIVEN a cancelled operation stored WHEN observeOperation THEN it is finished`() = runTest {
            // Arrange
            val entity = toEntity(createItem(operationId = "1", status = "cancelled"))
            every { jointOperationDao.observeById(WALLET_ID.stringValue, "1") } returns flowOf(entity)

            // Act
            val actual = repository.observeOperation(WALLET_ID, operationId = "1").first()

            // Assert
            assertThat(actual).isEqualTo(JointOperationRecord.Finished(FinishedJointOperationConverter().convert(entity)!!))
        }

        @Test
        fun `GIVEN nothing stored WHEN observeOperation THEN null`() = runTest {
            // Arrange
            every { jointOperationDao.observeById(WALLET_ID.stringValue, "1") } returns flowOf(null)

            // Act
            val actual = repository.observeOperation(WALLET_ID, operationId = "1").first()

            // Assert
            assertThat(actual).isNull()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FetchOperation {

        @Test
        fun `GIVEN the operation WHEN fetchOperation THEN stores it and returns the new tag`() = runTest {
            // Arrange
            val item = createItem(operationId = "1", status = "executed")
            coEvery { jointAccountApi.getJointOperation(WALLET_ID.stringValue, "1", eTag = "old") } returns
                ApiResponse.Success(data = item, headers = mapOf(ETAG_HEADER to listOf("new")))

            // Act
            val actual = repository.fetchOperation(WALLET_ID, operationId = "1", eTag = "old")

            // Assert
            assertThat(actual).isEqualTo(FetchResult.Updated(eTag = "new").right())
            coVerify { jointOperationDao.upsert(listOf(toEntity(item))) }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN an error answer WHEN fetchOperation THEN maps it and stores nothing`(model: ErrorModel) =
            runTest {
                // Arrange
                coEvery { jointAccountApi.getJointOperation(WALLET_ID.stringValue, "1", eTag = "old") } returns
                    ApiResponse.Error(ApiResponseError.HttpException(model.code, message = null, errorBody = null))
                        as ApiResponse<JointOperationHistoryItemDto>

                // Act
                val actual = repository.fetchOperation(WALLET_ID, operationId = "1", eTag = "old")

                // Assert
                assertThat(actual.getOrNull()).isEqualTo(model.expected)
                assertThat(actual.isLeft()).isEqualTo(model.expected == null)
                coVerify(exactly = 0) { jointOperationDao.upsert(any()) }
            }

        private fun provideTestModels() = listOf(
            ErrorModel(code = ApiResponseError.HttpException.Code.NOT_MODIFIED, expected = FetchResult.NotModified),
            ErrorModel(code = ApiResponseError.HttpException.Code.BAD_REQUEST, expected = FetchResult.Gone),
            ErrorModel(code = ApiResponseError.HttpException.Code.NOT_FOUND, expected = FetchResult.Gone),
            ErrorModel(code = ApiResponseError.HttpException.Code.FORBIDDEN, expected = FetchResult.Gone),
            ErrorModel(code = ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR, expected = null),
        )
    }

    private fun toEntity(item: JointOperationHistoryItemDto) =
        converter.convert(JointOperationEntityConverter.Input(userWalletId = WALLET_ID, dto = item))

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> badRequest(): ApiResponse<T> = ApiResponse.Error(
        ApiResponseError.HttpException(ApiResponseError.HttpException.Code.BAD_REQUEST, message = null, errorBody = null),
    ) as ApiResponse<T>

    data class ErrorModel(val code: ApiResponseError.HttpException.Code, val expected: FetchResult?)

    private fun stubSyncState(state: HistorySyncStateEntity?) {
        every {
            historySyncStateDao.observe(HistorySyncStateEntity.Type.JOINT_OPERATION.name, WALLET_ID.stringValue)
        } returns flowOf(state)
    }

    private fun syncState(isInitialCompleted: Boolean, afterCursor: String?, deltaCursor: String?) =
        HistorySyncStateEntity(
            type = HistorySyncStateEntity.Type.JOINT_OPERATION.name,
            userWalletId = WALLET_ID.stringValue,
            isInitialCompleted = isInitialCompleted,
            afterCursor = afterCursor,
            deltaCursor = deltaCursor,
        )

    private fun createItem(operationId: String = "1", status: String) = JointOperationHistoryItemDto(
        operationId = operationId,
        chainId = CHAIN_ID,
        safeTx = JointOperationDto.SafeTxDto(
            to = "0xRecipient",
            value = "10",
            data = "0x",
            operation = 0,
            safeTxGas = "1",
            baseGas = "1",
            gasPrice = "1",
            gasToken = "0x0",
            refundReceiver = "0xRelayer",
            nonce = "2",
        ),
        status = status,
        initiator = "0xAlice",
        createdAt = CREATED_AT,
        votes = listOf(
            JointOperationDto.VoteDto(address = "0xAlice", vote = "approve", votedAt = CREATED_AT),
            JointOperationDto.VoteDto(address = "0xBob", vote = "reject", votedAt = FINISHED_AT),
        ),
        cryptoAccountId = ACCOUNT_ID.value,
        safeAddress = "0xSafe",
        txHash = "0xHash".takeIf { status == "executed" },
        finishedAt = FINISHED_AT.takeIf { status != "pending" },
    )

    private companion object {
        val WALLET_ID: UserWalletId = MockAccounts.userWalletId
        val ACCOUNT_ID: AccountId = MockAccounts.createJointAccount(ownerKeyIndex = 0).accountId
        val SYNC_TYPE = HistorySyncStateEntity.Type.JOINT_OPERATION.name
        const val CHAIN_ID = 8453L
        const val CREATED_AT = "2026-09-08T09:20:37.000Z"
        const val FINISHED_AT = "2026-09-08T09:24:02.000Z"
    }
}