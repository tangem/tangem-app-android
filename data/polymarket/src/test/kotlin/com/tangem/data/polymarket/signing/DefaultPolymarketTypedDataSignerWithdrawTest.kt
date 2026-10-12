package com.tangem.data.polymarket.signing

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
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
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.derivation.POLYMARKET_OWNER_DERIVATION_PATH
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.signing.PolymarketApprovalsPayload
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.operations.derivation.ExtendedPublicKeysMap
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
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import java.math.BigInteger

internal class DefaultPolymarketTypedDataSignerWithdrawTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val derivationsRepository: DerivationsRepository = mockk()
    private val cardSdkConfigRepository: CardSdkConfigRepository = mockk()
    private val hotSignerFactory: TangemHotWalletSigner.Factory = mockk()
    private val transactionSigner: TransactionSigner = mockk()

    private val signer = DefaultPolymarketTypedDataSigner(
        userWalletsListRepository = userWalletsListRepository,
        derivationsRepository = derivationsRepository,
        cardSdkConfigRepository = cardSdkConfigRepository,
        hotSignerFactory = hotSignerFactory,
        addressFactory = PolymarketAddressFactory(),
        formatter = PolymarketSignatureFormatter(),
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val path = DerivationPath(POLYMARKET_OWNER_DERIVATION_PATH)
    private val seedKey = "02AABB".hexToBytes()

    private val ownerKey = ExtendedPublicKey(
        publicKey = "0279BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798".hexToBytes(),
        chainCode = "00".repeat(CHAIN_CODE_SIZE).hexToBytes(),
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
        every { userWalletsListRepository.getSyncStrict(USER_WALLET_ID) } returns coldWallet()
        coEvery { derivationsRepository.getExistingDerivedKeys(USER_WALLET_ID, ByteArrayKey(seedKey)) } returns
            ExtendedPublicKeysMap(mapOf(path to ownerKey))
        every { cardSdkConfigRepository.getCommonSigner(any(), any(), USER_WALLET_ID) } returns transactionSigner
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(UserWalletsListRepository::getSyncStrict)
    }

    @Test
    fun `GIVEN a withdrawal batch WHEN signed THEN exactly one digest is sent to the card`() = runTest {
        // Arrange
        val hashes = slot<List<ByteArray>>()
        coEvery { transactionSigner.sign(capture(hashes), any()) } answers {
            CompletionResult.Success(hashes.captured.map { sign(it) })
        }

        // Act
        signer.signRelayedBatch(userWalletId = USER_WALLET_ID, payload = payload())

        // Assert
        assertThat(hashes.captured).hasSize(1)
        coVerify(exactly = 1) { transactionSigner.sign(any<List<ByteArray>>(), any()) }
    }

    @Test
    fun `GIVEN a withdrawal batch WHEN signed THEN the digest is the one-call approvals batch`() = runTest {
        // Arrange
        val hashes = slot<List<ByteArray>>()
        val publicKey = slot<Wallet.PublicKey>()
        coEvery { transactionSigner.sign(capture(hashes), capture(publicKey)) } answers {
            CompletionResult.Success(hashes.captured.map { sign(it) })
        }

        // Act
        val actual = signer.signRelayedBatch(userWalletId = USER_WALLET_ID, payload = payload())

        // Assert
        val expectedDigest = EthereumUtils.makeTypedDataHash(
            PolymarketTypedDataBuilder.buildApprovalsBatch(
                depositWallet = payload().depositWalletAddress,
                nonce = payload().nonce,
                deadline = payload().deadline,
                calls = payload().calls,
            ),
        )
        assertThat(hashes.captured.single()).isEqualTo(expectedDigest)
        assertThat(actual.getOrNull()).isEqualTo(
            PolymarketSignatureFormatter().format(sign(expectedDigest), expectedDigest, publicKey.captured),
        )
    }

    private fun payload() = PolymarketApprovalsPayload(
        depositWalletAddress = DEPOSIT_WALLET,
        nonce = "9",
        deadline = "1787346200",
        calls = listOf(
            PolymarketApprovalCall(
                target = PolymarketContracts.COLLATERAL,
                value = "0",
                data = "0xa9059cbb",
            ),
        ),
    )

    private fun coldWallet(): UserWallet.Cold {
        val cardWallet = mockk<com.tangem.domain.models.scan.CardDTO.Wallet> {
            every { curve } returns EllipticCurve.Secp256k1
            every { publicKey } returns seedKey
        }
        return mockk {
            every { walletId } returns USER_WALLET_ID
            every { scanResponse } returns mockk(relaxed = true) {
                every { productType } returns ProductType.Wallet2
                every { card } returns mockk(relaxed = true) {
                    every { wallets } returns listOf(cardWallet)
                    every { cardId } returns "CB79"
                    every { backupStatus } returns null
                }
            }
        }
    }

    private fun sign(hash: ByteArray): ByteArray {
        val signature = Sign.signMessage(hash, ECKeyPair.create(OWNER_PRIVATE_KEY), false)
        return signature.r + signature.s
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val CHAIN_CODE_SIZE = 32
        const val DEPOSIT_WALLET = "0x5A057dc6394196fBa51D84457DA3FB8FA241Fc20"
        val OWNER_PRIVATE_KEY: BigInteger = BigInteger.ONE
    }
}