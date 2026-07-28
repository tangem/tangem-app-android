package com.tangem.data.polymarket.derivation

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.models.MobileWallet
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.derivation.POLYMARKET_OWNER_DERIVATION_PATH
import com.tangem.domain.polymarket.model.PolymarketDerivationError
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
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultPolymarketEoaDeriverTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val derivationsRepository: DerivationsRepository = mockk()
    private val addressFactory = PolymarketAddressFactory()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val deriver = DefaultPolymarketEoaDeriver(
        userWalletsListRepository = userWalletsListRepository,
        derivationsRepository = derivationsRepository,
        addressFactory = addressFactory,
        dispatchers = dispatchers,
    )

    private val userWalletId = UserWalletId("011")
    private val path = DerivationPath(POLYMARKET_OWNER_DERIVATION_PATH)
    private val seedKey = "02AABB".hexToBytes()
    private val seedKeyBAK = ByteArrayKey(seedKey)
    private val derivations = mapOf(seedKeyBAK to listOf(path))

    private val knownKey = ExtendedPublicKey(
        publicKey = "0279BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798".hexToBytes(),
        chainCode = "0000000000000000000000000000000000000000000000000000000000000000".hexToBytes(),
    )
    private val knownAddress = "0x7E5F4552091A69125d5DfCb7b8C2659029395Bdf"

    @BeforeEach
    fun setup() {
        clearMocks(userWalletsListRepository, derivationsRepository)
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
    fun `GIVEN derived key already stored WHEN deriveOwnerEoa THEN returns address without deriving`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
        coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, seedKeyBAK) } returns
            ExtendedPublicKeysMap(mapOf(path to knownKey))

        // Act
        val result = deriver.deriveOwnerEoa(userWalletId)

        // Assert
        assertThat(result).isEqualTo(knownAddress.right())
        coVerify(exactly = 0) { derivationsRepository.derivePublicKeys(userWalletId, derivations) }
    }

    @Test
    fun `GIVEN cold wallet without cache WHEN deriveOwnerEoa THEN derives via repository and returns address`() =
        runTest {
            // Arrange
            every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
            coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, seedKeyBAK) } returns
                ExtendedPublicKeysMap(emptyMap())
            coEvery { derivationsRepository.derivePublicKeys(userWalletId, derivations) } returns
                mapOf(seedKeyBAK to ExtendedPublicKeysMap(mapOf(path to knownKey)))

            // Act
            val result = deriver.deriveOwnerEoa(userWalletId)

            // Assert
            assertThat(result).isEqualTo(knownAddress.right())
            coVerify(exactly = 1) { derivationsRepository.derivePublicKeys(userWalletId, derivations) }
        }

    @Test
    fun `GIVEN hot wallet without cache WHEN deriveOwnerEoa THEN derives via repository and returns address`() =
        runTest {
            // Arrange
            every { userWalletsListRepository.getSyncStrict(userWalletId) } returns hotWallet()
            coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, seedKeyBAK) } returns
                ExtendedPublicKeysMap(emptyMap())
            coEvery { derivationsRepository.derivePublicKeys(userWalletId, derivations) } returns
                mapOf(seedKeyBAK to ExtendedPublicKeysMap(mapOf(path to knownKey)))

            // Act
            val result = deriver.deriveOwnerEoa(userWalletId)

            // Assert
            assertThat(result).isEqualTo(knownAddress.right())
            coVerify(exactly = 1) { derivationsRepository.derivePublicKeys(userWalletId, derivations) }
        }

    @Test
    fun `GIVEN cold firmware without HD WHEN deriveOwnerEoa THEN returns DerivationUnsupported`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet(firmwareSupportsHd = false)

        // Act
        val result = deriver.deriveOwnerEoa(userWalletId)

        // Assert
        assertThat(result).isEqualTo(PolymarketDerivationError.DerivationUnsupported.left())
    }

    @Test
    fun `GIVEN cold wallet without secp256k1 key WHEN deriveOwnerEoa THEN returns MissingWallet`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet(hasSecpWallet = false)

        // Act
        val result = deriver.deriveOwnerEoa(userWalletId)

        // Assert
        assertThat(result).isEqualTo(PolymarketDerivationError.MissingWallet.left())
    }

    @Test
    fun `GIVEN derivation returns no key for the path WHEN deriveOwnerEoa THEN returns Unknown`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
        coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, seedKeyBAK) } returns
            ExtendedPublicKeysMap(emptyMap())
        coEvery { derivationsRepository.derivePublicKeys(userWalletId, derivations) } returns emptyMap()

        // Act
        val result = deriver.deriveOwnerEoa(userWalletId)

        // Assert
        assertThat(result).isEqualTo(PolymarketDerivationError.Unknown.left())
    }

    @Test
    fun `GIVEN wallet is missing WHEN deriveOwnerEoa THEN returns Unknown`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } throws
            IllegalStateException("UserWallet $userWalletId not found")

        // Act
        val result = deriver.deriveOwnerEoa(userWalletId)

        // Assert
        assertThat(result).isEqualTo(PolymarketDerivationError.Unknown.left())
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN derivation throws WHEN deriveOwnerEoa THEN maps the throwable`(model: DerivationFailureModel) =
        runTest {
            // Arrange
            every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
            coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, seedKeyBAK) } returns
                ExtendedPublicKeysMap(emptyMap())
            coEvery { derivationsRepository.derivePublicKeys(userWalletId, derivations) } throws model.throwable

            // Act
            val result = deriver.deriveOwnerEoa(userWalletId)

            // Assert
            assertThat(result).isEqualTo(model.expected.left())
        }

    internal data class DerivationFailureModel(val throwable: Throwable, val expected: PolymarketDerivationError)

    private fun provideTestModels() = listOf(
        DerivationFailureModel(TangemSdkError.UserCancelled(), PolymarketDerivationError.UserCancelled),
        DerivationFailureModel(TangemSdkError.WalletNotFound(), PolymarketDerivationError.MissingWallet),
        DerivationFailureModel(TangemSdkError.MissingPreflightRead(), PolymarketDerivationError.CardError),
        DerivationFailureModel(IllegalStateException("boom"), PolymarketDerivationError.Unknown),
    )
}