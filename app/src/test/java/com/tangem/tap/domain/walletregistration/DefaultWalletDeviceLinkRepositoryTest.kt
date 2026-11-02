package com.tangem.tap.domain.walletregistration

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.TangemSdkError
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.registration.WalletLinkError
import com.tangem.lib.auth.session.AuthError
import com.tangem.lib.auth.session.WalletRegistrar
import com.tangem.lib.auth.session.WalletRegistrationError
import com.tangem.lib.auth.session.WalletSigner
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class DefaultWalletDeviceLinkRepositoryTest {

    private val walletRegistrar: WalletRegistrar = mockk()
    private val mobileSigner: MobileWalletRegistrationSigner = mockk()
    private val coldSigner: ColdWalletRegistrationSigner = mockk()

    private val repository = DefaultWalletDeviceLinkRepository(
        walletRegistrar = walletRegistrar,
        mobileSigner = mobileSigner,
        coldSigner = coldSigner,
    )

    private val coldWallet = MockUserWalletFactory.create()
    private val twinWallet = MockUserWalletFactory.create().let { wallet ->
        wallet.copy(
            scanResponse = wallet.scanResponse.copy(card = wallet.scanResponse.card.copy(cardId = TWIN_CARD_ID)),
        )
    }
    private val ed25519Wallet = MockUserWalletFactory.create().let { wallet ->
        val card = wallet.scanResponse.card
        val ed25519Wallets = card.wallets.map { it.copy(curve = EllipticCurve.Ed25519) }
        wallet.copy(scanResponse = wallet.scanResponse.copy(card = card.copy(wallets = ed25519Wallets)))
    }
    private val unsupportedColdWallets = listOf(twinWallet, ed25519Wallet)
    private val hotWallet: UserWallet.Hot = mockk {
        every { walletId } returns UserWalletId(HOT_WALLET_ID_HEX)
    }

    @BeforeEach
    fun setup() {
        clearMocks(walletRegistrar, mobileSigner, coldSigner)
        mockkStatic(android.util.Base64::class)
        every { android.util.Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg())
        }
    }

    @AfterEach
    fun teardown() = unmockkAll()

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsLinked {

        @Test
        fun `GIVEN registered ids change WHEN isLinked collected THEN emits only changes of this wallet`() = runTest {
            // Arrange
            val registeredWalletIds = MutableStateFlow(emptySet<String>())
            every { walletRegistrar.registeredWalletIds() } returns registeredWalletIds
            val emissions = mutableListOf<Boolean>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                repository.isLinked(coldWallet).toList(emissions)
            }

            // Act
            registeredWalletIds.value = setOf(OTHER_WALLET_ID)
            registeredWalletIds.value = setOf(OTHER_WALLET_ID, coldWallet.base64Id())
            registeredWalletIds.value = setOf(coldWallet.base64Id())
            registeredWalletIds.value = emptySet()

            // Assert
            assertThat(emissions).containsExactly(false, true, false).inOrder()
        }

        @Test
        fun `GIVEN hot wallet registered WHEN isLinked collected THEN emits true`() = runTest {
            // Arrange
            every { walletRegistrar.registeredWalletIds() } returns MutableStateFlow(setOf(hotWallet.base64Id()))

            // Act
            val actual = mutableListOf<Boolean>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                repository.isLinked(hotWallet).toList(actual)
            }

            // Assert
            assertThat(actual).containsExactly(true)
        }

        @ParameterizedTest
        @MethodSource("provideUnsupportedWallets")
        fun `GIVEN unsupported cold wallet WHEN isLinked collected THEN emits true without reading registrations`(
            wallet: UserWallet.Cold,
        ) = runTest {
            // Act
            val actual = mutableListOf<Boolean>()
            repository.isLinked(wallet).toList(actual)

            // Assert
            assertThat(actual).containsExactly(true)
            verify(exactly = 0) { walletRegistrar.registeredWalletIds() }
        }

        private fun provideUnsupportedWallets() = unsupportedColdWallets
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Link {

        @Test
        fun `GIVEN hot wallet WHEN link THEN registers its Base64 id with the mobile signer`() = runTest {
            // Arrange
            val signer = mockk<WalletSigner>()
            every { mobileSigner.signerFor(hotWallet) } returns signer
            coEvery { walletRegistrar.register(any(), any()) } returns Unit.right()
            val expectedWalletId = hotWallet.base64Id()

            // Act
            val actual = repository.link(hotWallet)

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerify(exactly = 1) { walletRegistrar.register(walletId = expectedWalletId, signer = signer) }
        }

        @ParameterizedTest
        @MethodSource("provideUnsupportedWallets")
        fun `GIVEN unsupported cold wallet WHEN link THEN returns NotSupported without registering`(
            wallet: UserWallet.Cold,
        ) = runTest {
            // Act
            val actual = repository.link(wallet)

            // Assert
            assertThat(actual).isEqualTo(WalletLinkError.NotSupported.left())
            coVerify(exactly = 0) { walletRegistrar.register(any(), any()) }
        }

        private fun provideUnsupportedWallets() = unsupportedColdWallets

        @Test
        fun `GIVEN cold wallet WHEN link THEN registers its Base64 id with the card signer`() = runTest {
            // Arrange
            val signer = mockk<WalletSigner>()
            every { coldSigner.signerFor(coldWallet) } returns signer
            coEvery { walletRegistrar.register(any(), any()) } returns Unit.right()
            val expectedWalletId = coldWallet.base64Id()

            // Act
            val actual = repository.link(coldWallet)

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerify(exactly = 1) { walletRegistrar.register(walletId = expectedWalletId, signer = signer) }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN registration fails WHEN link THEN maps the error`(model: ErrorMappingModel) = runTest {
            // Arrange
            every { mobileSigner.signerFor(hotWallet) } returns mockk()
            coEvery { walletRegistrar.register(any(), any()) } returns model.registrationError.left()

            // Act
            val actual = repository.link(hotWallet)

            // Assert
            assertThat(actual).isEqualTo(model.expected.left())
        }

        private fun provideTestModels(): List<ErrorMappingModel> {
            val signingFailed = WalletRegistrationError.SigningFailed(IllegalStateException("no key"))
            val apiFailed = WalletRegistrationError.Api(AuthError.Forbidden(problem = null))
            return listOf(
                ErrorMappingModel(
                    registrationError = WalletRegistrationError.SigningFailed(TangemSdkError.UserCancelled()),
                    expected = WalletLinkError.Cancelled,
                ),
                ErrorMappingModel(
                    registrationError = signingFailed,
                    expected = WalletLinkError.Failed(reason = signingFailed.toString()),
                ),
                ErrorMappingModel(
                    registrationError = apiFailed,
                    expected = WalletLinkError.Failed(reason = apiFailed.toString()),
                ),
                ErrorMappingModel(
                    registrationError = WalletRegistrationError.NoActiveSession,
                    expected = WalletLinkError.Failed(reason = WalletRegistrationError.NoActiveSession.toString()),
                ),
            )
        }
    }

    data class ErrorMappingModel(
        val registrationError: WalletRegistrationError,
        val expected: WalletLinkError,
    )

    private fun UserWallet.base64Id(): String = java.util.Base64.getEncoder().encodeToString(walletId.value)

    private companion object {
        const val TWIN_CARD_ID = "CB61000000000001"
        const val HOT_WALLET_ID_HEX = "0A0B0C"
        const val OTHER_WALLET_ID = "other-wallet"
    }
}