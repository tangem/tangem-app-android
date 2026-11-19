package com.tangem.data.tangempay.datasource

import arrow.core.Either
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.CryptoUtils.generatePublicKey
import com.tangem.crypto.Secp256k1
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.card.common.tangempay.TangemPayUtilities
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tangempay.WithdrawalSignatureResult
import com.tangem.domain.tangempay.datasource.TangemPayRemoteDataSource
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.tangempay.error.TangemPayActivationError
import com.tangem.domain.tangempay.error.TangemPayCardScanError
import com.tangem.domain.wallets.hot.HotWalletAccessor
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.DataToSign
import com.tangem.hot.sdk.model.DeriveWalletRequest
import com.tangem.hot.sdk.model.DerivedPublicKeyResponse
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.SignedData
import com.tangem.hot.sdk.model.UnlockHotWallet
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TangemPayHotSdkManagerTest {

    init {
        CryptoUtils.initCrypto()
    }

    private val hotWalletAccessor: HotWalletAccessor = mockk(relaxUnitFun = true)
    private val tangemHotSdk: TangemHotSdk = mockk()
    private val tangemPayAuthRemoteDataSource: TangemPayRemoteDataSource = mockk()

    private val manager = TangemPayHotSdkManager(
        hotWalletAccessor = hotWalletAccessor,
        tangemHotSdk = tangemHotSdk,
        tangemPayAuthRemoteDataSource = tangemPayAuthRemoteDataSource,
    )

    private val hotWalletId = HotWalletId(value = "hotWalletId", authType = HotWalletId.AuthType.Password)
    private val hotWallet: UserWallet.Hot = mockk {
        every { hotWalletId } returns this@TangemPayHotSdkManagerTest.hotWalletId
    }
    private val unlockHotWallet: UnlockHotWallet = mockk()

    private val derivedPrivateKey = ByteArray(KEY_SIZE) { 2 }
    private val derivedExtendedPublicKey = ExtendedPublicKey(
        publicKey = generatePublicKey(derivedPrivateKey, EllipticCurve.Secp256k1, compressed = true),
        chainCode = ByteArray(KEY_SIZE) { 3 },
    )
    private val tangemPayKeyRequest = DeriveWalletRequest(
        requests = listOf(
            DeriveWalletRequest.Request(
                curve = TangemPayUtilities.curve,
                paths = listOf(TangemPayUtilities.customDerivationPath),
            ),
        ),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(hotWalletAccessor, tangemHotSdk, tangemPayAuthRemoteDataSource)
    }

    @Test
    fun `GIVEN unlocked wallet WHEN getWithdrawalSignature THEN signs exactly the given hash with Tangem Pay key`() =
        runTest {
            // Arrange
            val hash = WITHDRAWAL_HASH.hexToBytes()
            val signature = Secp256k1.ecdsaSignDigest(hash, derivedPrivateKey)
            val dataToSign = slot<List<DataToSign>>()
            coEvery { hotWalletAccessor.getContextualUnlock(hotWalletId) } returns unlockHotWallet
            coEvery { tangemHotSdk.derivePublicKey(unlockHotWallet, tangemPayKeyRequest) } returns
                derivedTangemPayKey(derivedExtendedPublicKey)
            coEvery { tangemHotSdk.signHashes(unlockHotWallet, capture(dataToSign)) } returns listOf(
                SignedData(
                    curve = TangemPayUtilities.curve,
                    derivationPath = TangemPayUtilities.customDerivationPath,
                    signatures = listOf(signature),
                ),
            )

            // Act
            val actual = manager.getWithdrawalSignature(hotWallet = hotWallet, hash = WITHDRAWAL_HASH)

            // Assert
            val expectedSignature = TangemPayUtilities.unmarshallSignature(
                signature = signature,
                hash = hash,
                extendedPublicKey = derivedExtendedPublicKey,
            )
            assertThat(actual).isEqualTo(Either.Right(WithdrawalSignatureResult.Success(expectedSignature)))
            val request = dataToSign.captured.single()
            assertThat(request.curve).isEqualTo(TangemPayUtilities.curve)
            assertThat(request.derivationPath).isEqualTo(TangemPayUtilities.customDerivationPath)
            assertThat(request.hashes.map { it.toHexString() }).containsExactly(hash.toHexString())
            coVerify(exactly = 1) { hotWalletAccessor.clearContextualUnlock(hotWalletId) }
        }

    @Test
    fun `GIVEN Tangem Pay key not derived WHEN getWithdrawalSignature THEN returns MissingWallet and never signs`() =
        runTest {
            // Arrange
            coEvery { hotWalletAccessor.getContextualUnlock(hotWalletId) } returns unlockHotWallet
            coEvery { tangemHotSdk.derivePublicKey(unlockHotWallet, tangemPayKeyRequest) } returns
                DerivedPublicKeyResponse(responses = emptyList())

            // Act
            val actual = manager.getWithdrawalSignature(hotWallet = hotWallet, hash = WITHDRAWAL_HASH)

            // Assert
            assertThat((actual.leftOrNull() as? TangemError)?.code)
                .isEqualTo(TangemPayActivationError.MissingWallet.errorCode)
            coVerify(exactly = 0) { tangemHotSdk.signHashes(any(), any()) }
            coVerify(exactly = 1) { hotWalletAccessor.clearContextualUnlock(hotWalletId) }
        }

    @Test
    fun `GIVEN hot sdk returns no signature WHEN getWithdrawalSignature THEN returns FailedToSignChallenge`() =
        runTest {
            // Arrange
            coEvery { hotWalletAccessor.getContextualUnlock(hotWalletId) } returns unlockHotWallet
            coEvery { tangemHotSdk.derivePublicKey(unlockHotWallet, tangemPayKeyRequest) } returns
                derivedTangemPayKey(derivedExtendedPublicKey)
            coEvery { tangemHotSdk.signHashes(unlockHotWallet, any()) } returns emptyList()

            // Act
            val actual = manager.getWithdrawalSignature(hotWallet = hotWallet, hash = WITHDRAWAL_HASH)

            // Assert
            assertThat((actual.leftOrNull() as? TangemError)?.code)
                .isEqualTo(TangemPayCardScanError.FailedToSignChallenge.errorCode)
            coVerify(exactly = 1) { hotWalletAccessor.clearContextualUnlock(hotWalletId) }
        }

    @Test
    fun `GIVEN password prompt dismissed WHEN getWithdrawalSignature THEN returns Cancelled`() = runTest {
        // Arrange
        coEvery { hotWalletAccessor.getContextualUnlock(hotWalletId) } returns null
        coEvery { hotWalletAccessor.unlockContextual(hotWalletId) } throws TangemSdkError.UserCancelled()

        // Act
        val actual = manager.getWithdrawalSignature(hotWallet = hotWallet, hash = HASH)

        // Assert
        assertThat(actual).isEqualTo(Either.Right(WithdrawalSignatureResult.Cancelled))
        coVerify(exactly = 1) { hotWalletAccessor.clearContextualUnlock(hotWalletId) }
    }

    @Test
    fun `GIVEN unlock fails with other error WHEN getWithdrawalSignature THEN returns Left`() = runTest {
        // Arrange
        val error = IllegalStateException("keystore failure")
        coEvery { hotWalletAccessor.getContextualUnlock(hotWalletId) } returns null
        coEvery { hotWalletAccessor.unlockContextual(hotWalletId) } throws error

        // Act
        val actual = manager.getWithdrawalSignature(hotWallet = hotWallet, hash = HASH)

        // Assert
        assertThat(actual).isEqualTo(Either.Left(error))
        coVerify(exactly = 1) { hotWalletAccessor.clearContextualUnlock(hotWalletId) }
    }

    @Test
    fun `GIVEN coroutine cancellation WHEN getWithdrawalSignature THEN cancellation propagates`() = runTest {
        // Arrange
        coEvery { hotWalletAccessor.getContextualUnlock(hotWalletId) } returns null
        coEvery { hotWalletAccessor.unlockContextual(hotWalletId) } throws CancellationException("real cancel")

        // Act
        val actual = runCatching { manager.getWithdrawalSignature(hotWallet = hotWallet, hash = HASH) }
            .exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun `GIVEN password prompt dismissed WHEN produceInitialData THEN returns Left without throwing`() =
        runTest {
            // Arrange
            val error = TangemSdkError.UserCancelled()
            coEvery { hotWalletAccessor.getContextualUnlock(hotWalletId) } returns null
            coEvery { hotWalletAccessor.unlockContextual(hotWalletId) } throws error

            // Act
            val actual = manager.produceInitialData(hotWallet = hotWallet)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(error))
            coVerify(exactly = 1) { hotWalletAccessor.clearContextualUnlock(hotWalletId) }
        }

    private fun derivedTangemPayKey(extendedPublicKey: ExtendedPublicKey) = DerivedPublicKeyResponse(
        responses = listOf(
            DerivedPublicKeyResponse.ResponseEntry(
                curve = TangemPayUtilities.curve,
                seedKey = extendedPublicKey,
                publicKeys = mapOf(TangemPayUtilities.customDerivationPath to extendedPublicKey),
            ),
        ),
    )

    private companion object {
        const val HASH = "FFAA"
        const val KEY_SIZE = 32
        const val WITHDRAWAL_HASH = "0x1c8aff950685c2ed4bc3174f3472287b56d9517b9c948127319a09a7a36deac8"
    }
}