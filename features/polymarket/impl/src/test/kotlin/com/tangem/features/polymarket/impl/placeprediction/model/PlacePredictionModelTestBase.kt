package com.tangem.features.polymarket.impl.placeprediction.model

import arrow.core.right
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.interactor.GetPolymarketBalanceInteractor
import com.tangem.domain.polymarket.interactor.SignPredictionOrderInteractor
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.flow.PredictionFundingWindow
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketBalanceAllowance
import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.domain.polymarket.model.PredictionCollateral
import com.tangem.domain.polymarket.model.PredictionOrderFees
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionPlaceOrderResult
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.domain.polymarket.model.PredictionSignedOrder
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketCollateralCurrencyUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventUseCase
import com.tangem.domain.polymarket.usecase.GetPredictionOrderQuoteUseCase
import com.tangem.domain.polymarket.usecase.GetPredictionPriceUseCase
import com.tangem.domain.polymarket.usecase.AwaitDelayedPredictionOrderUseCase
import com.tangem.domain.polymarket.usecase.PlacePredictionOrderUseCase
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.positions.model.createPolymarketPosition
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.math.BigDecimal

import com.tangem.core.decompose.model.Model
import com.tangem.features.polymarket.impl.placeprediction.entity.PreparedPrediction

internal abstract class PlacePredictionModelTestBase {

    protected val router: Router = mockk(relaxed = true)
    protected val messageSender: UiMessageSender = mockk(relaxed = true)
    protected val urlOpener: UrlOpener = mockk(relaxed = true)
    protected val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase = mockk(relaxed = true)
    protected val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk(relaxed = true)
    protected val getEventUseCase: GetPolymarketEventUseCase = mockk()
    protected val deriveAddressesUseCase: DerivePolymarketAddressesUseCase = mockk()
    protected val getBalanceInteractor: GetPolymarketBalanceInteractor = mockk()
    protected val getQuoteUseCase: GetPredictionOrderQuoteUseCase = mockk()
    protected val getPriceUseCase: GetPredictionPriceUseCase = mockk()
    protected val checkGeoblockUseCase: CheckPolymarketGeoblockUseCase = mockk()
    protected val signOrderInteractor: SignPredictionOrderInteractor = mockk()
    protected val placeOrderUseCase: PlacePredictionOrderUseCase = mockk()
    protected val awaitDelayedOrderUseCase: AwaitDelayedPredictionOrderUseCase = mockk()
    protected val positionsRepository: PolymarketPositionsRepository = mockk()
    protected val fundingWindow: PredictionFundingWindow = mockk(relaxed = true)

    protected val models = mutableListOf<Model>()

    @BeforeEach
    fun setUp() {
        clearMocks(
            router,
            urlOpener,
            messageSender,
            getEventUseCase,
            deriveAddressesUseCase,
            getBalanceInteractor,
            getQuoteUseCase,
            getPriceUseCase,
            checkGeoblockUseCase,
            getWalletMetaInfoUseCase,
            sendFeedbackEmailUseCase,
            signOrderInteractor,
            placeOrderUseCase,
            awaitDelayedOrderUseCase,
            positionsRepository,
            fundingWindow,
        )

        coEvery { getEventUseCase(eventId = EVENT_ID) } returns createEvent().right()
        coEvery { deriveAddressesUseCase.stored(userWalletId = USER_WALLET_ID) } returns ADDRESSES
        coEvery { getBalanceInteractor(addresses = ADDRESSES) } returns
            PolymarketBalanceAllowance(balance = BigDecimal("100"), allowances = mapOf("0xspender" to BigDecimal("1000"))).right()
        coEvery { getQuoteUseCase(request = any()) } returns createQuote().right()
        coEvery { getPriceUseCase(assetId = any(), side = any()) } returns BigDecimal("0.48").right()
        coEvery { checkGeoblockUseCase() } returns false.right()
        coEvery { signOrderInteractor(request = any()) } returns SIGNED_ORDER.right()
        coEvery { placeOrderUseCase(any(), any(), any()) } returns createPlaceResult().right()
        coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns
            createPosition().right()
    }

    @AfterEach
    fun tearDown() {
        models.forEach(Model::onDestroy)
        models.clear()
    }

    protected fun createAmountModel(
        testScope: TestScope,
        params: PlacePredictionComponent.Params = PlacePredictionComponent.Params.Buy(
            userWalletId = USER_WALLET_ID,
            eventId = EVENT_ID,
            marketId = MARKET_ID,
            assetId = ASSET_ID,
        ),
        router: Router = this.router,
    ): PlacePredictionAmountModel = PlacePredictionAmountModel(
        paramsContainer = MutableParamsContainer(value = params),
        router = router,
        messageSender = messageSender,
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        getPolymarketEventUseCase = getEventUseCase,
        getPolymarketCollateralCurrencyUseCase = getCollateralCurrencyUseCase,
        derivePolymarketAddressesUseCase = deriveAddressesUseCase,
        getPolymarketBalanceInteractor = getBalanceInteractor,
        getPredictionOrderQuoteUseCase = getQuoteUseCase,
        getPredictionPriceUseCase = getPriceUseCase,
        checkPolymarketGeoblockUseCase = checkGeoblockUseCase,
        positionsRepository = positionsRepository,
    ).also(models::add)

    protected fun prepareOrder(amountModel: PlacePredictionAmountModel): PreparedPrediction {
        val route = slot<PlacePredictionRoute.Summary>()
        every { router.push(capture(route), any()) } returns Unit
        amountModel.onNextClick()
        amountModel.onPause()
        return route.captured.order
    }

    protected fun createSummaryModel(
        testScope: TestScope,
        amountModel: PlacePredictionAmountModel,
    ): PlacePredictionSummaryModel = createSummaryModel(testScope = testScope, order = prepareOrder(amountModel))

    protected fun createSummaryModel(
        testScope: TestScope,
        order: PreparedPrediction,
        router: Router = this.router,
    ): PlacePredictionSummaryModel =
        PlacePredictionSummaryModel(
            paramsContainer = MutableParamsContainer(value = order),
            router = router,
            urlOpener = urlOpener,
            getWalletMetaInfoUseCase = getWalletMetaInfoUseCase,
            sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
            messageSender = messageSender,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            derivePolymarketAddressesUseCase = deriveAddressesUseCase,
            getPolymarketBalanceInteractor = getBalanceInteractor,
            signPredictionOrderInteractor = signOrderInteractor,
            placePredictionOrderUseCase = placeOrderUseCase,
            awaitDelayedPredictionOrderUseCase = awaitDelayedOrderUseCase,
            positionsRepository = positionsRepository,
            predictionFundingWindow = fundingWindow,
        ).also(models::add)

    protected fun createStatusModel(
        testScope: TestScope,
        params: PlacePredictionRoute.Status,
        router: Router = this.router,
    ): PlacePredictionStatusModel = PlacePredictionStatusModel(
        paramsContainer = MutableParamsContainer(value = params),
        router = router,
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        derivePolymarketAddressesUseCase = deriveAddressesUseCase,
        positionsRepository = positionsRepository,
    ).also(models::add)

    protected val getCollateralCurrencyUseCase: GetPolymarketCollateralCurrencyUseCase = mockk {
        every { this@mockk.invoke() } returns createCollateralCurrency()
    }

    protected fun createCollateralCurrency(): CryptoCurrency.Token {
        val network = Network(
            id = Network.ID(value = NETWORK_RAW_ID, derivationPath = Network.DerivationPath.None),
            name = "Polygon",
            currencySymbol = "POL",
            derivationPath = Network.DerivationPath.None,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified(name = "Polygon"),
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )

        return CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(rawId = NETWORK_RAW_ID),
                suffix = CryptoCurrency.ID.Suffix.RawID(
                    rawId = PredictionCollateral.RAW_ID.value,
                    contractAddress = PolymarketContracts.COLLATERAL,
                ),
            ),
            network = network,
            name = PolymarketCollateralCurrencyFactory.COLLATERAL_NAME,
            symbol = PolymarketCollateralCurrencyFactory.COLLATERAL_SYMBOL,
            decimals = PolymarketContracts.COLLATERAL_DECIMALS,
            iconUrl = null,
            isCustom = false,
            contractAddress = PolymarketContracts.COLLATERAL,
        )
    }

    protected fun createQuote(
        total: BigDecimal = BigDecimal("10.4"),
        status: PredictionQuoteStatus = PredictionQuoteStatus.FULL,
        isLive: Boolean = false,
    ): PredictionOrderQuote = PredictionOrderQuote(
        status = status,
        side = PredictionOrderSide.BUY,
        shares = BigDecimal("23.2"),
        notional = BigDecimal("10"),
        expectedExecutionAmount = BigDecimal("23.8"),
        averagePrice = BigDecimal("0.42"),
        worstCasePrice = BigDecimal("0.43"),
        fees = PredictionOrderFees(
            market = BigDecimal("0.3"),
            builder = BigDecimal("0.1"),
            total = BigDecimal("0.4"),
        ),
        total = total,
        builderCode = "0xbuilder",
        minOrderSize = BigDecimal("5"),
        minOrderAmount = BigDecimal.ONE,
        tickSize = BigDecimal("0.001"),
        isLive = isLive,
    )

    protected fun createPosition(size: BigDecimal = BigDecimal("51.234567")): PolymarketPosition.Open =
        createPolymarketPosition(assetId = ASSET_ID).copy(
            id = POSITION_ID,
            marketId = MARKET_ID,
            size = size,
            isNegRisk = false,
        )

    protected fun createSellQuote(shares: BigDecimal = BigDecimal("12.8")): PredictionOrderQuote = createQuote().copy(
        side = PredictionOrderSide.SELL,
        shares = shares,
        notional = BigDecimal("10.88"),
        expectedExecutionAmount = BigDecimal("11.01"),
        total = BigDecimal("10.48"),
    )

    protected fun createEvent(isAcceptingOrders: Boolean = true, conditionId: String? = null): PolymarketEvent = PolymarketEvent(
        id = EVENT_ID,
        slug = "event-slug",
        title = "Event title",
        description = "Event description",
        rulesUrl = "https://polymarket.com/rules",
        iconUrl = null,
        imageUrl = null,
        status = PolymarketStatus.ACTIVE,
        startDate = null,
        endDate = null,
        volume = null,
        volume24h = null,
        liquidity = null,
        totalMarketsCount = 1,
        isNegRisk = false,
        displayMode = PolymarketDisplayMode.PLAIN_MARKETS,
        markets = listOf(
            PolymarketMarket(
                id = MARKET_ID,
                conditionId = conditionId,
                eventId = EVENT_ID,
                title = "Market question",
                slug = "market-slug",
                description = "Market description",
                groupItemTitle = null,
                iconUrl = null,
                imageUrl = null,
                status = PolymarketStatus.ACTIVE,
                isNegRisk = false,
                isAcceptingOrders = isAcceptingOrders,
                startDate = null,
                endDate = null,
                startDateIso = null,
                endDateIso = null,
                volume = null,
                volume24h = null,
                liquidity = null,
                orderIndex = 0,
                outcomes = listOf(
                    PolymarketOutcome(assetId = ASSET_ID, title = "Yes", probability = BigDecimal("0.42")),
                    PolymarketOutcome(assetId = "asset-2", title = "No", probability = BigDecimal("0.58")),
                ),
            ),
        ),
    )

    protected companion object {
        const val EVENT_ID = "event-1"
        const val MARKET_ID = "market-1"
        const val ASSET_ID = "asset-1"
        const val NETWORK_RAW_ID = "polygon-ecosystem-token"
        val USER_WALLET_ID = UserWalletId("011")
        const val CONDITION_ID = "condition-1"
        val POSITION_ID = PolymarketPositionId(conditionId = CONDITION_ID, assetId = ASSET_ID)
        val SELL_PARAMS = PlacePredictionComponent.Params.Sell(
            userWalletId = USER_WALLET_ID,
            positionId = POSITION_ID,
            marketId = MARKET_ID,
        )
        val ADDRESSES: PolymarketAddresses = mockk(relaxed = true)
        val SIGNED_ORDER: PredictionSignedOrder = mockk(relaxed = true)

        const val ORDER_ID = "0x6b2e"

        fun createPlaceResult(
            makingAmount: BigDecimal? = BigDecimal("10"),
            takingAmount: BigDecimal? = BigDecimal("23.2"),
            status: String? = "matched",
        ) = PredictionPlaceOrderResult(
            isSuccess = true,
            orderId = ORDER_ID,
            status = status,
            makingAmount = makingAmount,
            takingAmount = takingAmount,
            transactionHashes = listOf("0xdead"),
            errorMessage = null,
        )
    }
}