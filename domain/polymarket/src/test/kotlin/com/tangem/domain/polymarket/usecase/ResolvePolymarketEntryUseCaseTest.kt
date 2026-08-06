package com.tangem.domain.polymarket.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketAccessMode
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketDerivationError
import com.tangem.domain.polymarket.model.PolymarketEntry
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResolvePolymarketEntryUseCaseTest {

    private val checkGeoblock: CheckPolymarketGeoblockUseCase = mockk()
    private val deriveAddresses: DerivePolymarketAddressesUseCase = mockk()
    private val getWalletStatus: GetPolymarketWalletStatusUseCase = mockk()

    private val useCase = ResolvePolymarketEntryUseCase(
        checkPolymarketGeoblockUseCase = checkGeoblock,
        derivePolymarketAddressesUseCase = deriveAddresses,
        getPolymarketWalletStatusUseCase = getWalletStatus,
    )

    private val userWalletId = UserWalletId("011")
    private val addresses = PolymarketAddresses(
        ownerAddress = "0xOwner",
        depositWalletAddress = "0xDeposit",
        userWalletId = userWalletId,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(checkGeoblock, deriveAddresses, getWalletStatus)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun resolve(model: ResolveModel) = runTest {
        // Arrange
        coEvery { checkGeoblock() } returns model.isBlocked.right()
        coEvery { deriveAddresses(userWalletId) } returns addresses.right()
        coEvery { getWalletStatus(addresses) } returns PolymarketWalletState(
            depositWalletAddress = model.depositWalletAddress,
            status = model.status,
        ).right()

        // Act
        val actual = useCase(userWalletId)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(model.expected)
    }

    internal data class ResolveModel(
        val isBlocked: Boolean,
        val depositWalletAddress: String?,
        val status: PolymarketWalletStatus,
        val expected: PolymarketEntry,
    )

    private fun provideTestModels() = listOf(
        ResolveModel(
            isBlocked = false,
            depositWalletAddress = null,
            status = PolymarketWalletStatus.NOT_CREATED,
            expected = PolymarketEntry.Onboard(PolymarketWalletStatus.NOT_CREATED),
        ),
        ResolveModel(
            isBlocked = false,
            depositWalletAddress = "0xDeposit",
            status = PolymarketWalletStatus.DEPLOYED,
            expected = PolymarketEntry.Onboard(PolymarketWalletStatus.DEPLOYED),
        ),
        ResolveModel(
            isBlocked = false,
            depositWalletAddress = "0xDeposit",
            status = PolymarketWalletStatus.READY_TO_TRADE,
            expected = PolymarketEntry.Onboarded(accessMode = PolymarketAccessMode.TRADING),
        ),
        ResolveModel(
            isBlocked = true,
            depositWalletAddress = null,
            status = PolymarketWalletStatus.NOT_CREATED,
            expected = PolymarketEntry.RegionBlocked,
        ),
        ResolveModel(
            isBlocked = true,
            depositWalletAddress = "0xDeposit",
            status = PolymarketWalletStatus.READY_TO_TRADE,
            expected = PolymarketEntry.Onboarded(accessMode = PolymarketAccessMode.READ_ONLY),
        ),
        ResolveModel(
            isBlocked = true,
            depositWalletAddress = "0xDeposit",
            status = PolymarketWalletStatus.DEPLOYED,
            expected = PolymarketEntry.Onboarded(accessMode = PolymarketAccessMode.READ_ONLY),
        ),
        ResolveModel(
            isBlocked = true,
            depositWalletAddress = "0xDeposit",
            status = PolymarketWalletStatus.APPROVALS_FAILED,
            expected = PolymarketEntry.Onboarded(accessMode = PolymarketAccessMode.READ_ONLY),
        ),
    )

    @Test
    fun `GIVEN geoblock read fails WHEN invoke THEN fails without deriving`() = runTest {
        // Arrange
        coEvery { checkGeoblock() } returns PolymarketOnboardingError.Network.left()

        // Act
        val actual = useCase(userWalletId)

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(PolymarketOnboardingError.Network)
        coVerify(exactly = 0) { deriveAddresses(any()) }
    }

    @Test
    fun `GIVEN derivation cancelled WHEN invoke THEN fails without reading wallet status`() = runTest {
        // Arrange
        val error = PolymarketOnboardingError.Derivation(PolymarketDerivationError.UserCancelled)
        coEvery { checkGeoblock() } returns false.right()
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
        coEvery { checkGeoblock() } returns true.right()
        coEvery { deriveAddresses(userWalletId) } returns addresses.right()
        coEvery { getWalletStatus(addresses) } returns PolymarketOnboardingError.Network.left()

        // Act
        val actual = useCase(userWalletId)

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(PolymarketOnboardingError.Network)
    }
}