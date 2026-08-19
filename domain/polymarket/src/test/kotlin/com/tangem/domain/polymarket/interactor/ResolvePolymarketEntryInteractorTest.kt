package com.tangem.domain.polymarket.interactor

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketDerivationError
import com.tangem.domain.polymarket.model.PolymarketEntry
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketWalletStatusUseCase
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResolvePolymarketEntryInteractorTest {

    private val deriveAddresses: DerivePolymarketAddressesUseCase = mockk()
    private val getWalletStatus: GetPolymarketWalletStatusUseCase = mockk()
    private val getApiCredentials: GetPolymarketApiCredentialsUseCase = mockk()

    private val useCase = ResolvePolymarketEntryInteractor(
        derivePolymarketAddressesUseCase = deriveAddresses,
        getPolymarketWalletStatusUseCase = getWalletStatus,
        getPolymarketApiCredentialsUseCase = getApiCredentials,
    )

    private val userWalletId = UserWalletId("011")
    private val addresses = PolymarketAddresses(
        ownerAddress = "0xOwner",
        depositWalletAddress = "0xDeposit",
        userWalletId = userWalletId,
    )
    private val credentials = PolymarketApiCredentials(apiKey = "key", secret = "secret", passphrase = "pass")

    @BeforeEach
    fun resetMocks() {
        clearMocks(deriveAddresses, getWalletStatus, getApiCredentials)
        coEvery { getApiCredentials(any()) } returns credentials
    }

    @ParameterizedTest
    @ProvideTestModels
    fun resolve(model: ResolveModel) = runTest {
        // Arrange
        coEvery { deriveAddresses(userWalletId) } returns addresses.right()
        coEvery { getWalletStatus(addresses) } returns PolymarketWalletState(
            depositWalletAddress = model.depositWalletAddress,
            status = model.status,
        ).right()
        coEvery { getApiCredentials(userWalletId) } returns credentials.takeIf { model.hasCredentials }

        // Act
        val actual = useCase(userWalletId)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(model.expected)
    }

    internal data class ResolveModel(
        val depositWalletAddress: String?,
        val status: PolymarketWalletStatus,
        val hasCredentials: Boolean = true,
        val expected: PolymarketEntry,
    )

    private fun provideTestModels() = listOf(
        ResolveModel(
            depositWalletAddress = null,
            status = PolymarketWalletStatus.NOT_CREATED,
            expected = PolymarketEntry.Onboard(PolymarketWalletStatus.NOT_CREATED),
        ),
        ResolveModel(
            depositWalletAddress = "0xDeposit",
            status = PolymarketWalletStatus.DEPLOYED,
            expected = PolymarketEntry.Onboard(PolymarketWalletStatus.DEPLOYED),
        ),
        ResolveModel(
            depositWalletAddress = "0xDeposit",
            status = PolymarketWalletStatus.APPROVALS_FAILED,
            expected = PolymarketEntry.Onboard(PolymarketWalletStatus.APPROVALS_FAILED),
        ),
        ResolveModel(
            depositWalletAddress = "0xDeposit",
            status = PolymarketWalletStatus.READY_TO_TRADE,
            expected = PolymarketEntry.Onboarded,
        ),
        // A reinstall or a second device leaves the backend ready with nothing local to sign with: that user
        // owes a credential-restore run, not the feed.
        ResolveModel(
            depositWalletAddress = "0xDeposit",
            status = PolymarketWalletStatus.READY_TO_TRADE,
            hasCredentials = false,
            expected = PolymarketEntry.Onboard(PolymarketWalletStatus.READY_TO_TRADE),
        ),
    )

    @Test
    fun `GIVEN derivation cancelled WHEN invoke THEN fails without reading wallet status`() = runTest {
        // Arrange
        val error = PolymarketOnboardingError.Derivation(PolymarketDerivationError.UserCancelled)
        coEvery { deriveAddresses(userWalletId) } returns error.left()

        // Act
        val actual = useCase(userWalletId)

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(error)
        coVerify(exactly = 0) { getWalletStatus(any()) }
    }

    @Test
    fun `GIVEN wallet status read fails WHEN invoke THEN fails`() = runTest {
        // Arrange
        coEvery { deriveAddresses(userWalletId) } returns addresses.right()
        coEvery { getWalletStatus(addresses) } returns PolymarketOnboardingError.Network.left()

        // Act
        val actual = useCase(userWalletId)

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(PolymarketOnboardingError.Network)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class WithoutPrompting {

        @Test
        fun `GIVEN the address is not stored WHEN withoutPrompting THEN Undetermined AND nothing is derived`() =
            runTest {
                // Arrange
                coEvery { deriveAddresses.stored(userWalletId) } returns null

                // Act
                val actual = useCase.withoutPrompting(userWalletId)

                // Assert
                assertThat(actual.getOrNull()).isEqualTo(PolymarketEntry.Undetermined)
                coVerify(exactly = 0) { deriveAddresses(userWalletId) }
                coVerify(exactly = 0) { getWalletStatus(addresses) }
            }

        @Test
        fun `GIVEN the address is stored WHEN withoutPrompting THEN resolves in full without deriving`() = runTest {
            // Arrange
            coEvery { deriveAddresses.stored(userWalletId) } returns addresses
            coEvery { getWalletStatus(addresses) } returns PolymarketWalletState(
                depositWalletAddress = "0xDeposit",
                status = PolymarketWalletStatus.READY_TO_TRADE,
            ).right()

            // Act
            val actual = useCase.withoutPrompting(userWalletId)

            // Assert
            assertThat(actual.getOrNull()).isEqualTo(PolymarketEntry.Onboarded)
            coVerify(exactly = 0) { deriveAddresses(userWalletId) }
        }
    }
}