package com.tangem.domain.polymarket.interactor

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketOnboardedStore
import com.tangem.domain.polymarket.usecase.RecordPolymarketConfirmationUseCase
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
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
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
    private val onboardedStore: PolymarketOnboardedStore = mockk()

    private val useCase = ResolvePolymarketEntryInteractor(
        derivePolymarketAddressesUseCase = deriveAddresses,
        getPolymarketWalletStatusUseCase = getWalletStatus,
        getPolymarketApiCredentialsUseCase = getApiCredentials,
        polymarketOnboardedStore = onboardedStore,
        recordPolymarketConfirmation = RecordPolymarketConfirmationUseCase(onboardedStore = onboardedStore),
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
        clearMocks(deriveAddresses, getWalletStatus, getApiCredentials, onboardedStore)
        coEvery { getApiCredentials(any()) } returns credentials
        coEvery { onboardedStore.isOnboarded(any()) } returns false
        coEvery { onboardedStore.markOnboarded(any()) } just Runs
        coEvery { onboardedStore.clear(any()) } just Runs
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

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConfirmedOnboarded {

        @Test
        fun `GIVEN a confirmed wallet WHEN withoutPrompting THEN resolves without the backend`() = runTest {
            // Arrange
            coEvery { onboardedStore.isOnboarded(userWalletId) } returns true

            // Act
            val actual = useCase.withoutPrompting(userWalletId)

            // Assert
            assertThat(actual.getOrNull()).isEqualTo(PolymarketEntry.Onboarded)
            coVerify(exactly = 0) { getWalletStatus(any()) }
            coVerify(exactly = 0) { deriveAddresses.stored(any()) }
        }

        @Test
        fun `GIVEN a confirmed wallet WHEN invoke THEN nothing is derived AND no card session is opened`() = runTest {
            // Arrange
            coEvery { onboardedStore.isOnboarded(userWalletId) } returns true

            // Act
            val actual = useCase(userWalletId)

            // Assert
            assertThat(actual.getOrNull()).isEqualTo(PolymarketEntry.Onboarded)
            coVerify(exactly = 0) { deriveAddresses(any()) }
            coVerify(exactly = 0) { getWalletStatus(any()) }
        }

        @Test
        fun `GIVEN a confirmed wallet whose credentials are gone WHEN withoutPrompting THEN the backend is asked`() =
            runTest {
                // Arrange
                coEvery { onboardedStore.isOnboarded(userWalletId) } returns true
                coEvery { getApiCredentials(any()) } returns null
                coEvery { deriveAddresses.stored(userWalletId) } returns addresses
                coEvery { getWalletStatus(addresses) } returns PolymarketWalletState(
                    depositWalletAddress = addresses.depositWalletAddress,
                    status = PolymarketWalletStatus.READY_TO_TRADE,
                ).right()

                // Act
                val actual = useCase.withoutPrompting(userWalletId)

                // Assert
                assertThat(actual.getOrNull())
                    .isEqualTo(PolymarketEntry.Onboard(status = PolymarketWalletStatus.READY_TO_TRADE))
                coVerify(exactly = 1) { getWalletStatus(addresses) }
            }

        @Test
        fun `GIVEN the backend reports ready WHEN resolved THEN the wallet is recorded as confirmed`() = runTest {
            // Arrange
            coEvery { deriveAddresses(userWalletId) } returns addresses.right()
            coEvery { getWalletStatus(addresses) } returns PolymarketWalletState(
                depositWalletAddress = addresses.depositWalletAddress,
                status = PolymarketWalletStatus.READY_TO_TRADE,
            ).right()

            // Act
            useCase(userWalletId)

            // Assert
            coVerify(exactly = 1) { onboardedStore.markOnboarded(userWalletId) }
            coVerify(exactly = 0) { onboardedStore.clear(any()) }
        }

        /**
         * Without this the record would outlive the fact: a wallet the backend no longer has could never re-run
         * onboarding, because the gate would keep answering from the stale record.
         */
        @Test
        fun `GIVEN the backend no longer has the wallet WHEN resolved THEN the record is dropped`() = runTest {
            // Arrange
            coEvery { onboardedStore.isOnboarded(userWalletId) } returns true
            coEvery { getApiCredentials(any()) } returns null
            coEvery { deriveAddresses(userWalletId) } returns addresses.right()
            coEvery { getWalletStatus(addresses) } returns PolymarketWalletState(
                depositWalletAddress = addresses.depositWalletAddress,
                status = PolymarketWalletStatus.NOT_CREATED,
            ).right()

            // Act
            useCase(userWalletId)

            // Assert
            coVerify(exactly = 1) { onboardedStore.clear(userWalletId) }
            coVerify(exactly = 0) { onboardedStore.markOnboarded(any()) }
        }

        /** A failed setup is not the backend withdrawing a wallet, so a confirmation already given stands. */
        @Test
        fun `GIVEN the setup failed WHEN resolved THEN the record survives`() = runTest {
            // Arrange
            coEvery { onboardedStore.isOnboarded(userWalletId) } returns true
            coEvery { getApiCredentials(any()) } returns null
            coEvery { deriveAddresses(userWalletId) } returns addresses.right()
            coEvery { getWalletStatus(addresses) } returns PolymarketWalletState(
                depositWalletAddress = addresses.depositWalletAddress,
                status = PolymarketWalletStatus.APPROVALS_FAILED,
            ).right()

            // Act
            useCase(userWalletId)

            // Assert
            coVerify(exactly = 0) { onboardedStore.clear(any()) }
            coVerify(exactly = 0) { onboardedStore.markOnboarded(any()) }
        }
    }
}