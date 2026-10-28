package com.tangem.features.polymarket.impl.placeprediction.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.Model
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PredictionOrderFailure
import com.tangem.domain.polymarket.model.PolymarketEventError
import com.tangem.domain.polymarket.model.PolymarketPositionsError
import com.tangem.domain.polymarket.model.PredictionOrderFees
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderQuoteError
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.polymarket.impl.common.FakeSlotHost
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.entity.DEFAULT_SLIPPAGE_PERCENT
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceActionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceResultUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionFeeSheetUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SellPositionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SharesProceedsUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SlippageUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.features.polymarket.impl.placeprediction.entity.canShowFeeBreakdown
import com.tangem.features.polymarket.impl.placeprediction.entity.enteredAmount
import com.tangem.features.polymarket.impl.placeprediction.entity.isReadyToPlace
import com.tangem.features.polymarket.impl.placeprediction.model.converter.PredictionFeeSheetUMConverter
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
internal class PlacePredictionOrderModelTest : PlacePredictionModelTestBase() {

    private val defaultLocale = Locale.getDefault()

    @BeforeEach
    fun pinLocale() {
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    @Nested
    inner class Place {

        @Test
        fun `GIVEN a priced quote WHEN place clicked THEN the status step replaces the stack`() = runOrderTest {
            // Arrange
            val model = createQuotedModel()

            // Act
            model.onPlaceClick()
            runCurrent()

            // Assert
            coVerify(exactly = 1) { signOrderInteractor(request = any()) }
            verify(exactly = 1) {
                router.replaceAll(match<PlacePredictionRoute.Status> { it.side == BUY_PARAMS.side }, onComplete = any())
            }
        }

        @Test
        fun `GIVEN the screen leaves on success WHEN the order is placed THEN no quote is requested after it`() =
            runOrderTest {
                // Arrange
                val dispatcher = UnconfinedTestDispatcher(scheduler = testScheduler)
                val dispatchers = TestingCoroutineDispatcherProvider(
                    main = dispatcher,
                    mainImmediate = dispatcher,
                    io = dispatcher,
                    default = dispatcher,
                    single = dispatcher,
                )
                val model = createOrderModel(testScope = this, dispatchers = dispatchers)
                every { router.replaceAll(any(), onComplete = any()) } answers {
                    model.onPause()
                    model.onDestroy()
                }
                advanceUntilIdle()
                model.onResume()
                model.onAmountChange(value = "10")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

                // Act
                model.onPlaceClick()
                advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 3)

                // Assert
                verify(exactly = 1) { router.replaceAll(any(), onComplete = any()) }
                coVerify(exactly = 1) { getQuoteUseCase(userWalletId = any(), request = any()) }
            }

        @Test
        fun `GIVEN the quote is not ready WHEN place clicked THEN nothing is signed`() = runOrderTest {
            // Arrange
            val model = createOrderModel(testScope = this)
            advanceUntilIdle()
            model.onResume()
            model.onAmountChange(value = "10")

            // Act
            model.onPlaceClick()
            runCurrent()

            // Assert
            assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
            coVerify(exactly = 0) { signOrderInteractor(request = any()) }
            verify(exactly = 0) { router.replaceAll(any(), onComplete = any()) }
        }

        @Test
        fun `GIVEN signing is in flight WHEN place clicked again THEN the order is signed once`() = runOrderTest {
            // Arrange
            coEvery { signOrderInteractor(request = any()) } coAnswers {
                delay(timeMillis = SIGNING_MILLIS)
                SIGNED_ORDER.right()
            }
            val model = createQuotedModel()

            // Act
            model.onPlaceClick()
            model.onPlaceClick()
            advanceTimeBy(delayTimeMillis = SIGNING_MILLIS + 1)

            // Assert
            coVerify(exactly = 1) { signOrderInteractor(request = any()) }
            coVerify(exactly = 1) { placeOrderUseCase(any(), any(), any()) }
        }

        @Test
        fun `GIVEN place clicked WHEN the click returns THEN signing has already started`() = runOrderTest {
            // Arrange
            coEvery { signOrderInteractor(request = any()) } coAnswers {
                delay(timeMillis = SIGNING_MILLIS)
                SIGNED_ORDER.right()
            }
            val model = createQuotedModel()

            // Act
            model.onPlaceClick()

            // Assert
            assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Signing)
            assertThat(model.isSubmitting()).isTrue()
        }

        @Test
        fun `GIVEN the signature is declined WHEN place clicked THEN the quote loop resumes`() = runOrderTest {
            // Arrange
            coEvery { signOrderInteractor(request = any()) } returns
                PredictionOrderFailure.Signing(error = PolymarketSigningError.UserCancelled).left()
            val model = createQuotedModel()

            // Act
            model.onPlaceClick()
            runCurrent()

            // Assert
            assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
            coVerify(exactly = 2) { getQuoteUseCase(userWalletId = any(), request = any()) }
            verify(exactly = 0) { router.replaceAll(any(), onComplete = any()) }
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(FrozenInput::class)
        fun `GIVEN signing is in flight WHEN an input is used THEN the order stays as it was`(input: FrozenInput) =
            runOrderTest {
                // Arrange
                coEvery { signOrderInteractor(request = any()) } coAnswers {
                    delay(timeMillis = SIGNING_MILLIS)
                    SIGNED_ORDER.right()
                }
                val model = createQuotedModel()
                val fundingSheet = FakeSlotHost(model.fundingSheetNavigation)
                model.onPlaceClick()

                // Act
                input.apply(model)
                runCurrent()

                // Assert
                assertThat(model.uiState.value.amountValue).isEqualTo("10")
                assertThat(fundingSheet.configuration).isNull()
                verify(exactly = 0) { router.push(any(), any()) }
                verify(exactly = 0) { messageSender.send(any()) }
                verify(exactly = 0) { urlOpener.openUrl(any()) }
                coVerify(exactly = 1) { getQuoteUseCase(userWalletId = any(), request = any()) }
            }

        @Test
        fun `GIVEN signing is in flight WHEN close clicked THEN the flow stays`() = runOrderTest {
            // Arrange
            coEvery { signOrderInteractor(request = any()) } coAnswers {
                delay(timeMillis = SIGNING_MILLIS)
                SIGNED_ORDER.right()
            }
            val model = createQuotedModel()
            model.onPlaceClick()

            // Act
            model.onCloseClick()

            // Assert
            verify(exactly = 0) { router.pop(any()) }
        }
    }

    @Nested
    inner class WalletType {

        @Test
        fun `GIVEN a hot wallet WHEN the order screen opens THEN the order is placed by holding the button`() =
            runOrderTest {
                // Arrange
                every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(hotWallet()))

                // Act
                val model = createOrderModel(testScope = this)
                runCurrent()

                // Assert
                assertThat(model.uiState.value.isHoldToConfirm).isTrue()
            }

        @Test
        fun `GIVEN a card wallet WHEN the order screen opens THEN the order is placed by tapping the button`() =
            runOrderTest {
                // Arrange
                every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(coldWallet()))

                // Act
                val model = createOrderModel(testScope = this)
                runCurrent()

                // Assert
                assertThat(model.uiState.value.isHoldToConfirm).isFalse()
            }

        @Test
        fun `GIVEN the wallets are not loaded WHEN the hot wallet arrives THEN the order is placed by holding`() =
            runOrderTest {
                // Arrange
                val wallets = MutableStateFlow<List<UserWallet>?>(null)
                every { userWalletsListRepository.userWallets } returns wallets
                val model = createOrderModel(testScope = this)
                runCurrent()

                // Act
                wallets.value = listOf(hotWallet())
                runCurrent()

                // Assert
                assertThat(model.uiState.value.isHoldToConfirm).isTrue()
            }

        @Test
        fun `GIVEN the wallet is not in the list WHEN the order screen opens THEN the button is tapped`() =
            runOrderTest {
                // Arrange
                every { userWalletsListRepository.userWallets } returns MutableStateFlow(emptyList())

                // Act
                val model = createOrderModel(testScope = this)
                runCurrent()

                // Assert
                assertThat(model.uiState.value.isHoldToConfirm).isFalse()
            }
    }

    @Nested
    inner class HoldReset {

        @Test
        fun `GIVEN nothing to place WHEN place clicked THEN the hold button is reset`() = runOrderTest {
            // Arrange
            val model = createOrderModel(testScope = this)
            advanceUntilIdle()
            model.onResume()
            val before = model.uiState.value.holdResetKey

            // Act
            model.onPlaceClick()
            runCurrent()

            // Assert
            assertThat(model.uiState.value.holdResetKey).isEqualTo(before + 1)
        }

        @Test
        fun `GIVEN the signature is declined WHEN place clicked THEN the hold button is reset`() = runOrderTest {
            // Arrange
            coEvery { signOrderInteractor(request = any()) } returns
                PredictionOrderFailure.Signing(error = PolymarketSigningError.UserCancelled).left()
            val model = createQuotedModel()
            val before = model.uiState.value.holdResetKey

            // Act
            model.onPlaceClick()
            runCurrent()

            // Assert
            assertThat(model.uiState.value.holdResetKey).isEqualTo(before + 1)
        }

        @Test
        fun `GIVEN a priced quote WHEN the order is placed THEN the hold button is left as it is`() = runOrderTest {
            // Arrange
            val model = createQuotedModel()
            val before = model.uiState.value.holdResetKey

            // Act
            model.onPlaceClick()
            runCurrent()

            // Assert
            assertThat(model.uiState.value.holdResetKey).isEqualTo(before)
        }

        @Test
        fun `GIVEN a quote on the way WHEN place clicked THEN the hold button is left as it is`() = runOrderTest {
            // Arrange
            val model = createQuotingModel()
            val before = model.uiState.value.holdResetKey

            // Act
            model.onPlaceClick()
            runCurrent()

            // Assert
            assertThat(model.uiState.value.holdResetKey).isEqualTo(before)
        }

        @Test
        fun `GIVEN a tap while quoting WHEN the quote arrives and is placed THEN the hold button is left as it is`() =
            runOrderTest {
                // Arrange
                val model = createQuotingModel()
                val before = model.uiState.value.holdResetKey
                model.onPlaceClick()

                // Act
                advanceTimeBy(delayTimeMillis = QUOTE_MILLIS)
                runCurrent()

                // Assert
                verify(exactly = 1) { router.replaceAll(any(), onComplete = any()) }
                assertThat(model.uiState.value.holdResetKey).isEqualTo(before)
            }

        @Test
        fun `GIVEN a tap while quoting WHEN the quote fails THEN the hold button is reset`() = runOrderTest {
            // Arrange
            val model = createQuotingModel(result = PredictionOrderQuoteError.Network.left())
            val before = model.uiState.value.holdResetKey
            model.onPlaceClick()

            // Act
            advanceTimeBy(delayTimeMillis = QUOTE_MILLIS)
            runCurrent()

            // Assert
            assertThat(model.uiState.value.holdResetKey).isEqualTo(before + 1)
        }
    }

    @Nested
    inner class ClosePosition {

        @Test
        fun `GIVEN a sell WHEN its quote is priced THEN the whole position is quoted and shown with its proceeds`() =
            runOrderTest {
                // Arrange
                coEvery { getQuoteUseCase(userWalletId = any(), request = any()) } returns createSellQuote(shares = HELD).right()

                // Act
                val model = createQuotedModel(params = SELL_PARAMS)

                // Assert
                val state = model.uiState.value
                val expectedOrder = OrderSideUM.Sell(
                    position = SellPositionUM.Held(available = HELD),
                    proceeds = SharesProceedsUM.Content(expected = BigDecimal("11.01")),
                    balance = BigDecimal("100"),
                )
                assertThat(state.order).isEqualTo(expectedOrder)
                assertThat(state.canShowFeeBreakdown()).isTrue()
                assertThat(state.isReadyToPlace).isTrue()
                coVerify {
                    getQuoteUseCase(
                        userWalletId = any(),
                        request = match { it.side == PredictionOrderSide.SELL && it.amount.compareTo(HELD) == 0 },
                    )
                }
            }

        @Test
        fun `GIVEN a priced sell WHEN place clicked THEN it is signed and placed once without a summary`() =
            runOrderTest {
                // Arrange
                coEvery { getQuoteUseCase(userWalletId = any(), request = any()) } returns createSellQuote(shares = HELD).right()
                coEvery { placeOrderUseCase(any(), any(), any()) } returns
                    createPlaceResult(makingAmount = HELD, takingAmount = BigDecimal("11.01")).right()
                val model = createQuotedModel(params = SELL_PARAMS)
                val status = slot<PlacePredictionRoute.Status>()
                every { router.replaceAll(capture(status), onComplete = any()) } returns Unit

                // Act
                model.onPlaceClick()
                runCurrent()

                // Assert
                val expectedRoute = PlacePredictionRoute.Status(
                    result = PlaceResultUM.Filled(
                        placedAt = status.captured.result.placedAt,
                        collateralAmount = BigDecimal("11.01"),
                        shares = HELD,
                    ),
                    market = model.uiState.value.market,
                    side = PredictionOrderSide.SELL,
                    userWalletId = USER_WALLET_ID,
                    positionId = null,
                )
                assertThat(status.captured).isEqualTo(expectedRoute)
                coVerify(exactly = 1) {
                    signOrderInteractor(
                        request = match { it.side == PredictionOrderSide.SELL && it.assetId == ASSET_ID },
                    )
                }
                coVerify(exactly = 1) { placeOrderUseCase(any(), any(), any()) }
                verify(exactly = 0) { router.push(any<PlacePredictionRoute.Summary>(), any()) }
            }

        @Test
        fun `GIVEN a sell WHEN half the position is entered THEN only that part is quoted`() = runOrderTest {
            // Arrange
            val model = createQuotedModel(params = SELL_PARAMS)

            // Act
            model.onAmountChange(value = "25")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Assert
            coVerify {
                getQuoteUseCase(
                    userWalletId = any(),
                    request = match { it.side == PredictionOrderSide.SELL && it.amount.compareTo(BigDecimal("25")) == 0 },
                )
            }
        }
    }

    @Nested
    inner class RepeatedAmount {

        @Test
        fun `GIVEN a priced amount WHEN the same amount is entered again THEN the quote stays priced`() =
            runOrderTest {
                // Arrange
                val model = createQuotedModel()

                // Act
                model.onAmountChange(value = "10")

                // Assert
                assertThat(model.uiState.value.quote).isInstanceOf(QuoteUM.Content::class.java)
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
                coVerify(exactly = 1) { getQuoteUseCase(userWalletId = any(), request = any()) }
            }

        @Test
        fun `GIVEN max is priced WHEN max clicked again THEN the quote stays priced`() = runOrderTest {
            // Arrange
            val model = createQuotedModel()
            model.onMaxClick()
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Act
            model.onMaxClick()

            // Assert
            assertThat(model.uiState.value.amountValue).isEqualTo("90")
            assertThat(model.uiState.value.quote).isInstanceOf(QuoteUM.Content::class.java)
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            coVerify(exactly = 2) { getQuoteUseCase(userWalletId = any(), request = any()) }
        }

        @Test
        fun `GIVEN a typed amount with cents WHEN the equal preset clicked THEN the quote stays priced`() =
            runOrderTest {
                // Arrange
                val model = createQuotedModel()
                model.onAmountChange(value = "100.00")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

                // Act
                model.onPresetClick(preset = model.amountPresets.first())

                // Assert
                assertThat(model.uiState.value.amountValue).isEqualTo("100.00")
                assertThat(model.uiState.value.quote).isInstanceOf(QuoteUM.Content::class.java)
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
                coVerify(exactly = 2) { getQuoteUseCase(userWalletId = any(), request = any()) }
            }

        @Test
        fun `GIVEN the max typed with a comma WHEN max clicked THEN the quote stays priced`() = runOrderTest {
            // Arrange
            coEvery { getQuoteUseCase(userWalletId = any(), request = any()) } returns createQuote().copy(
                fees = PredictionOrderFees(
                    market = BigDecimal("1.2"),
                    builder = BigDecimal.ZERO,
                    total = BigDecimal("1.2"),
                ),
            ).right()
            val model = createQuotedModel()
            model.onAmountChange(value = "89,28")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Act
            model.onMaxClick()

            // Assert
            assertThat(model.uiState.value.amountValue).isEqualTo("89,28")
            assertThat(model.uiState.value.quote).isInstanceOf(QuoteUM.Content::class.java)
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            coVerify(exactly = 2) { getQuoteUseCase(userWalletId = any(), request = any()) }
        }

        @Test
        fun `GIVEN a typed amount WHEN a decimal separator is typed after it THEN the field keeps it`() =
            runOrderTest {
                // Arrange
                val model = createQuotedModel()

                // Act
                model.onAmountChange(value = "10.")

                // Assert
                assertThat(model.uiState.value.amountValue).isEqualTo("10.")
            }
    }

    @Nested
    inner class Presets {

        @Test
        fun `GIVEN a buy WHEN presets are read THEN they are 100, 500, 1000 and 5000`() = runOrderTest {
            // Arrange
            val model = createOrderModel(testScope = this)

            // Act
            val amounts = model.amountPresets.map { it.amount }

            // Assert
            assertThat(amounts).containsExactly("100", "500", "1000", "5000").inOrder()
        }

        @Test
        fun `GIVEN a sell WHEN presets are read THEN there are none`() = runOrderTest {
            // Arrange
            val model = createOrderModel(testScope = this, params = SELL_PARAMS)

            // Act
            val presets = model.amountPresets

            // Assert
            assertThat(presets).isEmpty()
        }

        @Test
        fun `GIVEN a buy WHEN a preset clicked THEN its amount is entered`() = runOrderTest {
            // Arrange
            val model = createOrderModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onPresetClick(preset = model.amountPresets[2])

            // Assert
            assertThat(model.uiState.value.amountValue).isEqualTo("1000")
        }

        @Test
        fun `GIVEN a buy WHEN a preset clicked THEN the field moves the cursor past it`() = runOrderTest {
            // Arrange
            val model = createOrderModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onPresetClick(preset = model.amountPresets.first())

            // Assert
            assertThat(model.uiState.value.isAmountFromFraction).isTrue()
        }

        @Test
        fun `GIVEN a picked preset WHEN an amount is typed THEN the field keeps the typed cursor`() = runOrderTest {
            // Arrange
            val model = createOrderModel(testScope = this)
            advanceUntilIdle()
            model.onPresetClick(preset = model.amountPresets.first())

            // Act
            model.onAmountChange(value = "1005")

            // Assert
            assertThat(model.uiState.value.isAmountFromFraction).isFalse()
        }
    }

    @Nested
    inner class Max {

        @ParameterizedTest(name = "balance {0} without a quote enters {1}")
        @CsvSource(
            "100, 90",
            "10.019, 9.01",
            "0.019, 0.01",
        )
        fun `GIVEN no quote WHEN max clicked THEN nine tenths of the balance floored to cents is entered`(
            balance: String,
            expected: String,
        ) = runOrderTest {
            // Arrange
            statusFlow.value = activeStatus(balance = BigDecimal(balance))
            val model = createOrderModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onMaxClick()

            // Assert
            assertThat(model.uiState.value.amountValue).isEqualTo(expected)
        }

        @ParameterizedTest(name = "fees {0} on notional {1} enter {2}")
        @CsvSource(
            "0.4, 10, 90",
            "20, 100, 83.33",
            "1.2, 10, 89.28",
            "0, 10, 90",
        )
        fun `GIVEN a quote WHEN max clicked THEN the lower of the balance cap and the fee cap is entered`(
            feeTotal: String,
            notional: String,
            expected: String,
        ) = runOrderTest {
            // Arrange
            coEvery { getQuoteUseCase(userWalletId = any(), request = any()) } returns createQuote().copy(
                notional = BigDecimal(notional),
                fees = PredictionOrderFees(
                    market = BigDecimal(feeTotal),
                    builder = BigDecimal.ZERO,
                    total = BigDecimal(feeTotal),
                ),
            ).right()
            val model = createQuotedModel()

            // Act
            model.onMaxClick()

            // Assert
            assertThat(model.uiState.value.amountValue).isEqualTo(expected)
        }

        @Test
        fun `GIVEN a fee capped max WHEN max clicked again before its quote returns THEN the fee cap holds`() =
            runOrderTest {
                // Arrange
                coEvery { getQuoteUseCase(userWalletId = any(), request = any()) } returns createQuote().copy(
                    notional = BigDecimal(100),
                    fees = PredictionOrderFees(
                        market = BigDecimal(20),
                        builder = BigDecimal.ZERO,
                        total = BigDecimal(20),
                    ),
                ).right()
                val model = createQuotedModel()
                model.onMaxClick()

                // Act
                model.onMaxClick()

                // Assert
                assertThat(model.uiState.value.amountValue).isEqualTo("83.33")
            }

        @Test
        fun `GIVEN a German locale WHEN max clicked THEN the amount is entered with a comma the screen reads back`() =
            runOrderTest {
                // Arrange
                Locale.setDefault(Locale.GERMANY)
                statusFlow.value = activeStatus(balance = BigDecimal("10.019"))
                val model = createOrderModel(testScope = this)
                advanceUntilIdle()

                // Act
                model.onMaxClick()

                // Assert
                assertThat(model.uiState.value.amountValue).isEqualTo("9,01")
                assertThat(model.uiState.value.enteredAmount())
                    .isEquivalentAccordingToCompareTo(BigDecimal("9.01"))
            }

        @Test
        fun `GIVEN a balance below a cent after the cap WHEN max clicked THEN nothing is entered`() = runOrderTest {
            // Arrange
            statusFlow.value = activeStatus(balance = BigDecimal("0.011"))
            val model = createOrderModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onMaxClick()

            // Assert
            assertThat(model.uiState.value.amountValue).isEmpty()
        }

        @Test
        fun `GIVEN the balance is not confirmed WHEN max clicked THEN nothing is entered`() = runOrderTest {
            // Arrange
            statusFlow.value = activeStatus(source = StatusSource.CACHE)
            val model = createOrderModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onMaxClick()

            // Assert
            assertThat(model.uiState.value.amountValue).isEmpty()
        }
    }

    @Nested
    inner class Lifecycle {

        @Test
        fun `GIVEN a model WHEN resumed THEN the price is polled until paused`() = runOrderTest {
            // Arrange
            val model = createOrderModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onResume()
            runCurrent()
            model.onPause()
            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 3)

            // Assert
            coVerify(exactly = 1) { getPriceUseCase(assetId = any(), side = any()) }
        }

        @Test
        fun `GIVEN a model WHEN not resumed THEN nothing is polled`() = runOrderTest {
            // Arrange
            val model = createOrderModel(testScope = this)

            // Act
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 0) { getPriceUseCase(assetId = any(), side = any()) }
            assertThat(model.isSubmitting()).isFalse()
        }
    }

    private fun runOrderTest(block: suspend TestScope.() -> Unit) = runTest {
        try {
            block()
        } finally {
            models.forEach(Model::onDestroy)
            models.clear()
        }
    }

    @Nested
    inner class FeeSheet {

        @Test
        fun `GIVEN a priced quote WHEN fee clicked THEN the sheet is titled total`() = runOrderTest {
            // Arrange
            val model = createQuotedModel()
            val slot = FakeSlotHost(model.feeSheet.navigation)
            val expected = PredictionFeeSheetUMConverter.convert(
                quote = createQuote(),
                slippage = SlippageUM.of(percent = DEFAULT_SLIPPAGE_PERCENT),
                tokenSymbol = PolymarketCollateralCurrencyFactory.COLLATERAL_SYMBOL,
                heading = PredictionFeeSheetUM.Heading.Total,
            )

            // Act
            model.onFeeClick()

            // Assert
            assertThat(slot.configuration).isEqualTo(Unit)
            assertThat(model.feeSheet.state.value).isEqualTo(expected)
        }
    }

    @Nested
    inner class TapWhileQuoting {

        @Test
        fun `GIVEN a quote on the way WHEN place clicked THEN the placement waits and nothing is signed`() =
            runOrderTest {
                // Arrange
                val model = createQuotingModel()

                // Act
                model.onPlaceClick()
                runCurrent()

                // Assert
                assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.AwaitingQuote)
                assertThat(model.isSubmitting()).isTrue()
                coVerify(exactly = 0) { signOrderInteractor(request = any()) }
            }

        @Test
        fun `GIVEN a tap while quoting WHEN the quote arrives THEN the order is placed`() = runOrderTest {
            // Arrange
            val model = createQuotingModel()
            model.onPlaceClick()

            // Act
            advanceTimeBy(delayTimeMillis = QUOTE_MILLIS)
            runCurrent()

            // Assert
            assertThat(model.uiState.value.submit).isNotEqualTo(SubmitUM.AwaitingQuote)
            coVerify(exactly = 1) { signOrderInteractor(request = any()) }
            verify(exactly = 1) {
                router.replaceAll(match<PlacePredictionRoute.Status> { it.side == BUY_PARAMS.side }, onComplete = any())
            }
        }

        @Test
        fun `GIVEN a tap while quoting WHEN the quote fails THEN the tap is released and nothing is signed`() =
            runOrderTest {
                // Arrange
                val model = createQuotingModel(result = PredictionOrderQuoteError.Network.left())
                model.onPlaceClick()

                // Act
                advanceTimeBy(delayTimeMillis = QUOTE_MILLIS)
                runCurrent()

                // Assert
                assertThat(model.uiState.value.submit).isNotEqualTo(SubmitUM.AwaitingQuote)
                assertThat(model.isSubmitting()).isFalse()
                coVerify(exactly = 0) { signOrderInteractor(request = any()) }
            }

        @Test
        fun `GIVEN a tap while quoting WHEN the arrived quote cannot be placed THEN the tap is released`() =
            runOrderTest {
                // Arrange
                val model = createQuotingModel(
                    result = createQuote(status = PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY).right(),
                )
                model.onPlaceClick()

                // Act
                advanceTimeBy(delayTimeMillis = QUOTE_MILLIS)
                runCurrent()

                // Assert
                assertThat(model.uiState.value.submit).isNotEqualTo(SubmitUM.AwaitingQuote)
                coVerify(exactly = 0) { signOrderInteractor(request = any()) }
                verify(exactly = 0) { router.pop(any()) }
            }

        @ParameterizedTest(name = "a quote arriving {0} ms after the tap signs {1} order(s)")
        @CsvSource("9999, 1", "10001, 0")
        fun `GIVEN a tap while quoting WHEN the quote arrives THEN it is placed only within ten seconds`(
            quoteMillis: Long,
            expectedSignings: Int,
        ) = runOrderTest {
            // Arrange
            val model = createQuotingModel(quoteMillis = quoteMillis)
            model.onPlaceClick()

            // Act
            advanceTimeBy(delayTimeMillis = quoteMillis)
            runCurrent()

            // Assert
            assertThat(model.uiState.value.submit).isNotEqualTo(SubmitUM.AwaitingQuote)
            coVerify(exactly = expectedSignings) { signOrderInteractor(request = any()) }
        }

        @Test
        fun `GIVEN a tap while quoting WHEN the screen is left THEN the tap is dropped`() = runOrderTest {
            // Arrange
            val model = createQuotingModel()
            model.onPlaceClick()

            // Act
            model.onPause()
            runCurrent()

            // Assert
            assertThat(model.uiState.value.submit).isNotEqualTo(SubmitUM.AwaitingQuote)
            model.onResume()
            advanceTimeBy(delayTimeMillis = QUOTE_MILLIS * 2)
            runCurrent()
            coVerify(exactly = 0) { signOrderInteractor(request = any()) }
        }

        @Test
        fun `GIVEN a tap while quoting WHEN the amount changes THEN the tap is dropped`() = runOrderTest {
            // Arrange
            val model = createQuotingModel()
            model.onPlaceClick()

            // Act
            model.onAmountChange(value = "25")
            runCurrent()

            // Assert
            assertThat(model.uiState.value.submit).isNotEqualTo(SubmitUM.AwaitingQuote)
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + QUOTE_MILLIS * 2)
            runCurrent()
            coVerify(exactly = 0) { signOrderInteractor(request = any()) }
        }

        @Test
        fun `GIVEN a tap while quoting WHEN place clicked again THEN the order is signed once`() = runOrderTest {
            // Arrange
            val model = createQuotingModel()

            // Act
            model.onPlaceClick()
            model.onPlaceClick()
            advanceTimeBy(delayTimeMillis = QUOTE_MILLIS)
            runCurrent()

            // Assert
            coVerify(exactly = 1) { signOrderInteractor(request = any()) }
            coVerify(exactly = 1) { placeOrderUseCase(any(), any(), any()) }
        }

        @Test
        fun `GIVEN a tap while quoting WHEN place clicked again THEN the first tap still bounds the wait`() =
            runOrderTest {
                // Arrange
                val model = createQuotingModel(quoteMillis = 10_001L)
                model.onPlaceClick()
                advanceTimeBy(delayTimeMillis = 5_000L)

                // Act
                model.onPlaceClick()
                advanceTimeBy(delayTimeMillis = 5_001L)
                runCurrent()

                // Assert
                assertThat(model.uiState.value.submit).isNotEqualTo(SubmitUM.AwaitingQuote)
                coVerify(exactly = 0) { signOrderInteractor(request = any()) }
            }

        @ParameterizedTest(name = "{0}")
        @EnumSource(MarketLoad::class)
        fun `GIVEN no quote request runs WHEN an amount is entered THEN place is not tappable`(load: MarketLoad) =
            runOrderTest {
                // Arrange
                when (load) {
                    MarketLoad.Hanging -> coEvery { getEventUseCase(userWalletId = any(), eventId = any()) } coAnswers { awaitCancellation() }
                    MarketLoad.Failed -> coEvery { getEventUseCase(userWalletId = any(), eventId = any()) } returns
                        PolymarketEventError.Network.left()
                }
                val model = createOrderModel(testScope = this)
                runCurrent()
                model.onResume()

                // Act
                model.onAmountChange(value = "10")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
                model.onPlaceClick()

                // Assert
                assertThat(model.uiState.value.quote).isEqualTo(QuoteUM.Loading(isAnswerExpected = false))
                assertThat(model.uiState.value.placeAction).isEqualTo(PlaceActionUM.Unavailable)
                assertThat(model.uiState.value.submit).isNotEqualTo(SubmitUM.AwaitingQuote)
            }

        @Test
        fun `GIVEN the quote request hangs WHEN ten seconds pass after the tap THEN the tap is released`() =
            runOrderTest {
                // Arrange
                coEvery { getQuoteUseCase(userWalletId = any(), request = any()) } coAnswers { awaitCancellation() }
                val model = createOrderModel(testScope = this)
                advanceUntilIdle()
                model.onResume()
                model.onAmountChange(value = "10")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS)
                runCurrent()
                model.onPlaceClick()

                // Act
                advanceTimeBy(delayTimeMillis = PENDING_PLACEMENT_MILLIS + 1)
                runCurrent()
                model.onCloseClick()

                // Assert
                assertThat(model.uiState.value.submit).isNotEqualTo(SubmitUM.AwaitingQuote)
                assertThat(model.isSubmitting()).isFalse()
                assertThat(model.uiState.value.placeAction).isEqualTo(PlaceActionUM.Unavailable)
                coVerify(exactly = 0) { signOrderInteractor(request = any()) }
                verify(exactly = 1) { router.pop(any()) }
            }

        @Test
        fun `GIVEN a tap released by the timeout WHEN a new amount is requested THEN place is tappable again`() =
            runOrderTest {
                // Arrange
                coEvery { getQuoteUseCase(userWalletId = any(), request = any()) } coAnswers { awaitCancellation() }
                val model = createOrderModel(testScope = this)
                advanceUntilIdle()
                model.onResume()
                model.onAmountChange(value = "10")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS)
                runCurrent()
                model.onPlaceClick()
                advanceTimeBy(delayTimeMillis = PENDING_PLACEMENT_MILLIS + 1)
                runCurrent()

                // Act
                model.onAmountChange(value = "25")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS)
                runCurrent()

                // Assert
                assertThat(model.uiState.value.placeAction).isEqualTo(PlaceActionUM.ReadyOnceQuoted)
            }

        @Test
        fun `GIVEN a sell tap WHEN the position read restarts the quote mid-wait THEN the order is placed on the re-quote`() =
            runOrderTest {
                // Arrange
                var positionReads = 0
                coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } coAnswers {
                    if (positionReads++ == 1) delay(timeMillis = POSITION_READ_MILLIS)
                    createPosition().right()
                }
                coEvery { getQuoteUseCase(userWalletId = any(), request = any()) } coAnswers {
                    delay(timeMillis = QUOTE_MILLIS)
                    createSellQuote(shares = BigDecimal("51.234567")).right()
                }
                val model = createOrderModel(testScope = this, params = SELL_PARAMS)
                runCurrent()
                model.onResume()
                runCurrent()
                model.onPlaceClick()

                // Act
                advanceTimeBy(delayTimeMillis = POSITION_READ_MILLIS)
                runCurrent()
                val submitAfterRestart = model.uiState.value.submit
                advanceTimeBy(delayTimeMillis = QUOTE_MILLIS)
                runCurrent()

                // Assert
                assertThat(submitAfterRestart).isEqualTo(SubmitUM.AwaitingQuote)
                coVerify(atLeast = 2) { getQuoteUseCase(userWalletId = any(), request = any()) }
                coVerify(exactly = 1) { signOrderInteractor(request = any()) }
            }

        @ParameterizedTest(name = "{0}")
        @EnumSource(MidWaitRead::class)
        fun `GIVEN a sell tap WHEN the position re-read stops the quote loop mid-wait THEN the tap is released at once`(
            read: MidWaitRead,
        ) = runOrderTest {
            // Arrange
            var positionReads = 0
            coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } coAnswers {
                if (positionReads++ == 0) {
                    createPosition().right()
                } else {
                    delay(timeMillis = POSITION_READ_MILLIS)
                    when (read) {
                        MidWaitRead.Fails -> PolymarketPositionsError.Network.left()
                        MidWaitRead.StopsAcceptingOrders -> createPosition().copy(isAcceptingOrders = false).right()
                    }
                }
            }
            coEvery { getQuoteUseCase(userWalletId = any(), request = any()) } coAnswers {
                delay(timeMillis = QUOTE_MILLIS)
                createSellQuote(shares = BigDecimal("51.234567")).right()
            }
            val model = createOrderModel(testScope = this, params = SELL_PARAMS)
            runCurrent()
            model.onResume()
            runCurrent()
            model.onPlaceClick()

            // Act
            advanceTimeBy(delayTimeMillis = POSITION_READ_MILLIS)
            runCurrent()

            // Assert
            assertThat(model.uiState.value.submit).isNotEqualTo(SubmitUM.AwaitingQuote)
            assertThat(model.isSubmitting()).isFalse()
            coVerify(exactly = 0) { signOrderInteractor(request = any()) }
        }

        @Test
        fun `GIVEN a pending tap WHEN the gate closes mid-wait THEN the spinner stays until the quote and the tap drops`() =
            runOrderTest {
                // Arrange
                val model = createQuotingModel()
                model.onPlaceClick()

                // Act
                statusFlow.value = activeStatus(balance = BigDecimal("5"))
                runCurrent()
                val midWait = model.uiState.value
                advanceTimeBy(delayTimeMillis = QUOTE_MILLIS)
                runCurrent()

                // Assert
                assertThat(midWait.placeAction).isEqualTo(PlaceActionUM.AwaitingQuote)
                assertThat(model.uiState.value.submit).isNotEqualTo(SubmitUM.AwaitingQuote)
                coVerify(exactly = 0) { signOrderInteractor(request = any()) }
            }

        @Test
        fun `GIVEN a dropped tap WHEN a new tap waits for the next amount THEN it stays pending`() = runOrderTest {
            // Arrange
            val model = createQuotingModel()
            model.onPlaceClick()
            model.onAmountChange(value = "25")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS)
            runCurrent()

            // Act
            model.onPlaceClick()
            runCurrent()

            // Assert
            assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.AwaitingQuote)
            assertThat(model.isSubmitting()).isTrue()
        }

        @Test
        fun `GIVEN a tap while quoting WHEN close clicked THEN the flow stays`() = runOrderTest {
            // Arrange
            val model = createQuotingModel()
            model.onPlaceClick()

            // Act
            model.onCloseClick()

            // Assert
            verify(exactly = 0) { router.pop(any()) }
        }

        @Test
        fun `GIVEN the balance does not cover the amount WHEN place clicked while quoting THEN nothing waits`() =
            runOrderTest {
                // Arrange
                statusFlow.value = activeStatus(balance = BigDecimal("5"))
                val model = createQuotingModel()

                // Act
                model.onPlaceClick()

                // Assert
                assertThat(model.uiState.value.placeAction).isEqualTo(PlaceActionUM.Unavailable)
                assertThat(model.uiState.value.submit).isNotEqualTo(SubmitUM.AwaitingQuote)
            }
    }

    @Nested
    inner class FreshQuote {

        @ParameterizedTest(name = "{0}")
        @EnumSource(UnplaceableResult::class)
        fun `GIVEN an unplaceable quote WHEN the next poll reloads it THEN the button stays disabled`(
            result: UnplaceableResult,
        ) = runOrderTest {
            // Arrange
            val model = createQuotingModel(result = result.toQuoteResult())
            advanceTimeBy(delayTimeMillis = QUOTE_MILLIS)
            runCurrent()
            val answeredQuote = model.uiState.value.quote

            // Act
            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS)
            runCurrent()

            // Assert
            assertThat(model.uiState.value.quote).isEqualTo(answeredQuote)
            assertThat(model.uiState.value.placeAction).isEqualTo(PlaceActionUM.Unavailable)
        }

        @Test
        fun `GIVEN a sell WHEN the whole position is prefilled THEN place is tappable while its first quote loads`() =
            runOrderTest {
                // Arrange
                coEvery { getQuoteUseCase(userWalletId = any(), request = any()) } coAnswers {
                    delay(timeMillis = QUOTE_MILLIS)
                    createSellQuote().right()
                }
                val model = createOrderModel(testScope = this, params = SELL_PARAMS)
                runCurrent()

                // Act
                model.onResume()
                runCurrent()

                // Assert
                assertThat(model.uiState.value.amountValue).isNotEmpty()
                assertThat(model.uiState.value.quote).isEqualTo(QuoteUM.Loading(isAnswerExpected = true))
                assertThat(model.uiState.value.placeAction).isEqualTo(PlaceActionUM.ReadyOnceQuoted)
            }

        @Test
        fun `GIVEN a first quote in flight WHEN the screen pauses THEN place stops being tappable at once`() =
            runOrderTest {
                // Arrange
                val model = createQuotingModel()

                // Act
                model.onPause()

                // Assert
                assertThat(model.uiState.value.quote).isEqualTo(QuoteUM.Loading(isAnswerExpected = false))
                assertThat(model.uiState.value.placeAction).isEqualTo(PlaceActionUM.Unavailable)
            }

        @Test
        fun `GIVEN an amount paused within the debounce WHEN the screen resumes THEN the reload is not tappable`() =
            runOrderTest {
                // Arrange
                coEvery { getQuoteUseCase(userWalletId = any(), request = any()) } coAnswers {
                    delay(timeMillis = QUOTE_MILLIS)
                    createQuote().right()
                }
                val model = createOrderModel(testScope = this)
                advanceUntilIdle()
                model.onResume()
                model.onAmountChange(value = "10")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS / 2)
                model.onPause()

                // Act
                model.onResume()
                runCurrent()

                // Assert
                assertThat(model.uiState.value.quote).isEqualTo(QuoteUM.Loading(isAnswerExpected = false))
                assertThat(model.uiState.value.placeAction).isEqualTo(PlaceActionUM.Unavailable)
                coVerify(exactly = 1) { getQuoteUseCase(userWalletId = any(), request = any()) }
            }

        @Test
        fun `GIVEN the first quote is cancelled by a pause WHEN the screen resumes THEN the reload is not tappable`() =
            runOrderTest {
                // Arrange
                val model = createQuotingModel()
                model.onPause()

                // Act
                model.onResume()
                runCurrent()

                // Assert
                assertThat(model.uiState.value.quote).isEqualTo(QuoteUM.Loading(isAnswerExpected = false))
                assertThat(model.uiState.value.placeAction).isEqualTo(PlaceActionUM.Unavailable)
            }

        @Test
        fun `GIVEN a failed quote WHEN the screen resumes and reloads it THEN the button stays disabled`() =
            runOrderTest {
                // Arrange
                val model = createQuotingModel(result = PredictionOrderQuoteError.Network.left())
                advanceTimeBy(delayTimeMillis = QUOTE_MILLIS)
                runCurrent()
                model.onPause()
                val failedQuote = model.uiState.value.quote

                // Act
                model.onResume()
                runCurrent()

                // Assert
                assertThat(model.uiState.value.quote).isEqualTo(failedQuote)
                assertThat(model.uiState.value.placeAction).isEqualTo(PlaceActionUM.Unavailable)
            }

        @Test
        fun `GIVEN a failed quote WHEN a new amount is requested THEN the button is enabled while it loads`() =
            runOrderTest {
                // Arrange
                val model = createQuotingModel(result = PredictionOrderQuoteError.Network.left())
                advanceTimeBy(delayTimeMillis = QUOTE_MILLIS)
                runCurrent()

                // Act
                model.onAmountChange(value = "25")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS)
                runCurrent()

                // Assert
                assertThat(model.uiState.value.quote).isEqualTo(QuoteUM.Loading(isAnswerExpected = true))
                assertThat(model.uiState.value.placeAction).isEqualTo(PlaceActionUM.ReadyOnceQuoted)
            }
    }

    enum class UnplaceableResult { NoLiquidity, QuoteError }

    enum class MarketLoad { Hanging, Failed }

    enum class MidWaitRead { Fails, StopsAcceptingOrders }

    private fun UnplaceableResult.toQuoteResult(): Either<PredictionOrderQuoteError, PredictionOrderQuote> =
        when (this) {
            UnplaceableResult.NoLiquidity -> createQuote(status = PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY).right()
            UnplaceableResult.QuoteError -> PredictionOrderQuoteError.Network.left()
        }

    private fun TestScope.createQuotingModel(
        quoteMillis: Long = QUOTE_MILLIS,
        result: Either<PredictionOrderQuoteError, PredictionOrderQuote> = createQuote().right(),
    ): PlacePredictionOrderModel {
        coEvery { getQuoteUseCase(userWalletId = any(), request = any()) } coAnswers {
            delay(timeMillis = quoteMillis)
            result
        }
        val model = createOrderModel(testScope = this)
        advanceUntilIdle()
        model.onResume()
        model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS)
        runCurrent()
        return model
    }

    private fun TestScope.createQuotedModel(
        params: PlacePredictionComponent.Params = BUY_PARAMS,
    ): PlacePredictionOrderModel {
        val model = createOrderModel(testScope = this, params = params)
        advanceUntilIdle()
        model.onResume()
        if (params is PlacePredictionComponent.Params.Buy) model.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
        return model
    }

    enum class FrozenInput(val apply: (PlacePredictionOrderModel) -> Unit) {
        AmountChange(apply = { it.onAmountChange(value = "25") }),
        Preset(apply = { it.onPresetClick(preset = it.amountPresets.first()) }),
        Max(apply = PlacePredictionOrderModel::onMaxClick),
        Slippage(apply = PlacePredictionOrderModel::onSlippageClick),
        AddFunds(apply = PlacePredictionOrderModel::onAddFundsClick),
        Swap(apply = PlacePredictionOrderModel::onSwapClick),
        Receive(apply = PlacePredictionOrderModel::onReceiveClick),
        PolymarketTerms(apply = PlacePredictionOrderModel::onPolymarketTermsClick),
        TangemTerms(apply = PlacePredictionOrderModel::onTangemTermsClick),
    }

    private companion object {
        val HELD = BigDecimal("51.234567")
        const val SIGNING_MILLIS = 1_000L
        const val QUOTE_MILLIS = 1_000L
        const val PENDING_PLACEMENT_MILLIS = 10_000L
        const val POSITION_READ_MILLIS = 300L
    }
}