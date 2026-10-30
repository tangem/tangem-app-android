package com.tangem.domain.polymarket.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import com.tangem.test.core.ProvideTestModels
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetPolymarketWalletStatusUseCaseTest {

    private val polymarketRepository: PolymarketRepository = mockk()

    private val useCase = GetPolymarketWalletStatusUseCase(polymarketRepository = polymarketRepository)

    private val addresses = PolymarketAddresses(
        ownerAddress = OWNER,
        depositWalletAddress = DEPOSIT_WALLET,
        userWalletId = UserWalletId("011"),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(polymarketRepository)
    }

    @Test
    fun `GIVEN the backend knows no wallet yet WHEN invoke THEN returns the state looked up by owner`() = runTest {
        // Arrange
        val byWalletId = PolymarketWalletState(
            depositWalletAddress = null,
            status = PolymarketWalletStatus.NOT_CREATED,
        )
        val byOwner = PolymarketWalletState(
            depositWalletAddress = DEPOSIT_WALLET,
            status = PolymarketWalletStatus.NOT_CREATED,
        )
        coEvery { polymarketRepository.getWalletStatusByWalletId("011") } returns byWalletId.right()
        coEvery { polymarketRepository.getWalletStatus(UserWalletId("011"), OWNER) } returns byOwner.right()

        // Act
        val actual = useCase(addresses = addresses)

        // Assert
        assertThat(actual).isEqualTo(byOwner.right())
    }

    @Test
    fun `GIVEN the backend returns the same address in another case WHEN invoke THEN returns the state`() = runTest {
        // Arrange
        val state = PolymarketWalletState(
            depositWalletAddress = DEPOSIT_WALLET.lowercase(),
            status = PolymarketWalletStatus.DEPLOYED,
        )
        coEvery { polymarketRepository.getWalletStatusByWalletId("011") } returns state.right()

        // Act
        val actual = useCase(addresses = addresses)

        // Assert
        assertThat(actual).isEqualTo(state.right())
    }

    @Test
    fun `GIVEN the backend returns another address WHEN invoke THEN returns AddressMismatch`() = runTest {
        // Arrange
        val state = PolymarketWalletState(
            depositWalletAddress = OTHER_DEPOSIT_WALLET,
            status = PolymarketWalletStatus.DEPLOYED,
        )
        coEvery { polymarketRepository.getWalletStatusByWalletId("011") } returns state.right()

        // Act
        val actual = useCase(addresses = addresses)

        // Assert
        val expected = PolymarketOnboardingError.AddressMismatch(
            expected = DEPOSIT_WALLET,
            actual = OTHER_DEPOSIT_WALLET,
        )
        assertThat(actual).isEqualTo(expected.left())
    }

    @Test
    fun `GIVEN both lookups fail WHEN invoke THEN wraps the owner lookup error`() = runTest {
        // Arrange
        coEvery { polymarketRepository.getWalletStatusByWalletId("011") } returns
            PolymarketWalletError.InvalidRequest.left()
        coEvery { polymarketRepository.getWalletStatus(UserWalletId("011"), OWNER) } returns
            PolymarketWalletError.Unauthorized.left()

        // Act
        val actual = useCase(addresses = addresses)

        // Assert
        assertThat(actual).isEqualTo(PolymarketOnboardingError.Wallet(PolymarketWalletError.Unauthorized).left())
    }

    @Test
    fun `GIVEN both lookups fail with a network error WHEN invoke THEN returns Network`() = runTest {
        // Arrange
        coEvery { polymarketRepository.getWalletStatusByWalletId("011") } returns PolymarketWalletError.Network.left()
        coEvery { polymarketRepository.getWalletStatus(UserWalletId("011"), OWNER) } returns
            PolymarketWalletError.Network.left()

        // Act
        val actual = useCase(addresses = addresses)

        // Assert
        assertThat(actual).isEqualTo(PolymarketOnboardingError.Network.left())
    }

    @Test
    fun `GIVEN the wallet id lookup finds a deployed wallet WHEN invoke THEN the owner is not looked up`() = runTest {
        // Arrange
        val state = PolymarketWalletState(depositWalletAddress = DEPOSIT_WALLET, status = PolymarketWalletStatus.DEPLOYED)
        coEvery { polymarketRepository.getWalletStatusByWalletId("011") } returns state.right()

        // Act
        val actual = useCase(addresses = addresses)

        // Assert
        assertThat(actual).isEqualTo(state.right())
        coVerify(exactly = 0) { polymarketRepository.getWalletStatus(any(), any()) }
    }

    @Test
    fun `GIVEN the wallet id is NOT_CREATED and the owner is deployed WHEN invoke THEN returns the owner state`() =
        runTest {
            // Arrange
            val ownerState = PolymarketWalletState(
                depositWalletAddress = DEPOSIT_WALLET,
                status = PolymarketWalletStatus.DEPLOYED,
            )
            coEvery { polymarketRepository.getWalletStatusByWalletId("011") } returns notCreated.right()
            coEvery { polymarketRepository.getWalletStatus(UserWalletId("011"), OWNER) } returns ownerState.right()

            // Act
            val actual = useCase(addresses = addresses)

            // Assert
            assertThat(actual).isEqualTo(ownerState.right())
            coVerify(exactly = 1) { polymarketRepository.getWalletStatus(UserWalletId("011"), OWNER) }
        }

    @Test
    fun `GIVEN the wallet id lookup answers 404 and the owner is deployed WHEN invoke THEN returns the owner state`() =
        runTest {
            // Arrange
            val ownerState = PolymarketWalletState(
                depositWalletAddress = DEPOSIT_WALLET,
                status = PolymarketWalletStatus.READY_TO_TRADE,
            )
            coEvery { polymarketRepository.getWalletStatusByWalletId("011") } returns
                PolymarketWalletError.Unexpected(httpCode = 404, detail = null).left()
            coEvery { polymarketRepository.getWalletStatus(UserWalletId("011"), OWNER) } returns ownerState.right()

            // Act
            val actual = useCase(addresses = addresses)

            // Assert
            assertThat(actual).isEqualTo(ownerState.right())
        }

    @Test
    fun `GIVEN both lookups answer NOT_CREATED WHEN invoke THEN returns NOT_CREATED after one owner lookup`() = runTest {
        // Arrange
        coEvery { polymarketRepository.getWalletStatusByWalletId("011") } returns notCreated.right()
        coEvery { polymarketRepository.getWalletStatus(UserWalletId("011"), OWNER) } returns notCreated.right()

        // Act
        val actual = useCase(addresses = addresses)

        // Assert
        assertThat(actual).isEqualTo(notCreated.right())
        coVerify(exactly = 1) { polymarketRepository.getWalletStatus(UserWalletId("011"), OWNER) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN the wallet id lookup fails WHEN invoke THEN returns the owner state after one owner lookup`(
        error: PolymarketWalletError,
    ) = runTest {
        // Arrange
        val ownerState = PolymarketWalletState(
            depositWalletAddress = DEPOSIT_WALLET,
            status = PolymarketWalletStatus.READY_TO_TRADE,
        )
        coEvery { polymarketRepository.getWalletStatusByWalletId("011") } returns error.left()
        coEvery { polymarketRepository.getWalletStatus(UserWalletId("011"), OWNER) } returns ownerState.right()

        // Act
        val actual = useCase(addresses = addresses)

        // Assert
        assertThat(actual).isEqualTo(ownerState.right())
        coVerify(exactly = 1) { polymarketRepository.getWalletStatus(UserWalletId("011"), OWNER) }
    }

    private fun provideTestModels() = listOf(
        PolymarketWalletError.Network,
        PolymarketWalletError.Unauthorized,
        PolymarketWalletError.InvalidRequest,
        PolymarketWalletError.Unexpected(httpCode = 503, detail = null),
    )

    private val notCreated = PolymarketWalletState(
        depositWalletAddress = null,
        status = PolymarketWalletStatus.NOT_CREATED,
    )

    private companion object {
        const val OWNER = "0x1111111111111111111111111111111111111111"
        const val DEPOSIT_WALLET = "0xfAeA0f08159fcF2f573fE24E9E989B0d48f7651B"
        const val OTHER_DEPOSIT_WALLET = "0x57ffBc34De23124fAeb8387fcd689d314E57aCcD"
    }
}