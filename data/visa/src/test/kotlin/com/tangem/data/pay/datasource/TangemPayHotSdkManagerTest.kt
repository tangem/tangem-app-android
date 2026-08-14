package com.tangem.data.pay.datasource

import arrow.core.Either
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.WithdrawalSignatureResult
import com.tangem.domain.visa.datasource.TangemPayRemoteDataSource
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.wallets.hot.HotWalletAccessor
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.HotWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TangemPayHotSdkManagerTest {

    private val hotWalletAccessor: HotWalletAccessor = mockk(relaxUnitFun = true)
    private val tangemHotSdk: TangemHotSdk = mockk()
    private val tangemPayAuthRemoteDataSource: TangemPayRemoteDataSource = mockk()

    private val manager = TangemPayHotSdkManager(
        hotWalletAccessor = hotWalletAccessor,
        tangemHotSdk = tangemHotSdk,
        tangemPayAuthRemoteDataSource = tangemPayAuthRemoteDataSource,
    )

    private val hotWalletId = HotWalletId(value = "hotWalletId", authType = HotWalletId.AuthType.Password)
    private val hotWallet: UserWallet.Hot = mockk {
        every { hotWalletId } returns this@TangemPayHotSdkManagerTest.hotWalletId
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(hotWalletAccessor, tangemHotSdk, tangemPayAuthRemoteDataSource)
    }

    @Test
    fun `GIVEN password prompt dismissed WHEN getWithdrawalSignature THEN returns Cancelled`() = runTest {
        // Arrange
        coEvery { hotWalletAccessor.getContextualUnlock(hotWalletId) } returns null
        coEvery { hotWalletAccessor.unlockContextual(hotWalletId) } throws TangemSdkError.UserCancelled()

        // Act
        val actual = manager.getWithdrawalSignature(hotWallet = hotWallet, hash = HASH)

        // Assert
        assertThat(actual).isEqualTo(Either.Right(WithdrawalSignatureResult.Cancelled))
        coVerify(exactly = 1) { hotWalletAccessor.clearContextualUnlock(hotWalletId) }
    }

    @Test
    fun `GIVEN unlock fails with other error WHEN getWithdrawalSignature THEN returns Left`() = runTest {
        // Arrange
        val error = IllegalStateException("keystore failure")
        coEvery { hotWalletAccessor.getContextualUnlock(hotWalletId) } returns null
        coEvery { hotWalletAccessor.unlockContextual(hotWalletId) } throws error

        // Act
        val actual = manager.getWithdrawalSignature(hotWallet = hotWallet, hash = HASH)

        // Assert
        assertThat(actual).isEqualTo(Either.Left(error))
        coVerify(exactly = 1) { hotWalletAccessor.clearContextualUnlock(hotWalletId) }
    }

    @Test
    fun `GIVEN coroutine cancellation WHEN getWithdrawalSignature THEN cancellation propagates`() = runTest {
        // Arrange
        coEvery { hotWalletAccessor.getContextualUnlock(hotWalletId) } returns null
        coEvery { hotWalletAccessor.unlockContextual(hotWalletId) } throws CancellationException("real cancel")

        // Act
        val actual = runCatching { manager.getWithdrawalSignature(hotWallet = hotWallet, hash = HASH) }
            .exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun `GIVEN password prompt dismissed WHEN produceInitialCredentials THEN returns Left without throwing`() =
        runTest {
            // Arrange
            val error = TangemSdkError.UserCancelled()
            coEvery { hotWalletAccessor.getContextualUnlock(hotWalletId) } returns null
            coEvery { hotWalletAccessor.unlockContextual(hotWalletId) } throws error

            // Act
            val actual = manager.produceInitialCredentials(hotWallet = hotWallet)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(error))
            coVerify(exactly = 1) { hotWalletAccessor.clearContextualUnlock(hotWalletId) }
        }

    private companion object {
        const val HASH = "FFAA"
    }
}