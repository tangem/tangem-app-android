package com.tangem.features.send.send.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.features.send.api.SendComponent
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.entity.PredefinedValues
import com.tangem.features.send.common.CommonSendRoute
import com.tangem.features.send.common.ui.state.ConfirmUM
import com.tangem.features.send.loadedStatus
import com.tangem.features.send.send.SendModelTestBase
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM as FeeSelectorUMRedesigned

@OptIn(ExperimentalCoroutinesApi::class)
internal class SendModelTest : SendModelTestBase() {

    @Nested
    inner class OnNextClick {

        @Test
        fun `GIVEN amount route AND predefined main screen QR WHEN onNextClick THEN push Confirm`() = runTest {
            // Arrange
            val model = createSendModel(this)
            model.predefinedValues = PredefinedValues.Content.QrCode(
                amount = "1.0",
                address = "addr123",
                memo = null,
                source = PredefinedValues.Source.MAIN_SCREEN,
            )

            // Act
            model.onNextClick(CommonSendRoute.Amount(isEditMode = false))

            // Assert
            verify(exactly = 1) { router.push(CommonSendRoute.Confirm, any()) }
        }

        @Test
        fun `GIVEN amount route AND NOT main screen QR WHEN onNextClick THEN push Destination`() = runTest {
            // Arrange
            val model = createSendModel(this)
            model.predefinedValues = PredefinedValues.Empty

            // Act
            model.onNextClick(CommonSendRoute.Amount(isEditMode = false))

            // Assert
            verify(exactly = 1) { router.push(CommonSendRoute.Destination(isEditMode = false), any()) }
        }

        @Test
        fun `GIVEN destination route WHEN onNextClick THEN push Confirm`() = runTest {
            // Arrange
            val model = createSendModel(this)

            // Act
            model.onNextClick(CommonSendRoute.Destination(isEditMode = false))

            // Assert
            verify(exactly = 1) { router.push(CommonSendRoute.Confirm, any()) }
        }

        @Test
        fun `GIVEN route in edit mode WHEN onNextClick THEN pop without push`() = runTest {
            // Arrange
            val model = createSendModel(this)

            // Act
            model.onNextClick(CommonSendRoute.Amount(isEditMode = true))

            // Assert
            verify(exactly = 1) { router.pop(any()) }
            verify(exactly = 0) { router.push(any(), any()) }
        }

        @Test
        fun `GIVEN confirm route WHEN onNextClick THEN push ConfirmSuccess`() =
            runTest {
                // Arrange
                // CommonSendRoute.Confirm.isEditMode == false, so onNextClick pushes ConfirmSuccess.
                val model = createSendModel(this)

                // Act
                model.onNextClick(CommonSendRoute.Confirm)

                // Assert
                verify(exactly = 1) { router.push(CommonSendRoute.ConfirmSuccess, any()) }
                verify(exactly = 0) { router.pop(any()) }
            }
    }

    @Nested
    inner class ConsumeEntryType {

        @Test
        fun `GIVEN entry type QR WHEN consumeEntryType first call THEN return QR`() = runTest {
            // Arrange
            val params = defaultSendParams().copy(entryType = SendComponent.EntryType.QR)
            val model = createSendModel(this, MutableParamsContainer(params))

            // Act
            val result = model.consumeEntryType()

            // Assert
            assertThat(result).isEqualTo(CommonSendAnalyticEvents.SendEntryType.QR)
        }

        @Test
        fun `GIVEN entry type QR WHEN consumeEntryType called twice THEN second returns Manual`() = runTest {
            // Arrange
            val params = defaultSendParams().copy(entryType = SendComponent.EntryType.QR)
            val model = createSendModel(this, MutableParamsContainer(params))

            // Act
            val first = model.consumeEntryType()
            val second = model.consumeEntryType()

            // Assert
            assertThat(first).isEqualTo(CommonSendAnalyticEvents.SendEntryType.QR)
            assertThat(second).isEqualTo(CommonSendAnalyticEvents.SendEntryType.Manual)
        }

        @Test
        fun `GIVEN entry type Manual WHEN consumeEntryType THEN return Manual`() = runTest {
            // Arrange
            val params = defaultSendParams().copy(entryType = SendComponent.EntryType.Manual)
            val model = createSendModel(this, MutableParamsContainer(params))

            // Act
            val result = model.consumeEntryType()

            // Assert
            assertThat(result).isEqualTo(CommonSendAnalyticEvents.SendEntryType.Manual)
        }
    }

    @Nested
    inner class LoadFee {

        @Test
        fun `GIVEN transaction created WHEN loadFee THEN return fee from use case`() = runTest {
            // Arrange
            val model = createSendModel(this)
            advanceUntilIdle()
            model.predefinedValues = deeplink(amount = "1.0")
            val expectedFee = mockk<TransactionFee>(relaxed = true)
            coEvery { getFeeUseCase(any(), any(), any()) } returns expectedFee.right()

            // Act
            val result = model.loadFee()

            // Assert
            assertThat(result).isEqualTo(expectedFee.right())
        }

        @Test
        fun `GIVEN transaction creation fails WHEN loadFee THEN return DataError`() = runTest {
            // Arrange
            val model = createSendModel(this)
            advanceUntilIdle()
            model.predefinedValues = deeplink(amount = "1.0")
            coEvery {
                createTransferTransactionUseCase(any(), any<String>(), any(), any(), any(), any())
            } returns IllegalStateException("boom").left()

            // Act
            val result = model.loadFee()

            // Assert
            assertThat(result.leftOrNull()).isInstanceOf(GetFeeError.DataError::class.java)
        }

        @Test
        fun `GIVEN fee use case fails WHEN loadFee THEN return that error`() = runTest {
            // Arrange
            val model = createSendModel(this)
            advanceUntilIdle()
            model.predefinedValues = deeplink(amount = "1.0")
            coEvery { getFeeUseCase(any(), any(), any()) } returns GetFeeError.UnknownError.left()

            // Act
            val result = model.loadFee()

            // Assert
            assertThat(result).isEqualTo(GetFeeError.UnknownError.left())
        }
    }

    @Nested
    inner class LoadFeeExtended {

        private val defaultFee: TransactionFeeExtended = mockk(relaxed = true)
        private val tokenFee: TransactionFeeExtended = mockk(relaxed = true)

        @Test
        fun `GIVEN no fee token picked WHEN loadFeeExtended THEN the default fee is resolved`() = runTest {
            // Arrange
            val model = createLoadedSendModel()
            givenDefaultFee(isNativeFeePreferred = false)

            // Act
            val result = model.loadFeeExtended(maybeToken = null)

            // Assert
            assertThat(result).isEqualTo(defaultFee.right())
            coVerify(exactly = 0) { getFeeForTokenUseCase(any(), any(), any(), any()) }
        }

        @Test
        fun `GIVEN a fee token picked WHEN loadFeeExtended THEN the fee is quoted in that token`() = runTest {
            // Arrange
            val model = createLoadedSendModel()
            val feeToken: CryptoCurrencyStatus = mockk(relaxed = true)
            coEvery { getFeeForTokenUseCase(any(), feeToken.currency, any(), any()) } returns tokenFee.right()

            // Act
            val result = model.loadFeeExtended(maybeToken = feeToken)

            // Assert
            assertThat(result).isEqualTo(tokenFee.right())
            coVerify(exactly = 0) { getFeeForGaslessUseCase(any(), any(), any(), any(), any(), any()) }
        }

        @Test
        fun `GIVEN coin not preferred over a token fee WHEN loading again THEN token fee is re-evaluated`() =
            runTest {
                // Arrange
                val model = createLoadedSendModel()
                givenDefaultFee(isNativeFeePreferred = false)

                // Act
                model.loadFeeExtended(maybeToken = null)
                model.loadFeeExtended(maybeToken = null)

                // Assert
                coVerify(exactly = 2) { getFeeForGaslessUseCase(any(), any(), any(), any(), any(), any()) }
                coVerify(exactly = 0) { getFeeUseCase(any(), any(), any()) }
            }

        @Test
        fun `GIVEN the coin was preferred over a token fee WHEN loading again THEN the coin fee stays`() = runTest {
            // Arrange
            val model = createLoadedSendModel()
            givenDefaultFee(isNativeFeePreferred = true)
            coEvery { getFeeUseCase(any(), any(), any()) } returns mockk<TransactionFee>(relaxed = true).right()

            // Act
            model.loadFeeExtended(maybeToken = null)
            model.loadFeeExtended(maybeToken = null)

            // Assert
            coVerify(exactly = 1) { getFeeForGaslessUseCase(any(), any(), any(), any(), any(), any()) }
            coVerify(exactly = 1) { getFeeUseCase(any(), any(), any()) }
        }

        private fun TestScope.createLoadedSendModel(): SendModel {
            val model = createSendModel(this)
            advanceUntilIdle()
            model.predefinedValues = deeplink(amount = "1.0")
            return model
        }

        private fun givenDefaultFee(isNativeFeePreferred: Boolean) {
            coEvery { getFeeForGaslessUseCase(any(), any(), any(), any(), any(), any()) } answers {
                if (isNativeFeePreferred) arg<() -> Unit>(5).invoke()
                defaultFee.right()
            }
        }
    }

    @Nested
    inner class OnBackClick {

        @Test
        fun `GIVEN amount route non-edit WHEN onBackClick THEN send analytics and pop`() = runTest {
            // Arrange
            val model = createSendModel(this)

            // Act
            model.onBackClick(CommonSendRoute.Amount(isEditMode = false))

            // Assert
            verify(exactly = 1) { analyticsEventHandler.send(any<CommonSendAnalyticEvents.CloseButtonClicked>()) }
            verify(exactly = 1) { router.pop(any()) }
        }

        @Test
        fun `GIVEN destination route edit WHEN onBackClick THEN pop without analytics`() = runTest {
            // Arrange
            val model = createSendModel(this)

            // Act
            model.onBackClick(CommonSendRoute.Destination(isEditMode = true))

            // Assert
            verify(exactly = 0) { analyticsEventHandler.send(any<CommonSendAnalyticEvents.CloseButtonClicked>()) }
            verify(exactly = 1) { router.pop(any()) }
        }
    }

    @Nested
    inner class ResetSendNavigation {

        @Test
        fun `GIVEN any state WHEN resetSendNavigation THEN reset states and popTo Amount`() = runTest {
            // Arrange
            val model = createSendModel(this)

            // Act
            model.resetSendNavigation()

            // Assert
            val state = model.uiState.value
            assertThat(state.feeSelectorUM).isEqualTo(FeeSelectorUMRedesigned.Loading)
            assertThat(state.confirmUM).isEqualTo(ConfirmUM.Empty)
            assertThat(state.confirmData).isNull()
            verify(exactly = 1) { router.popTo(CommonSendRoute.Amount(isEditMode = false), any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IntentRoute {

        @BeforeEach
        fun resetRouter() {
            clearMocks(router)
        }

        @Test
        fun `GIVEN intent params WHEN model created THEN predefined values are editable main screen QR`() = runTest {
            // Arrange
            val params = intentParams(amount = "5")

            // Act
            val model = createSendModel(this, MutableParamsContainer(params))

            // Assert
            assertThat(model.predefinedValues).isEqualTo(
                PredefinedValues.Content.QrCode(
                    amount = "5",
                    address = INTENT_ADDRESS,
                    memo = INTENT_MEMO,
                    source = PredefinedValues.Source.MAIN_SCREEN,
                ),
            )
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN intent params WHEN currency status loaded THEN opens initial route by balance`(
            model: InitialRouteModel,
        ) = runTest {
            // Arrange
            val status = loadedStatus(currency = testCryptoCurrency, balance = model.balance)
            val account = mockk<Account.Personal>(relaxed = true) {
                every { cryptoCurrencies } returns listOf(testCryptoCurrency)
            }
            every { getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency) } returns
                flowOf(AccountCryptoCurrencyStatus(account = account, status = status))
            coEvery { getFeePaidCryptoCurrencyStatusSyncUseCase(any(), any()) } returns status.right()
            val sendModel = createSendModel(this, MutableParamsContainer(intentParams(amount = model.amount)))

            // Act
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { router.replaceAll(model.expectedRoute, onComplete = any()) }
            sendModel.onDestroy()
        }

        private fun provideTestModels() = listOf(
            InitialRouteModel(
                amount = "5",
                balance = BigDecimal.TEN,
                expectedRoute = CommonSendRoute.Confirm,
            ),
            InitialRouteModel(
                amount = "5",
                balance = BigDecimal.ONE,
                expectedRoute = CommonSendRoute.Amount(isEditMode = false),
            ),
        )

        private fun intentParams(amount: String) = defaultSendParams().copy(
            amount = amount,
            destinationAddress = INTENT_ADDRESS,
            tag = INTENT_MEMO,
            intentId = "intent-1",
        )
    }

    data class InitialRouteModel(
        val amount: String,
        val balance: BigDecimal,
        val expectedRoute: CommonSendRoute,
    )

    private fun deeplink(amount: String) = PredefinedValues.Content.Deeplink(
        amount = amount,
        address = "addr123",
        memo = null,
        transactionId = "tx123",
    )

    private companion object {
        const val INTENT_ADDRESS = "intentAddress"
        const val INTENT_MEMO = "intentMemo"
    }
}