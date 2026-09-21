package com.tangem.features.polymarket.impl.positionresult.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.flow.PredictionPositionsUpdateEmitter
import com.tangem.domain.polymarket.interactor.PredictionClaimFailure
import com.tangem.domain.polymarket.interactor.SubmitPredictionClaimInteractor
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketPositionResult
import com.tangem.domain.polymarket.model.PolymarketRedeemError
import com.tangem.domain.polymarket.model.PolymarketResolutionStatus
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PredictionClaimReceipt
import com.tangem.domain.polymarket.model.PredictionClaimState
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.features.polymarket.impl.positionresult.PolymarketPositionResultComponent
import com.tangem.features.polymarket.impl.positionresult.ui.state.PolymarketPositionResultUM
import com.tangem.features.polymarket.impl.positions.model.createPolymarketPosition
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketPositionResultModelTest {

    private val claimInteractor: SubmitPredictionClaimInteractor = mockk()
    private val deriveAddresses: DerivePolymarketAddressesUseCase = mockk()
    private val positionsUpdateEmitter: PredictionPositionsUpdateEmitter = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val router: Router = mockk(relaxed = true)
    private val addresses: PolymarketAddresses = mockk {
        every { ownerAddress } returns OWNER
        every { depositWalletAddress } returns DEPOSIT_WALLET
    }
    private var model: PolymarketPositionResultModel? = null

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
    }

    @Test
    fun `GIVEN a claimable position WHEN the claim confirms THEN the positions are invalidated and the screen pops`() =
        runTest {
            // Arrange
            val model = createModel()
            coEvery { claimInteractor.claim(any()) } returns receipt(PredictionClaimState.CONFIRMED).right()

            // Act
            model.onActionClick()
            runCurrent()

            // Assert
            verify(exactly = 1) { positionsUpdateEmitter.triggerUpdate(WALLET) }
            verify(exactly = 1) { router.pop() }
        }

    @Test
    fun `GIVEN a claimable position WHEN claimed THEN the market and the stored addresses are passed on`() = runTest {
        // Arrange
        val model = createModel()
        coEvery { claimInteractor.claim(any()) } returns receipt(PredictionClaimState.CONFIRMED).right()

        // Act
        model.onActionClick()
        runCurrent()

        // Assert
        coVerify(exactly = 1) {
            claimInteractor.claim(
                SubmitPredictionClaimInteractor.Params(
                    userWalletId = WALLET,
                    walletId = WALLET.stringValue,
                    ownerAddress = OWNER,
                    depositWalletAddress = DEPOSIT_WALLET,
                    conditionId = "condition",
                    isNegRisk = true,
                ),
            )
        }
    }

    @Test
    fun `GIVEN the user cancels the signature WHEN claiming THEN no alert is shown and the button is idle`() =
        runTest {
            // Arrange
            val model = createModel()
            coEvery { claimInteractor.claim(any()) } returns
                PredictionClaimFailure.SigningFailed(PolymarketSigningError.UserCancelled).left()

            // Act
            model.onActionClick()
            runCurrent()

            // Assert
            assertThat(model.uiState.value.action)
                .isEqualTo(
                    PolymarketPositionResultUM.Action.Claim(
                        isInProgress = false,
                        label = resourceReference(R.string.prediction_position_result_claim_action),
                    ),
                )
            verify(exactly = 0) { messageSender.send(any()) }
            verify(exactly = 0) { router.pop() }
        }

    @Test
    fun `GIVEN the claim does not settle in time WHEN it returns THEN the positions are invalidated and an alert is shown`() =
        runTest {
            // Arrange
            val model = createModel()
            coEvery { claimInteractor.claim(any()) } returns
                PredictionClaimFailure.Unconfirmed(receipt(PredictionClaimState.IN_PROGRESS)).left()

            // Act
            model.onActionClick()
            runCurrent()

            // Assert
            verify(exactly = 1) { positionsUpdateEmitter.triggerUpdate(WALLET) }
            verify(exactly = 1) { messageSender.send(any()) }
            verify(exactly = 0) { router.pop() }
        }

    @Test
    fun `GIVEN the relayer refuses the claim WHEN it returns THEN the positions are invalidated and an alert is shown`() =
        runTest {
            // Arrange
            val model = createModel()
            coEvery { claimInteractor.claim(any()) } returns
                PredictionClaimFailure.Rejected(PolymarketRedeemError.RelayerRejected(detail = "not claimable")).left()

            // Act
            model.onActionClick()
            runCurrent()

            // Assert
            verify(exactly = 1) { positionsUpdateEmitter.triggerUpdate(WALLET) }
            verify(exactly = 1) { messageSender.send(any()) }
            verify(exactly = 0) { router.pop() }
        }

    @Test
    fun `GIVEN the relayer is unreachable WHEN the claim returns THEN only an alert is shown`() = runTest {
        // Arrange
        val model = createModel()
        coEvery { claimInteractor.claim(any()) } returns
            PredictionClaimFailure.Rejected(PolymarketRedeemError.RelayerUnavailable).left()

        // Act
        model.onActionClick()
        runCurrent()

        // Assert
        verify(exactly = 0) { positionsUpdateEmitter.triggerUpdate(any()) }
        verify(exactly = 1) { messageSender.send(any()) }
        verify(exactly = 0) { router.pop() }
    }

    @Test
    fun `GIVEN a dismiss action WHEN tapped THEN the screen pops without claiming`() = runTest {
        // Arrange
        val model = createModel(isRedeemable = false)

        // Act
        model.onActionClick()
        runCurrent()

        // Assert
        verify(exactly = 1) { router.pop() }
        coVerify(exactly = 0) { claimInteractor.claim(any()) }
    }

    private fun TestScope.createModel(isRedeemable: Boolean = true): PolymarketPositionResultModel {
        coEvery { deriveAddresses.stored(WALLET) } returns addresses
        val position = createPolymarketPosition(
            result = PolymarketPositionResult.WON,
            resolutionStatus = PolymarketResolutionStatus.RESOLVED,
        ).copy(isRedeemable = isRedeemable)
        val dispatcher = StandardTestDispatcher(testScheduler)
        return PolymarketPositionResultModel(
            paramsContainer = MutableParamsContainer(
                PolymarketPositionResultComponent.Params(userWalletId = WALLET, position = position),
            ),
            dispatchers = TestingCoroutineDispatcherProvider(
                dispatcher,
                dispatcher,
                dispatcher,
                dispatcher,
                dispatcher,
            ),
            claimInteractor = claimInteractor,
            deriveAddresses = deriveAddresses,
            positionsUpdateEmitter = positionsUpdateEmitter,
            messageSender = messageSender,
            router = router,
        ).also { model = it }
    }

    private fun receipt(state: PredictionClaimState) = PredictionClaimReceipt(
        transactionId = "0x4f2c",
        state = state,
        transactionHash = null,
    )

    private companion object {
        val WALLET = UserWalletId("033")
        const val OWNER = "0xabc0000000000000000000000000000000000001"
        const val DEPOSIT_WALLET = "0xdef0000000000000000000000000000000000002"
    }
}