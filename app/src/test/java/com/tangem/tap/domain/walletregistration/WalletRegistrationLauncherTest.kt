package com.tangem.tap.domain.walletregistration

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.domain.wallets.hot.HotWalletAccessor
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.lib.auth.AuthFeatureToggles
import com.tangem.lib.auth.session.WalletNonce
import com.tangem.lib.auth.session.WalletRegistrar
import com.tangem.lib.auth.session.WalletSigner
import com.tangem.utils.coroutines.AppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
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
    private val hotWalletAccessor: HotWalletAccessor = mockk(relaxed = true)
    private val appCoroutineScope: AppCoroutineScope = mockk(relaxed = true)

    private val launcher = WalletRegistrationLauncher(
        walletRegistrar = walletRegistrar,
        mobileSigner = mobileSigner,
        coldSigner = coldSigner,
        authFeatureToggles = authFeatureToggles,
        hotWalletAccessor = hotWalletAccessor,
        appCoroutineScope = appCoroutineScope,
    )

    @BeforeEach
    fun setup() {
        clearMocks(walletRegistrar, mobileSigner, authFeatureToggles, hotWalletAccessor)
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

    @Test
    fun `registerUnlockedMobileWallet establishes a context, registers, then clears the context`() = runTest {
        every { authFeatureToggles.isBackendAuthenticationEnabled } returns true
        val wallet = hotWallet(authType = HotWalletId.AuthType.Biometry)
        val hotWalletId = wallet.hotWalletId
        coEvery { walletRegistrar.isRegistered(any()) } returns false
        every { hotWalletAccessor.getContextualUnlock(hotWalletId) } returns null
        coEvery { hotWalletAccessor.unlockContextual(hotWalletId) } returns mockk()

        launcher.registerUnlockedMobileWallet(wallet)

        coVerify(exactly = 1) { hotWalletAccessor.unlockContextual(hotWalletId) }
        coVerify(exactly = 1) { walletRegistrar.register(any(), any()) }
        verify(exactly = 1) { hotWalletAccessor.clearContextualUnlock(hotWalletId) }
    }

    @Test
    fun `registerUnlockedMobileWallet skips an already-registered wallet without any auth`() = runTest {
        every { authFeatureToggles.isBackendAuthenticationEnabled } returns true
        val wallet = hotWallet(authType = HotWalletId.AuthType.Biometry)
        coEvery { walletRegistrar.isRegistered(any()) } returns true

        launcher.registerUnlockedMobileWallet(wallet)

        coVerify(exactly = 0) { hotWalletAccessor.unlockContextual(any()) }
        coVerify(exactly = 0) { walletRegistrar.register(any(), any()) }
    }

    @Test
    fun `registerUnlockedMobileWallet reuses an existing context and leaves it untouched`() = runTest {
        every { authFeatureToggles.isBackendAuthenticationEnabled } returns true
        val wallet = hotWallet(authType = HotWalletId.AuthType.Biometry)
        coEvery { walletRegistrar.isRegistered(any()) } returns false
        every { hotWalletAccessor.getContextualUnlock(wallet.hotWalletId) } returns mockk()

        launcher.registerUnlockedMobileWallet(wallet)

        coVerify(exactly = 1) { walletRegistrar.register(any(), any()) }
        coVerify(exactly = 0) { hotWalletAccessor.unlockContextual(any()) }
        verify(exactly = 0) { hotWalletAccessor.clearContextualUnlock(any<HotWalletId>()) }
    }

    @Test
    fun `registerColdInSession from Card is a no-op when backend auth is disabled`() = runTest {
        // The toggle is checked before the card is mapped, so no cold signing/prepare is attempted.
        every { authFeatureToggles.isBackendAuthenticationEnabled } returns false

        launcher.registerColdInSession(mockk<CardSession>(), mockk<Card>(), mockk<WalletNonce>())

        coVerify(exactly = 0) { walletRegistrar.prepare(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN forced registration without nonce WHEN registerColdInSession THEN drops marker only`() = runTest {
        // Arrange
        every { authFeatureToggles.isBackendAuthenticationEnabled } returns true
        stubColdCard()
        coEvery { walletRegistrar.forgetRegistration(any()) } returns Unit.right()

        // Act
        launcher.registerColdInSession(mockk<CardSession>(), mockk<Card>(relaxed = true), null, shouldForce = true)

        // Assert
        coVerify(exactly = 1) { walletRegistrar.forgetRegistration(COLD_WALLET_ID_BASE64) }
        coVerify(exactly = 0) { walletRegistrar.prepare(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN forced registration with nonce WHEN registerColdInSession THEN drops marker and prepares forced`() =
        runTest {
            // Arrange
            every { authFeatureToggles.isBackendAuthenticationEnabled } returns true
            stubColdCard()
            val nonce = mockk<WalletNonce>()
            coEvery { walletRegistrar.forgetRegistration(any()) } returns Unit.right()
            coEvery { walletRegistrar.prepare(any(), any(), any(), any()) } returns null.right()

            // Act
            launcher.registerColdInSession(mockk<CardSession>(), mockk<Card>(relaxed = true), nonce, shouldForce = true)

            // Assert
            coVerifyOrder {
                walletRegistrar.forgetRegistration(COLD_WALLET_ID_BASE64)
                walletRegistrar.prepare(COLD_WALLET_ID_BASE64, nonce, any(), true)
            }
        }

    @Test
    fun `GIVEN regular registration without nonce WHEN registerColdInSession THEN keeps marker`() = runTest {
        // Arrange
        every { authFeatureToggles.isBackendAuthenticationEnabled } returns true
        stubColdCard()

        // Act
        launcher.registerColdInSession(mockk<CardSession>(), mockk<Card>(relaxed = true), null)

        // Assert
        coVerify(exactly = 0) { walletRegistrar.forgetRegistration(any()) }
        coVerify(exactly = 0) { walletRegistrar.prepare(any(), any(), any(), any()) }
    }

    @Test
    fun `prefetchColdNonce is a no-op when backend auth is disabled`() = runTest {
        every { authFeatureToggles.isBackendAuthenticationEnabled } returns false

        val nonce = launcher.prefetchColdNonce()

        assertThat(nonce).isNull()
        coVerify(exactly = 0) { walletRegistrar.requestNonce() }
    }

    @Test
    fun `prefetchColdNonce returns the registrar nonce when enabled`() = runTest {
        every { authFeatureToggles.isBackendAuthenticationEnabled } returns true
        val expected = mockk<WalletNonce>()
        coEvery { walletRegistrar.requestNonce() } returns expected.right()

        val nonce = launcher.prefetchColdNonce()

        assertThat(nonce).isEqualTo(expected)
    }

    @Test
    fun `unregister is a no-op when backend auth is disabled`() = runTest {
        every { authFeatureToggles.isBackendAuthenticationEnabled } returns false

        launcher.unregister(UserWalletId(value = ByteArray(32) { 7 }))

        coVerify(exactly = 0) { walletRegistrar.unregister(any()) }
    }

    @Test
    fun `unregister calls registrar with the Base64 walletId`() = runTest {
        every { authFeatureToggles.isBackendAuthenticationEnabled } returns true
        val walletIdBytes = ByteArray(32) { 9 }
        val slot = slot<String>()
        coEvery { walletRegistrar.unregister(capture(slot)) } returns Unit.right()

        launcher.unregister(UserWalletId(value = walletIdBytes))

        assertThat(slot.captured).isEqualTo(java.util.Base64.getEncoder().encodeToString(walletIdBytes))
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

    private fun stubColdCard() {
        mockkObject(UserWalletIdBuilder.Companion)
        val walletIdBuilder = mockk<UserWalletIdBuilder> {
            every { build() } returns UserWalletId(value = COLD_WALLET_ID)
        }
        every { UserWalletIdBuilder.card(any<CardDTO>()) } returns walletIdBuilder
        every { coldSigner.signerFor(any(), any<CardDTO>()) } returns mockk()
    }

    private companion object {
        val COLD_WALLET_ID = ByteArray(32) { 3 }
        val COLD_WALLET_ID_BASE64: String = java.util.Base64.getEncoder().encodeToString(COLD_WALLET_ID)
    }
}