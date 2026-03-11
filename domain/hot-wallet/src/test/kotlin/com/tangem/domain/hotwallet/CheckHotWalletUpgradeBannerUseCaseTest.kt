package com.tangem.domain.hotwallet

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.hotwallet.repository.HotWalletRepository
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.TimeUnit

class CheckHotWalletUpgradeBannerUseCaseTest {

    private val hotWalletRepository: HotWalletRepository = mockk(relaxed = true)
    private val useCase = CheckHotWalletUpgradeBannerUseCase(hotWalletRepository)

    private val walletId = UserWalletId("0123456789ABCDEF")

    @Test
    fun `GIVEN creation timestamp is null WHEN invoke THEN set timestamp and return false`() = runTest {
        coEvery { hotWalletRepository.getWalletCreationTimestamp(walletId) } returns null
        coEvery { hotWalletRepository.hasHadFirstTopUp(walletId) } returns false
        every { hotWalletRepository.isFirstTopUpDetectedThisSession(walletId) } returns false

        val result = useCase(
            walletId = walletId,
            hasBalance = false,
            shouldShowUpgradeBanner = false,
            closureTimestamp = null,
        )

        assertThat(result).isInstanceOf(Either.Right::class.java)
        assertThat((result as Either.Right).value).isFalse()
        coVerify { hotWalletRepository.setWalletCreationTimestamp(walletId, any()) }
    }

    @Test
    fun `GIVEN shouldShowUpgradeBanner is true and hasBalance WHEN invoke THEN return true`() = runTest {
        val creationTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5)
        coEvery { hotWalletRepository.getWalletCreationTimestamp(walletId) } returns creationTimestamp
        coEvery { hotWalletRepository.hasHadFirstTopUp(walletId) } returns true
        every { hotWalletRepository.isFirstTopUpDetectedThisSession(walletId) } returns false

        val result = useCase(
            walletId = walletId,
            hasBalance = true,
            shouldShowUpgradeBanner = true,
            closureTimestamp = null,
        )

        assertThat(result).isInstanceOf(Either.Right::class.java)
        assertThat((result as Either.Right).value).isTrue()
    }

    @Test
    fun `GIVEN shouldShowUpgradeBanner is true WHEN invoke THEN return true regardless of balance`() = runTest {
        val creationTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5)
        coEvery { hotWalletRepository.getWalletCreationTimestamp(walletId) } returns creationTimestamp
        coEvery { hotWalletRepository.hasHadFirstTopUp(walletId) } returns true
        every { hotWalletRepository.isFirstTopUpDetectedThisSession(walletId) } returns false

        val result = useCase(
            walletId = walletId,
            hasBalance = false,
            shouldShowUpgradeBanner = true,
            closureTimestamp = null,
        )

        assertThat(result).isInstanceOf(Either.Right::class.java)
        assertThat((result as Either.Right).value).isTrue()
    }

    @Test
    fun `GIVEN closure timestamp exists and 30 days since closure WHEN invoke THEN return true`() = runTest {
        val creationTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(60)
        val closureTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(31)
        coEvery { hotWalletRepository.getWalletCreationTimestamp(walletId) } returns creationTimestamp
        coEvery { hotWalletRepository.hasHadFirstTopUp(walletId) } returns true
        every { hotWalletRepository.isFirstTopUpDetectedThisSession(walletId) } returns false

        val result = useCase(
            walletId = walletId,
            hasBalance = true,
            shouldShowUpgradeBanner = false,
            closureTimestamp = closureTimestamp,
        )

        assertThat(result).isInstanceOf(Either.Right::class.java)
        assertThat((result as Either.Right).value).isTrue()
    }

    @Test
    fun `GIVEN closure timestamp exists but less than 30 days since closure WHEN invoke THEN return false`() =
        runTest {
            val creationTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(60)
            val closureTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(15)
            coEvery { hotWalletRepository.getWalletCreationTimestamp(walletId) } returns creationTimestamp
            coEvery { hotWalletRepository.hasHadFirstTopUp(walletId) } returns true
            every { hotWalletRepository.isFirstTopUpDetectedThisSession(walletId) } returns false

            val result = useCase(
                walletId = walletId,
                hasBalance = true,
                shouldShowUpgradeBanner = false,
                closureTimestamp = closureTimestamp,
            )

            assertThat(result).isInstanceOf(Either.Right::class.java)
            assertThat((result as Either.Right).value).isFalse()
        }

    @Test
    fun `GIVEN no flags set and no closure and 30 days since creation WHEN invoke THEN return true`() = runTest {
        val creationTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(31)
        coEvery { hotWalletRepository.getWalletCreationTimestamp(walletId) } returns creationTimestamp
        coEvery { hotWalletRepository.hasHadFirstTopUp(walletId) } returns false
        every { hotWalletRepository.isFirstTopUpDetectedThisSession(walletId) } returns false

        val result = useCase(
            walletId = walletId,
            hasBalance = false,
            shouldShowUpgradeBanner = false,
            closureTimestamp = null,
        )

        assertThat(result).isInstanceOf(Either.Right::class.java)
        assertThat((result as Either.Right).value).isTrue()
    }

    @Test
    fun `GIVEN no flags set but less than 30 days since creation WHEN invoke THEN return false`() = runTest {
        val creationTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(15)
        coEvery { hotWalletRepository.getWalletCreationTimestamp(walletId) } returns creationTimestamp
        coEvery { hotWalletRepository.hasHadFirstTopUp(walletId) } returns false
        every { hotWalletRepository.isFirstTopUpDetectedThisSession(walletId) } returns false

        val result = useCase(
            walletId = walletId,
            hasBalance = false,
            shouldShowUpgradeBanner = false,
            closureTimestamp = null,
        )

        assertThat(result).isInstanceOf(Either.Right::class.java)
        assertThat((result as Either.Right).value).isFalse()
    }

    @Test
    fun `GIVEN no flags set but closure timestamp exists WHEN invoke THEN return false`() = runTest {
        val creationTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(60)
        val closureTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5)
        coEvery { hotWalletRepository.getWalletCreationTimestamp(walletId) } returns creationTimestamp
        coEvery { hotWalletRepository.hasHadFirstTopUp(walletId) } returns false
        every { hotWalletRepository.isFirstTopUpDetectedThisSession(walletId) } returns false

        val result = useCase(
            walletId = walletId,
            hasBalance = false,
            shouldShowUpgradeBanner = false,
            closureTimestamp = closureTimestamp,
        )

        assertThat(result).isInstanceOf(Either.Right::class.java)
        assertThat((result as Either.Right).value).isFalse()
    }

    @Test
    fun `GIVEN first top-up detected WHEN invoke THEN return false and mark session`() = runTest {
        val creationTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5)
        coEvery { hotWalletRepository.getWalletCreationTimestamp(walletId) } returns creationTimestamp
        coEvery { hotWalletRepository.hasHadFirstTopUp(walletId) } returns false
        every { hotWalletRepository.isFirstTopUpDetectedThisSession(walletId) } returns false

        val result = useCase(
            walletId = walletId,
            hasBalance = true,
            shouldShowUpgradeBanner = false,
            closureTimestamp = null,
        )

        assertThat(result).isInstanceOf(Either.Right::class.java)
        assertThat((result as Either.Right).value).isFalse()
        coVerify { hotWalletRepository.setHasHadFirstTopUp(walletId, true) }
        coVerify { hotWalletRepository.setShouldShowUpgradeBanner(walletId, true) }
        coVerify { hotWalletRepository.setUpgradeBannerClosureTimestamp(walletId, null) }
        verify { hotWalletRepository.markFirstTopUpDetectedThisSession(walletId) }
    }

    @Test
    fun `GIVEN first top-up detected this session WHEN invoke THEN return false`() = runTest {
        val creationTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5)
        coEvery { hotWalletRepository.getWalletCreationTimestamp(walletId) } returns creationTimestamp
        coEvery { hotWalletRepository.hasHadFirstTopUp(walletId) } returns true
        every { hotWalletRepository.isFirstTopUpDetectedThisSession(walletId) } returns true

        val result = useCase(
            walletId = walletId,
            hasBalance = true,
            shouldShowUpgradeBanner = true,
            closureTimestamp = null,
        )

        assertThat(result).isInstanceOf(Either.Right::class.java)
        assertThat((result as Either.Right).value).isFalse()
    }

    @Test
    fun `GIVEN already had first top-up WHEN invoke with balance THEN do not set flags again`() = runTest {
        val creationTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5)
        coEvery { hotWalletRepository.getWalletCreationTimestamp(walletId) } returns creationTimestamp
        coEvery { hotWalletRepository.hasHadFirstTopUp(walletId) } returns true
        every { hotWalletRepository.isFirstTopUpDetectedThisSession(walletId) } returns false

        useCase(
            walletId = walletId,
            hasBalance = true,
            shouldShowUpgradeBanner = true,
            closureTimestamp = null,
        )

        coVerify(exactly = 0) { hotWalletRepository.setHasHadFirstTopUp(any(), any()) }
        coVerify(exactly = 0) { hotWalletRepository.setShouldShowUpgradeBanner(any(), any()) }
    }

    @Test
    fun `GIVEN multiple re-emissions with same state WHEN invoke THEN return same result`() = runTest {
        val creationTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(31)
        coEvery { hotWalletRepository.getWalletCreationTimestamp(walletId) } returns creationTimestamp
        coEvery { hotWalletRepository.hasHadFirstTopUp(walletId) } returns false
        every { hotWalletRepository.isFirstTopUpDetectedThisSession(walletId) } returns false

        val result1 = useCase(
            walletId = walletId,
            hasBalance = false,
            shouldShowUpgradeBanner = false,
            closureTimestamp = null,
        )
        val result2 = useCase(
            walletId = walletId,
            hasBalance = false,
            shouldShowUpgradeBanner = false,
            closureTimestamp = null,
        )
        val result3 = useCase(
            walletId = walletId,
            hasBalance = false,
            shouldShowUpgradeBanner = false,
            closureTimestamp = null,
        )

        assertThat((result1 as Either.Right).value).isTrue()
        assertThat((result2 as Either.Right).value).isTrue()
        assertThat((result3 as Either.Right).value).isTrue()
    }
}