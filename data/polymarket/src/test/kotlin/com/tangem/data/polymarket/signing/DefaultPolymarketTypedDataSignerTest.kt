package com.tangem.data.polymarket.signing

import arrow.core.left
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.data.polymarket.builder.PolymarketTypedDataBuilder
import com.tangem.data.polymarket.derivation.PolymarketAddressFactory
import com.tangem.data.wallets.hot.TangemHotWalletSigner
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.derivation.POLYMARKET_OWNER_DERIVATION_PATH
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PolymarketOnboardingSignatures
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.signing.PolymarketApprovalsPayload
import com.tangem.domain.polymarket.signing.PolymarketClobAuthData
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import com.tangem.utils.extensions.hexToBytes
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultPolymarketTypedDataSignerTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val derivationsRepository: DerivationsRepository = mockk()
    private val cardSdkConfigRepository: CardSdkConfigRepository = mockk()
    private val hotSignerFactory: TangemHotWalletSigner.Factory = mockk()
    private val transactionSigner: TransactionSigner = mockk()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val signer = DefaultPolymarketTypedDataSigner(
        userWalletsListRepository = userWalletsListRepository,
        derivationsRepository = derivationsRepository,
        cardSdkConfigRepository = cardSdkConfigRepository,
        hotSignerFactory = hotSignerFactory,
        addressFactory = PolymarketAddressFactory(),
        formatter = PolymarketSignatureFormatter(),
        dispatchers = dispatchers,
    )

    private val userWalletId = UserWalletId("011")
    private val path = DerivationPath(POLYMARKET_OWNER_DERIVATION_PATH)
    private val seedKey = "02AABB".hexToBytes()
    private val seedKeyBAK = ByteArrayKey(seedKey)

    private val ownerKey = ExtendedPublicKey(
        publicKey = "0279BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798".hexToBytes(),
        chainCode = "00".repeat(CHAIN_CODE_SIZE).hexToBytes(),
    )

    private val clobAuth = PolymarketClobAuthData(timestamp = "1735689600")
    private val approvals = PolymarketApprovalsPayload(
        depositWalletAddress = "0xfAeA0f08159fcF2f573fE24E9E989B0d48f7651B",
        nonce = "0",
        deadline = "1735690200",
        calls = listOf(
            PolymarketApprovalCall(
                target = "0xC011a7E12a19f7B1f670d46F03B03f3342E82DFB",
                value = "0",
                data = "0x095ea7b3",
            ),
        ),
    )

    @BeforeEach
    fun setup() {
        clearMocks(
            userWalletsListRepository,
            derivationsRepository,
            cardSdkConfigRepository,
            hotSignerFactory,
            transactionSigner,
        )
        mockkStatic(UserWalletsListRepository::getSyncStrict)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(UserWalletsListRepository::getSyncStrict)
    }

    private fun coldWallet(hasSecpWallet: Boolean = true): UserWallet.Cold {
        val cardWallet = mockk<com.tangem.domain.models.scan.CardDTO.Wallet> {
            every { curve } returns EllipticCurve.Secp256k1
            every { publicKey } returns seedKey
        }
        return mockk {
            every { walletId } returns userWalletId
            every { scanResponse } returns mockk(relaxed = true) {
                every { productType } returns ProductType.Wallet2
                every { card } returns mockk(relaxed = true) {
                    every { wallets } returns if (hasSecpWallet) listOf(cardWallet) else emptyList()
                    every { cardId } returns "CB79"
                    every { backupStatus } returns null
                }
            }
        }
    }

    private fun stubDerivedKey(present: Boolean = true) {
        coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, seedKeyBAK) } returns
            if (present) ExtendedPublicKeysMap(mapOf(path to ownerKey)) else ExtendedPublicKeysMap(emptyMap())
    }

    private fun stubColdSigner() {
        every { cardSdkConfigRepository.getCommonSigner(any(), any(), userWalletId) } returns transactionSigner
    }

    private fun sign(hash: ByteArray): ByteArray {
        val signature = Sign.signMessage(hash, ECKeyPair.create(OWNER_PRIVATE_KEY), false)
        return signature.r + signature.s
    }

    @Test
    fun `GIVEN both payloads WHEN signOnboarding THEN signs both hashes in one call`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
        stubDerivedKey()
        stubColdSigner()
        val hashes = slot<List<ByteArray>>()
        coEvery { transactionSigner.sign(capture(hashes), any()) } answers {
            CompletionResult.Success(hashes.captured.map { sign(it) })
        }

        // Act
        val result = signer.signOnboarding(userWalletId, clobAuth, approvals)

        // Assert
        assertThat(result.isRight()).isTrue()
        assertThat(hashes.captured).hasSize(2)
        coVerify(exactly = 1) { transactionSigner.sign(any<List<ByteArray>>(), any()) }
        // Card is not backed up (backupStatus == null) and not a twin, so its id is passed to the signer
        verify(exactly = 1) { cardSdkConfigRepository.getCommonSigner(cardId = "CB79", twinKey = null, userWalletId) }
    }

    @Test
    fun `GIVEN both payloads WHEN signOnboarding THEN maps signatures to the matching fields`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
        stubDerivedKey()
        stubColdSigner()
        val hashes = slot<List<ByteArray>>()
        val publicKey = slot<Wallet.PublicKey>()
        coEvery { transactionSigner.sign(capture(hashes), capture(publicKey)) } answers {
            CompletionResult.Success(hashes.captured.map { sign(it) })
        }
        val formatter = PolymarketSignatureFormatter()

        // Act
        val result = signer.signOnboarding(userWalletId, clobAuth, approvals)

        // Assert
        val expected = PolymarketOnboardingSignatures(
            l1Signature = formatter.format(sign(hashes.captured[0]), hashes.captured[0], publicKey.captured),
            batchSignature = formatter.format(sign(hashes.captured[1]), hashes.captured[1], publicKey.captured),
        )
        assertThat(result.getOrNull()).isEqualTo(expected)
        val ownerAddress = PolymarketAddressFactory().createAddress(ownerKey)
        val expectedClobDigest = EthereumUtils.makeTypedDataHash(
            PolymarketTypedDataBuilder.buildClobAuth(
                address = ownerAddress,
                timestamp = clobAuth.timestamp,
                nonce = clobAuth.nonce,
            ),
        )
        val expectedBatchDigest = EthereumUtils.makeTypedDataHash(
            PolymarketTypedDataBuilder.buildApprovalsBatch(
                depositWallet = approvals.depositWalletAddress,
                nonce = approvals.nonce,
                deadline = approvals.deadline,
                calls = approvals.calls,
            ),
        )
        assertThat(hashes.captured[0]).isEqualTo(expectedClobDigest)
        assertThat(hashes.captured[1]).isEqualTo(expectedBatchDigest)
    }

    @Test
    fun `GIVEN signing key WHEN signOnboarding THEN passes seed key and owner path`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
        stubDerivedKey()
        stubColdSigner()
        val hashes = slot<List<ByteArray>>()
        val publicKey = slot<Wallet.PublicKey>()
        coEvery { transactionSigner.sign(capture(hashes), capture(publicKey)) } answers {
            CompletionResult.Success(hashes.captured.map { sign(it) })
        }

        // Act
        signer.signOnboarding(userWalletId, clobAuth, approvals)

        // Assert
        assertThat(publicKey.captured.seedKey).isEqualTo(seedKey)
        assertThat(publicKey.captured.derivationPath).isEqualTo(path)
        assertThat(publicKey.captured.blockchainKey).isEqualTo(ownerKey.publicKey)
    }

    @Test
    fun `GIVEN owner key not derived WHEN signOnboarding THEN returns NotDerived`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
        stubDerivedKey(present = false)

        // Act
        val result = signer.signOnboarding(userWalletId, clobAuth, approvals)

        // Assert
        assertThat(result).isEqualTo(PolymarketSigningError.NotDerived.left())
    }

    @Test
    fun `GIVEN wallet without secp256k1 key WHEN signOnboarding THEN returns MissingWallet`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet(hasSecpWallet = false)

        // Act
        val result = signer.signOnboarding(userWalletId, clobAuth, approvals)

        // Assert
        assertThat(result).isEqualTo(PolymarketSigningError.MissingWallet.left())
    }

    @Test
    fun `GIVEN signer returns fewer signatures than hashes WHEN signOnboarding THEN returns Unknown`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
        stubDerivedKey()
        stubColdSigner()
        val hashes = slot<List<ByteArray>>()
        coEvery { transactionSigner.sign(capture(hashes), any()) } answers {
            CompletionResult.Success(listOf(sign(hashes.captured.first())))
        }

        // Act
        val result = signer.signOnboarding(userWalletId, clobAuth, approvals)

        // Assert
        assertThat(result).isEqualTo(PolymarketSigningError.Unknown.left())
    }

    @Test
    fun `GIVEN signer returns no signatures WHEN signClobAuth THEN returns Unknown`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
        stubDerivedKey()
        stubColdSigner()
        coEvery { transactionSigner.sign(any<List<ByteArray>>(), any()) } returns
            CompletionResult.Success(emptyList())

        // Act
        val result = signer.signClobAuth(userWalletId, clobAuth)

        // Assert
        assertThat(result).isEqualTo(PolymarketSigningError.Unknown.left())
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN signing fails WHEN signOnboarding THEN maps the error`(model: SigningFailureModel) = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
        stubDerivedKey()
        stubColdSigner()
        coEvery { transactionSigner.sign(any<List<ByteArray>>(), any()) } returns
            CompletionResult.Failure(model.error)

        // Act
        val result = signer.signOnboarding(userWalletId, clobAuth, approvals)

        // Assert
        assertThat(result).isEqualTo(model.expected.left())
    }

    internal data class SigningFailureModel(val error: TangemSdkError, val expected: PolymarketSigningError)

    @Test
    fun `GIVEN clob auth only WHEN signClobAuth THEN signs a single hash`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
        stubDerivedKey()
        stubColdSigner()
        val hashes = slot<List<ByteArray>>()
        val publicKey = slot<Wallet.PublicKey>()
        coEvery { transactionSigner.sign(capture(hashes), capture(publicKey)) } answers {
            CompletionResult.Success(hashes.captured.map { sign(it) })
        }

        // Act
        val result = signer.signClobAuth(userWalletId, clobAuth)

        // Assert
        assertThat(hashes.captured).hasSize(1)
        assertThat(result.getOrNull()).isEqualTo(
            PolymarketSignatureFormatter().format(sign(hashes.captured[0]), hashes.captured[0], publicKey.captured),
        )
    }

    @Test
    fun `GIVEN hot wallet WHEN signOnboarding THEN uses the hot signer`() = runTest {
        // Arrange
        val hotWallet = mockk<UserWallet.Hot> {
            every { walletId } returns userWalletId
            every { wallets } returns listOf(
                com.tangem.domain.models.MobileWallet(
                    publicKey = seedKey,
                    chainCode = null,
                    curve = EllipticCurve.Secp256k1,
                    derivedKeys = emptyMap(),
                ),
            )
        }
        val hotSigner = mockk<TangemHotWalletSigner>()
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns hotWallet
        every { hotSignerFactory.create(hotWallet) } returns hotSigner
        stubDerivedKey()
        val hashes = slot<List<ByteArray>>()
        coEvery { hotSigner.sign(capture(hashes), any()) } answers {
            CompletionResult.Success(hashes.captured.map { sign(it) })
        }

        // Act
        val result = signer.signOnboarding(userWalletId, clobAuth, approvals)

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) { hotSigner.sign(any<List<ByteArray>>(), any()) }
        coVerify(exactly = 0) { cardSdkConfigRepository.getCommonSigner(any(), any(), any()) }
    }

    private fun provideTestModels() = listOf(
        SigningFailureModel(TangemSdkError.UserCancelled(), PolymarketSigningError.UserCancelled),
        SigningFailureModel(TangemSdkError.WalletNotFound(), PolymarketSigningError.MissingWallet),
        SigningFailureModel(TangemSdkError.MissingPreflightRead(), PolymarketSigningError.CardError),
    )

    private companion object {
        const val CHAIN_CODE_SIZE = 32
        val OWNER_PRIVATE_KEY: BigInteger = BigInteger.ONE
    }
}