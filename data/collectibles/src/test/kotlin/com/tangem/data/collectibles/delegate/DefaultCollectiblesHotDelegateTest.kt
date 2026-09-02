package com.tangem.data.collectibles.delegate

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.blockchain.extensions.encodeBase58
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.collectibles.delegate.CollectiblesHotDelegateError
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
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultCollectiblesHotDelegateTest {

    private val tangemHotSdk: TangemHotSdk = mockk()
    private val store: CollectiblesHotDelegateStore = mockk(relaxUnitFun = true)

    private val delegate = DefaultCollectiblesHotDelegate(
        tangemHotSdk = tangemHotSdk,
        store = store,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(tangemHotSdk, store)
    }

    @Test
    fun `GIVEN nothing stored WHEN provision THEN wallet is generated and its key stored`() = runTest {
        // Arrange
        coEvery { store.get(ACCOUNT_ID) } returns null
        coEvery { tangemHotSdk.generateWallet(any(), any()) } returns HOT_WALLET_ID
        coEvery { tangemHotSdk.derivePublicKey(any(), any()) } returns derivedResponse()

        // Act
        val actual = delegate.provision(ACCOUNT_ID)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(HotDelegatePublicKey(PUBLIC_KEY.encodeBase58()))
        coVerify(exactly = 1) {
            store.put(
                accountId = ACCOUNT_ID,
                record = CollectiblesHotDelegateRecord(
                    hotWalletId = HOT_WALLET_ID.value,
                    authType = HotWalletId.AuthType.NoPassword.name,
                    publicKeyBase58 = PUBLIC_KEY.encodeBase58(),
                ),
            )
        }
    }

    @Test
    fun `GIVEN key already stored WHEN provision THEN no wallet is generated`() = runTest {
        // Arrange
        coEvery { store.get(ACCOUNT_ID) } returns RECORD

        // Act
        val actual = delegate.provision(ACCOUNT_ID)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(HotDelegatePublicKey(RECORD.publicKeyBase58))
        coVerify(exactly = 0) { tangemHotSdk.generateWallet(any(), any()) }
    }

    @Test
    fun `GIVEN generation throws WHEN provision THEN nothing is stored`() = runTest {
        // Arrange
        val cause = IllegalStateException("keystore unavailable")
        coEvery { store.get(ACCOUNT_ID) } returns null
        coEvery { tangemHotSdk.generateWallet(any(), any()) } throws cause

        // Act
        val actual = delegate.provision(ACCOUNT_ID)

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(CollectiblesHotDelegateError.KeyCreationFailed(cause))
        coVerify(exactly = 0) { store.put(any(), any()) }
    }

    @Test
    fun `GIVEN another account's delegate stored WHEN provision THEN this account gets its own`() = runTest {
        // Arrange
        coEvery { store.get(ACCOUNT_ID) } returns null
        coEvery { store.get(OTHER_ACCOUNT_ID) } returns RECORD
        coEvery { tangemHotSdk.generateWallet(any(), any()) } returns HOT_WALLET_ID
        coEvery { tangemHotSdk.derivePublicKey(any(), any()) } returns derivedResponse()

        // Act
        val actual = delegate.provision(ACCOUNT_ID)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(HotDelegatePublicKey(PUBLIC_KEY.encodeBase58()))
        coVerify(exactly = 1) { store.put(accountId = ACCOUNT_ID, record = any()) }
        coVerify(exactly = 0) { store.put(accountId = OTHER_ACCOUNT_ID, record = any()) }
    }

    @Test
    fun `GIVEN nothing stored WHEN sign THEN reports not provisioned without calling the sdk`() = runTest {
        // Arrange
        coEvery { store.get(ACCOUNT_ID) } returns null

        // Act
        val actual = delegate.sign(ACCOUNT_ID, MESSAGE)

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(CollectiblesHotDelegateError.NotProvisioned)
        coVerify(exactly = 0) { tangemHotSdk.signHashes(any(), any()) }
    }

    @Test
    fun `GIVEN key stored WHEN sign THEN signs with no auth`() = runTest {
        // Arrange
        val unlock = slot<UnlockHotWallet>()
        coEvery { store.get(ACCOUNT_ID) } returns RECORD
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
        coEvery { store.get(ACCOUNT_ID) } returns RECORD
        coEvery { tangemHotSdk.delete(any()) } returns Unit

        // Act
        delegate.forget(ACCOUNT_ID)

        // Assert
        coVerify(exactly = 1) {
            store.remove(ACCOUNT_ID)
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

        val ACCOUNT_ID = AccountId.forCollectiblesAccount(UserWalletId("011977"))
        val OTHER_ACCOUNT_ID = AccountId.forCollectiblesAccount(UserWalletId("011988"))
        val HOT_WALLET_ID = HotWalletId(value = "hot-wallet-1", authType = HotWalletId.AuthType.NoPassword)
        val PUBLIC_KEY = ByteArray(size = 32) { it.toByte() }
        val SIGNATURE = ByteArray(size = 64) { it.toByte() }
        val MESSAGE = ByteArray(size = 96) { it.toByte() }

        val RECORD = CollectiblesHotDelegateRecord(
            hotWalletId = HOT_WALLET_ID.value,
            authType = HOT_WALLET_ID.authType.name,
            publicKeyBase58 = PUBLIC_KEY.encodeBase58(),
        )
    }
}