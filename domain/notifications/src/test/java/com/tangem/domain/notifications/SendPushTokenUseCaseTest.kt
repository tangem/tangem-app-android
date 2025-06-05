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
    private lateinit var pushNotificationsTokenProvider: PushNotificationsTokenProvider
    private lateinit var sendPushTokenUseCase: SendPushTokenUseCase

    @Before
    fun setup() {
        notificationsRepository = mockk()
        pushNotificationsTokenProvider = mockk()
        sendPushTokenUseCase = SendPushTokenUseCase(
            notificationsRepository = notificationsRepository,
            pushNotificationsTokenProvider = pushNotificationsTokenProvider,
        )
    }

    @Test
    fun `GIVEN valid application ID and token WHEN invoke THEN token is sent successfully`() = runTest {
        // GIVEN
        val applicationId = ApplicationId("test-app-id")
        val token = "test-token"
        coEvery { pushNotificationsTokenProvider.getToken() } returns token
        coEvery { notificationsRepository.sendPushToken(applicationId, token) } returns Unit

        // WHEN
        val result = sendPushTokenUseCase(applicationId)

        // THEN
        assertThat(result).isEqualTo(Either.Right(Unit))
        coVerify(exactly = 1) { notificationsRepository.sendPushToken(applicationId, token) }
    }

    @Test
    fun `GIVEN repository throws error WHEN invoke THEN returns error`() = runTest {
        // GIVEN
        val applicationId = ApplicationId("test-app-id")
        val token = "test-token"
        val expectedError = RuntimeException("Network error")
        coEvery { pushNotificationsTokenProvider.getToken() } returns token
        coEvery { notificationsRepository.sendPushToken(applicationId, token) } throws expectedError

        // WHEN
        val result = sendPushTokenUseCase(applicationId)

        // THEN
        assertThat(result).isEqualTo(Either.Left(expectedError))
        coVerify(exactly = 1) { notificationsRepository.sendPushToken(applicationId, token) }
    }
}