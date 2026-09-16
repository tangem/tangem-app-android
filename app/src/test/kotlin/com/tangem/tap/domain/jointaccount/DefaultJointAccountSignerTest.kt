package com.tangem.tap.domain.jointaccount

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.Secp256k1
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.jointaccount.derivation.jointAccountOwnerDerivationPath
import com.tangem.domain.jointaccount.model.JointAccountSignInput
import com.tangem.domain.jointaccount.model.JointAccountSignResult
import com.tangem.domain.jointaccount.signing.JointAccountSignablePayload
import com.tangem.domain.models.MobileWallet
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.domain.wallets.hot.HotWalletAccessor
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.DataToSign
import com.tangem.hot.sdk.model.DerivedPublicKeyResponse
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.SignedData
import com.tangem.hot.sdk.model.UnlockHotWallet
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.domain.tasks.jointaccount.JointAccountSignTask
import com.tangem.tap.domain.tasks.jointaccount.JointAccountSigning
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * The hot path must cost the user one authorization, like the cold path costs one tap: the owner key is derived
 * and the payload signed under a single contextual unlock, released right after.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultJointAccountSignerTest {

    init {
        CryptoUtils.initCrypto()
    }

    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val tangemSdkManager: TangemSdkManager = mockk()
    private val signTaskFactory: JointAccountSignTask.Factory = mockk()
    private val derivationsRepository: DerivationsRepository = mockk()
    private val hotWalletAccessor: HotWalletAccessor = mockk()
    private val tangemHotSdk: TangemHotSdk = mockk()

    private val signer = DefaultJointAccountSigner(
        userWalletsListRepository = userWalletsListRepository,
        tangemSdkManager = tangemSdkManager,
        signTaskFactory = signTaskFactory,
        derivationsRepository = derivationsRepository,
        hotWalletAccessor = hotWalletAccessor,
        tangemHotSdk = tangemHotSdk,
    )

    private val curve = EllipticCurve.Secp256k1
    private val userWalletId = UserWalletId(value = ByteArray(KEY_SIZE) { 5 })
    private val hotWalletId = HotWalletId(value = "hot", authType = HotWalletId.AuthType.Password)
    private val unlock = UnlockHotWallet(walletId = hotWalletId, auth = HotAuth.Password("code".toCharArray()))

    private val seedPrivateKey = ByteArray(KEY_SIZE) { 1 }
    private val seedPublicKey = CryptoUtils.generatePublicKey(seedPrivateKey, curve)
    private val ownerPrivateKey = ByteArray(KEY_SIZE) { 2 }
    private val ownerKey = ExtendedPublicKey(
        publicKey = CryptoUtils.generatePublicKey(ownerPrivateKey, curve, compressed = true),
        chainCode = ByteArray(KEY_SIZE) { 3 },
    )

    private val derivationIndex = DerivationIndex(value = 1).getOrNull()!!
    private val derivationPath = jointAccountOwnerDerivationPath(index = derivationIndex.value)
    private val input = JointAccountSignInput(
        derivationIndex = derivationIndex,
        makePayload = { ownerAddress ->
            object : JointAccountSignablePayload {
                override fun toCanonicalMap(): Map<String, Any> = mapOf("address" to ownerAddress)
            }
        },
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            userWalletsListRepository,
            tangemSdkManager,
            signTaskFactory,
            derivationsRepository,
            hotWalletAccessor,
            tangemHotSdk,
        )
        coEvery { derivationsRepository.storeDerivedKeys(userWalletId = any(), derivedKeys = any()) } just Runs
    }

    @Test
    fun `GIVEN hot wallet WHEN sign THEN one unlock serves both derivation and signature`() = runTest {
        // Arrange
        stubHotWallet()
        stubHotSdk()

        // Act
        val actual = signer.sign(userWalletId = userWalletId, input = input)

        // Assert
        val result = (actual as Either.Right).value
        assertThat(result.ownerAddress).isEqualTo(JointAccountSigning.evmAddress(ownerKey))
        assertThat(result.canonicalPayload.decodeToString()).isEqualTo("""{"address":"${result.ownerAddress}"}""")
        assertThat(result.signature).matches("0x[0-9a-f]{130}")
        assertThat(result.derivedKeys.keys).containsExactly(seedPublicKey.toMapKey())
        assertThat(result.derivedKeys.getValue(seedPublicKey.toMapKey())[derivationPath]).isEqualTo(ownerKey)
        coVerifyOrder {
            hotWalletAccessor.unlockContextual(hotWalletId)
            tangemHotSdk.derivePublicKey(unlockHotWallet = unlock, request = any())
            tangemHotSdk.signHashes(unlockHotWallet = unlock, dataToSign = any())
            hotWalletAccessor.clearContextualUnlock(hotWalletId)
            derivationsRepository.storeDerivedKeys(userWalletId = userWalletId, derivedKeys = result.derivedKeys)
        }
        coVerify(exactly = 1) { hotWalletAccessor.unlockContextual(hotWalletId) }
        coVerify(exactly = 0) {
            hotWalletAccessor.derivePublicKeys(hotWalletId = any(), request = any())
            hotWalletAccessor.signHashes(hotWalletId = any(), dataToSign = any())
        }
    }

    @Test
    fun `GIVEN hot wallet already unlocked in context WHEN sign THEN a fresh unlock is asked anyway`() = runTest {
        // Arrange
        stubHotWallet()
        stubHotSdk()
        val staleUnlock = UnlockHotWallet(walletId = hotWalletId, auth = HotAuth.Password("stale".toCharArray()))
        every { hotWalletAccessor.getContextualUnlock(hotWalletId) } returns staleUnlock

        // Act
        val actual = signer.sign(userWalletId = userWalletId, input = input)

        // Assert
        assertThat(actual.isRight()).isTrue()
        coVerify(exactly = 1) { hotWalletAccessor.unlockContextual(hotWalletId) }
        coVerify(exactly = 0) { tangemHotSdk.signHashes(unlockHotWallet = staleUnlock, dataToSign = any()) }
        verify(exactly = 1) { hotWalletAccessor.clearContextualUnlock(hotWalletId) }
    }

    @Test
    fun `GIVEN hot wallet WHEN signing fails THEN error is returned and the unlock is still released`() = runTest {
        // Arrange
        stubHotWallet()
        stubHotSdk()
        val error = TangemSdkError.UserCancelled()
        coEvery { tangemHotSdk.signHashes(unlockHotWallet = unlock, dataToSign = any()) } throws error

        // Act
        val actual = signer.sign(userWalletId = userWalletId, input = input)

        // Assert
        assertThat(actual).isEqualTo(Either.Left(error))
        verify(exactly = 1) { hotWalletAccessor.clearContextualUnlock(hotWalletId) }
        coVerify(exactly = 0) { derivationsRepository.storeDerivedKeys(userWalletId = any(), derivedKeys = any()) }
    }

    @Test
    fun `GIVEN hot wallet without secp256k1 key WHEN sign THEN wallet not found without any unlock`() = runTest {
        // Arrange
        stubHotWallet(wallets = emptyList())

        // Act
        val actual = signer.sign(userWalletId = userWalletId, input = input)

        // Assert
        assertThat((actual as Either.Left).value).isInstanceOf(TangemSdkError.WalletNotFound::class.java)
        coVerify(exactly = 0) { hotWalletAccessor.unlockContextual(any()) }
    }

    @Test
    fun `GIVEN cold wallet WHEN sign THEN task result is returned and its keys are stored`() = runTest {
        // Arrange
        val coldWallet = mockk<UserWallet.Cold> { every { walletId } returns userWalletId }
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(coldWallet))
        val task: JointAccountSignTask = mockk()
        every { signTaskFactory.create(coroutineScope = any(), input = input) } returns task
        val expected = JointAccountSignResult(
            ownerAddress = "0xE31C6A9eE83A0f6f2e44dDcb9837B162802DeC12",
            canonicalPayload = "{}".toByteArray(),
            signature = "0x" + "ab".repeat(SIGNATURE_RSV_SIZE),
            derivedKeys = mapOf(seedPublicKey.toMapKey() to ExtendedPublicKeysMap(mapOf(derivationPath to ownerKey))),
        )
        coEvery {
            tangemSdkManager.runTaskAsync(runnable = task, preflightReadFilter = any(), registerColdWallet = false)
        } returns CompletionResult.Success(expected)

        // Act
        val actual = signer.sign(userWalletId = userWalletId, input = input)

        // Assert
        assertThat(actual).isEqualTo(Either.Right(expected))
        coVerify(exactly = 1) {
            derivationsRepository.storeDerivedKeys(userWalletId = userWalletId, derivedKeys = expected.derivedKeys)
        }
        coVerify(exactly = 0) { hotWalletAccessor.unlockContextual(any()) }
    }

    private fun stubHotWallet(wallets: List<MobileWallet> = listOf(seedWallet())) {
        val userWallet = mockk<UserWallet.Hot>()
        every { userWallet.walletId } returns userWalletId
        every { userWallet.hotWalletId } returns hotWalletId
        every { userWallet.wallets } returns wallets
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
    }

    private fun seedWallet(): MobileWallet {
        val wallet = mockk<MobileWallet>()
        every { wallet.curve } returns curve
        every { wallet.publicKey } returns seedPublicKey
        return wallet
    }

    private fun stubHotSdk() {
        coEvery { hotWalletAccessor.unlockContextual(hotWalletId) } returns unlock
        every { hotWalletAccessor.clearContextualUnlock(hotWalletId) } just Runs
        coEvery { tangemHotSdk.derivePublicKey(unlockHotWallet = unlock, request = any()) } returns
            DerivedPublicKeyResponse(
                responses = listOf(
                    DerivedPublicKeyResponse.ResponseEntry(
                        curve = curve,
                        seedKey = ExtendedPublicKey(publicKey = seedPublicKey, chainCode = ByteArray(KEY_SIZE)),
                        publicKeys = mapOf<DerivationPath, ExtendedPublicKey>(derivationPath to ownerKey),
                    ),
                ),
            )
        // The hot SDK signs the given digest as is, like the real signHashes
        coEvery { tangemHotSdk.signHashes(unlockHotWallet = unlock, dataToSign = any()) } answers {
            val digest = secondArg<List<DataToSign>>().single().hashes.single()
            listOf(SignedData(curve = curve, signatures = listOf(Secp256k1.ecdsaSignDigest(digest, ownerPrivateKey))))
        }
    }

    private companion object {
        const val KEY_SIZE = 32
        const val SIGNATURE_RSV_SIZE = 65
    }
}