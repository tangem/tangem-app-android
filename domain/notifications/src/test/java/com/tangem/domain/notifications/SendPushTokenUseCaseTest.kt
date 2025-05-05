package com.tangem.domain.notifications

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.notifications.models.ApplicationId
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.utils.notifications.PushNotificationsTokenProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SendPushTokenUseCaseTest {

    private lateinit var notificationsRepository: NotificationsRepository
    private lateinit var getApplicationIdUseCase: GetApplicationIdUseCase
    private lateinit var pushNotificationsTokenProvider: PushNotificationsTokenProvider
    private lateinit var sendPushTokenUseCase: SendPushTokenUseCase

    @Before
    fun setup() {
        notificationsRepository = mockk()
        getApplicationIdUseCase = mockk()
        pushNotificationsTokenProvider = mockk()
        sendPushTokenUseCase = SendPushTokenUseCase(
            notificationsRepository = notificationsRepository,
            getApplicationIdUseCase = getApplicationIdUseCase,
            pushNotificationsTokenProvider = pushNotificationsTokenProvider,
        )
    }

    @Test
    fun `GIVEN valid application ID and token WHEN invoke THEN token is sent successfully`() = runTest {
        // GIVEN
        val applicationId = ApplicationId("test-app-id")
        val token = "test-token"
        coEvery { getApplicationIdUseCase() } returns Either.Right(applicationId)
        coEvery { pushNotificationsTokenProvider.getToken() } returns token
        coEvery { notificationsRepository.sendPushToken(applicationId, token) } returns Unit

        // WHEN
        val result = sendPushTokenUseCase()

        // THEN
        assertThat(result).isEqualTo(Either.Right(Unit))
        coVerify(exactly = 1) { notificationsRepository.sendPushToken(applicationId, token) }
    }

    @Test
    fun `GIVEN application ID is not found WHEN invoke THEN throws error`() = runTest {
        // GIVEN
        coEvery { getApplicationIdUseCase() } returns Either.Left(Throwable("Application ID not found"))

        // WHEN & THEN
        val result = sendPushTokenUseCase()
        assertThat(result.isLeft()).isTrue()
        assertThat(result.fold({ it.message }, { null })).isEqualTo("Application ID not found")
        coVerify(exactly = 0) { notificationsRepository.sendPushToken(any(), any()) }
    }

    @Test
    fun `GIVEN repository throws error WHEN invoke THEN returns error`() = runTest {
        // GIVEN
        val applicationId = ApplicationId("test-app-id")
        val token = "test-token"
        val expectedError = RuntimeException("Network error")
        coEvery { getApplicationIdUseCase() } returns Either.Right(applicationId)
        coEvery { pushNotificationsTokenProvider.getToken() } returns token
        coEvery { notificationsRepository.sendPushToken(applicationId, token) } throws expectedError

        // WHEN
        val result = sendPushTokenUseCase()

        // THEN
        assertThat(result).isEqualTo(Either.Left(expectedError))
        coVerify(exactly = 1) { notificationsRepository.sendPushToken(applicationId, token) }
    }
}