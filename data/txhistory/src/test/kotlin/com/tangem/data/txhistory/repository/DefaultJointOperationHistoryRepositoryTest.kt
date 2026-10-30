package com.tangem.data.txhistory.repository

import arrow.core.right
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ETAG_HEADER
import com.tangem.domain.jointaccount.model.JointOperationFetchResult
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
import com.tangem.store.datasource.jointaccount.models.JointOperationHistoryItemDto
import com.tangem.store.datasource.txhistory.db.dao.JointOperationDao
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
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
    private val converter = JointOperationEntityConverter()

    private val repository = DefaultJointOperationHistoryRepository(
        jointAccountApi = jointAccountApi,
        jointOperationDao = jointOperationDao,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(jointAccountApi, jointOperationDao)
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
        fun `GIVEN a pending operation stored WHEN observeOperation THEN null`() = runTest {
            // Arrange
            val entity = toEntity(createItem(operationId = "1", status = "pending"))
            every { jointOperationDao.observeById(WALLET_ID.stringValue, "1") } returns flowOf(entity)

            // Act
            val actual = repository.observeOperation(WALLET_ID, operationId = "1").first()

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN a cancelled operation stored WHEN observeOperation THEN it is finished`() = runTest {
            // Arrange
            val entity = toEntity(createItem(operationId = "1", status = "cancelled"))
            every { jointOperationDao.observeById(WALLET_ID.stringValue, "1") } returns flowOf(entity)

            // Act
            val actual = repository.observeOperation(WALLET_ID, operationId = "1").first()

            // Assert
            assertThat(actual).isEqualTo(FinishedJointOperationConverter().convert(entity))
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
        fun `GIVEN a finished operation WHEN fetchOperation THEN stores it`() = runTest {
            // Arrange
            val item = createItem(operationId = "1", status = "executed")
            coEvery { jointAccountApi.getJointOperation(WALLET_ID.stringValue, "1", eTag = null) } returns
                ApiResponse.Success(data = item, headers = mapOf(ETAG_HEADER to listOf("tag")))

            // Act
            val actual = repository.fetchOperation(WALLET_ID, operationId = "1")

            // Assert
            assertThat(actual).isEqualTo(JointOperationFetchResult.Updated.right())
            coVerify { jointOperationDao.upsert(listOf(toEntity(item))) }
        }

        @Test
        fun `GIVEN the operation in flight WHEN fetchOperation THEN stores nothing`() = runTest {
            // Arrange
            val item = createItem(operationId = "2", status = "awaiting_signatures")
            coEvery { jointAccountApi.getJointOperation(WALLET_ID.stringValue, "2", eTag = null) } returns
                ApiResponse.Success(data = item, headers = mapOf(ETAG_HEADER to listOf("tag")))

            // Act
            val actual = repository.fetchOperation(WALLET_ID, operationId = "2")

            // Assert
            assertThat(actual).isEqualTo(JointOperationFetchResult.InFlight.right())
            coVerify(exactly = 0) { jointOperationDao.upsert(any()) }
        }

        @Test
        fun `GIVEN a tagged answer WHEN fetchOperation again THEN sends that tag`() = runTest {
            // Arrange
            val item = createItem(operationId = "3", status = "awaiting_signatures")
            coEvery { jointAccountApi.getJointOperation(WALLET_ID.stringValue, "3", eTag = null) } returns
                ApiResponse.Success(data = item, headers = mapOf(ETAG_HEADER to listOf("tag-3")))
            coEvery { jointAccountApi.getJointOperation(WALLET_ID.stringValue, "3", eTag = "tag-3") } returns
                httpError(ApiResponseError.HttpException.Code.NOT_MODIFIED)
            repository.fetchOperation(WALLET_ID, operationId = "3")

            // Act
            val actual = repository.fetchOperation(WALLET_ID, operationId = "3")

            // Assert
            assertThat(actual).isEqualTo(JointOperationFetchResult.NotModified.right())
        }

        @Test
        fun `GIVEN the operation finished WHEN fetchOperation again THEN sends no tag`() = runTest {
            // Arrange
            val item = createItem(operationId = "8", status = "executed")
            coEvery { jointAccountApi.getJointOperation(WALLET_ID.stringValue, "8", eTag = any()) } returns
                ApiResponse.Success(data = item, headers = mapOf(ETAG_HEADER to listOf("tag-8")))
            repository.fetchOperation(WALLET_ID, operationId = "8")

            // Act
            repository.fetchOperation(WALLET_ID, operationId = "8")

            // Assert
            coVerify(exactly = 2) { jointAccountApi.getJointOperation(WALLET_ID.stringValue, "8", eTag = null) }
        }

        @Test
        fun `GIVEN the operation gone WHEN fetchOperation again THEN sends no tag`() = runTest {
            // Arrange
            val item = createItem(operationId = "4", status = "awaiting_signatures")
            coEvery { jointAccountApi.getJointOperation(WALLET_ID.stringValue, "4", eTag = any()) } returnsMany listOf(
                ApiResponse.Success(data = item, headers = mapOf(ETAG_HEADER to listOf("tag-4"))),
                httpError(ApiResponseError.HttpException.Code.NOT_FOUND),
                httpError(ApiResponseError.HttpException.Code.NOT_FOUND),
            )
            repository.fetchOperation(WALLET_ID, operationId = "4")
            repository.fetchOperation(WALLET_ID, operationId = "4")

            // Act
            repository.fetchOperation(WALLET_ID, operationId = "4")

            // Assert
            coVerifyOrder {
                jointAccountApi.getJointOperation(WALLET_ID.stringValue, "4", eTag = null)
                jointAccountApi.getJointOperation(WALLET_ID.stringValue, "4", eTag = "tag-4")
                jointAccountApi.getJointOperation(WALLET_ID.stringValue, "4", eTag = null)
            }
        }

        @Test
        fun `GIVEN an unknown status WHEN fetchOperation THEN it is gone and nothing is stored`() = runTest {
            // Arrange
            val item = createItem(operationId = "6", status = "frozen")
            coEvery { jointAccountApi.getJointOperation(WALLET_ID.stringValue, "6", eTag = null) } returns
                ApiResponse.Success(data = item, headers = emptyMap())

            // Act
            val actual = repository.fetchOperation(WALLET_ID, operationId = "6")

            // Assert
            assertThat(actual).isEqualTo(JointOperationFetchResult.Gone.right())
            coVerify(exactly = 0) { jointOperationDao.upsert(any()) }
        }

        @Test
        fun `GIVEN a cancelled operation without finishedAt WHEN fetchOperation THEN it is gone and nothing is stored`() =
            runTest {
                // Arrange
                val item = createItem(operationId = "7", status = "cancelled").copy(finishedAt = null)
                coEvery { jointAccountApi.getJointOperation(WALLET_ID.stringValue, "7", eTag = null) } returns
                    ApiResponse.Success(data = item, headers = emptyMap())

                // Act
                val actual = repository.fetchOperation(WALLET_ID, operationId = "7")

                // Assert
                assertThat(actual).isEqualTo(JointOperationFetchResult.Gone.right())
                coVerify(exactly = 0) { jointOperationDao.upsert(any()) }
            }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN an error answer WHEN fetchOperation THEN maps it and stores nothing`(model: ErrorModel) =
            runTest {
                // Arrange
                coEvery { jointAccountApi.getJointOperation(WALLET_ID.stringValue, "5", eTag = any()) } returns
                    httpError(model.code)

                // Act
                val actual = repository.fetchOperation(WALLET_ID, operationId = "5")

                // Assert
                assertThat(actual.getOrNull()).isEqualTo(model.expected)
                assertThat(actual.isLeft()).isEqualTo(model.expected == null)
                coVerify(exactly = 0) { jointOperationDao.upsert(any()) }
            }

        private fun provideTestModels() = listOf(
            ErrorModel(
                code = ApiResponseError.HttpException.Code.NOT_MODIFIED,
                expected = JointOperationFetchResult.NotModified,
            ),
            ErrorModel(code = ApiResponseError.HttpException.Code.BAD_REQUEST, expected = null),
            ErrorModel(code = ApiResponseError.HttpException.Code.NOT_FOUND, expected = JointOperationFetchResult.Gone),
            ErrorModel(code = ApiResponseError.HttpException.Code.FORBIDDEN, expected = JointOperationFetchResult.Gone),
            ErrorModel(code = ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR, expected = null),
        )
    }

    private fun toEntity(item: JointOperationHistoryItemDto) =
        converter.convert(JointOperationEntityConverter.Input(userWalletId = WALLET_ID, dto = item))

    @Suppress("UNCHECKED_CAST")
    private fun httpError(code: ApiResponseError.HttpException.Code): ApiResponse<JointOperationHistoryItemDto> =
        ApiResponse.Error(ApiResponseError.HttpException(code, message = null, errorBody = null))
            as ApiResponse<JointOperationHistoryItemDto>

    data class ErrorModel(val code: ApiResponseError.HttpException.Code, val expected: JointOperationFetchResult?)

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
        const val CHAIN_ID = 8453L
        const val CREATED_AT = "2026-09-08T09:20:37.000Z"
        const val FINISHED_AT = "2026-09-08T09:24:02.000Z"
    }
}