package com.tangem.domain.wallets.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.repository.WalletsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SetNotificationsEnabledUseCaseTest {

    private lateinit var useCase: SetNotificationsEnabledUseCase
    private lateinit var walletsRepository: WalletsRepository
    private lateinit var accountsCRUDRepository: AccountsCRUDRepository

    @Before
    fun setup() {
        walletsRepository = mockk()
        accountsCRUDRepository = mockk()
        useCase = SetNotificationsEnabledUseCase(
            walletsRepository = walletsRepository,
            accountsCRUDRepository = accountsCRUDRepository,
        )
    }

    @Test
    fun `GIVEN notifications enabled successfully WHEN invoke THEN return Right with Unit`() = runTest {
        // GIVEN
        val userWalletId = UserWalletId("0A0B0C0D")
        val isEnabled = true
        coEvery { walletsRepository.setNotificationsEnabled(userWalletId, isEnabled) } just runs
        coEvery { accountsCRUDRepository.syncTokens(userWalletId) } just runs

        // WHEN
        val result = useCase(userWalletId, isEnabled)

        // THEN
        assertThat(result.isRight()).isTrue()
        coVerifyOrder {
            walletsRepository.setNotificationsEnabled(userWalletId, isEnabled)
            accountsCRUDRepository.syncTokens(userWalletId)
        }
    }

    @Test
    fun `GIVEN notifications disabled successfully WHEN invoke THEN return Right with Unit`() = runTest {
        // GIVEN
        val userWalletId = UserWalletId("0A0B0C0D")
        val isEnabled = false
        coEvery { walletsRepository.setNotificationsEnabled(userWalletId, isEnabled) } just runs
        coEvery { accountsCRUDRepository.syncTokens(userWalletId) } just runs

        // WHEN
        val result = useCase(userWalletId, isEnabled)

        // THEN
        assertThat(result.isRight()).isTrue()
        coVerifyOrder {
            walletsRepository.setNotificationsEnabled(userWalletId, isEnabled)
            accountsCRUDRepository.syncTokens(userWalletId)
        }
    }

    @Test
    fun `GIVEN setNotificationsEnabled throws exception WHEN invoke THEN return Left and revert notifications`() = runTest {
        // GIVEN
        val userWalletId = UserWalletId("0A0B0C0D")
        val isEnabled = true
        val exception = RuntimeException("Network error")
        coEvery { walletsRepository.setNotificationsEnabled(userWalletId, isEnabled) } throws exception
        coEvery { walletsRepository.setNotificationsEnabled(userWalletId, !isEnabled) } just runs

        // WHEN
        val result = useCase(userWalletId, isEnabled)

        // THEN
        assertThat(result.isLeft()).isTrue()
        result.onLeft { throwable ->
            assertThat(throwable).isEqualTo(exception)
        }
        coVerify { walletsRepository.setNotificationsEnabled(userWalletId, !isEnabled) }
    }

    @Test
    fun `GIVEN syncTokens throws exception WHEN invoke THEN return Left and revert notifications`() = runTest {
        // GIVEN
        val userWalletId = UserWalletId("0A0B0C0D")
        val isEnabled = true
        val exception = RuntimeException("Sync error")
        coEvery { walletsRepository.setNotificationsEnabled(userWalletId, isEnabled) } just runs
        coEvery { accountsCRUDRepository.syncTokens(userWalletId) } throws exception
        coEvery { walletsRepository.setNotificationsEnabled(userWalletId, !isEnabled) } just runs

        // WHEN
        val result = useCase(userWalletId, isEnabled)

        // THEN
        assertThat(result.isLeft()).isTrue()
        result.onLeft { throwable ->
            assertThat(throwable).isEqualTo(exception)
        }
        coVerify { walletsRepository.setNotificationsEnabled(userWalletId, !isEnabled) }
    }

    @Test
    fun `GIVEN disabling notifications fails WHEN invoke THEN return Left and revert to enabled`() = runTest {
        // GIVEN
        val userWalletId = UserWalletId("0A0B0C0D")
        val isEnabled = false
        val exception = RuntimeException("Network error")
        coEvery { walletsRepository.setNotificationsEnabled(userWalletId, isEnabled) } throws exception
        coEvery { walletsRepository.setNotificationsEnabled(userWalletId, !isEnabled) } just runs

        // WHEN
        val result = useCase(userWalletId, isEnabled)

        // THEN
        assertThat(result.isLeft()).isTrue()
        result.onLeft { throwable ->
            assertThat(throwable).isEqualTo(exception)
        }
        coVerify { walletsRepository.setNotificationsEnabled(userWalletId, true) }
    }
}