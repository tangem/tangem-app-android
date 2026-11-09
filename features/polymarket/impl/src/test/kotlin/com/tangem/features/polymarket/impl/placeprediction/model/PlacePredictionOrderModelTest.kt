package com.tangem.features.polymarket.impl.placeprediction.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.decompose.model.Model
import com.tangem.domain.models.StatusSource
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
import com.tangem.domain.polymarket.model.PolymarketEventError
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PredictionOrderFailure
import com.tangem.domain.polymarket.model.PredictionOrderFees
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderQuoteError
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionOrderStatus
import com.tangem.domain.polymarket.model.PredictionOrderState
import com.tangem.domain.polymarket.model.PredictionOrderSigningRoute
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.polymarket.model.PredictionPlaceOrderError
import com.tangem.domain.polymarket.model.PredictionPriceError
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.domain.polymarket.usecase.DelayedPredictionOrderOutcome
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.features.polymarket.impl.common.FakeSlotHost
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.entity.DEFAULT_SLIPPAGE_PERCENT
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceActionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceButtonStyleUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceResultUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionFeeSheetUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PriceErrorUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SellPositionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SharesProceedsUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SlippageUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.features.polymarket.impl.placeprediction.entity.canShowFeeBreakdown
import com.tangem.features.polymarket.impl.placeprediction.entity.enteredAmount
import com.tangem.features.polymarket.impl.placeprediction.entity.isReadyToPlace
import com.tangem.features.polymarket.impl.placeprediction.entity.isTradable
import com.tangem.features.polymarket.impl.placeprediction.model.converter.PredictionFeeSheetUMConverter
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.math.BigDecimal
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
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
            model.tapPlace()
            runCurrent()

            // Assert
            coVerify(exactly = 1) { signOrderInteractor(request = any()) }
            verify(exactly = 1) {
                router.replaceAll(match<PlacePredictionRoute.Status> { it.side == BUY_PARAMS.side }, onComplete = any())
            }
        }

        @Test
        fun `GIVEN a delayed order the book never settles WHEN place clicked THEN the still-processing result replaces the stack`() =
            runOrderTest {
                // Arrange
                coEvery { placeOrderUseCase(any(), any(), any()) } returns createPlaceResult(status = "delayed").right()
                coEvery { awaitDelayedOrderUseCase(addresses = ADDRESSES, orderId = ORDER_ID) } returns
                    DelayedPredictionOrderOutcome.Unconfirmed
                val model = createQuotedModel()

                // Act
                model.tapPlace()
                runCurrent()

                // Assert
                val submit = model.uiState.value.submit as SubmitUM.Result
                assertThat(submit.result).isEqualTo(PlaceResultUM.StillProcessing(placedAt = submit.result.placedAt))
                verify(exactly = 0) { messageSender.send(any()) }
                verify(exactly = 1) { router.replaceAll(any<PlacePredictionRoute.Status>(), onComplete = any()) }
            }

        @Test
        fun `GIVEN a delayed order matched with no shares WHEN close is tapped on the failure dialog THEN the order closes`() =
            runOrderTest {
                // Arrange
                coEvery { placeOrderUseCase(any(), any(), any()) } returns createPlaceResult(status = "delayed").right()
                coEvery { awaitDelayedOrderUseCase(addresses = ADDRESSES, orderId = ORDER_ID) } returns
                    DelayedPredictionOrderOutcome.Matched(
                        order = PredictionOrderState(
                            status = PredictionOrderStatus.Matched,
                            price = BigDecimal("0.5"),
                            originalSize = BigDecimal("23.2"),
                            sizeMatched = BigDecimal.ZERO,
                        ),
                    )
                val dialog = slot<DialogMessage>()
                every { messageSender.send(capture(dialog)) } returns Unit
                val model = createQuotedModel()
                model.tapPlace()
                runCurrent()

                // Act
                dialog.captured.secondAction?.onClick()

                // Assert
                assertThat(dialog.captured.isDismissable).isFalse()
                verify(exactly = 1) { router.pop(onComplete = any()) }
                verify(exactly = 0) { router.replaceAll(any<PlacePredictionRoute.Status>(), onComplete = any()) }
            }

        @Test
        fun `GIVEN a matched order filled no shares WHEN close is tapped on the failure dialog THEN the order closes`() =
            runOrderTest {
                // Arrange
                coEvery { placeOrderUseCase(any(), any(), any()) } returns
                    createPlaceResult(takingAmount = BigDecimal.ZERO).right()
                val dialog = slot<DialogMessage>()
                every { messageSender.send(capture(dialog)) } returns Unit
                val model = createQuotedModel()
                model.tapPlace()
                runCurrent()

                // Act
                dialog.captured.secondAction?.onClick()

                // Assert
                assertThat(dialog.captured.isDismissable).isFalse()
                verify(exactly = 1) { router.pop(onComplete = any()) }
                verify(exactly = 0) { router.replaceAll(any<PlacePredictionRoute.Status>(), onComplete = any()) }
            }

        @Test
        fun `GIVEN a matched order filled no shares WHEN support is tapped on the failure dialog THEN the order closes after the email`() =
            runOrderTest {
                // Arrange
                coEvery { placeOrderUseCase(any(), any(), any()) } returns
                    createPlaceResult(takingAmount = BigDecimal.ZERO).right()
                val dialog = slot<DialogMessage>()
                every { messageSender.send(capture(dialog)) } returns Unit
                coEvery { getWalletMetaInfoUseCase(userWalletId = USER_WALLET_ID) } returns mockk<WalletMetaInfo>().right()
                val model = createQuotedModel()
                model.tapPlace()
                runCurrent()

                // Act
                dialog.captured.firstAction.onClick()
                dialog.captured.onDismissRequest()
                runCurrent()

                // Assert
                coVerifyOrder {
                    sendFeedbackEmailUseCase(any())
                    router.pop(onComplete = any())
                }
                verify(exactly = 1) { router.pop(onComplete = any()) }
            }

        @Test
        fun `GIVEN no answer from the exchange WHEN the failure dialog is dismissed THEN the order stays open`() =
            runOrderTest {
                // Arrange
                coEvery { placeOrderUseCase(any(), any(), any()) } returns
                    PredictionPlaceOrderError.Ambiguous(detail = "timeout").left()
                val dialog = slot<DialogMessage>()
                every { messageSender.send(capture(dialog)) } returns Unit
                val model = createQuotedModel()
                model.tapPlace()
                runCurrent()

                // Act
                dialog.captured.onDismissRequest()

                // Assert
                verify(exactly = 0) { router.pop(onComplete = any()) }
                assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
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
                model.tapPlace()
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
            model.tapPlace()
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
            model.tapPlace()
            model.tapPlace()
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
            model.tapPlace()

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
            model.tapPlace()
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
                model.tapPlace()

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
            model.tapPlace()

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
                every { getUserWalletUseCase(any()) } returns hotWallet().right()

                // Act
                val model = createOrderModel(testScope = this)
                runCurrent()

                // Assert
                assertThat(model.uiState.value.placeButtonStyle).isEqualTo(PlaceButtonStyleUM.HOLD)
            }

        @Test
        fun `GIVEN a card wallet WHEN the order screen opens THEN the order is placed by tapping the button`() =
            runOrderTest {
                // Arrange
                every { getUserWalletUseCase(any()) } returns coldWallet().right()

                // Act
                val model = createOrderModel(testScope = this)
                runCurrent()

                // Assert
                assertThat(model.uiState.value.placeButtonStyle).isEqualTo(PlaceButtonStyleUM.CARD)
            }

        @Test
        fun `GIVEN the wallet is not in the list WHEN the order screen opens THEN the order is placed by holding`() =
            runOrderTest {
                // Arrange
                every { getUserWalletUseCase(any()) } returns GetUserWalletError.UserWalletNotFound.left()

                // Act
                val model = createOrderModel(testScope = this)
                runCurrent()

                // Assert
                assertThat(model.uiState.value.placeButtonStyle).isEqualTo(PlaceButtonStyleUM.HOLD)
            }
    }

    @Nested
    inner class SessionKey {

        @BeforeEach
        fun enableSessionKeys() {
            every { featureToggles.isPolymarketSessionKeysEnabled } returns true
        }

        @Test
        fun `GIVEN a card wallet with a usable session key WHEN the screen opens THEN the button is plain`() =
            runOrderTest {
                // Arrange
                coEvery { signingRouter.canSignWithSessionKey(userWalletId = USER_WALLET_ID) } returns true

                // Act
                val model = openOrderModel()

                // Assert
                assertThat(model.uiState.value.placeButtonStyle).isEqualTo(PlaceButtonStyleUM.PLAIN)
            }

        @Test
        fun `GIVEN a card wallet without a usable session key WHEN the screen opens THEN the card is asked`() =
            runOrderTest {
                // Arrange
                coEvery { signingRouter.canSignWithSessionKey(userWalletId = USER_WALLET_ID) } returns false

                // Act
                val model = openOrderModel()

                // Assert
                assertThat(model.uiState.value.placeButtonStyle).isEqualTo(PlaceButtonStyleUM.CARD)
            }

        @Test
        fun `GIVEN a card wallet WHEN the screen opens THEN the session key is checked once`() = runOrderTest {
            // Act
            openOrderModel()

            // Assert
            coVerify(exactly = 1) { signingRouter.canSignWithSessionKey(userWalletId = USER_WALLET_ID) }
        }

        @Test
        fun `GIVEN the session key becomes usable WHEN the screen resumes THEN the button turns plain`() =
            runOrderTest {
                // Arrange
                coEvery { signingRouter.canSignWithSessionKey(userWalletId = USER_WALLET_ID) } returnsMany
                    listOf(false, true)
                val model = openOrderModel()
                model.onPause()

                // Act
                model.onResume()
                runCurrent()

                // Assert
                assertThat(model.uiState.value.placeButtonStyle).isEqualTo(PlaceButtonStyleUM.PLAIN)
            }

        @Test
        fun `GIVEN the session key stops being usable WHEN the screen resumes THEN the card is asked`() =
            runOrderTest {
                // Arrange
                coEvery { signingRouter.canSignWithSessionKey(userWalletId = USER_WALLET_ID) } returnsMany
                    listOf(true, false)
                val model = openOrderModel()
                model.onPause()

                // Act
                model.onResume()
                runCurrent()

                // Assert
                assertThat(model.uiState.value.placeButtonStyle).isEqualTo(PlaceButtonStyleUM.CARD)
            }

        @Test
        fun `GIVEN an order being signed WHEN the session key check answers THEN the style waits for idle`() =
            runOrderTest {
                // Arrange
                coEvery { signingRouter.route(addresses = ADDRESSES) } returns PredictionOrderSigningRoute.Owner
                coEvery { signOrderInteractor(request = any()) } coAnswers {
                    delay(timeMillis = SIGNING_MILLIS)
                    PredictionOrderFailure.Signing(error = PolymarketSigningError.UserCancelled).left()
                }
                val model = createQuotedModel()
                runCurrent()
                model.tapPlace()
                coEvery { signingRouter.canSignWithSessionKey(userWalletId = USER_WALLET_ID) } returns true

                // Act
                model.onPause()
                model.onResume()
                runCurrent()
                val whileSigning = model.uiState.value
                advanceTimeBy(delayTimeMillis = SIGNING_MILLIS + 1)
                runCurrent()

                // Assert
                assertThat(whileSigning.submit).isEqualTo(SubmitUM.Signing)
                assertThat(whileSigning.placeButtonStyle).isEqualTo(PlaceButtonStyleUM.CARD)
                assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
                assertThat(model.uiState.value.placeButtonStyle).isEqualTo(PlaceButtonStyleUM.PLAIN)
            }

        @Test
        fun `GIVEN a hot wallet with a usable session key WHEN the screen opens THEN the button is plain`() =
            runOrderTest {
                // Arrange
                coEvery { signingRouter.canSignWithSessionKey(userWalletId = USER_WALLET_ID) } returns true
                every { getUserWalletUseCase(any()) } returns hotWallet().right()

                // Act
                val model = openOrderModel()

                // Assert
                assertThat(model.uiState.value.placeButtonStyle).isEqualTo(PlaceButtonStyleUM.PLAIN)
            }

        @Test
        fun `GIVEN a hot wallet without a usable session key WHEN the screen opens THEN the button is held`() =
            runOrderTest {
                // Arrange
                coEvery { signingRouter.canSignWithSessionKey(userWalletId = USER_WALLET_ID) } returns false
                every { getUserWalletUseCase(any()) } returns hotWallet().right()

                // Act
                val model = openOrderModel()

                // Assert
                assertThat(model.uiState.value.placeButtonStyle).isEqualTo(PlaceButtonStyleUM.HOLD)
            }

        @Test
        fun `GIVEN a hot wallet and session keys disabled WHEN the screen opens THEN it is held unchecked`() =
            runOrderTest {
                // Arrange
                every { featureToggles.isPolymarketSessionKeysEnabled } returns false
                coEvery { signingRouter.canSignWithSessionKey(userWalletId = USER_WALLET_ID) } returns true
                every { getUserWalletUseCase(any()) } returns hotWallet().right()

                // Act
                val model = openOrderModel()

                // Assert
                assertThat(model.uiState.value.placeButtonStyle).isEqualTo(PlaceButtonStyleUM.HOLD)
                coVerify(exactly = 0) { signingRouter.canSignWithSessionKey(userWalletId = any()) }
            }

        @Test
        fun `GIVEN the wallet is not found WHEN the screen opens THEN the session key is not checked`() =
            runOrderTest {
                // Arrange
                coEvery { signingRouter.canSignWithSessionKey(userWalletId = USER_WALLET_ID) } returns true
                every { getUserWalletUseCase(any()) } returns GetUserWalletError.UserWalletNotFound.left()

                // Act
                val model = openOrderModel()

                // Assert
                assertThat(model.uiState.value.placeButtonStyle).isEqualTo(PlaceButtonStyleUM.HOLD)
                coVerify(exactly = 0) { signingRouter.canSignWithSessionKey(userWalletId = any()) }
            }

        @Test
        fun `GIVEN a plain button WHEN the router picks the owner key THEN nothing is signed and owner confirmation is asked`() =
            runOrderTest {
                // Arrange
                coEvery { signingRouter.canSignWithSessionKey(userWalletId = USER_WALLET_ID) } returns true
                coEvery { signingRouter.route(addresses = ADDRESSES) } returns PredictionOrderSigningRoute.Owner
                val model = createQuotedModel()
                runCurrent()

                // Act
                model.tapPlace()
                runCurrent()

                // Assert
                coVerify(exactly = 0) { signOrderInteractor(request = any()) }
                verify(exactly = 1) { messageSender.send(any()) }
                assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
                assertThat(model.uiState.value.placeButtonStyle).isEqualTo(PlaceButtonStyleUM.CARD)
            }

        @Test
        fun `GIVEN the session key could not sign WHEN the screen resumes THEN a fresh check decides the style`() =
            runOrderTest {
                // Arrange
                coEvery { signingRouter.canSignWithSessionKey(userWalletId = USER_WALLET_ID) } returns true
                coEvery { signingRouter.route(addresses = ADDRESSES) } returns PredictionOrderSigningRoute.Owner
                val model = createQuotedModel()
                runCurrent()
                model.tapPlace()
                runCurrent()
                model.onPause()

                // Act
                model.onResume()
                runCurrent()

                // Assert
                assertThat(model.uiState.value.placeButtonStyle).isEqualTo(PlaceButtonStyleUM.PLAIN)
            }

        @Test
        fun `GIVEN a plain hot wallet button WHEN the exchange refuses the session key THEN the owner key never signs`() =
            runOrderTest {
                // Arrange
                every { getUserWalletUseCase(any()) } returns hotWallet().right()
                coEvery { signingRouter.canSignWithSessionKey(userWalletId = USER_WALLET_ID) } returns true
                coEvery { signingRouter.route(addresses = ADDRESSES) } returns SESSION_ROUTE
                coEvery { placeOrderUseCase(any(), any(), any(), SESSION_ROUTE) } returns
                    PredictionPlaceOrderError.Unauthorized(detail = "Unauthorized/Invalid api key").left()
                val model = createQuotedModel()
                runCurrent()

                // Act
                model.tapPlace()
                runCurrent()

                // Assert
                coVerify(exactly = 1) { signOrderInteractor(request = any()) }
                coVerify(exactly = 0) { signOrderInteractor(request = match { it.route == PredictionOrderSigningRoute.Owner }) }
                verify(exactly = 1) { messageSender.send(any()) }
                assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
                assertThat(model.uiState.value.placeButtonStyle).isEqualTo(PlaceButtonStyleUM.HOLD)
            }

        @Test
        fun `GIVEN a card button WHEN the router picks the owner key THEN the owner key signs`() =
            runOrderTest {
                // Arrange
                coEvery { signingRouter.canSignWithSessionKey(userWalletId = USER_WALLET_ID) } returns false
                coEvery { signingRouter.route(addresses = ADDRESSES) } returns PredictionOrderSigningRoute.Owner
                val model = createQuotedModel()
                runCurrent()

                // Act
                model.tapPlace()
                runCurrent()

                // Assert
                coVerify(exactly = 1) { signOrderInteractor(request = match { it.route == PredictionOrderSigningRoute.Owner }) }
            }

        @Test
        fun `GIVEN a plain tap WHEN the button already turned to the card THEN only the session key may sign`() =
            runOrderTest {
                // Arrange
                coEvery { signingRouter.canSignWithSessionKey(userWalletId = USER_WALLET_ID) } returns false
                coEvery { signingRouter.route(addresses = ADDRESSES) } returns PredictionOrderSigningRoute.Owner
                val model = createQuotedModel()
                runCurrent()

                // Act
                model.onPlaceClick(confirmedWith = PlaceButtonStyleUM.PLAIN)
                runCurrent()

                // Assert
                assertThat(model.uiState.value.placeButtonStyle).isEqualTo(PlaceButtonStyleUM.CARD)
                coVerify(exactly = 0) { signOrderInteractor(request = any()) }
                verify(exactly = 1) { messageSender.send(any()) }
            }

        @Test
        fun `GIVEN a card tap WHEN the button already turned plain THEN the owner key may sign`() = runOrderTest {
            // Arrange
            coEvery { signingRouter.canSignWithSessionKey(userWalletId = USER_WALLET_ID) } returns true
            coEvery { signingRouter.route(addresses = ADDRESSES) } returns PredictionOrderSigningRoute.Owner
            val model = createQuotedModel()
            runCurrent()

            // Act
            model.onPlaceClick(confirmedWith = PlaceButtonStyleUM.CARD)
            runCurrent()

            // Assert
            coVerify(exactly = 1) {
                signOrderInteractor(request = match { it.route == PredictionOrderSigningRoute.Owner })
            }
        }

        @Test
        fun `GIVEN session keys turned off WHEN a plain tap places THEN the owner key never signs`() = runOrderTest {
            // Arrange
            every { featureToggles.isPolymarketSessionKeysEnabled } returns false
            coEvery { signingRouter.route(addresses = ADDRESSES) } returns PredictionOrderSigningRoute.Owner
            val model = createQuotedModel()
            runCurrent()

            // Act
            model.onPlaceClick(confirmedWith = PlaceButtonStyleUM.PLAIN)
            runCurrent()

            // Assert
            coVerify(exactly = 0) { signingRouter.route(addresses = any()) }
            coVerify(exactly = 0) { signOrderInteractor(request = any()) }
            verify(exactly = 1) { messageSender.send(any()) }
            assertThat(model.uiState.value.placeButtonStyle).isEqualTo(PlaceButtonStyleUM.CARD)
        }

        @Test
        fun `GIVEN a cached plain style WHEN an owner placement fails THEN the session key is checked again`() =
            runOrderTest {
                // Arrange
                coEvery { signingRouter.canSignWithSessionKey(userWalletId = USER_WALLET_ID) } returnsMany
                    listOf(true, false)
                coEvery { signingRouter.route(addresses = ADDRESSES) } returns PredictionOrderSigningRoute.Owner
                coEvery { signOrderInteractor(request = any()) } returns
                    PredictionOrderFailure.Signing(error = PolymarketSigningError.UserCancelled).left()
                val model = createQuotedModel()
                runCurrent()

                // Act
                model.onPlaceClick(confirmedWith = PlaceButtonStyleUM.CARD)
                runCurrent()

                // Assert
                coVerify(exactly = 2) { signingRouter.canSignWithSessionKey(userWalletId = USER_WALLET_ID) }
                assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
                assertThat(model.uiState.value.placeButtonStyle).isEqualTo(PlaceButtonStyleUM.CARD)
            }

        @Test
        fun `GIVEN session keys disabled WHEN the screen opens and resumes THEN the session key is not checked`() =
            runOrderTest {
                // Arrange
                every { featureToggles.isPolymarketSessionKeysEnabled } returns false
                coEvery { signingRouter.canSignWithSessionKey(userWalletId = USER_WALLET_ID) } returns true
                val model = openOrderModel()
                model.onPause()

                // Act
                model.onResume()
                runCurrent()

                // Assert
                assertThat(model.uiState.value.placeButtonStyle).isEqualTo(PlaceButtonStyleUM.CARD)
                coVerify(exactly = 0) { signingRouter.canSignWithSessionKey(userWalletId = any()) }
            }

        private fun TestScope.openOrderModel(): PlacePredictionOrderModel {
            val model = createOrderModel(testScope = this)
            runCurrent()
            model.onResume()
            runCurrent()
            return model
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
            model.tapPlace()
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
            model.tapPlace()
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
            model.tapPlace()
            runCurrent()

            // Assert
            assertThat(model.uiState.value.holdResetKey).isEqualTo(before)
        }

        @Test
        fun `GIVEN a quote on the way WHEN place clicked THEN the hold button is reset`() = runOrderTest {
            // Arrange
            val model = createQuotingModel()
            val before = model.uiState.value.holdResetKey

            // Act
            model.tapPlace()
            runCurrent()

            // Assert
            assertThat(model.uiState.value.holdResetKey).isEqualTo(before + 1)
        }

        @Test
        fun `GIVEN the submitter refuses the order WHEN place clicked THEN the hold button is reset`() = runOrderTest {
            // Arrange
            val refusingSubmitter = mockk<PlacePredictionOrderSubmitter> {
                coEvery { submit(prepared = any(), confirmedWith = any(), onKeyLost = any()) } returns null
                every { isSubmitting() } returns false
            }
            val model = createOrderModel(
                testScope = this,
                orderSubmitterFactory = mockk { every { create(any(), any(), any(), any()) } returns refusingSubmitter },
            )
            advanceUntilIdle()
            model.onResume()
            model.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val before = model.uiState.value.holdResetKey

            // Act
            model.tapPlace()
            runCurrent()

            // Assert
            coVerify(exactly = 1) { refusingSubmitter.submit(prepared = any(), confirmedWith = any(), onKeyLost = any()) }
            assertThat(model.uiState.value.holdResetKey).isEqualTo(before + 1)
        }

        @Test
        fun `GIVEN signing is in flight WHEN place clicked again THEN the hold button is left as it is`() =
            runOrderTest {
                // Arrange
                coEvery { signOrderInteractor(request = any()) } coAnswers {
                    delay(timeMillis = SIGNING_MILLIS)
                    SIGNED_ORDER.right()
                }
                val model = createQuotedModel()
                model.tapPlace()
                runCurrent()
                val before = model.uiState.value.holdResetKey

                // Act
                model.tapPlace()
                runCurrent()

                // Assert
                assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Signing)
                assertThat(model.uiState.value.holdResetKey).isEqualTo(before)
            }

        @Test
        fun `GIVEN posting is in flight WHEN place clicked again THEN the hold button is left as it is`() =
            runOrderTest {
                // Arrange
                coEvery { placeOrderUseCase(any(), any(), any()) } coAnswers {
                    delay(timeMillis = SIGNING_MILLIS)
                    createPlaceResult().right()
                }
                val model = createQuotedModel()
                model.tapPlace()
                runCurrent()
                val before = model.uiState.value.holdResetKey

                // Act
                model.tapPlace()
                runCurrent()

                // Assert
                assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Posting)
                assertThat(model.uiState.value.holdResetKey).isEqualTo(before)
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
                model.tapPlace()
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
                    isPositionClosed = true,
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
        fun `GIVEN a steep fee quote WHEN max clicked again before the next quote returns THEN its fee rate is not reused`() =
            runOrderTest {
                // Arrange
                coEvery { getQuoteUseCase(userWalletId = any(), request = any()) } returns steepFeeQuote().right()
                val model = createQuotedModel()
                model.onMaxClick()
                val afterFirstClick = model.uiState.value.amountValue

                // Act
                model.onMaxClick()

                // Assert
                assertThat(afterFirstClick).isEqualTo("83.33")
                assertThat(model.uiState.value.amountValue).isEqualTo("90")
            }

        @Test
        fun `GIVEN a steep fee quote followed by a failed one WHEN max clicked THEN the old fee rate is not reused`() =
            runOrderTest {
                // Arrange
                coEvery { getQuoteUseCase(userWalletId = any(), request = any()) } returnsMany listOf(
                    steepFeeQuote().right(),
                    PredictionOrderQuoteError.Network.left(),
                )
                val model = createQuotedModel()
                model.onAmountChange(value = "20")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
                val quoteBeforeClick = model.uiState.value.quote

                // Act
                model.onMaxClick()

                // Assert
                assertThat(quoteBeforeClick).isInstanceOf(QuoteUM.Error::class.java)
                assertThat(model.uiState.value.amountValue).isEqualTo("90")
            }

        private fun steepFeeQuote(): PredictionOrderQuote = createQuote().copy(
            notional = BigDecimal(100),
            fees = PredictionOrderFees(market = BigDecimal(20), builder = BigDecimal.ZERO, total = BigDecimal(20)),
        )

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

    @Nested
    inner class UntradableMarket {

        @Test
        fun `GIVEN the market takes no orders WHEN an amount is entered THEN it is quoted and the screen cannot trade`() =
            runOrderTest {
                // Arrange
                coEvery { getEventUseCase(any(), eventId = any()) } returns createEvent(isAcceptingOrders = false).right()
                val model = createOrderModel(testScope = this)
                advanceUntilIdle()
                model.onResume()

                // Act
                model.onAmountChange(value = "10")
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

                // Assert
                coVerify(exactly = 1) { getQuoteUseCase(any(), request = any()) }
                assertThat(model.uiState.value.isTradable()).isFalse()
                assertThat(model.uiState.value.isReadyToPlace).isFalse()
            }

        @ParameterizedTest
        @CsvSource("true, MarketClosed", "false, Unknown")
        fun `GIVEN the price cannot be read before an amount WHEN polled THEN the screen shows why and stops polling`(
            isMarketClosed: Boolean,
            expected: PriceErrorUM,
        ) = runOrderTest {
            // Arrange
            val error = if (isMarketClosed) PredictionPriceError.MarketClosed else PredictionPriceError.InvalidAsset
            coEvery { getPriceUseCase(assetId = any(), side = any()) } returns error.left()
            val model = createOrderModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onResume()
            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 3)

            // Assert
            assertThat(model.uiState.value.priceError).isEqualTo(expected)
            coVerify(exactly = 1) { getPriceUseCase(assetId = any(), side = any()) }
        }

        @Test
        fun `GIVEN the untradable sheet WHEN it is dismissed twice THEN the flow is left once`() = runOrderTest {
            // Arrange
            val model = createOrderModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onUntradableSheetDismiss()
            model.onUntradableSheetDismiss()

            // Assert
            verify(exactly = 1) { router.pop(any()) }
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
    inner class QuoteLock {

        @Test
        fun `GIVEN an amount within the debounce WHEN place clicked THEN nothing is signed once its quote arrives`() =
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
                val beforeTap = model.uiState.value.placeAction

                // Act
                model.tapPlace()
                advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + QUOTE_MILLIS)
                runCurrent()

                // Assert
                assertThat(beforeTap).isEqualTo(PlaceActionUM.Unavailable)
                assertThat(model.uiState.value.placeAction).isEqualTo(PlaceActionUM.Ready)
                assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
                coVerify(exactly = 0) { signOrderInteractor(request = any()) }
            }

        @Test
        fun `GIVEN the first quote in flight WHEN place clicked THEN nothing is signed once it arrives`() =
            runOrderTest {
                // Arrange
                val model = createQuotingModel()
                val beforeTap = model.uiState.value

                // Act
                model.tapPlace()
                advanceTimeBy(delayTimeMillis = QUOTE_MILLIS)
                runCurrent()

                // Assert
                assertThat(beforeTap.quote).isEqualTo(QuoteUM.Loading)
                assertThat(beforeTap.placeAction).isEqualTo(PlaceActionUM.Unavailable)
                assertThat(model.uiState.value.placeAction).isEqualTo(PlaceActionUM.Ready)
                assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
                coVerify(exactly = 0) { signOrderInteractor(request = any()) }
            }

        @Test
        fun `GIVEN a priced quote WHEN the next poll is in flight THEN place stays tappable`() = runOrderTest {
            // Arrange
            val model = createQuotingModel()
            advanceTimeBy(delayTimeMillis = QUOTE_MILLIS)
            runCurrent()

            // Act
            advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS + QUOTE_MILLIS / 2)
            runCurrent()

            // Assert
            coVerify(exactly = 2) { getQuoteUseCase(userWalletId = any(), request = any()) }
            assertThat(model.uiState.value.placeAction).isEqualTo(PlaceActionUM.Ready)
        }

        @Test
        fun `GIVEN the first quote in flight WHEN place then close clicked THEN the flow is left`() = runOrderTest {
            // Arrange
            val model = createQuotingModel()
            model.tapPlace()

            // Act
            model.onCloseClick()

            // Assert
            assertThat(model.isSubmitting()).isFalse()
            verify(exactly = 1) { router.pop(any()) }
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
                model.tapPlace()

                // Assert
                assertThat(model.uiState.value.quote).isEqualTo(QuoteUM.Loading)
                assertThat(model.uiState.value.placeAction).isEqualTo(PlaceActionUM.Unavailable)
                coVerify(exactly = 0) { signOrderInteractor(request = any()) }
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
        fun `GIVEN a sell WHEN the whole position is prefilled THEN place is not tappable while its first quote loads`() =
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
                assertThat(model.uiState.value.quote).isEqualTo(QuoteUM.Loading)
                assertThat(model.uiState.value.placeAction).isEqualTo(PlaceActionUM.Unavailable)
            }

        @Test
        fun `GIVEN a first quote in flight WHEN the screen pauses THEN place stops being tappable at once`() =
            runOrderTest {
                // Arrange
                val model = createQuotingModel()

                // Act
                model.onPause()

                // Assert
                assertThat(model.uiState.value.quote).isEqualTo(QuoteUM.Loading)
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
                assertThat(model.uiState.value.quote).isEqualTo(QuoteUM.Loading)
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
                assertThat(model.uiState.value.quote).isEqualTo(QuoteUM.Loading)
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
        fun `GIVEN a failed quote WHEN a new amount is requested THEN the button stays disabled while it loads`() =
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
                assertThat(model.uiState.value.quote).isEqualTo(QuoteUM.Loading)
                assertThat(model.uiState.value.placeAction).isEqualTo(PlaceActionUM.Unavailable)
            }
    }

    @Nested
    inner class StaleQuote {

        @Test
        fun `GIVEN a quote received 30 s ago WHEN place clicked THEN the order is signed`() = runOrderTest {
            // Arrange
            val model = createQuotedModel()
            nowMillis += MAX_QUOTE_AGE_MILLIS

            // Act
            model.tapPlace()
            runCurrent()

            // Assert
            coVerify(exactly = 1) { signOrderInteractor(request = any()) }
            verify(exactly = 0) { messageSender.send(any()) }
        }

        @Test
        fun `GIVEN a quote older than 30 s WHEN place clicked THEN it is refused and a new quote is requested`() =
            runOrderTest {
                // Arrange
                val model = createQuotingModel()
                advanceTimeBy(delayTimeMillis = QUOTE_MILLIS)
                runCurrent()
                nowMillis += MAX_QUOTE_AGE_MILLIS + 1
                val dialog = slot<DialogMessage>()
                every { messageSender.send(capture(dialog)) } returns Unit

                // Act
                model.tapPlace()
                runCurrent()

                // Assert
                coVerify(exactly = 0) { signOrderInteractor(request = any()) }
                assertThat(dialog.captured.title).isNull()
                assertThat(dialog.captured.message)
                    .isEqualTo(resourceReference(R.string.prediction_place_price_changed_description))
                assertThat(dialog.captured.secondAction).isNull()
                assertThat(model.uiState.value.submit).isEqualTo(SubmitUM.Idle)
                assertThat(model.uiState.value.quote).isEqualTo(QuoteUM.Loading)
                assertThat(model.uiState.value.placeAction).isEqualTo(PlaceActionUM.Unavailable)
                coVerify(exactly = 2) { getQuoteUseCase(userWalletId = any(), request = any()) }
            }

        @Test
        fun `GIVEN the clock moved back before the quote time WHEN place clicked THEN the quote is refused`() =
            runOrderTest {
                // Arrange
                val model = createQuotedModel()
                nowMillis -= 1

                // Act
                model.tapPlace()
                runCurrent()

                // Assert
                coVerify(exactly = 0) { signOrderInteractor(request = any()) }
                verify(exactly = 1) { messageSender.send(any()) }
            }

        @Test
        fun `GIVEN a refused stale quote WHEN the new quote arrives and place clicked THEN the new one is signed`() =
            runOrderTest {
                // Arrange
                val model = createQuotingModel()
                advanceTimeBy(delayTimeMillis = QUOTE_MILLIS)
                runCurrent()
                nowMillis += MAX_QUOTE_AGE_MILLIS + 1
                model.tapPlace()
                runCurrent()
                advanceTimeBy(delayTimeMillis = QUOTE_MILLIS)
                runCurrent()

                // Act
                model.tapPlace()
                runCurrent()

                // Assert
                coVerify(exactly = 1) { signOrderInteractor(request = any()) }
                verify(exactly = 1) { messageSender.send(any()) }
            }
    }

    enum class UnplaceableResult { NoLiquidity, QuoteError }

    enum class MarketLoad { Hanging, Failed }

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
        const val MAX_QUOTE_AGE_MILLIS = 30_000L
    }
}