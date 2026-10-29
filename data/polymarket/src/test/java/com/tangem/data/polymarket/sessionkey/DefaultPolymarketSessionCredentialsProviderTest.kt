package com.tangem.data.polymarket.sessionkey

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.data.polymarket.signing.PolymarketDigestFactory
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketClobIdentity
import com.tangem.domain.polymarket.model.PolymarketL1Headers
import com.tangem.domain.polymarket.model.PolymarketSessionCredentialsError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStore
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultPolymarketSessionCredentialsProviderTest {

    private val sessionKeyStore: PolymarketSessionKeyStore = mockk()
    private val repository: PolymarketRepository = mockk()

    private lateinit var provider: DefaultPolymarketSessionCredentialsProvider

    @BeforeEach
    fun setUp() {
        clearMocks(sessionKeyStore, repository)
        provider = DefaultPolymarketSessionCredentialsProvider(
            sessionKeyStore = sessionKeyStore,
            polymarketRepository = repository,
            digestFactory = PolymarketDigestFactory(),
        )
        coEvery { sessionKeyStore.heldAddress(WALLET_ID) } returns KEY_ADDRESS
        coEvery { sessionKeyStore.sign(WALLET_ID, any(), any()) } returns SIGNATURE.right()
    }

    @Test
    fun `GIVEN no session key WHEN get THEN reports the missing key and asks the CLOB nothing`() = runTest {
        // Arrange
        coEvery { sessionKeyStore.heldAddress(WALLET_ID) } returns null

        // Act
        val actual = provider.get(WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(PolymarketSessionCredentialsError.Key(PolymarketSessionKeyError.KeyMissing).left())
        coVerify(exactly = 0) { repository.deriveApiCredentials(any()) }
    }

    @Test
    fun `GIVEN the key cannot sign WHEN get THEN reports the key error and asks the CLOB nothing`() = runTest {
        // Arrange
        val error = PolymarketSessionKeyError.Failure(IllegalStateException("keystore"))
        coEvery { sessionKeyStore.sign(WALLET_ID, any(), any()) } returns error.left()

        // Act
        val actual = provider.get(WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(PolymarketSessionCredentialsError.Key(error).left())
        coVerify(exactly = 0) { repository.deriveApiCredentials(any()) }
    }

    @Test
    fun `GIVEN derive succeeds WHEN get THEN the key signs ClobAuth for its own address with nonce 0`() = runTest {
        // Arrange
        val headers = slot<PolymarketL1Headers>()
        coEvery { repository.deriveApiCredentials(capture(headers)) } returns CREDENTIALS.right()

        // Act
        val actual = provider.get(WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(PolymarketClobIdentity.Session(address = KEY_ADDRESS, credentials = CREDENTIALS).right())
        assertThat(headers.captured.address).isEqualTo(KEY_ADDRESS)
        assertThat(headers.captured.nonce).isEqualTo("0")
        assertThat(headers.captured.signature).isEqualTo("0x" + "1b".repeat(SIGNATURE.size))
        coVerify(exactly = 1) { sessionKeyStore.sign(WALLET_ID, KEY_ADDRESS, any()) }
        coVerify(exactly = 0) { repository.createApiCredentials(any()) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN derive refused WHEN get THEN creates only on a client refusal`(model: FallbackModel) = runTest {
        // Arrange
        coEvery { repository.deriveApiCredentials(any()) } returns model.deriveError.left()
        coEvery { repository.createApiCredentials(any()) } returns CREDENTIALS.right()

        // Act
        val actual = provider.get(WALLET_ID)

        // Assert
        if (model.isCreated) {
            assertThat(actual).isEqualTo(
                PolymarketClobIdentity.Session(address = KEY_ADDRESS, credentials = CREDENTIALS).right(),
            )
        } else {
            assertThat(actual).isEqualTo(PolymarketSessionCredentialsError.Auth(model.deriveError).left())
        }
        coVerify(exactly = if (model.isCreated) 1 else 0) { repository.createApiCredentials(any()) }
    }

    @Test
    fun `GIVEN derive and create both refused WHEN get THEN reports the create error`() = runTest {
        // Arrange
        coEvery { repository.deriveApiCredentials(any()) } returns PolymarketAuthError.KeyNotFound.left()
        coEvery { repository.createApiCredentials(any()) } returns PolymarketAuthError.Network.left()

        // Act
        val actual = provider.get(WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(PolymarketSessionCredentialsError.Auth(PolymarketAuthError.Network).left())
    }

    @Test
    fun `GIVEN credentials obtained WHEN get again THEN serves them from memory`() = runTest {
        // Arrange
        coEvery { repository.deriveApiCredentials(any()) } returns CREDENTIALS.right()
        val first = provider.get(WALLET_ID)

        // Act
        val second = provider.get(WALLET_ID)

        // Assert
        assertThat(second).isEqualTo(first)
        coVerify(exactly = 1) { repository.deriveApiCredentials(any()) }
        coVerify(exactly = 1) { sessionKeyStore.sign(WALLET_ID, any(), any()) }
    }

    @Test
    fun `GIVEN the key was replaced WHEN get THEN derives for the new key`() = runTest {
        // Arrange
        coEvery { repository.deriveApiCredentials(any()) } returnsMany listOf(CREDENTIALS.right(), OTHER.right())
        provider.get(WALLET_ID)
        coEvery { sessionKeyStore.heldAddress(WALLET_ID) } returns OTHER_KEY_ADDRESS

        // Act
        val actual = provider.get(WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(PolymarketClobIdentity.Session(address = OTHER_KEY_ADDRESS, credentials = OTHER).right())
        coVerify(exactly = 2) { repository.deriveApiCredentials(any()) }
    }

    @Test
    fun `GIVEN a 401 on the session credentials WHEN invalidate THEN the next get derives again`() = runTest {
        // Arrange
        coEvery { repository.deriveApiCredentials(any()) } returnsMany listOf(CREDENTIALS.right(), OTHER.right())
        val refused = provider.get(WALLET_ID).getOrNull()!!

        // Act
        provider.invalidate(WALLET_ID, refused)
        val actual = provider.get(WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(PolymarketClobIdentity.Session(address = KEY_ADDRESS, credentials = OTHER).right())
        coVerify(exactly = 2) { repository.deriveApiCredentials(any()) }
    }

    @Test
    fun `GIVEN newer credentials cached WHEN a stale identity is invalidated THEN the newer ones are kept`() = runTest {
        // Arrange
        coEvery { repository.deriveApiCredentials(any()) } returnsMany listOf(CREDENTIALS.right(), OTHER.right())
        val stale = provider.get(WALLET_ID).getOrNull()!!
        provider.invalidate(WALLET_ID, stale)
        val fresh = provider.get(WALLET_ID)

        // Act
        provider.invalidate(WALLET_ID, stale)
        val actual = provider.get(WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(fresh)
        coVerify(exactly = 2) { repository.deriveApiCredentials(any()) }
    }

    @Test
    fun `GIVEN credentials of one wallet WHEN another wallet asks THEN it derives its own`() = runTest {
        // Arrange
        coEvery { sessionKeyStore.heldAddress(OTHER_WALLET_ID) } returns KEY_ADDRESS
        coEvery { sessionKeyStore.sign(OTHER_WALLET_ID, any(), any()) } returns SIGNATURE.right()
        coEvery { repository.deriveApiCredentials(any()) } returnsMany listOf(CREDENTIALS.right(), OTHER.right())
        provider.get(WALLET_ID)

        // Act
        val actual = provider.get(OTHER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(PolymarketClobIdentity.Session(address = KEY_ADDRESS, credentials = OTHER).right())
    }

    data class FallbackModel(val deriveError: PolymarketAuthError, val isCreated: Boolean)

    private fun provideTestModels() = listOf(
        FallbackModel(deriveError = PolymarketAuthError.KeyNotFound, isCreated = true),
        FallbackModel(deriveError = PolymarketAuthError.InvalidSignature, isCreated = true),
        FallbackModel(deriveError = PolymarketAuthError.Unknown(httpCode = 400, detail = null), isCreated = true),
        FallbackModel(deriveError = PolymarketAuthError.Unknown(httpCode = 403, detail = null), isCreated = true),
        FallbackModel(deriveError = PolymarketAuthError.RateLimited, isCreated = false),
        FallbackModel(deriveError = PolymarketAuthError.Network, isCreated = false),
        FallbackModel(deriveError = PolymarketAuthError.Unknown(httpCode = 500, detail = null), isCreated = false),
        FallbackModel(deriveError = PolymarketAuthError.Unknown(httpCode = null, detail = null), isCreated = false),
    )

    private companion object {
        val WALLET_ID = UserWalletId("11")
        val OTHER_WALLET_ID = UserWalletId("22")
        const val KEY_ADDRESS = "0x5e55000000000000000000000000000000000001"
        const val OTHER_KEY_ADDRESS = "0x5e55000000000000000000000000000000000002"
        val SIGNATURE = ByteArray(size = 65) { 0x1b }
        val CREDENTIALS = PolymarketApiCredentials(apiKey = "k", secret = "s", passphrase = "p")
        val OTHER = PolymarketApiCredentials(apiKey = "k2", secret = "s2", passphrase = "p2")
    }
}