package com.tangem.domain.notifications

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.notifications.repository.NotificationsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.net.SocketTimeoutException

class GetApplicationIdUseCaseTest {

    private val notificationsRepository: NotificationsRepository = mockk()
    private val useCase = GetApplicationIdUseCase(notificationsRepository)

    @Test
    fun `GIVEN local application ID exists WHEN invoke THEN return local application ID`() = runTest {
        // GIVEN
        val expectedApplicationId = "test-app-id"
        coEvery { notificationsRepository.getApplicationId() } returns expectedApplicationId

        // WHEN
        val result = useCase()

        // THEN
        assertThat(result).isInstanceOf(Either.Right::class.java)
        assertThat((result as Either.Right).value).isEqualTo(expectedApplicationId)
        coVerify(exactly = 1) { notificationsRepository.getApplicationId() }
        coVerify(inverse = true) {
            notificationsRepository.createApplicationId()
            notificationsRepository.saveApplicationId(any())
        }
    }

    @Test
    fun `GIVEN local application ID does not exist WHEN invoke THEN create and save new application ID`() = runTest {
        // GIVEN
        val newApplicationId = "new-app-id"
        coEvery { notificationsRepository.getApplicationId() } returns null
        coEvery { notificationsRepository.createApplicationId() } returns newApplicationId
        coEvery { notificationsRepository.saveApplicationId(newApplicationId) } returns Unit

        // WHEN
        val result = useCase()

        // THEN
        assertThat(result).isInstanceOf(Either.Right::class.java)
        assertThat((result as Either.Right).value).isEqualTo(newApplicationId)
        coVerifyOrder {
            notificationsRepository.getApplicationId()
            notificationsRepository.getApplicationId()
            notificationsRepository.createApplicationId()
            notificationsRepository.saveApplicationId(newApplicationId)
        }
    }

    @Test
    fun `GIVEN repository throws exception WHEN invoke THEN return Either Left with error`() = runTest {
        // GIVEN
        val expectedError = SocketTimeoutException("Test error")
        coEvery { notificationsRepository.getApplicationId() } throws expectedError

        // WHEN
        val result = useCase()

        // THEN
        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isEqualTo(expectedError)
        coVerify(exactly = 1) { notificationsRepository.getApplicationId() }
        coVerify(inverse = true) {
            notificationsRepository.createApplicationId()
            notificationsRepository.saveApplicationId(any())
        }
    }

    @Test
    fun `GIVEN no local application ID WHEN multiple concurrent invokes THEN create only one application ID`() =
        runTest {
            // GIVEN
            val newApplicationId = "new-app-id"
            var isIdCreated = false

            coEvery { notificationsRepository.getApplicationId() } answers {
                if (!isIdCreated) null else newApplicationId
            }
            coEvery { notificationsRepository.createApplicationId() } answers {
                isIdCreated = true
                newApplicationId
            }
            coEvery { notificationsRepository.saveApplicationId(newApplicationId) } returns Unit

            // WHEN
            val results = coroutineScope {
                List(PARALLEL_COUNT) {
                    async {
                        useCase()
                    }
                }.awaitAll()
            }

            // THEN
            results.forEach { result ->
                assertThat(result).isInstanceOf(Either.Right::class.java)
                assertThat((result as Either.Right).value).isEqualTo(newApplicationId)
            }
            coVerify(exactly = PARALLEL_COUNT + 1) { notificationsRepository.getApplicationId() }
            coVerify(exactly = 1) { notificationsRepository.createApplicationId() }
            coVerify(exactly = 1) { notificationsRepository.saveApplicationId(newApplicationId) }
        }

    @Test
    fun `GIVEN no local application ID WHEN multiple concurrent invokes with delay THEN create only one application ID`() = runTest {
        // GIVEN
        val newApplicationId = "new-app-id"
        var isIdCreated = false

        coEvery { notificationsRepository.getApplicationId() } answers {
            if (!isIdCreated) null else newApplicationId
        }
        coEvery { notificationsRepository.createApplicationId() } answers {
            isIdCreated = true
            newApplicationId
        }
        coEvery { notificationsRepository.saveApplicationId(newApplicationId) } returns Unit

        // WHEN
        val results = coroutineScope {
            List(PARALLEL_COUNT) {
                async {
                    delay(100)
                    useCase()
                }
            }.awaitAll()
        }

        // THEN
        results.forEach { result ->
            assertThat(result).isInstanceOf(Either.Right::class.java)
            assertThat((result as Either.Right).value).isEqualTo(newApplicationId)
        }
        coVerify(exactly = PARALLEL_COUNT + 1) { notificationsRepository.getApplicationId() }
        coVerify(exactly = 1) { notificationsRepository.createApplicationId() }
        coVerify(exactly = 1) { notificationsRepository.saveApplicationId(newApplicationId) }
    }

    companion object {
        private const val PARALLEL_COUNT = 100
    }
}