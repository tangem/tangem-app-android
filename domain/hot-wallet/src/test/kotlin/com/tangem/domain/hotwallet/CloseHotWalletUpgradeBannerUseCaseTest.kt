package com.tangem.domain.hotwallet

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.hotwallet.repository.HotWalletRepository
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CloseHotWalletUpgradeBannerUseCaseTest {

    private val hotWalletRepository: HotWalletRepository = mockk(relaxed = true)
    private val useCase = CloseHotWalletUpgradeBannerUseCase(hotWalletRepository)

    private val walletId = UserWalletId("0123456789ABCDEF")

    @Test
    fun `WHEN invoke THEN set banner flag to false and closure timestamp`() = runTest {
        val result = useCase(walletId)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        coVerify { hotWalletRepository.setShouldShowUpgradeBanner(walletId, false) }
        coVerify { hotWalletRepository.setUpgradeBannerClosureTimestamp(walletId, any()) }
    }

    @Test
    fun `GIVEN repository throws exception WHEN invoke THEN return Either Left`() = runTest {
        val exception = RuntimeException("Test error")
        coEvery { hotWalletRepository.setShouldShowUpgradeBanner(walletId, false) } throws exception

        val result = useCase(walletId)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isEqualTo(exception)
    }
}