package com.tangem.domain.polymarket.interactor

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketNewAccountAllowance
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketWalletStatusUseCase
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
internal class CheckNewPolymarketAccountAllowedInteractorTest {

    private val getWalletStatus: GetPolymarketWalletStatusUseCase = mockk()
    private val checkGeoblock: CheckPolymarketGeoblockUseCase = mockk()

    private val interactor = CheckNewPolymarketAccountAllowedInteractor(
        getWalletStatus = getWalletStatus,
        checkGeoblock = checkGeoblock,
    )

    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun resetMocks() {
        clearMocks(getWalletStatus, checkGeoblock)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun invoke(model: AllowanceModel) = runTest {
        // Arrange
        coEvery { getWalletStatus.byWalletId(userWalletId) } returns model.status
        coEvery { checkGeoblock() } returns model.geoblock

        // Act
        val actual = interactor(userWalletId)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN an account exists WHEN invoked THEN the region is not asked`() = runTest {
        // Arrange
        coEvery { getWalletStatus.byWalletId(userWalletId) } returns state(PolymarketWalletStatus.READY_TO_TRADE)

        // Act
        interactor(userWalletId)

        // Assert
        coVerify(exactly = 0) { checkGeoblock() }
    }

    @Test
    fun `GIVEN the status is unreadable WHEN invoked THEN the region is still asked`() = runTest {
        // Arrange
        coEvery { getWalletStatus.byWalletId(userWalletId) } returns PolymarketOnboardingError.Network.left()
        coEvery { checkGeoblock() } returns false.right()

        // Act
        interactor(userWalletId)

        // Assert
        coVerify(exactly = 1) { checkGeoblock() }
    }

    internal data class AllowanceModel(
        val status: Either<PolymarketOnboardingError, PolymarketWalletState>,
        val geoblock: Either<PolymarketOnboardingError, Boolean>,
        val expected: PolymarketNewAccountAllowance,
    )

    private fun provideTestModels() = listOf(
        AllowanceModel(
            status = state(PolymarketWalletStatus.NOT_CREATED),
            geoblock = false.right(),
            expected = PolymarketNewAccountAllowance.ALLOWED,
        ),
        AllowanceModel(
            status = state(PolymarketWalletStatus.NOT_CREATED),
            geoblock = true.right(),
            expected = PolymarketNewAccountAllowance.REGION_BLOCKED,
        ),
        AllowanceModel(
            status = state(PolymarketWalletStatus.DEPLOYMENT_FAILED),
            geoblock = true.right(),
            expected = PolymarketNewAccountAllowance.REGION_BLOCKED,
        ),
        AllowanceModel(
            status = state(PolymarketWalletStatus.NOT_CREATED),
            geoblock = PolymarketOnboardingError.Network.left(),
            expected = PolymarketNewAccountAllowance.UNDETERMINED,
        ),
        AllowanceModel(
            status = state(PolymarketWalletStatus.DEPLOYED),
            geoblock = true.right(),
            expected = PolymarketNewAccountAllowance.ALLOWED,
        ),
        AllowanceModel(
            status = state(PolymarketWalletStatus.APPROVALS_FAILED),
            geoblock = true.right(),
            expected = PolymarketNewAccountAllowance.ALLOWED,
        ),
        AllowanceModel(
            status = state(PolymarketWalletStatus.READY_TO_TRADE),
            geoblock = true.right(),
            expected = PolymarketNewAccountAllowance.ALLOWED,
        ),
        AllowanceModel(
            status = state(PolymarketWalletStatus.UNKNOWN),
            geoblock = true.right(),
            expected = PolymarketNewAccountAllowance.ALLOWED,
        ),
        AllowanceModel(
            status = PolymarketOnboardingError.Network.left(),
            geoblock = true.right(),
            expected = PolymarketNewAccountAllowance.REGION_BLOCKED,
        ),
        AllowanceModel(
            status = PolymarketOnboardingError.Network.left(),
            geoblock = false.right(),
            expected = PolymarketNewAccountAllowance.ALLOWED,
        ),
        AllowanceModel(
            status = PolymarketOnboardingError.Network.left(),
            geoblock = PolymarketOnboardingError.Network.left(),
            expected = PolymarketNewAccountAllowance.UNDETERMINED,
        ),
    )

    private companion object {

        fun state(status: PolymarketWalletStatus): Either<PolymarketOnboardingError, PolymarketWalletState> =
            PolymarketWalletState(depositWalletAddress = null, status = status).right()
    }
}