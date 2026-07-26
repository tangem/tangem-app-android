package com.tangem.data.polymarket.derivation

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.hexToBytes
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.models.MobileWallet
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.derivation.OWNER_DERIVATION_PATH
import com.tangem.domain.polymarket.model.PolymarketDerivationError
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.sdk.api.polymarket.PolymarketOwnerKeyData
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultPolymarketEoaDeriverTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val derivationsRepository: DerivationsRepository = mockk()
    private val tangemSdkManager: TangemSdkManager = mockk()
    private val addressFactory = PolymarketAddressFactory()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val deriver = DefaultPolymarketEoaDeriver(
        userWalletsListRepository = userWalletsListRepository,
        derivationsRepository = derivationsRepository,
        tangemSdkManager = tangemSdkManager,
        addressFactory = addressFactory,
        dispatchers = dispatchers,
    )

    private val userWalletId = UserWalletId("011")
    private val path = DerivationPath(OWNER_DERIVATION_PATH)
    private val seedKey = "02AABB".hexToBytes()
    private val seedKeyBAK = ByteArrayKey(seedKey)

    // Known-answer key from PolymarketAddressFactoryTest → 0x7E5F...Bdf
    private val knownKey = ExtendedPublicKey(
        publicKey = "0279BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798".hexToBytes(),
        chainCode = "0000000000000000000000000000000000000000000000000000000000000000".hexToBytes(),
    )
    private val knownAddress = "0x7E5F4552091A69125d5DfCb7b8C2659029395Bdf"

    @BeforeEach
    fun setup() {
        clearMocks(userWalletsListRepository, derivationsRepository, tangemSdkManager)
        mockkStatic(UserWalletsListRepository::getSyncStrict)
    }

    @AfterEach
    fun tearDownStaticMocks() {
        unmockkStatic(UserWalletsListRepository::getSyncStrict)
    }

    private fun coldWallet(firmwareSupportsHd: Boolean = true, hasSecpWallet: Boolean = true): UserWallet.Cold {
        val firmware = mockk<com.tangem.domain.models.scan.CardDTO.FirmwareVersion> {
            every { compareTo(FirmwareVersion.HDWalletAvailable) } returns if (firmwareSupportsHd) 0 else -1
        }
        val cardWallet = mockk<com.tangem.domain.models.scan.CardDTO.Wallet> {
            every { curve } returns EllipticCurve.Secp256k1
            every { publicKey } returns seedKey
        }
        return mockk {
            every { walletId } returns userWalletId
            every { scanResponse } returns mockk {
                every { card } returns mockk {
                    every { this@mockk.firmwareVersion } returns firmware
                    every { wallets } returns if (hasSecpWallet) listOf(cardWallet) else emptyList()
                }
            }
        }
    }

    private fun hotWallet(hasSecpWallet: Boolean = true): UserWallet.Hot {
        val mobileWallet = MobileWallet(
            publicKey = seedKey,
            chainCode = null,
            curve = EllipticCurve.Secp256k1,
            derivedKeys = emptyMap(),
        )
        return mockk {
            every { walletId } returns userWalletId
            every { wallets } returns if (hasSecpWallet) listOf(mobileWallet) else emptyList()
        }
    }

    @Test
    fun `GIVEN derived key already stored WHEN deriveOwnerEoa THEN returns address without touching card`() =
        runTest {
            // Arrange
            every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
            coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, seedKeyBAK) } returns
                ExtendedPublicKeysMap(mapOf(path to knownKey))

            // Act
            val result = deriver.deriveOwnerEoa(userWalletId)

            // Assert
            assertThat(result).isEqualTo(knownAddress.right())
            coVerify(exactly = 0) { tangemSdkManager.polymarketProduceOwnerKeyData(any()) }
            coVerify(exactly = 0) { derivationsRepository.derivePublicKeys(any(), any<Map<ByteArrayKey, List<DerivationPath>>>()) }
        }

    @Test
    fun `GIVEN cold wallet without cache WHEN deriveOwnerEoa THEN taps card stores keys and returns address`() =
        runTest {
            // Arrange
            val derivedKeys = mapOf(seedKeyBAK to ExtendedPublicKeysMap(mapOf(path to knownKey)))
            every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
            coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, seedKeyBAK) } returns
                ExtendedPublicKeysMap(emptyMap())
            coEvery { tangemSdkManager.polymarketProduceOwnerKeyData(any()) } returns
                PolymarketOwnerKeyData(derivedKeys = derivedKeys).right()
            coEvery { derivationsRepository.storeDerivedKeys(userWalletId, derivedKeys) } returns Unit

            // Act
            val result = deriver.deriveOwnerEoa(userWalletId)

            // Assert
            assertThat(result).isEqualTo(knownAddress.right())
            coVerify(exactly = 1) { derivationsRepository.storeDerivedKeys(userWalletId, derivedKeys) }
        }

    @Test
    fun `GIVEN hot wallet without cache WHEN deriveOwnerEoa THEN derives via repository and returns address`() =
        runTest {
            // Arrange
            every { userWalletsListRepository.getSyncStrict(userWalletId) } returns hotWallet()
            coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, seedKeyBAK) } returns
                ExtendedPublicKeysMap(emptyMap())
            coEvery {
                derivationsRepository.derivePublicKeys(userWalletId, mapOf(seedKeyBAK to listOf(path)))
            } returns mapOf(seedKeyBAK to ExtendedPublicKeysMap(mapOf(path to knownKey)))

            // Act
            val result = deriver.deriveOwnerEoa(userWalletId)

            // Assert
            assertThat(result).isEqualTo(knownAddress.right())
            coVerify(exactly = 1) {
                derivationsRepository.derivePublicKeys(userWalletId, mapOf(seedKeyBAK to listOf(path)))
            }
        }

    @Test
    fun `GIVEN cold firmware without HD WHEN deriveOwnerEoa THEN returns DerivationUnsupported`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns
            coldWallet(firmwareSupportsHd = false)

        // Act
        val result = deriver.deriveOwnerEoa(userWalletId)

        // Assert
        assertThat(result).isEqualTo(PolymarketDerivationError.DerivationUnsupported.left())
    }

    @Test
    fun `GIVEN cold wallet without secp256k1 key WHEN deriveOwnerEoa THEN returns MissingWallet`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns
            coldWallet(hasSecpWallet = false)

        // Act
        val result = deriver.deriveOwnerEoa(userWalletId)

        // Assert
        assertThat(result).isEqualTo(PolymarketDerivationError.MissingWallet.left())
    }

    @Test
    fun `GIVEN card task cancelled WHEN deriveOwnerEoa THEN returns UserCancelled`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
        coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, seedKeyBAK) } returns
            ExtendedPublicKeysMap(emptyMap())
        coEvery { tangemSdkManager.polymarketProduceOwnerKeyData(any()) } returns
            TangemSdkError.UserCancelled().left()

        // Act
        val result = deriver.deriveOwnerEoa(userWalletId)

        // Assert
        assertThat(result).isEqualTo(PolymarketDerivationError.UserCancelled.left())
    }

    @Test
    fun `GIVEN card task generic sdk error WHEN deriveOwnerEoa THEN returns CardError`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
        coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, seedKeyBAK) } returns
            ExtendedPublicKeysMap(emptyMap())
        coEvery { tangemSdkManager.polymarketProduceOwnerKeyData(any()) } returns
            TangemSdkError.MissingPreflightRead().left()

        // Act
        val result = deriver.deriveOwnerEoa(userWalletId)

        // Assert
        assertThat(result).isEqualTo(PolymarketDerivationError.CardError.left())
    }

    @Test
    fun `GIVEN card task WalletNotFound WHEN deriveOwnerEoa THEN returns MissingWallet`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
        coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, seedKeyBAK) } returns
            ExtendedPublicKeysMap(emptyMap())
        coEvery { tangemSdkManager.polymarketProduceOwnerKeyData(any()) } returns
            TangemSdkError.WalletNotFound().left()

        // Act
        val result = deriver.deriveOwnerEoa(userWalletId)

        // Assert
        assertThat(result).isEqualTo(PolymarketDerivationError.MissingWallet.left())
    }

    @Test
    fun `GIVEN card task non-sdk throwable WHEN deriveOwnerEoa THEN returns Unknown`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
        coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, seedKeyBAK) } returns
            ExtendedPublicKeysMap(emptyMap())
        coEvery { tangemSdkManager.polymarketProduceOwnerKeyData(any()) } returns
            IllegalStateException("boom").left()

        // Act
        val result = deriver.deriveOwnerEoa(userWalletId)

        // Assert
        assertThat(result).isEqualTo(PolymarketDerivationError.Unknown.left())
    }

    @Test
    fun `GIVEN hot derivation throws WHEN deriveOwnerEoa THEN returns Unknown`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns hotWallet()
        coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, seedKeyBAK) } returns
            ExtendedPublicKeysMap(emptyMap())
        coEvery {
            derivationsRepository.derivePublicKeys(userWalletId, mapOf(seedKeyBAK to listOf(path)))
        } throws IllegalStateException("hot sdk failed")

        // Act
        val result = deriver.deriveOwnerEoa(userWalletId)

        // Assert
        assertThat(result).isEqualTo(PolymarketDerivationError.Unknown.left())
    }
}