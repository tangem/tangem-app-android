package com.tangem.domain.notifications

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.notifications.models.ApplicationId
import com.tangem.domain.notifications.models.NotificationsError
import com.tangem.domain.notifications.repository.PushNotificationsRepository
import com.tangem.utils.notifications.PushNotificationsTokenProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SendPushTokenUseCaseTest {

    private lateinit var pushNotificationsRepository: PushNotificationsRepository
    private lateinit var pushNotificationsTokenProvider: PushNotificationsTokenProvider
    private lateinit var sendPushTokenUseCase: SendPushTokenUseCase

    @Before
    fun setup() {
        pushNotificationsRepository = mockk()
        pushNotificationsTokenProvider = mockk()
        sendPushTokenUseCase = SendPushTokenUseCase(
            pushNotificationsRepository = pushNotificationsRepository,
            pushNotificationsTokenProvider = pushNotificationsTokenProvider,
        )
    }

    @Test
    fun `GIVEN valid application ID and token WHEN invoke THEN token is sent successfully`() = runTest {
        // GIVEN
        val applicationId = ApplicationId("test-app-id")
        val token = "test-token"
        coEvery { pushNotificationsTokenProvider.getToken() } returns token
        coEvery { pushNotificationsRepository.sendPushToken(applicationId, token) } returns Either.Right(Unit)

        // WHEN
        val result = sendPushTokenUseCase(applicationId)

        // THEN
        assertThat(result).isEqualTo(Either.Right(Unit))
        coVerify(exactly = 1) { pushNotificationsRepository.sendPushToken(applicationId, token) }
    }

    @Test
    fun `GIVEN repository returns DataError WHEN invoke THEN returns DataError`() = runTest {
        // GIVEN
        val applicationId = ApplicationId("test-app-id")
        val token = "test-token"
        val expectedError = NotificationsError.DataError("Network error")
        coEvery { pushNotificationsTokenProvider.getToken() } returns token
        coEvery { pushNotificationsRepository.sendPushToken(applicationId, token) } returns Either.Left(expectedError)

        // WHEN
        val result = sendPushTokenUseCase(applicationId)

        // THEN
        assertThat(result).isEqualTo(Either.Left(expectedError))
        coVerify(exactly = 1) { pushNotificationsRepository.sendPushToken(applicationId, token) }
    }

    @Test
    fun `GIVEN repository returns ApplicationIdNotFound WHEN invoke THEN returns ApplicationIdNotFound`() = runTest {
        // GIVEN
        val applicationId = ApplicationId("test-app-id")
        val token = "test-token"
        coEvery { pushNotificationsTokenProvider.getToken() } returns token
        coEvery {
            pushNotificationsRepository.sendPushToken(applicationId, token)
        } returns Either.Left(NotificationsError.ApplicationIdNotFound)

        // WHEN
        val result = sendPushTokenUseCase(applicationId)

        // THEN
        assertThat(result).isEqualTo(Either.Left(NotificationsError.ApplicationIdNotFound))
        coVerify(exactly = 1) { pushNotificationsRepository.sendPushToken(applicationId, token) }
    }

    @Test
    fun `GIVEN empty token WHEN invoke THEN sends empty token to repository`() = runTest {
        // GIVEN
        val applicationId = ApplicationId("test-app-id")
        val emptyToken = ""
        coEvery { pushNotificationsTokenProvider.getToken() } returns emptyToken
        coEvery { pushNotificationsRepository.sendPushToken(applicationId, emptyToken) } returns Either.Right(Unit)

        // WHEN
        val result = sendPushTokenUseCase(applicationId)

        // THEN
        assertThat(result).isEqualTo(Either.Right(Unit))
        coVerify(exactly = 1) { pushNotificationsRepository.sendPushToken(applicationId, emptyToken) }
    }
}