package com.tangem.domain.polymarket.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.interactor.PolymarketSessionKeyOverview
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyAssessment
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyDeviceState
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyReconciliation
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStateService
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperation
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRequest
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRunner
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStage
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationState
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ObservePolymarketFastTradingUseCaseTest {

    private val stateService: PolymarketSessionKeyStateService = mockk()
    private val runner: PolymarketSessionKeyOperationRunner = mockk()
    private val useCase = ObservePolymarketFastTradingUseCase(stateService, runner)

    @BeforeEach
    fun setup() {
        clearMocks(stateService, runner)
        every { runner.observe(USER_WALLET_ID) } returns MutableStateFlow(PolymarketSessionKeyOperationState.IDLE)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a device state and nothing running WHEN observe THEN activation is offered as expected`(
        model: ActivationModel,
    ) = runTest {
        // Arrange
        every { stateService.observe(USER_WALLET_ID) } returns flowOf(overview(model.deviceState).right())

        // Act
        val actual = useCase(USER_WALLET_ID).first()

        // Assert
        assertThat(actual.isActivationOffered).isEqualTo(model.isOffered)
    }

    @Test
    fun `GIVEN reenable and extend running WHEN observe THEN activation is not offered`() = runTest {
        // Arrange
        val operation = PolymarketSessionKeyOperation(
            id = 1,
            request = PolymarketSessionKeyOperationRequest.Extend,
            targetAddress = KEY.address,
            stage = PolymarketSessionKeyOperationStage.REVOKED,
        )
        every { stateService.observe(USER_WALLET_ID) } returns flowOf(
            overview(PolymarketSessionKeyDeviceState.Reenable(KEY)).right(),
        )
        every { runner.observe(USER_WALLET_ID) } returns MutableStateFlow(
            PolymarketSessionKeyOperationState(running = operation, finished = null),
        )

        // Act
        val actual = useCase(USER_WALLET_ID).first()

        // Assert
        assertThat(actual.operation).isEqualTo(operation)
        assertThat(actual.isActivationOffered).isFalse()
    }

    @Test
    fun `GIVEN the overview failed WHEN observe THEN activation is not offered`() = runTest {
        // Arrange
        every { stateService.observe(USER_WALLET_ID) } returns flowOf(PolymarketWalletError.Network.left())

        // Act
        val actual = useCase(USER_WALLET_ID).first()

        // Assert
        assertThat(actual.isActivationOffered).isFalse()
    }

    private fun provideTestModels() = listOf(
        ActivationModel(PolymarketSessionKeyDeviceState.OfferEnable, isOffered = true),
        ActivationModel(PolymarketSessionKeyDeviceState.Reenable(KEY), isOffered = true),
        ActivationModel(PolymarketSessionKeyDeviceState.RepairByRenewal(KEY), isOffered = true),
        ActivationModel(PolymarketSessionKeyDeviceState.Replace.ByRevocation(KEY), isOffered = true),
        ActivationModel(PolymarketSessionKeyDeviceState.Replace.ByOverwrite(KEY), isOffered = true),
        ActivationModel(PolymarketSessionKeyDeviceState.Active(KEY, isRenewDue = false), isOffered = false),
        ActivationModel(PolymarketSessionKeyDeviceState.Disabled(KEY, isRenewDue = false), isOffered = false),
        ActivationModel(PolymarketSessionKeyDeviceState.RenewalRequired(KEY), isOffered = false),
        ActivationModel(PolymarketSessionKeyDeviceState.Enabling(KEY), isOffered = false),
        ActivationModel(PolymarketSessionKeyDeviceState.Revoking(KEY), isOffered = false),
        ActivationModel(PolymarketSessionKeyDeviceState.WaitingForPrevious(KEY), isOffered = false),
    )

    data class ActivationModel(val deviceState: PolymarketSessionKeyDeviceState, val isOffered: Boolean)

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val NOW: Instant = Instant.ofEpochSecond(1_789_430_000)
        val KEY = PolymarketSessionKey(
            address = "0x5e55000000000000000000000000000000000001",
            deviceId = "device",
            deviceName = "Pixel",
            isEnabled = true,
            status = PolymarketSessionKeyStatus.ACTIVE,
            scopes = listOf("CLOB"),
            validUntil = NOW.plusSeconds(1_000_000),
            renewAfter = null,
            authorizedAt = null,
            createdAt = NOW,
        )

        fun overview(deviceState: PolymarketSessionKeyDeviceState) = PolymarketSessionKeyOverview(
            wallet = PolymarketWalletState(
                depositWalletAddress = "0x2222222222222222222222222222222222222222",
                status = PolymarketWalletStatus.READY_TO_TRADE,
                sessionKeys = listOf(KEY),
            ),
            localKeyAddress = KEY.address,
            assessment = PolymarketSessionKeyAssessment(
                deviceState = deviceState,
                reconciliation = PolymarketSessionKeyReconciliation.Unavailable,
                nextChangeAt = null,
            ),
            isTradable = false,
        )
    }
}