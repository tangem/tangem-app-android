package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.domain.pushnotificationpreferences.models.PushNotificationPreference
import com.tangem.domain.pushnotificationpreferences.models.WalletPushNotificationPreferences
import com.tangem.domain.pushnotificationpreferences.repository.WalletPushNotificationPreferencesRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ApplyPushNotificationFirstActivationUseCaseTest {

    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase = mockk()
    private val preferencesRepository: WalletPushNotificationPreferencesRepository = mockk()
    private val notificationsRepository: NotificationsRepository = mockk()

    private val useCase = ApplyPushNotificationFirstActivationUseCase(
        setNotificationsEnabledUseCase = setNotificationsEnabledUseCase,
        preferencesRepository = preferencesRepository,
        notificationsRepository = notificationsRepository,
    )

    private val userWalletId = UserWalletId("0A0B0C0D")

    @BeforeEach
    fun resetMocks() {
        clearMocks(setNotificationsEnabledUseCase, preferencesRepository, notificationsRepository)
        coEvery { preferencesRepository.isFirstActivationDone(userWalletId) } returns false
        coEvery { preferencesRepository.markFirstActivationDone(userWalletId) } just runs
        coEvery { notificationsRepository.getWalletAutomaticallyEnabledList() } returns emptyList()
        coEvery { preferencesRepository.observePreferences(userWalletId) } returns flowOf(allFalse())
        coEvery { setNotificationsEnabledUseCase(userWalletId, any()) } returns Either.Right(Unit)
        coEvery {
            preferencesRepository.setAllPreferences(userWalletId, any(), any(), any())
        } returns Either.Right(Unit)
    }

    @Test
    fun `GIVEN activation already done WHEN invoke THEN returns Right without any writes`() = runTest {
        // Arrange
        coEvery { preferencesRepository.isFirstActivationDone(userWalletId) } returns true

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 0) { setNotificationsEnabledUseCase(any(), any()) }
        coVerify(exactly = 0) { preferencesRepository.setAllPreferences(any(), any(), any(), any()) }
        coVerify(exactly = 0) { preferencesRepository.markFirstActivationDone(any()) }
    }

    @Test
    fun `GIVEN wallet enabled by legacy flow WHEN invoke THEN flag adopted without enabling categories`() = runTest {
        // Arrange
        coEvery { notificationsRepository.getWalletAutomaticallyEnabledList() } returns
            listOf(userWalletId.stringValue)

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) { preferencesRepository.markFirstActivationDone(userWalletId) }
        coVerify(exactly = 0) { setNotificationsEnabledUseCase(any(), any()) }
        coVerify(exactly = 0) { preferencesRepository.setAllPreferences(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN preferences unavailable WHEN invoke THEN returns Left without side effects`() = runTest {
        // Arrange: the wallet is not created on the backend yet (e.g. right after onboarding).
        coEvery { preferencesRepository.observePreferences(userWalletId) } returns
            flow { throw RuntimeException("404") }

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result.isLeft()).isTrue()
        coVerify(exactly = 0) { setNotificationsEnabledUseCase(any(), any()) }
        coVerify(exactly = 0) { preferencesRepository.setAllPreferences(any(), any(), any(), any()) }
        coVerify(exactly = 0) { preferencesRepository.markFirstActivationDone(any()) }
    }

    @Test
    fun `GIVEN token subscription fails WHEN invoke THEN returns Left and preferences not written`() = runTest {
        // Arrange
        coEvery { setNotificationsEnabledUseCase(userWalletId, isEnabled = true) } returns
            Either.Left(RuntimeException("net"))

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result.isLeft()).isTrue()
        coVerify(exactly = 0) { preferencesRepository.setAllPreferences(any(), any(), any(), any()) }
        coVerify(exactly = 0) { preferencesRepository.markFirstActivationDone(any()) }
    }

    @Test
    fun `GIVEN preferences write fails WHEN invoke THEN tokens undone and flag not marked`() = runTest {
        // Arrange
        coEvery { preferencesRepository.setAllPreferences(userWalletId, true, true, true) } returns
            Either.Left(RuntimeException("net"))

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result.isLeft()).isTrue()
        coVerifyOrder {
            setNotificationsEnabledUseCase(userWalletId, isEnabled = true)
            preferencesRepository.setAllPreferences(userWalletId, true, true, true)
            setNotificationsEnabledUseCase(userWalletId, isEnabled = false)
        }
        coVerify(exactly = 0) { preferencesRepository.markFirstActivationDone(any()) }
    }

    @Test
    fun `GIVEN whole chain succeeds WHEN invoke THEN all three enabled and flag marked`() = runTest {
        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerifyOrder {
            setNotificationsEnabledUseCase(userWalletId, isEnabled = true)
            preferencesRepository.setAllPreferences(userWalletId, true, true, true)
            preferencesRepository.markFirstActivationDone(userWalletId)
        }
    }

    private fun allFalse() = WalletPushNotificationPreferences(
        transactionAlerts = PushNotificationPreference(isEnabled = false),
        offersUpdates = PushNotificationPreference(isEnabled = false),
        priceAlerts = PushNotificationPreference(isEnabled = false),
    )
}