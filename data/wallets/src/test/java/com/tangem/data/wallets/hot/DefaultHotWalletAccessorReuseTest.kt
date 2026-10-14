package com.tangem.data.wallets.hot

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.settings.repositories.LegacySettingsRepository
import com.tangem.domain.wallets.hot.HotWalletPasswordRequester
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.DeriveWalletRequest
import com.tangem.hot.sdk.model.DerivedPublicKeyResponse
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.UnlockHotWallet
import com.tangem.utils.coroutines.AppCoroutineScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultHotWalletAccessorReuseTest {

    private val tangemHotSdk: TangemHotSdk = mockk()
    private val passwordRequester: HotWalletPasswordRequester = mockk()

    private val accessor = DefaultHotWalletAccessor(
        tangemHotSdk = tangemHotSdk,
        userWalletsListRepository = mockk<UserWalletsListRepository>(),
        hotWalletPasswordRequester = passwordRequester,
        walletsRepository = mockk<WalletsRepository>(),
        legacySettingsRepository = mockk<LegacySettingsRepository>(),
        scope = mockk<AppCoroutineScope>(relaxed = true),
    )

    private val hotWalletId: HotWalletId = mockk(relaxed = true)
    private val request = DeriveWalletRequest(requests = emptyList())
    private val response: DerivedPublicKeyResponse = mockk()

    @Test
    fun `GIVEN a contextual unlock WHEN derivePublicKeysReusingContextualUnlock THEN derives with it without a prompt`() =
        runTest {
            // Arrange
            val unlock = UnlockHotWallet(hotWalletId, HotAuth.NoAuth)
            coEvery { tangemHotSdk.getContextUnlock(any()) } returns unlock
            accessor.unlockContextual(hotWalletId, HotAuth.NoAuth)
            coEvery { tangemHotSdk.derivePublicKey(unlockHotWallet = unlock, request = request) } returns response

            // Act
            val actual = accessor.derivePublicKeysReusingContextualUnlock(hotWalletId, request)

            // Assert
            assertThat(actual).isSameInstanceAs(response)
            coVerify(exactly = 0) { passwordRequester.requestPassword(any()) }
        }
}