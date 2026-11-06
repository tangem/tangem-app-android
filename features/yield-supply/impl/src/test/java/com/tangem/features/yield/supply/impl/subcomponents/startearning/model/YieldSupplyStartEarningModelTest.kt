package com.tangem.features.yield.supply.impl.subcomponents.startearning.model

import arrow.core.left
import arrow.core.none
import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.yield.supply.usecase.YieldSupplyPendingTracker
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class YieldSupplyStartEarningModelTest : YieldSupplyStartEarningModelTestBase() {

    @Test
    fun `GIVEN successful fee load WHEN model created THEN fee content and button enabled`() = runTest {
        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.yieldSupplyFeeUM).isInstanceOf(YieldSupplyFeeUM.Content::class.java)
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isTrue()
        coVerify { notificationsUpdateTrigger.triggerUpdate(any()) }
    }

    @Test
    fun `GIVEN estimate fee fails WHEN model created THEN fee error state`() = runTest {
        // Arrange
        coEvery { estimateEnterFeeUseCase(any(), any(), any()) } returns GetFeeError.UnknownError.left()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.yieldSupplyFeeUM).isEqualTo(YieldSupplyFeeUM.Error)
    }

    @Test
    fun `GIVEN max fee unavailable WHEN model created THEN fee error state`() = runTest {
        // Arrange
        coEvery { getMaxFeeUseCase(any(), any()) } returns Throwable("no max fee").left()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.yieldSupplyFeeUM).isEqualTo(YieldSupplyFeeUM.Error)
        coVerify(exactly = 0) { estimateEnterFeeUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN user wallet unavailable WHEN model created THEN shows generic error`() = runTest {
        // Arrange
        every { getUserWalletUseCase(userWalletId) } returns mockk<GetUserWalletError>(relaxed = true).left()

        // Act
        createModel()
        advanceUntilIdle()

        // Assert
        verify { alertFactory.getGenericErrorState(any(), any()) }
        coVerify(exactly = 0) { getMaxFeeUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN currency status not found WHEN model created THEN shows generic error`() = runTest {
        // Arrange
        stubCurrencyStatusLookup(none())

        // Act
        createModel()
        advanceUntilIdle()

        // Assert
        verify { alertFactory.getGenericErrorState(any(), any()) }
        coVerify(exactly = 0) { getMaxFeeUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN content loaded WHEN onClick THEN tracks pending reloads history and notifies sent`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        coVerify { yieldSupplyRepository.saveTokenProtocolPendingStatus(userWalletId, any(), any()) }
        coVerify { pendingTracker.addPending(userWalletId, any(), any(), any()) }
        verify(exactly = 1) { txHistoryUpdater.scheduleUpdates(userWalletId, any(), any()) }
        verify { callback.onTransactionSent() }
    }

    @Test
    fun `GIVEN upgrade-wrapped enter in batch WHEN onClick THEN deposit context carries the enter hash`() = runTest {
        // Arrange
        val wrappedEnter = uncompiledTx().copy(
            extras = EthereumTransactionExtras(
                callData = EthereumYieldSupplyUpgradeToAndCallCallData(
                    newImplementation = "0x5555555555555555555555555555555555555555",
                    innerCallData = mockk<EthereumYieldSupplyEnterCallData>(),
                ),
            ),
        )
        coEvery { estimateEnterFeeUseCase(any(), any(), any()) } returns listOf(uncompiledTx(), wrappedEnter).right()
        coEvery {
            sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
        } returns listOf("0xfirst", "0xenter").right()
        val depositContextSlot = slot<YieldSupplyPendingTracker.DepositContext>()
        coEvery {
            pendingTracker.addPending(any(), any(), any(), capture(depositContextSlot))
        } returns Unit
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(depositContextSlot.captured.enterTransactionHash).isEqualTo("0xenter")
    }

    @Test
    fun `GIVEN content loaded WHEN onClick THEN does not activate protocol before the enter tx settles`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { activateUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN content loaded WHEN onClick and send fails THEN shows error and not sent`() = runTest {
        // Arrange
        coEvery {
            sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
        } returns SendTransactionError.UnknownError().left()
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isTransactionSending).isFalse()
        verify { alertFactory.getSendTransactionErrorState(any(), any(), any()) }
        verify(exactly = 0) { callback.onTransactionSent() }
    }

    @Test
    fun `GIVEN fee not loaded WHEN onClick THEN does not send transactions`() = runTest {
        // Arrange — estimate fee fails so the fee state is Error; onClick must early-return before sending
        coEvery { estimateEnterFeeUseCase(any(), any(), any()) } returns GetFeeError.UnknownError.left()
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) {
            sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
        }
    }

    @Test
    fun `GIVEN notifications report an error WHEN flag emitted THEN primary button disabled`() = runTest {
        // Arrange
        val hasErrorFlow = MutableStateFlow(false)
        every { notificationsUpdateTrigger.hasErrorFlow } returns hasErrorFlow
        val model = createModel()
        advanceUntilIdle()
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isTrue()

        // Act
        hasErrorFlow.value = true
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN a fee load in flight WHEN the status turns loading THEN the load still delivers its fee`() = runTest {
        // Arrange
        val loadingAccountStatusList: AccountStatusList = mockk()
        every {
            with(CryptoCurrencyStatusOperations) {
                loadingAccountStatusList.getCryptoCurrencyStatus(any<CryptoCurrency>())
            }
        } returns CryptoCurrencyStatus(currency = token, value = CryptoCurrencyStatus.Loading).some()
        val accountStatusLists = MutableStateFlow(accountStatusList)
        every { accountStatusListSupplier(userWalletId) } returns accountStatusLists
        coEvery { estimateEnterFeeUseCase(any(), any(), any()) } coAnswers {
            delay(1_000)
            listOf(uncompiledTx()).right()
        }
        val model = createModel()
        runCurrent()

        // Act
        accountStatusLists.value = loadingAccountStatusList
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.yieldSupplyFeeUM).isEqualTo(YieldSupplyFeeUM.Loading)
        coVerify(exactly = 1) {
            notificationsUpdateTrigger.triggerUpdate(match { it.feeValue == BigDecimal("0.001") && it.feeError == null })
        }
    }
}