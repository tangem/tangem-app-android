package com.tangem.data.polymarket.sessionkey

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.data.polymarket.derivation.PolymarketAddressFactory
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.DataToSign
import com.tangem.hot.sdk.model.DerivedPublicKeyResponse
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.SignedData
import com.tangem.hot.sdk.model.UnlockHotWallet
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.math.BigInteger

internal class DefaultPolymarketSessionKeyStoreTest {

    private val tangemHotSdk: TangemHotSdk = mockk(relaxUnitFun = true)
    private val keys = mutableMapOf<String, ECKeyPair>()
    private var secureStorage = InMemorySecureStorage()

    private var store = createStore()

    @BeforeEach
    fun setUp() {
        clearMocks(tangemHotSdk)
        keys.clear()
        secureStorage = InMemorySecureStorage()
        store = createStore()
        givenGeneratedWallets(HOT_WALLET_A to KNOWN_KEY_PAIR, HOT_WALLET_B to OTHER_KEY_PAIR)
        coEvery { tangemHotSdk.derivePublicKey(any(), any()) } answers {
            val unlock = firstArg<UnlockHotWallet>()
            val keyPair = keys[unlock.walletId.value] ?: error("No private info found for ${unlock.walletId}")
            DerivedPublicKeyResponse(
                responses = listOf(
                    DerivedPublicKeyResponse.ResponseEntry(
                        curve = EllipticCurve.Secp256k1,
                        seedKey = ExtendedPublicKey(publicKey = compressed(keyPair), chainCode = ByteArray(32)),
                        publicKeys = emptyMap(),
                    ),
                ),
            )
        }
        coEvery { tangemHotSdk.signHashes(any(), any()) } answers {
            val keyPair = keys.getValue(firstArg<UnlockHotWallet>().walletId.value)
            secondArg<List<DataToSign>>().map { request ->
                SignedData(
                    curve = request.curve,
                    derivationPath = request.derivationPath,
                    signatures = request.hashes.map { hash ->
                        val signature = Sign.signMessage(hash, keyPair, false)
                        signature.r + signature.s
                    },
                )
            }
        }
    }

    @Test
    fun `GIVEN a known master key WHEN getOrCreate THEN returns its lower-case Ethereum address`() = runTest {
        // Act
        val actual = store.getOrCreate(WALLET)

        // Assert
        assertThat(actual).isEqualTo(Either.Right(KNOWN_ADDRESS))
    }

    @Test
    fun `GIVEN no key WHEN heldAddress THEN returns null without creating one`() = runTest {
        // Act
        val actual = store.heldAddress(WALLET)

        // Assert
        assertThat(actual).isNull()
        coVerify(exactly = 0) { tangemHotSdk.generateWallet(any(), any()) }
    }

    @Test
    fun `GIVEN a created key WHEN getOrCreate and heldAddress again THEN the same key is reused`() = runTest {
        // Arrange
        store.getOrCreate(WALLET)

        // Act
        val again = store.getOrCreate(WALLET)
        val held = createStore().heldAddress(WALLET)

        // Assert
        assertThat(again).isEqualTo(Either.Right(KNOWN_ADDRESS))
        assertThat(held).isEqualTo(KNOWN_ADDRESS)
        coVerify(exactly = 1) { tangemHotSdk.generateWallet(any(), any()) }
    }

    @Test
    fun `GIVEN two wallets WHEN getOrCreate THEN each gets its own key`() = runTest {
        // Act
        val first = store.getOrCreate(WALLET)
        val second = store.getOrCreate(OTHER_WALLET)

        // Assert
        assertThat(first).isEqualTo(Either.Right(KNOWN_ADDRESS))
        assertThat(second).isEqualTo(Either.Right(OTHER_ADDRESS))
    }

    @Test
    fun `GIVEN concurrent calls WHEN getOrCreate THEN a single key is created`() = runTest {
        // Arrange
        coEvery { tangemHotSdk.generateWallet(any(), any()) } coAnswers {
            delay(100)
            keys[HOT_WALLET_A.value] = KNOWN_KEY_PAIR
            HOT_WALLET_A
        }

        // Act
        val actual = List(3) { async { store.getOrCreate(WALLET) } }.awaitAll()

        // Assert
        assertThat(actual.toSet()).containsExactly(Either.Right(KNOWN_ADDRESS))
        coVerify(exactly = 1) { tangemHotSdk.generateWallet(any(), any()) }
    }

    @Test
    fun `GIVEN concurrent calls with the wallet id in another case WHEN getOrCreate THEN a single key is created`() =
        runTest {
            // Arrange
            coEvery { tangemHotSdk.generateWallet(any(), any()) } coAnswers {
                delay(100)
                keys[HOT_WALLET_A.value] = KNOWN_KEY_PAIR
                HOT_WALLET_A
            }

            // Act
            val actual = listOf(HEX_WALLET, UserWalletId(HEX_WALLET.stringValue.uppercase()))
                .map { async { store.getOrCreate(it) } }
                .awaitAll()

            // Assert
            assertThat(actual.toSet()).containsExactly(Either.Right(KNOWN_ADDRESS))
            coVerify(exactly = 1) { tangemHotSdk.generateWallet(any(), any()) }
        }

    @Test
    fun `GIVEN the hot SDK times out WHEN getOrCreate THEN returns Failure`() = runTest {
        // Arrange
        coEvery { tangemHotSdk.generateWallet(any(), any()) } coAnswers { withTimeout(1) { awaitCancellation() } }

        // Act
        val actual = store.getOrCreate(WALLET)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(PolymarketSessionKeyError.Failure::class.java)
        assertThat(secureStorage.entries).isEmpty()
    }

    @Test
    fun `GIVEN the hot SDK times out on derive WHEN getOrCreate THEN the generated key is deleted`() = runTest {
        // Arrange
        coEvery { tangemHotSdk.derivePublicKey(any(), any()) } coAnswers { withTimeout(1) { awaitCancellation() } }

        // Act
        val actual = store.getOrCreate(WALLET)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(PolymarketSessionKeyError.Failure::class.java)
        assertThat(secureStorage.entries).isEmpty()
        coVerify(exactly = 1) { tangemHotSdk.delete(HOT_WALLET_A) }
    }

    @Test
    fun `GIVEN an unreadable stored key WHEN getOrCreate THEN returns Failure and keeps the key`() = runTest {
        // Arrange
        store.getOrCreate(WALLET)
        keys.remove(HOT_WALLET_A.value)
        store = createStore()

        // Act
        val actual = store.getOrCreate(WALLET)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(PolymarketSessionKeyError.Failure::class.java)
        assertThat(secureStorage.entries.values).containsExactly(HOT_WALLET_A.value)
        coVerify(exactly = 1) { tangemHotSdk.generateWallet(any(), any()) }
        coVerify(exactly = 0) { tangemHotSdk.delete(any()) }
    }

    @Test
    fun `GIVEN the mapping cannot be read WHEN getOrCreate THEN returns Failure without creating a key`() = runTest {
        // Arrange
        store.getOrCreate(WALLET)
        secureStorage.failReads = true

        // Act
        val actual = createStore().getOrCreate(WALLET)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(PolymarketSessionKeyError.Failure::class.java)
        coVerify(exactly = 1) { tangemHotSdk.generateWallet(any(), any()) }
        coVerify(exactly = 0) { tangemHotSdk.delete(any()) }
    }

    @Test
    fun `GIVEN the mapping cannot be stored WHEN getOrCreate THEN the generated key is deleted`() = runTest {
        // Arrange
        secureStorage.failWrites = true

        // Act
        val actual = store.getOrCreate(WALLET)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(PolymarketSessionKeyError.Failure::class.java)
        coVerify(exactly = 1) { tangemHotSdk.delete(HOT_WALLET_A) }
    }

    @Test
    fun `GIVEN a key WHEN sign THEN returns r s v recoverable to the key address`() = runTest {
        // Arrange
        store.getOrCreate(WALLET)
        val expected = Sign.signMessage(DIGEST, KNOWN_KEY_PAIR, false).let { it.r + it.s + it.v }

        // Act
        val actual = store.sign(WALLET, KNOWN_ADDRESS, DIGEST)

        // Assert
        val signature = actual.getOrNull()!!
        assertThat(signature).isEqualTo(expected)
        assertThat(recoverAddress(signature)).isEqualTo(KNOWN_ADDRESS)
        coVerify { tangemHotSdk.signHashes(any(), match { it.single().derivationPath == null }) }
    }

    @Test
    fun `GIVEN another key is held WHEN sign THEN returns KeyMissing without signing`() = runTest {
        // Arrange
        store.getOrCreate(WALLET)

        // Act
        val actual = store.sign(WALLET, OTHER_ADDRESS, DIGEST)

        // Assert
        assertThat(actual).isEqualTo(Either.Left(PolymarketSessionKeyError.KeyMissing))
        coVerify(exactly = 0) { tangemHotSdk.signHashes(any(), any()) }
    }

    @Test
    fun `GIVEN the address in another case WHEN sign THEN the held key signs`() = runTest {
        // Arrange
        store.getOrCreate(WALLET)

        // Act
        val actual = store.sign(WALLET, KNOWN_ADDRESS.uppercase().replace("0X", "0x"), DIGEST)

        // Assert
        assertThat(actual.isRight()).isTrue()
    }

    @Test
    fun `GIVEN no key WHEN sign THEN returns KeyMissing`() = runTest {
        // Act
        val actual = store.sign(WALLET, KNOWN_ADDRESS, DIGEST)

        // Assert
        assertThat(actual).isEqualTo(Either.Left(PolymarketSessionKeyError.KeyMissing))
    }

    @Test
    fun `GIVEN a digest that is not 32 bytes WHEN sign THEN returns InvalidDigest`() = runTest {
        // Arrange
        store.getOrCreate(WALLET)

        // Act
        val actual = store.sign(WALLET, KNOWN_ADDRESS, ByteArray(31))

        // Assert
        assertThat(actual).isEqualTo(Either.Left(PolymarketSessionKeyError.InvalidDigest))
    }

    @Test
    fun `GIVEN a key WHEN delete THEN the hot wallet and the mapping are removed`() = runTest {
        // Arrange
        store.getOrCreate(WALLET)

        // Act
        val actual = store.delete(WALLET)

        // Assert
        assertThat(actual).isEqualTo(Either.Right(Unit))
        assertThat(store.heldAddress(WALLET)).isNull()
        assertThat(secureStorage.entries).isEmpty()
        coVerify(exactly = 1) { tangemHotSdk.delete(HOT_WALLET_A) }
    }

    @Test
    fun `GIVEN no key WHEN delete THEN succeeds without touching the hot SDK`() = runTest {
        // Act
        val actual = store.delete(WALLET)

        // Assert
        assertThat(actual).isEqualTo(Either.Right(Unit))
        coVerify(exactly = 0) { tangemHotSdk.delete(any()) }
    }

    @Test
    fun `GIVEN the hot SDK fails to delete WHEN delete THEN the mapping is removed anyway`() = runTest {
        // Arrange
        store.getOrCreate(WALLET)
        coEvery { tangemHotSdk.delete(any()) } throws IllegalStateException("keystore unavailable")

        // Act
        val actual = store.delete(WALLET)

        // Assert
        assertThat(actual).isEqualTo(Either.Right(Unit))
        assertThat(secureStorage.entries).isEmpty()
        assertThat(store.heldAddress(WALLET)).isNull()
    }

    @Test
    fun `GIVEN the hot SDK times out on delete WHEN delete THEN the mapping is removed anyway`() = runTest {
        // Arrange
        store.getOrCreate(WALLET)
        coEvery { tangemHotSdk.delete(any()) } coAnswers { withTimeout(1) { awaitCancellation() } }

        // Act
        val actual = store.delete(WALLET)

        // Assert
        assertThat(actual).isEqualTo(Either.Right(Unit))
        assertThat(secureStorage.entries).isEmpty()
    }

    @Test
    fun `GIVEN the mapping cannot be removed WHEN delete THEN returns Failure and keeps the hot wallet`() = runTest {
        // Arrange
        store.getOrCreate(WALLET)
        secureStorage.failDeletes = true

        // Act
        val actual = store.delete(WALLET)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(PolymarketSessionKeyError.Failure::class.java)
        assertThat(secureStorage.entries.values).containsExactly(HOT_WALLET_A.value)
        coVerify(exactly = 0) { tangemHotSdk.delete(any()) }
    }

    @Test
    fun `GIVEN creation is cancelled WHEN getOrCreate THEN the generated key is deleted`() =
        runTest {
            // Arrange
            val deriving = CompletableDeferred<Unit>()
            coEvery { tangemHotSdk.derivePublicKey(any(), any()) } coAnswers {
                deriving.complete(Unit)
                awaitCancellation()
            }
            val creation = launch { store.getOrCreate(WALLET) }
            deriving.await()

            // Act
            creation.cancelAndJoin()

            // Assert
            assertThat(secureStorage.entries).isEmpty()
            coVerify(exactly = 1) { tangemHotSdk.delete(HOT_WALLET_A) }
        }

    private fun createStore() = DefaultPolymarketSessionKeyStore(
        tangemHotSdk = tangemHotSdk,
        secureStorage = secureStorage,
        addressFactory = PolymarketAddressFactory(),
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private fun givenGeneratedWallets(vararg wallets: Pair<HotWalletId, ECKeyPair>) {
        val queue = ArrayDeque(wallets.toList())
        coEvery { tangemHotSdk.generateWallet(any(), any()) } answers {
            val (hotWalletId, keyPair) = queue.removeFirst()
            keys[hotWalletId.value] = keyPair
            hotWalletId
        }
    }

    private fun compressed(keyPair: ECKeyPair): ByteArray {
        val uncompressed = Numeric.toBytesPadded(keyPair.publicKey, 64)
        val prefix: Byte = if (uncompressed.last().toInt() and 1 == 0) 0x02 else 0x03
        return byteArrayOf(prefix) + uncompressed.copyOfRange(0, 32)
    }

    private fun recoverAddress(signature: ByteArray): String {
        val data = Sign.SignatureData(signature[64], signature.copyOfRange(0, 32), signature.copyOfRange(32, 64))
        return "0x" + Keys.getAddress(Sign.signedMessageHashToKey(DIGEST, data))
    }

    private companion object {
        val WALLET = UserWalletId("011")
        val OTHER_WALLET = UserWalletId("022")
        val HEX_WALLET = UserWalletId("0abc")

        val HOT_WALLET_A = HotWalletId(value = "hot-a", authType = HotWalletId.AuthType.NoPassword)
        val HOT_WALLET_B = HotWalletId(value = "hot-b", authType = HotWalletId.AuthType.NoPassword)

        val KNOWN_KEY_PAIR: ECKeyPair = ECKeyPair.create(
            BigInteger("4c0883a69102937d6231471b5dbb6204fe5129617082792ae468d01a3f362318", 16),
        )
        const val KNOWN_ADDRESS = "0x2c7536e3605d9c16a7a3d7b1898e529396a65c23"

        val OTHER_KEY_PAIR: ECKeyPair = ECKeyPair.create(BigInteger.ONE)
        const val OTHER_ADDRESS = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf"

        val DIGEST: ByteArray = ByteArray(32) { it.toByte() }
    }
}