package com.tangem.data.collectibles.account.derivation

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.collectibles.account.derivation.COLLECTIBLES_OWNER_DERIVATION_PATH
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.models.MobileWallet
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultCollectiblesOwnerDeriverTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val derivationsRepository: DerivationsRepository = mockk()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val deriver = DefaultCollectiblesOwnerDeriver(
        userWalletsListRepository = userWalletsListRepository,
        derivationsRepository = derivationsRepository,
        dispatchers = dispatchers,
    )

    private val userWalletId = UserWalletId("011")
    private val path = DerivationPath(rawPath = COLLECTIBLES_OWNER_DERIVATION_PATH)
    private val seedKey = "02AABB".hexToBytes()
    private val wrappedSeedKey = ByteArrayKey(seedKey)
    private val derivations = mapOf(wrappedSeedKey to listOf(path))

    private val knownKey = ExtendedPublicKey(
        publicKey = "3B6A27BCCEB6A42D62A3A8D02A6F0D73653215771DE243A63AC048A18B59DA29".hexToBytes(),
        chainCode = "0000000000000000000000000000000000000000000000000000000000000000".hexToBytes(),
    )
    private val knownOwner = "4zvwRjXUKGfvwnParsHAS3HuSVzV5cA4McphgmoCtajS"

    @BeforeEach
    fun setup() {
        clearMocks(userWalletsListRepository, derivationsRepository)
        mockkStatic(UserWalletsListRepository::getSyncStrict)
        mockkStatic(UserWalletsListRepository::getSyncOrNull)
    }

    @AfterEach
    fun tearDownStaticMocks() {
        unmockkStatic(UserWalletsListRepository::getSyncStrict)
        unmockkStatic(UserWalletsListRepository::getSyncOrNull)
    }

    private fun coldWallet(
        firmwareSupportsHd: Boolean = true,
        hasEdWallet: Boolean = true,
    ): UserWallet.Cold {
        val firmware = if (firmwareSupportsHd) {
            CardDTO.FirmwareVersion(major = 6, minor = 33, patch = 0, type = FirmwareVersion.FirmwareType.Sdk)
        } else {
            CardDTO.FirmwareVersion(major = 4, minor = 0, patch = 0, type = FirmwareVersion.FirmwareType.Sdk)
        }
        val cardWallet = mockk<CardDTO.Wallet> {
            every { curve } returns EllipticCurve.Ed25519Slip0010
            every { publicKey } returns seedKey
        }
        return mockk {
            every { walletId } returns userWalletId
            every { scanResponse } returns mockk {
                every { card } returns mockk {
                    every { this@mockk.firmwareVersion } returns firmware
                    every { wallets } returns if (hasEdWallet) listOf(cardWallet) else emptyList()
                }
            }
        }
    }

    private fun hotWallet(hasEdWallet: Boolean = true): UserWallet.Hot {
        val mobileWallet = MobileWallet(
            publicKey = seedKey,
            chainCode = null,
            curve = EllipticCurve.Ed25519Slip0010,
            derivedKeys = emptyMap(),
        )
        return mockk {
            every { walletId } returns userWalletId
            every { wallets } returns if (hasEdWallet) listOf(mobileWallet) else emptyList()
        }
    }

    @Test
    fun `GIVEN derived key already stored WHEN deriveOwner THEN returns owner without deriving`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
        coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, wrappedSeedKey) } returns
            ExtendedPublicKeysMap(mapOf(path to knownKey))

        // Act
        val result = deriver.deriveOwner(userWalletId)

        // Assert
        assertThat(result).isEqualTo(knownOwner.right())
        coVerify(exactly = 0) { derivationsRepository.derivePublicKeys(userWalletId, derivations) }
    }

    @Test
    fun `GIVEN cold wallet without cache WHEN deriveOwner THEN derives via repository and returns owner`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
        coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, wrappedSeedKey) } returns
            ExtendedPublicKeysMap(emptyMap())
        coEvery { derivationsRepository.derivePublicKeys(userWalletId, derivations) } returns
            mapOf(wrappedSeedKey to ExtendedPublicKeysMap(mapOf(path to knownKey)))

        // Act
        val result = deriver.deriveOwner(userWalletId)

        // Assert
        assertThat(result).isEqualTo(knownOwner.right())
        coVerify(exactly = 1) { derivationsRepository.derivePublicKeys(userWalletId, derivations) }
    }

    @Test
    fun `GIVEN hot wallet without cache WHEN deriveOwner THEN derives via repository and returns owner`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns hotWallet()
        coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, wrappedSeedKey) } returns
            ExtendedPublicKeysMap(emptyMap())
        coEvery { derivationsRepository.derivePublicKeys(userWalletId, derivations) } returns
            mapOf(wrappedSeedKey to ExtendedPublicKeysMap(mapOf(path to knownKey)))

        // Act
        val result = deriver.deriveOwner(userWalletId)

        // Assert
        assertThat(result).isEqualTo(knownOwner.right())
        coVerify(exactly = 1) { derivationsRepository.derivePublicKeys(userWalletId, derivations) }
    }

    @Test
    fun `GIVEN cold firmware without HD WHEN deriveOwner THEN returns WalletUnavailable`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet(firmwareSupportsHd = false)

        // Act
        val result = deriver.deriveOwner(userWalletId)

        // Assert
        assertThat(result).isEqualTo(CollectiblesAccountError.Vault(GachaVaultError.WalletUnavailable).left())
        coVerify(exactly = 0) { derivationsRepository.derivePublicKeys(userWalletId, derivations) }
    }

    @Test
    fun `GIVEN cold wallet without Solana key WHEN deriveOwner THEN returns WalletUnavailable`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet(hasEdWallet = false)

        // Act
        val result = deriver.deriveOwner(userWalletId)

        // Assert
        assertThat(result).isEqualTo(CollectiblesAccountError.Vault(GachaVaultError.WalletUnavailable).left())
        coVerify(exactly = 0) { derivationsRepository.derivePublicKeys(userWalletId, derivations) }
    }

    @Test
    fun `GIVEN hot wallet without Solana key WHEN deriveOwner THEN returns WalletUnavailable`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns hotWallet(hasEdWallet = false)

        // Act
        val result = deriver.deriveOwner(userWalletId)

        // Assert
        assertThat(result).isEqualTo(CollectiblesAccountError.Vault(GachaVaultError.WalletUnavailable).left())
        coVerify(exactly = 0) { derivationsRepository.derivePublicKeys(userWalletId, derivations) }
    }

    @Test
    fun `GIVEN derivation returns no key for the path WHEN deriveOwner THEN returns Unexpected`() = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
        coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, wrappedSeedKey) } returns
            ExtendedPublicKeysMap(emptyMap())
        coEvery { derivationsRepository.derivePublicKeys(userWalletId, derivations) } returns emptyMap()

        // Act
        val result = deriver.deriveOwner(userWalletId)

        // Assert
        val error = (result.leftOrNull() as CollectiblesAccountError.Vault).error
        assertThat(error).isInstanceOf(GachaVaultError.Unexpected::class.java)
        assertThat((error as GachaVaultError.Unexpected).cause)
            .hasMessageThat()
            .isEqualTo("No key derived for $COLLECTIBLES_OWNER_DERIVATION_PATH")
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN derivation throws WHEN deriveOwner THEN maps the throwable`(model: DerivationFailureModel) = runTest {
        // Arrange
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
        coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, wrappedSeedKey) } returns
            ExtendedPublicKeysMap(emptyMap())
        coEvery { derivationsRepository.derivePublicKeys(userWalletId, derivations) } throws model.throwable

        // Act
        val result = deriver.deriveOwner(userWalletId)

        // Assert
        assertThat(result).isEqualTo(model.expected.left())
    }

    @Test
    fun `GIVEN the wallet is missing WHEN deriveOwner THEN returns Unexpected`() = runTest {
        // Arrange
        val throwable = IllegalStateException("UserWallet $userWalletId not found")
        every { userWalletsListRepository.getSyncStrict(userWalletId) } throws throwable

        // Act
        val result = deriver.deriveOwner(userWalletId)

        // Assert
        assertThat(result).isEqualTo(CollectiblesAccountError.Vault(GachaVaultError.Unexpected(cause = throwable)).left())
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class StoredOwner {

        @Test
        fun `GIVEN the key is stored WHEN storedOwner THEN returns the owner without deriving`() = runTest {
            // Arrange
            every { userWalletsListRepository.getSyncOrNull(userWalletId) } returns coldWallet()
            coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, wrappedSeedKey) } returns
                ExtendedPublicKeysMap(mapOf(path to knownKey))

            // Act
            val result = deriver.storedOwner(userWalletId)

            // Assert
            assertThat(result).isEqualTo(knownOwner)
            coVerify(exactly = 0) { derivationsRepository.derivePublicKeys(userWalletId, derivations) }
        }

        @Test
        fun `GIVEN the key is not stored WHEN storedOwner THEN returns null AND never opens a card session`() =
            runTest {
                // Arrange
                every { userWalletsListRepository.getSyncOrNull(userWalletId) } returns coldWallet()
                coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, wrappedSeedKey) } returns
                    ExtendedPublicKeysMap(emptyMap())

                // Act
                val result = deriver.storedOwner(userWalletId)

                // Assert
                assertThat(result).isNull()
                coVerify(exactly = 0) { derivationsRepository.derivePublicKeys(userWalletId, derivations) }
            }

        @Test
        fun `GIVEN a wallet without Solana key WHEN storedOwner THEN returns null without touching derivations`() =
            runTest {
                // Arrange
                every { userWalletsListRepository.getSyncOrNull(userWalletId) } returns hotWallet(hasEdWallet = false)

                // Act
                val result = deriver.storedOwner(userWalletId)

                // Assert
                assertThat(result).isNull()
                coVerify(exactly = 0) { derivationsRepository.getExistingDerivedKeys(userWalletId, wrappedSeedKey) }
                coVerify(exactly = 0) { derivationsRepository.derivePublicKeys(userWalletId, derivations) }
            }

        @Test
        fun `GIVEN the wallet is missing WHEN storedOwner THEN returns null`() = runTest {
            // Arrange
            every { userWalletsListRepository.getSyncOrNull(userWalletId) } returns null

            // Act
            val result = deriver.storedOwner(userWalletId)

            // Assert
            assertThat(result).isNull()
            coVerify(exactly = 0) { derivationsRepository.derivePublicKeys(userWalletId, derivations) }
        }

        @Test
        fun `GIVEN derivations lookup throws WHEN storedOwner THEN returns null`() = runTest {
            // Arrange
            every { userWalletsListRepository.getSyncOrNull(userWalletId) } returns coldWallet()
            coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, wrappedSeedKey) } throws
                TangemSdkError.MissingPreflightRead()

            // Act
            val result = deriver.storedOwner(userWalletId)

            // Assert
            assertThat(result).isNull()
        }
    }

    internal data class DerivationFailureModel(
        val throwable: Throwable,
        val expected: CollectiblesAccountError,
    )

    private fun provideTestModels(): List<DerivationFailureModel> {
        val cardError = TangemSdkError.MissingPreflightRead()
        val unknown = IllegalStateException("boom")

        return listOf(
            DerivationFailureModel(
                throwable = TangemSdkError.UserCancelled(),
                expected = CollectiblesAccountError.Cancelled,
            ),
            DerivationFailureModel(
                throwable = cardError,
                expected = CollectiblesAccountError.CardError(code = cardError.code, message = cardError.customMessage),
            ),
            DerivationFailureModel(
                throwable = unknown,
                expected = CollectiblesAccountError.Vault(GachaVaultError.Unexpected(cause = unknown)),
            ),
        )
    }
}