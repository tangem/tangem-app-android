package com.tangem.features.tangempay.txhistory.details

import android.text.format.DateFormat
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.CashbackRepository
import com.tangem.domain.tangempay.repository.TangemPayTxHistoryRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.cashback
import com.tangem.features.tangempay.components.TangemPayTransactionBottomSheetComponent
import com.tangem.features.tangempay.paymentTransaction
import com.tangem.features.tangempay.spendTransaction
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayTxHistoryDetailsModelTest {

    private val userWalletId = UserWalletId("123")
    private val repository: TangemPayTxHistoryRepository = mockk()
    private val cashbackRepository: CashbackRepository = mockk()
    private val featureToggles: TangemPayFeatureToggles = mockk()
    private val balanceHidingSettings: GetBalanceHidingSettingsUseCase = mockk()

    @BeforeEach
    fun setup() {
        clearMocks(repository, cashbackRepository, featureToggles, balanceHidingSettings)
        every { balanceHidingSettings.isBalanceHidden() } returns flowOf(false)
        every { featureToggles.isCashbackEnabled } returns true
        coEvery { cashbackRepository.getCashbackDetails(any(), any()) } returns null.right()
        mockkStatic(DateFormat::class)
        every { DateFormat.getBestDateTimePattern(any(), any()) } answers { secondArg() }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(DateFormat::class)
    }

    @Test
    fun `GIVEN spend transaction WHEN detail fetch in flight THEN card is Loading`() = runTest {
        // Arrange
        coEvery { repository.getTransaction(any(), any()) } coAnswers { awaitCancellation() }
        val model = createModel(testScope = this, transaction = spendTransaction())

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.detail).isEqualTo(TransactionDetailUM.Loading)
        model.onDestroy()
    }

    @Test
    fun `GIVEN spend transaction WHEN detail fetch succeeds THEN card shows loaded name and last4`() = runTest {
        // Arrange — the sheet opens with no card info, the detail fetch back-fills it
        val loaded = spendTransaction(cardName = "Loaded card", cardNumberLast4 = "4321")
        coEvery { repository.getTransaction(any(), any()) } returns loaded.right()
        val model = createModel(
            testScope = this,
            transaction = spendTransaction(cardName = null, cardNumberLast4 = null),
        )

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.detail)
            .isEqualTo(
                TransactionDetailUM.Content(
                    cardNumber = stringReference("*4321"),
                    cardName = stringReference("Loaded card"),
                ),
            )
        model.onDestroy()
    }

    @Test
    fun `GIVEN spend transaction WHEN detail fetch succeeds without card info THEN card row is hidden`() = runTest {
        // Arrange
        coEvery { repository.getTransaction(any(), any()) } returns
            spendTransaction(cardName = null, cardNumberLast4 = null).right()
        val model = createModel(testScope = this, transaction = spendTransaction())

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.detail).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN spend transaction WHEN detail fetch fails THEN card is Error`() = runTest {
        // Arrange
        coEvery { repository.getTransaction(any(), any()) } returns VisaApiError.Unspecified.left()
        val model = createModel(testScope = this, transaction = spendTransaction())

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.detail).isInstanceOf(TransactionDetailUM.Error::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN card error WHEN refresh clicked and fetch succeeds THEN card shows value`() = runTest {
        // Arrange — first fetch fails, retry succeeds
        val loaded = spendTransaction(cardName = "Loaded card", cardNumberLast4 = "4321")
        coEvery {
            repository.getTransaction(any(), any())
        } returnsMany listOf<Either<VisaApiError, TangemPayTxHistoryItem?>>(
            VisaApiError.Unspecified.left(),
            loaded.right(),
        )
        val model = createModel(testScope = this, transaction = spendTransaction())
        advanceUntilIdle()
        val errorState = model.uiState.value.detail
        assertThat(errorState).isInstanceOf(TransactionDetailUM.Error::class.java)

        // Act — tap refresh
        (errorState as TransactionDetailUM.Error).onRefreshClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.detail)
            .isEqualTo(
                TransactionDetailUM.Content(
                    cardNumber = stringReference("*4321"),
                    cardName = stringReference("Loaded card"),
                ),
            )
        coVerify(exactly = 2) { repository.getTransaction(any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN non-spend transaction WHEN model created THEN card is hidden and detail not fetched`() = runTest {
        // Arrange
        val model = createModel(testScope = this, transaction = paymentTransaction())

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.detail).isNull()
        coVerify(exactly = 0) { repository.getTransaction(any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN cashback enabled WHEN details fetch in flight THEN cashback is Loading`() = runTest {
        // Arrange
        coEvery { repository.getTransaction(any(), any()) } returns spendTransaction().right()
        coEvery { cashbackRepository.getCashbackDetails(any(), any()) } coAnswers { awaitCancellation() }
        val model = createModel(testScope = this, transaction = spendTransaction())

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.cashbackDetail).isEqualTo(CashbackDetailUM.Loading)
        model.onDestroy()
    }

    @Test
    fun `GIVEN cashback enabled WHEN details fetch succeeds THEN cashback row is Content`() = runTest {
        // Arrange
        coEvery { repository.getTransaction(any(), any()) } returns spendTransaction().right()
        coEvery { cashbackRepository.getCashbackDetails(any(), any()) } returns
            cashback().right()
        val model = createModel(testScope = this, transaction = spendTransaction())

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.cashbackDetail).isInstanceOf(CashbackDetailUM.Content::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN cashback enabled WHEN details fetch fails THEN cashback is Error`() = runTest {
        // Arrange
        coEvery { repository.getTransaction(any(), any()) } returns spendTransaction().right()
        coEvery { cashbackRepository.getCashbackDetails(any(), any()) } returns VisaApiError.Unspecified.left()
        val model = createModel(testScope = this, transaction = spendTransaction())

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.cashbackDetail).isInstanceOf(CashbackDetailUM.Error::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN cashback error WHEN refresh clicked and fetch succeeds THEN cashback shown`() = runTest {
        // Arrange — first fetch fails, retry succeeds
        coEvery { repository.getTransaction(any(), any()) } returns spendTransaction().right()
        coEvery {
            cashbackRepository.getCashbackDetails(any(), any())
        } returnsMany listOf(
            VisaApiError.Unspecified.left(),
            cashback().right(),
        )
        val model = createModel(testScope = this, transaction = spendTransaction())
        advanceUntilIdle()
        val errorState = model.uiState.value.cashbackDetail
        assertThat(errorState).isInstanceOf(CashbackDetailUM.Error::class.java)

        // Act — tap refresh
        (errorState as CashbackDetailUM.Error).onRefreshClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.cashbackDetail).isInstanceOf(CashbackDetailUM.Content::class.java)
        coVerify(exactly = 2) { cashbackRepository.getCashbackDetails(any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN cashback disabled WHEN model created THEN cashback hidden and not fetched`() = runTest {
        // Arrange
        every { featureToggles.isCashbackEnabled } returns false
        coEvery { repository.getTransaction(any(), any()) } returns spendTransaction().right()
        val model = createModel(testScope = this, transaction = spendTransaction())

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.cashbackDetail).isNull()
        coVerify(exactly = 0) { cashbackRepository.getCashbackDetails(any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN non-spend transaction WHEN model created THEN cashback hidden and not fetched`() = runTest {
        // Arrange
        val model = createModel(testScope = this, transaction = paymentTransaction())

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.cashbackDetail).isNull()
        coVerify(exactly = 0) { cashbackRepository.getCashbackDetails(any(), any()) }
        model.onDestroy()
    }

    private fun createModel(
        testScope: TestScope,
        transaction: TangemPayTxHistoryItem,
    ): TangemPayTxHistoryDetailsModel {
        val params = TangemPayTransactionBottomSheetComponent.Params(
            isBalanceHidden = false,
            transaction = transaction,
            userWalletId = userWalletId,
            customerId = "customer_1",
            onDismiss = {},
        )
        return TangemPayTxHistoryDetailsModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            getWalletMetaInfoUseCase = mockk(relaxed = true),
            sendFeedbackEmailUseCase = mockk(relaxed = true),
            urlOpener = mockk(relaxed = true),
            balanceHidingSettings = balanceHidingSettings,
            tangemPayTxHistoryRepository = repository,
            cashbackRepository = cashbackRepository,
            featureToggles = featureToggles,
            analytics = mockk(relaxed = true),
            paramsContainer = MutableParamsContainer(params),
        )
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }
}