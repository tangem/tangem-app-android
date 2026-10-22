package com.tangem.data.polymarket.signing

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.data.polymarket.derivation.PolymarketAddressFactory
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.models.MobileWallet
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.signing.PolymarketApprovalsPayload
import com.tangem.domain.polymarket.signing.PolymarketClobAuthData
import com.tangem.domain.polymarket.signing.PolymarketSignRequest
import com.tangem.domain.polymarket.signing.PolymarketTypedPayload
import com.tangem.domain.wallets.signing.DeriveAndSignError
import com.tangem.domain.wallets.signing.DeriveAndSignSession
import com.tangem.domain.wallets.signing.SignRequest
import com.tangem.domain.wallets.signing.Signed
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import com.tangem.utils.extensions.hexToBytes
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
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
internal class DefaultPolymarketSigningSessionTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val deriveAndSignSession: DeriveAndSignSession = mockk()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val userWalletId = UserWalletId("011")
    private val seedKey = "02AABB".hexToBytes()

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
    private val bothPayloads = listOf(
        PolymarketTypedPayload.ClobAuth(clobAuth),
        PolymarketTypedPayload.RelayedBatch(approvals),
    )

    @BeforeEach
    fun setup() {
        clearMocks(userWalletsListRepository, deriveAndSignSession)
        mockkStatic(UserWalletsListRepository::getSyncStrict)
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns hotWallet()
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(UserWalletsListRepository::getSyncStrict)
    }

    private fun sut(formatter: PolymarketSignatureFormatter = PolymarketSignatureFormatter()) =
        DefaultPolymarketSigningSession(
            userWalletsListRepository = userWalletsListRepository,
            deriveAndSignSession = deriveAndSignSession,
            digestFactory = PolymarketDigestFactory(),
            addressFactory = PolymarketAddressFactory(),
            formatter = formatter,
            dispatchers = dispatchers,
        )

    private fun hotWallet(): UserWallet.Hot {
        val mobileWallet = MobileWallet(
            publicKey = seedKey,
            chainCode = null,
            curve = EllipticCurve.Secp256k1,
            derivedKeys = emptyMap(),
        )
        return mockk {
            every { walletId } returns userWalletId
            every { wallets } returns listOf(mobileWallet)
        }
    }

    private fun givenColdWalletWithOldFirmware() {
        val firmware = mockk<com.tangem.domain.models.scan.CardDTO.FirmwareVersion> {
            every { compareTo(FirmwareVersion.HDWalletAvailable) } returns -1
        }
        val cold = mockk<UserWallet.Cold> {
            every { walletId } returns userWalletId
            every { scanResponse } returns mockk {
                every { card } returns mockk {
                    every { firmwareVersion } returns firmware
                }
            }
        }
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns cold
    }

    private fun stubDeriveAndSignSession(handler: (List<ByteArray>) -> Either<DeriveAndSignError, List<ByteArray>>) {
        coEvery { deriveAndSignSession.run<Any?>(any(), any(), any(), any()) } coAnswers {
            val prepare = arg<suspend (ExtendedPublicKey) -> SignRequest<Any?>>(3)
            val request = prepare(ownerKey)
            handler(request.hashes).map { signatures -> Signed(request.value, signatures) }
        }
    }

    private fun rawSignature(hash: ByteArray): ByteArray {
        val signature = Sign.signMessage(hash, ECKeyPair.create(OWNER_PRIVATE_KEY), false)
        return signature.r + signature.s
    }

    @Test
    fun `GIVEN two payloads WHEN run THEN one signature request carries both digests, in order`() = runTest {
        // Arrange
        val requests = mutableListOf<List<ByteArray>>()
        stubDeriveAndSignSession { hashes ->
            requests += hashes
            hashes.map(::rawSignature).right()
        }

        // Act
        val actual = sut().run(userWalletId) { PolymarketSignRequest(value = VALUE, payloads = bothPayloads) }

        // Assert
        val digestFactory = PolymarketDigestFactory()
        val ownerAddress = PolymarketAddressFactory().createAddress(ownerKey)
        assertThat(requests).hasSize(1)
        assertThat(requests.single()[0]).isEqualTo(digestFactory.clobAuth(ownerAddress, clobAuth))
        assertThat(requests.single()[1]).isEqualTo(digestFactory.relayedBatch(approvals))
        assertThat(actual.getOrNull()?.value).isEqualTo(VALUE)
        assertThat(actual.getOrNull()?.signatures).hasSize(2)
    }

    @Test
    fun `GIVEN prepare gets the owner address WHEN run THEN it is the address of the derived key`() = runTest {
        // Arrange
        var seenAddress: String? = null
        stubDeriveAndSignSession { hashes -> hashes.map(::rawSignature).right() }

        // Act
        sut().run(userWalletId) { ownerAddress ->
            seenAddress = ownerAddress
            PolymarketSignRequest(value = VALUE, payloads = emptyList())
        }

        // Assert
        assertThat(seenAddress).isEqualTo(PolymarketAddressFactory().createAddress(ownerKey))
    }

    @Test
    fun `GIVEN nothing to sign WHEN run THEN the value returns with no signatures`() = runTest {
        // Arrange
        val requests = mutableListOf<List<ByteArray>>()
        stubDeriveAndSignSession { hashes ->
            requests += hashes
            emptyList<ByteArray>().right()
        }

        // Act
        val actual = sut().run(userWalletId) { PolymarketSignRequest(value = VALUE, payloads = emptyList()) }

        // Assert
        assertThat(requests.single()).isEmpty()
        assertThat(actual.getOrNull()?.value).isEqualTo(VALUE)
        assertThat(actual.getOrNull()?.signatures).isEmpty()
    }

    @Test
    fun `GIVEN the session returns fewer signatures WHEN run THEN Unknown`() = runTest {
        // Arrange
        stubDeriveAndSignSession { hashes -> listOf(rawSignature(hashes.first())).right() }

        // Act
        val actual = sut().run(userWalletId) { PolymarketSignRequest(value = VALUE, payloads = bothPayloads) }

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(PolymarketSigningError.Unknown)
    }

    @Test
    fun `GIVEN the formatter throws WHEN run THEN Unknown and no exception`() = runTest {
        // Arrange
        val formatter: PolymarketSignatureFormatter = mockk()
        every { formatter.format(any(), any(), any()) } throws IllegalArgumentException()
        stubDeriveAndSignSession { hashes -> hashes.map(::rawSignature).right() }

        // Act
        val actual = sut(formatter).run(userWalletId) { PolymarketSignRequest(value = VALUE, payloads = bothPayloads) }

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(PolymarketSigningError.Unknown)
    }

    @Test
    fun `GIVEN a card older than HD wallets WHEN run THEN DerivationUnsupported without a session`() = runTest {
        // Arrange
        givenColdWalletWithOldFirmware()

        // Act
        val actual = sut().run(userWalletId) { PolymarketSignRequest(value = VALUE, payloads = emptyList()) }

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(PolymarketSigningError.DerivationUnsupported)
        coVerify(exactly = 0) { deriveAndSignSession.run<Any?>(any(), any(), any(), any()) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN the session fails WHEN run THEN the error maps to a signing error`(model: ErrorModel) = runTest {
        // Arrange
        stubDeriveAndSignSession { model.sessionError.left() }

        // Act
        val actual = sut().run(userWalletId) { PolymarketSignRequest(value = VALUE, payloads = bothPayloads) }

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        ErrorModel(DeriveAndSignError.UserCancelled, PolymarketSigningError.UserCancelled),
        ErrorModel(DeriveAndSignError.WalletNotFound, PolymarketSigningError.MissingWallet),
        ErrorModel(DeriveAndSignError.CardError, PolymarketSigningError.CardError),
        ErrorModel(DeriveAndSignError.Unknown, PolymarketSigningError.Unknown),
    )

    data class ErrorModel(val sessionError: DeriveAndSignError, val expected: PolymarketSigningError)

    private companion object {
        const val VALUE = "prepared"
        const val CHAIN_CODE_SIZE = 32
        val OWNER_PRIVATE_KEY: BigInteger = BigInteger.ONE
    }
}