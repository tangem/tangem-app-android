package com.tangem.domain.wallets.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.hexToBytes
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.jointaccount.repository.JointOwnerKeysRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeriveCurrencyAndOwnerKeysUseCaseTest {

    private val derivationsRepository: DerivationsRepository = mockk()
    private val jointOwnerKeysRepository: JointOwnerKeysRepository = mockk()

    private val useCase = DeriveCurrencyAndOwnerKeysUseCase(
        derivationsRepository = derivationsRepository,
        jointOwnerKeysRepository = jointOwnerKeysRepository,
    )

    private val userWalletId = UserWalletId("011")
    private val currencies = listOf<CryptoCurrency>(mockk())
    private val currencySeedKey = ByteArrayKey("01AA".hexToBytes())
    private val jointSeedKey = ByteArrayKey("02BB".hexToBytes())
    private val currencyPath = DerivationPath(rawPath = "m/44'/60'/0'/0/0")
    private val jointPath = DerivationPath(rawPath = "m/44'/60'/888888'/0/2")
    private val derivedKey: ExtendedPublicKey = mockk()

    @BeforeEach
    fun setUp() {
        clearMocks(derivationsRepository, jointOwnerKeysRepository)
        coEvery { derivationsRepository.derivePublicKeys(userWalletId = any(), derivations = any()) } returns
            mapOf(currencySeedKey to ExtendedPublicKeysMap(mapOf(currencyPath to derivedKey)))
    }

    @Test
    fun `GIVEN only currency derivations missing WHEN invoke THEN derived alone`() = runTest {
        // Arrange
        coEvery { derivationsRepository.findMissingDerivations(userWalletId, currencies) } returns
            mapOf(currencySeedKey to listOf(currencyPath))
        coEvery { jointOwnerKeysRepository.findMissingOwnerKeyDerivations(userWalletId) } returns emptyMap()

        // Act
        val actual = useCase(userWalletId, currencies)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(DerivationOutcome.DERIVED)
        coVerify(exactly = 1) {
            derivationsRepository.derivePublicKeys(userWalletId, mapOf(currencySeedKey to listOf(currencyPath)))
        }
    }

    @Test
    fun `GIVEN only joint owner keys missing WHEN invoke THEN derived alone`() = runTest {
        // Arrange
        coEvery { derivationsRepository.findMissingDerivations(userWalletId, currencies) } returns emptyMap()
        coEvery { jointOwnerKeysRepository.findMissingOwnerKeyDerivations(userWalletId) } returns
            mapOf(jointSeedKey to listOf(jointPath))

        // Act
        val actual = useCase(userWalletId, currencies)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(DerivationOutcome.DERIVED)
        coVerify(exactly = 1) {
            derivationsRepository.derivePublicKeys(userWalletId, mapOf(jointSeedKey to listOf(jointPath)))
        }
    }

    @Test
    fun `GIVEN currency and joint derivations missing under different seed keys WHEN invoke THEN merged in one call`() =
        runTest {
            // Arrange
            coEvery { derivationsRepository.findMissingDerivations(userWalletId, currencies) } returns
                mapOf(currencySeedKey to listOf(currencyPath))
            coEvery { jointOwnerKeysRepository.findMissingOwnerKeyDerivations(userWalletId) } returns
                mapOf(jointSeedKey to listOf(jointPath))

            // Act
            val actual = useCase(userWalletId, currencies)

            // Assert
            assertThat(actual.getOrNull()).isEqualTo(DerivationOutcome.DERIVED)
            coVerify(exactly = 1) {
                derivationsRepository.derivePublicKeys(
                    userWalletId,
                    mapOf(currencySeedKey to listOf(currencyPath), jointSeedKey to listOf(jointPath)),
                )
            }
        }

    @Test
    fun `GIVEN currency and joint derivations share a seed key WHEN invoke THEN their paths are combined`() = runTest {
        // Arrange
        coEvery { derivationsRepository.findMissingDerivations(userWalletId, currencies) } returns
            mapOf(currencySeedKey to listOf(currencyPath))
        coEvery { jointOwnerKeysRepository.findMissingOwnerKeyDerivations(userWalletId) } returns
            mapOf(currencySeedKey to listOf(jointPath))

        // Act
        val actual = useCase(userWalletId, currencies)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(DerivationOutcome.DERIVED)
        coVerify(exactly = 1) {
            derivationsRepository.derivePublicKeys(
                userWalletId,
                mapOf(currencySeedKey to listOf(currencyPath, jointPath)),
            )
        }
    }

    @Test
    fun `GIVEN nothing missing WHEN invoke THEN nothing is derived`() = runTest {
        // Arrange
        coEvery { derivationsRepository.findMissingDerivations(userWalletId, currencies) } returns emptyMap()
        coEvery { jointOwnerKeysRepository.findMissingOwnerKeyDerivations(userWalletId) } returns emptyMap()

        // Act
        val actual = useCase(userWalletId, currencies)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(DerivationOutcome.NOTHING_MISSING)
        coVerify(exactly = 0) { derivationsRepository.derivePublicKeys(userWalletId = any(), derivations = any()) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN card derives nothing WHEN invoke THEN outcome is NOTHING_DERIVED`(derivedKeys: DerivedKeysModel) =
        runTest {
            // Arrange
            coEvery { derivationsRepository.findMissingDerivations(userWalletId, currencies) } returns
                mapOf(currencySeedKey to listOf(currencyPath))
            coEvery { jointOwnerKeysRepository.findMissingOwnerKeyDerivations(userWalletId) } returns emptyMap()
            coEvery { derivationsRepository.derivePublicKeys(userWalletId = any(), derivations = any()) } returns
                derivedKeys.keys

            // Act
            val actual = useCase(userWalletId, currencies)

            // Assert
            assertThat(actual.getOrNull()).isEqualTo(DerivationOutcome.NOTHING_DERIVED)
        }

    @Test
    fun `GIVEN card derives a part of the requested keys WHEN invoke THEN outcome is DERIVED`() = runTest {
        // Arrange
        coEvery { derivationsRepository.findMissingDerivations(userWalletId, currencies) } returns
            mapOf(currencySeedKey to listOf(currencyPath))
        coEvery { jointOwnerKeysRepository.findMissingOwnerKeyDerivations(userWalletId) } returns
            mapOf(jointSeedKey to listOf(jointPath))
        coEvery { derivationsRepository.derivePublicKeys(userWalletId = any(), derivations = any()) } returns mapOf(
            currencySeedKey to ExtendedPublicKeysMap(mapOf(currencyPath to derivedKey)),
            jointSeedKey to ExtendedPublicKeysMap(emptyMap()),
        )

        // Act
        val actual = useCase(userWalletId, currencies)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(DerivationOutcome.DERIVED)
    }

    @Test
    fun `GIVEN the wallet could not derive at all WHEN invoke THEN outcome is not NOTHING_DERIVED`() = runTest {
        // Arrange — a locked mobile wallet answers with no entries instead of empty key maps
        coEvery { derivationsRepository.findMissingDerivations(userWalletId, currencies) } returns
            mapOf(currencySeedKey to listOf(currencyPath))
        coEvery { jointOwnerKeysRepository.findMissingOwnerKeyDerivations(userWalletId) } returns emptyMap()
        coEvery { derivationsRepository.derivePublicKeys(userWalletId = any(), derivations = any()) } returns emptyMap()

        // Act
        val actual = useCase(userWalletId, currencies)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(DerivationOutcome.DERIVED)
    }

    @Test
    fun `GIVEN the only missing wallet has no paths WHEN invoke THEN nothing is derived`() = runTest {
        // Arrange
        coEvery { derivationsRepository.findMissingDerivations(userWalletId, currencies) } returns
            mapOf(currencySeedKey to emptyList())
        coEvery { jointOwnerKeysRepository.findMissingOwnerKeyDerivations(userWalletId) } returns emptyMap()

        // Act
        val actual = useCase(userWalletId, currencies)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(DerivationOutcome.NOTHING_MISSING)
        coVerify(exactly = 0) { derivationsRepository.derivePublicKeys(userWalletId = any(), derivations = any()) }
    }

    private fun provideTestModels() = listOf(
        DerivedKeysModel(
            case = "an empty key map for the only requested wallet",
            keys = mapOf(currencySeedKey to ExtendedPublicKeysMap(emptyMap())),
        ),
        DerivedKeysModel(
            case = "an empty key map for every requested wallet",
            keys = mapOf(
                currencySeedKey to ExtendedPublicKeysMap(emptyMap()),
                jointSeedKey to ExtendedPublicKeysMap(emptyMap()),
            ),
        ),
    )

    internal data class DerivedKeysModel(
        val case: String,
        val keys: Map<ByteArrayKey, ExtendedPublicKeysMap>,
    ) {
        override fun toString(): String = case
    }

    @Test
    fun `GIVEN derivation throws WHEN invoke THEN Left is returned`() = runTest {
        // Arrange
        val error = IllegalStateException("Card session failed")
        coEvery { derivationsRepository.findMissingDerivations(userWalletId, currencies) } returns
            mapOf(currencySeedKey to listOf(currencyPath))
        coEvery { jointOwnerKeysRepository.findMissingOwnerKeyDerivations(userWalletId) } returns emptyMap()
        coEvery { derivationsRepository.derivePublicKeys(userWalletId = any(), derivations = any()) } throws error

        // Act
        val actual = useCase(userWalletId, currencies)

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(error)
    }
}