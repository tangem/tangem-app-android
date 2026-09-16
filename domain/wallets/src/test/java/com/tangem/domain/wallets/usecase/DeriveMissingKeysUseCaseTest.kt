package com.tangem.domain.wallets.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.hexToBytes
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.jointaccount.repository.JointOwnerKeysRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.derivations.DerivationsRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeriveMissingKeysUseCaseTest {

    private val derivationsRepository: DerivationsRepository = mockk()
    private val jointOwnerKeysRepository: JointOwnerKeysRepository = mockk()

    private val useCase = DeriveMissingKeysUseCase(
        derivationsRepository = derivationsRepository,
        jointOwnerKeysRepository = jointOwnerKeysRepository,
    )

    private val userWalletId = UserWalletId("011")
    private val currencies = listOf<CryptoCurrency>(mockk())
    private val currencySeedKey = ByteArrayKey("01AA".hexToBytes())
    private val jointSeedKey = ByteArrayKey("02BB".hexToBytes())
    private val currencyPath = DerivationPath(rawPath = "m/44'/60'/0'/0/0")
    private val jointPath = DerivationPath(rawPath = "m/44'/60'/888888'/0/2")

    @BeforeEach
    fun setUp() {
        clearMocks(derivationsRepository, jointOwnerKeysRepository)
        coEvery { derivationsRepository.derivePublicKeys(userWalletId = any(), derivations = any()) } returns emptyMap()
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
        assertThat(actual.isRight()).isTrue()
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
        assertThat(actual.isRight()).isTrue()
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
            assertThat(actual.isRight()).isTrue()
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
        assertThat(actual.isRight()).isTrue()
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
        assertThat(actual.isRight()).isTrue()
        coVerify(exactly = 0) { derivationsRepository.derivePublicKeys(userWalletId = any(), derivations = any()) }
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