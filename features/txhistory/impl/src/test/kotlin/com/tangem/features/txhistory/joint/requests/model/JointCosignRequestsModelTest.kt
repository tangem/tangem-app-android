package com.tangem.features.txhistory.joint.requests.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.jointaccount.polling.JointAccountPollingCoordinator
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.txhistory.component.JointCosignRequestsComponent
import com.tangem.features.txhistory.joint.requests.JointOperationSlotConfig
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Covers only what this model adds on top of [JointCosignRequestsDelegate]'s reactive state — resume/pause
 * driven polling for a joint-account currency, and consuming a pending operation deep-linked from a push
 * notification. State-building itself is delegate territory.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointCosignRequestsModelTest {

    private val delegate: JointCosignRequestsDelegate = mockk()
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase = mockk()
    private val pollingCoordinator: JointAccountPollingCoordinator = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(delegate, getBalanceHidingSettingsUseCase, pollingCoordinator)
        every { delegate.observe(userWalletId = any(), currency = any()) } returns flowOf(null)
        every { getBalanceHidingSettingsUseCase() } returns flowOf(
            BalanceHidingSettings(
                isHidingEnabledInSettings = false,
                isBalanceHidden = false,
                isBalanceHidingNotificationEnabled = false,
            ),
        )
        every { pollingCoordinator.poll(walletId = any()) } returns emptyFlow()
    }

    @Test
    fun `GIVEN no cosign request WHEN model created THEN state is empty`() = runTest {
        // Act
        val model = createModel(currency = nonJointCurrency)

        // Assert
        assertThat(model.state.value.items).isEmpty()
    }

    @Test
    fun `GIVEN joint currency WHEN model created but not resumed THEN polling does not start`() = runTest {
        // Act
        createModel(currency = jointCurrency)

        // Assert
        verify(exactly = 0) { pollingCoordinator.poll(walletId = any()) }
    }

    @Test
    fun `GIVEN joint currency WHEN resumed THEN polling starts for the wallet`() = runTest {
        // Arrange
        val model = createModel(currency = jointCurrency)

        // Act
        model.onResume()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { pollingCoordinator.poll(walletId = USER_WALLET_ID) }
    }

    /**
     * No request is currently known (delegate emits null), yet polling still starts — this is the whole point:

     */
    @Test
    fun `GIVEN no known cosign request WHEN resumed THEN polling still starts`() = runTest {
        // Arrange
        every { delegate.observe(userWalletId = any(), currency = any()) } returns flowOf(null)
        val model = createModel(currency = jointCurrency)

        // Act
        model.onResume()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { pollingCoordinator.poll(walletId = USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN non-joint currency WHEN resumed THEN polling never starts`() = runTest {
        // Arrange
        val model = createModel(currency = nonJointCurrency)

        // Act
        model.onResume()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { pollingCoordinator.poll(walletId = any()) }
    }

    @Test
    fun `GIVEN resumed with a joint currency WHEN paused THEN polling stops`() = runTest {
        // Arrange
        val pollCancelled = MutableStateFlow(false)
        every { pollingCoordinator.poll(walletId = USER_WALLET_ID) } returns flow {
            try {
                awaitCancellation()
            } finally {
                pollCancelled.value = true
            }
        }
        val model = createModel(currency = jointCurrency)
        model.onResume()
        advanceUntilIdle()

        // Act
        model.onPause()
        advanceUntilIdle()

        // Assert
        assertThat(pollCancelled.value).isTrue()
    }

    @Test
    fun `GIVEN no pending operation id WHEN pending operation consumed THEN operation slot stays closed`() =
        runTest {
            // Arrange
            val model = createModel(currency = nonJointCurrency, pendingOperationId = null)
            val openedOperations = model.operationNavigation.trackSlot()

            // Act
            model.consumePendingOperation()

            // Assert
            assertThat(openedOperations).isEmpty()
        }

    @Test
    fun `GIVEN pending operation id from push notification WHEN pending operation consumed THEN operation slot is activated`() =
        runTest {
            // Arrange
            val model = createModel(currency = nonJointCurrency, pendingOperationId = OPERATION_ID)
            val openedOperations = model.operationNavigation.trackSlot()

            // Act
            model.consumePendingOperation()

            // Assert
            assertThat(openedOperations.last()).isEqualTo(JointOperationSlotConfig(operationId = OPERATION_ID))
        }

    @Test
    fun `GIVEN pending operation id WHEN consumed twice THEN operation slot is activated only once`() = runTest {
        // Arrange
        val model = createModel(currency = nonJointCurrency, pendingOperationId = OPERATION_ID)
        val openedOperations = model.operationNavigation.trackSlot()

        // Act
        model.consumePendingOperation()
        model.consumePendingOperation()

        // Assert
        assertThat(openedOperations).hasSize(1)
    }

    /** Mirrors what a real `childSlot` would track, since bare [SlotNavigation] only relays events. */
    private fun SlotNavigation<JointOperationSlotConfig>.trackSlot(): List<JointOperationSlotConfig?> {
        val tracked = mutableListOf<JointOperationSlotConfig?>()
        subscribe { event -> tracked.add(event.transformer(tracked.lastOrNull())) }
        return tracked
    }

    private fun TestScope.createModel(
        currency: CryptoCurrency,
        pendingOperationId: String? = null,
    ): JointCosignRequestsModel {
        val model = JointCosignRequestsModel(
            paramsContainer = MutableParamsContainer(
                JointCosignRequestsComponent.Params(
                    userWalletId = USER_WALLET_ID,
                    currency = currency,
                    pendingOperationId = pendingOperationId,
                ),
            ),
            dispatchers = createTestingCoroutineDispatcherProvider(),
            delegate = delegate,
            getBalanceHidingSettingsUseCase = getBalanceHidingSettingsUseCase,
            pollingCoordinator = pollingCoordinator,
        )
        advanceUntilIdle()

        return model
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val OPERATION_ID = "operation-1"

        val jointCurrency: CryptoCurrency by lazy {
            val ethereum = MockCryptoCurrencyFactory().ethereum
            val derivationPath = Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/0")
            ethereum.copy(
                network = ethereum.network.copy(
                    id = ethereum.network.id.copy(derivationPath = derivationPath),
                    derivationPath = derivationPath,
                ),
            )
        }

        val nonJointCurrency: CryptoCurrency by lazy { MockCryptoCurrencyFactory().ethereum }
    }
}