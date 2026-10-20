package com.tangem.data.wallets.hot

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.settings.repositories.LegacySettingsRepository
import com.tangem.domain.wallets.hot.HotWalletPasswordRequester
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.UnlockHotWallet
import com.tangem.utils.coroutines.AppCoroutineScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultHotWalletAccessorTest {

    private val tangemHotSdk: TangemHotSdk = mockk(relaxUnitFun = true)
    private val hotWalletPasswordRequester: HotWalletPasswordRequester = mockk(relaxUnitFun = true)
    private val walletsRepository: WalletsRepository = mockk()
    private val legacySettingsRepository: LegacySettingsRepository = mockk()

    @Test
    fun `GIVEN the request fails after storing WHEN unlockContextual THEN the stored unlock is removed`() =
        runTest {
            // Arrange
            val sut = createAccessor()
            val stored = UnlockHotWallet(HOT_WALLET_ID, HotAuth.NoAuth)
            coEvery { tangemHotSdk.getContextUnlock(any()) } returns stored
            coEvery { hotWalletPasswordRequester.dismiss() } throws IllegalStateException("dismiss failed")

            // Act
            val actual = runCatching { sut.unlockContextual(HOT_WALLET_ID) }.exceptionOrNull()
            runCurrent()

            // Assert
            assertThat(actual).isInstanceOf(IllegalStateException::class.java)
            assertThat(sut.getContextualUnlock(HOT_WALLET_ID)).isNull()
            coVerify(exactly = 1) { tangemHotSdk.clearUnlockContext(HOT_WALLET_ID) }
        }

    @Test
    fun `GIVEN the request succeeds WHEN unlockContextual THEN the unlock stays stored`() = runTest {
        // Arrange
        val sut = createAccessor()
        val stored = UnlockHotWallet(HOT_WALLET_ID, HotAuth.NoAuth)
        coEvery { tangemHotSdk.getContextUnlock(any()) } returns stored

        // Act
        val actual = sut.unlockContextual(HOT_WALLET_ID)
        runCurrent()

        // Assert
        assertThat(actual).isSameInstanceAs(stored)
        assertThat(sut.getContextualUnlock(HOT_WALLET_ID)).isSameInstanceAs(stored)
        coVerify(exactly = 0) { tangemHotSdk.clearUnlockContext(any()) }
    }

    private fun TestScope.createAccessor(): DefaultHotWalletAccessor {
        coEvery { walletsRepository.requireAccessCode() } returns false
        coEvery { walletsRepository.useBiometricAuthentication() } returns false
        coEvery { legacySettingsRepository.canUseBiometryStrict() } returns true
        return DefaultHotWalletAccessor(
            tangemHotSdk = tangemHotSdk,
            userWalletsListRepository = mockk<UserWalletsListRepository>(),
            hotWalletPasswordRequester = hotWalletPasswordRequester,
            walletsRepository = walletsRepository,
            legacySettingsRepository = legacySettingsRepository,
            scope = object : AppCoroutineScope, CoroutineScope by backgroundScope {},
        )
    }

    private companion object {
        val HOT_WALLET_ID = HotWalletId(value = "hot", authType = HotWalletId.AuthType.NoPassword)
    }
}