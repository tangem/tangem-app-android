package com.tangem.tap.domain.walletregistration

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.lib.auth.AuthFeatureToggles
import com.tangem.lib.auth.session.WalletRegistrar
import com.tangem.lib.auth.session.WalletSigner
import com.tangem.utils.coroutines.AppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WalletRegistrationLauncherTest {

    private val walletRegistrar: WalletRegistrar = mockk()
    private val mobileSigner: MobileWalletRegistrationSigner = mockk()
    private val coldSigner: ColdWalletRegistrationSigner = mockk()
    private val authFeatureToggles: AuthFeatureToggles = mockk()
    private val appCoroutineScope: AppCoroutineScope = mockk(relaxed = true)

    private val launcher = WalletRegistrationLauncher(
        walletRegistrar = walletRegistrar,
        mobileSigner = mobileSigner,
        coldSigner = coldSigner,
        authFeatureToggles = authFeatureToggles,
        appCoroutineScope = appCoroutineScope,
    )

    @BeforeEach
    fun setup() {
        clearMocks(walletRegistrar, mobileSigner, authFeatureToggles)
        mockkStatic(android.util.Base64::class)
        every { android.util.Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg())
        }
        every { mobileSigner.signerFor(any()) } returns mockk<WalletSigner>()
        coEvery { walletRegistrar.register(any(), any()) } returns Unit.right()
    }

    @AfterEach
    fun teardown() = unmockkAll()

    @Test
    fun `registerMobile is a no-op when backend auth is disabled`() = runTest {
        every { authFeatureToggles.isBackendAuthenticationEnabled } returns false

        launcher.registerMobile(hotWallet())

        coVerify(exactly = 0) { walletRegistrar.register(any(), any()) }
    }

    @Test
    fun `registerMobile registers with the Base64 walletId`() = runTest {
        every { authFeatureToggles.isBackendAuthenticationEnabled } returns true
        val walletIdBytes = ByteArray(32) { 5 }
        val slot = slot<String>()
        coEvery { walletRegistrar.register(capture(slot), any()) } returns Unit.right()

        launcher.registerMobile(hotWallet(walletIdBytes))

        assertThat(slot.captured).isEqualTo(java.util.Base64.getEncoder().encodeToString(walletIdBytes))
    }

    @Test
    fun `retryMobileRegistrations registers only silently-signable hot wallets`() = runTest {
        every { authFeatureToggles.isBackendAuthenticationEnabled } returns true

        launcher.retryMobileRegistrations(
            listOf(
                hotWallet(authType = HotWalletId.AuthType.NoPassword), // registered — signs silently
                hotWallet(authType = HotWalletId.AuthType.Password), // skipped — would prompt
                hotWallet(authType = HotWalletId.AuthType.Biometry), // skipped — would prompt
                mockk<UserWallet.Cold>(), // skipped — not a hot wallet
            ),
        )

        coVerify(exactly = 1) { walletRegistrar.register(any(), any()) }
    }

    private fun hotWallet(
        walletIdValue: ByteArray = ByteArray(32) { 1 },
        authType: HotWalletId.AuthType = HotWalletId.AuthType.NoPassword,
    ): UserWallet.Hot {
        val hotWalletId = mockk<HotWalletId> { every { this@mockk.authType } returns authType }
        val wallet = mockk<UserWallet.Hot>()
        every { wallet.walletId } returns UserWalletId(value = walletIdValue)
        every { wallet.hotWalletId } returns hotWalletId
        return wallet
    }
}