package com.tangem.data.collectibles.delegate

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.blockchain.extensions.encodeBase58
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.data.collectibles.account.store.CollectiblesAccountStore
import com.tangem.domain.collectibles.delegate.HotDelegatePublicKey
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.DerivedPublicKeyResponse
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.SignedData
import com.tangem.hot.sdk.model.UnlockHotWallet
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.tangem.domain.collectibles.CollectiblesAccountError

internal class DefaultCollectiblesHotDelegateTest {

    private val tangemHotSdk: TangemHotSdk = mockk()
    private val accountStore: CollectiblesAccountStore = mockk(relaxUnitFun = true)

    private val delegate = DefaultCollectiblesHotDelegate(
        tangemHotSdk = tangemHotSdk,
        accountStore = accountStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(tangemHotSdk, accountStore)
    }

    @Test
    fun `GIVEN nothing stored WHEN provision THEN wallet is generated and its key stored`() = runTest {
        // Arrange
        coEvery { accountStore.getDelegate(WALLET_ID) } returns null
        coEvery { tangemHotSdk.generateWallet(any(), any()) } returns HOT_WALLET_ID
        coEvery { tangemHotSdk.derivePublicKey(any(), any()) } returns derivedResponse()

        // Act
        val actual = delegate.provision(ACCOUNT_ID)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(HotDelegatePublicKey(PUBLIC_KEY.encodeBase58()))
        coVerify(exactly = 1) {
            accountStore.storeDelegate(userWalletId = WALLET_ID, hotWalletId = HOT_WALLET_ID.value)
        }
    }

    @Test
    fun `GIVEN key already stored WHEN provision THEN it is re-derived and no wallet is generated`() = runTest {
        // Arrange
        coEvery { accountStore.getDelegate(WALLET_ID) } returns RECORD
        coEvery { tangemHotSdk.derivePublicKey(any(), any()) } returns derivedResponse()

        // Act
        val actual = delegate.provision(ACCOUNT_ID)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(HotDelegatePublicKey(PUBLIC_KEY.encodeBase58()))
        coVerify(exactly = 0) {
            tangemHotSdk.generateWallet(any(), any())
            accountStore.storeDelegate(any(), any())
        }
    }

    @Test
    fun `GIVEN the record cannot be persisted WHEN provision THEN the generated wallet is deleted`() = runTest {
        // Arrange
        val cause = IllegalStateException("disk is full")
        coEvery { accountStore.getDelegate(WALLET_ID) } returns null
        coEvery { tangemHotSdk.generateWallet(any(), any()) } returns HOT_WALLET_ID
        coEvery { tangemHotSdk.derivePublicKey(any(), any()) } returns derivedResponse()
        coEvery { accountStore.storeDelegate(any(), any()) } throws cause

        // Act
        val actual = delegate.provision(ACCOUNT_ID)

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(CollectiblesAccountError.DelegateKeyCreationFailed(cause))
        coVerify(exactly = 1) { tangemHotSdk.delete(HOT_WALLET_ID) }
    }

    @Test
    fun `GIVEN generation throws WHEN provision THEN nothing is stored`() = runTest {
        // Arrange
        val cause = IllegalStateException("keystore unavailable")
        coEvery { accountStore.getDelegate(WALLET_ID) } returns null
        coEvery { tangemHotSdk.generateWallet(any(), any()) } throws cause

        // Act
        val actual = delegate.provision(ACCOUNT_ID)

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(CollectiblesAccountError.DelegateKeyCreationFailed(cause))
        coVerify(exactly = 0) { accountStore.storeDelegate(any(), any()) }
    }

    @Test
    fun `GIVEN derivation throws WHEN provision THEN the generated wallet is deleted`() = runTest {
        // Arrange
        val cause = IllegalStateException("derivation failed")
        coEvery { accountStore.getDelegate(WALLET_ID) } returns null
        coEvery { tangemHotSdk.generateWallet(any(), any()) } returns HOT_WALLET_ID
        coEvery { tangemHotSdk.derivePublicKey(any(), any()) } throws cause
        coEvery { tangemHotSdk.delete(any()) } returns Unit

        // Act
        val actual = delegate.provision(ACCOUNT_ID)

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(CollectiblesAccountError.DelegateKeyCreationFailed(cause))
        coVerify(exactly = 1) { tangemHotSdk.delete(HOT_WALLET_ID) }
        coVerify(exactly = 0) { accountStore.storeDelegate(any(), any()) }
    }

    @Test
    fun `GIVEN keystore delete fails WHEN forget THEN the record is kept for a retry`() = runTest {
        // Arrange
        coEvery { accountStore.getDelegate(WALLET_ID) } returns RECORD
        coEvery { tangemHotSdk.delete(any()) } throws IllegalStateException("keystore unavailable")

        // Act
        delegate.forget(ACCOUNT_ID)

        // Assert
        coVerify(exactly = 0) { accountStore.removeDelegate(WALLET_ID) }
    }

    @Test
    fun `GIVEN another account's delegate stored WHEN provision THEN this account gets its own`() = runTest {
        // Arrange
        coEvery { accountStore.getDelegate(WALLET_ID) } returns null
        coEvery { accountStore.getDelegate(OTHER_WALLET_ID) } returns RECORD
        coEvery { tangemHotSdk.generateWallet(any(), any()) } returns HOT_WALLET_ID
        coEvery { tangemHotSdk.derivePublicKey(any(), any()) } returns derivedResponse()

        // Act
        val actual = delegate.provision(ACCOUNT_ID)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(HotDelegatePublicKey(PUBLIC_KEY.encodeBase58()))
        coVerify(exactly = 1) { accountStore.storeDelegate(WALLET_ID, any()) }
        coVerify(exactly = 0) { accountStore.storeDelegate(OTHER_WALLET_ID, any()) }
    }

    @Test
    fun `GIVEN provision in flight WHEN provision again THEN the second call reuses the first key`() = runTest {
        // Arrange
        var stored: String? = null
        val generationStarted = CompletableDeferred<Unit>()
        val generationRelease = CompletableDeferred<Unit>()

        coEvery { accountStore.getDelegate(WALLET_ID) } answers { stored }
        coEvery { accountStore.storeDelegate(WALLET_ID, any()) } answers { stored = secondArg() }
        coEvery { tangemHotSdk.derivePublicKey(any(), any()) } returns derivedResponse()
        coEvery { tangemHotSdk.generateWallet(any(), any()) } coAnswers {
            generationStarted.complete(Unit)
            generationRelease.await()
            HOT_WALLET_ID
        }

        // Act
        val first = async { delegate.provision(ACCOUNT_ID) }
        generationStarted.await()
        val second = async { delegate.provision(ACCOUNT_ID) }
        advanceUntilIdle()
        generationRelease.complete(Unit)

        // Assert
        val expected = HotDelegatePublicKey(PUBLIC_KEY.encodeBase58())
        assertThat(first.await().getOrNull()).isEqualTo(expected)
        assertThat(second.await().getOrNull()).isEqualTo(expected)
        coVerify(exactly = 1) { tangemHotSdk.generateWallet(any(), any()) }
        coVerify(exactly = 1) { accountStore.storeDelegate(WALLET_ID, any()) }
        coVerify(exactly = 0) { tangemHotSdk.delete(any()) }
    }

    @Test
    fun `GIVEN nothing stored WHEN sign THEN reports not provisioned without calling the sdk`() = runTest {
        // Arrange
        coEvery { accountStore.getDelegate(WALLET_ID) } returns null

        // Act
        val actual = delegate.sign(ACCOUNT_ID, MESSAGE)

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(CollectiblesAccountError.DelegateNotProvisioned)
        coVerify(exactly = 0) { tangemHotSdk.signHashes(any(), any()) }
    }

    @Test
    fun `GIVEN key stored WHEN sign THEN signs with no auth`() = runTest {
        // Arrange
        val unlock = slot<UnlockHotWallet>()
        coEvery { accountStore.getDelegate(WALLET_ID) } returns RECORD
        coEvery { tangemHotSdk.signHashes(capture(unlock), any()) } returns listOf(
            SignedData(curve = CURVE, derivationPath = DERIVATION_PATH, signatures = listOf(SIGNATURE)),
        )

        // Act
        val actual = delegate.sign(ACCOUNT_ID, MESSAGE)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(SIGNATURE)
        assertThat(unlock.captured.auth).isEqualTo(HotAuth.NoAuth)
        assertThat(unlock.captured.walletId).isEqualTo(HOT_WALLET_ID)
    }

    @Test
    fun `GIVEN key stored WHEN forget THEN record and keystore entry are dropped`() = runTest {
        // Arrange
        coEvery { accountStore.getDelegate(WALLET_ID) } returns RECORD
        coEvery { tangemHotSdk.delete(any()) } returns Unit

        // Act
        delegate.forget(ACCOUNT_ID)

        // Assert
        coVerify(exactly = 1) {
            accountStore.removeDelegate(WALLET_ID)
            tangemHotSdk.delete(HOT_WALLET_ID)
        }
    }

    private fun derivedResponse() = DerivedPublicKeyResponse(
        responses = listOf(
            DerivedPublicKeyResponse.ResponseEntry(
                curve = CURVE,
                seedKey = extendedKey(PUBLIC_KEY),
                publicKeys = mapOf(DERIVATION_PATH to extendedKey(PUBLIC_KEY)),
            ),
        ),
    )

    private fun extendedKey(publicKey: ByteArray) = ExtendedPublicKey(
        publicKey = publicKey,
        chainCode = ByteArray(size = 32),
    )

    private companion object {

        val CURVE = EllipticCurve.Ed25519Slip0010
        val DERIVATION_PATH = requireNotNull(Blockchain.Solana.derivationPath(DerivationStyle.V3))

        val WALLET_ID = UserWalletId("011977")
        val OTHER_WALLET_ID = UserWalletId("011988")
        val ACCOUNT_ID = AccountId.forCollectiblesAccount(WALLET_ID)
        val OTHER_ACCOUNT_ID = AccountId.forCollectiblesAccount(OTHER_WALLET_ID)
        val HOT_WALLET_ID = HotWalletId(value = "hot-wallet-1", authType = HotWalletId.AuthType.NoPassword)
        val PUBLIC_KEY = ByteArray(size = 32) { it.toByte() }
        val SIGNATURE = ByteArray(size = 64) { it.toByte() }
        val MESSAGE = ByteArray(size = 96) { it.toByte() }

        val RECORD = HOT_WALLET_ID.value
    }
}