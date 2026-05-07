package com.tangem.feature.swap.domain

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.account.status.usecase.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.quotes.QuotesRepository
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.swap.models.SwapPairModel
import com.tangem.domain.swap.usecase.GetSwapPairUseCase
import com.tangem.domain.tokens.GetAssetRequirementsUseCase
import com.tangem.domain.tokens.GetCurrencyCheckUseCase
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.transaction.usecase.*
import com.tangem.domain.transaction.usecase.gasless.CreateAndSendGaslessTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.EstimateFeeForGaslessTxUseCase
import com.tangem.domain.transaction.usecase.gasless.EstimateFeeForTokenUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForTokenUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.ui.AmountFormatter
import com.tangem.feature.swap.domain.models.ui.TxFee
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import java.math.BigDecimal

/**
 * Base class that wires all ~30 dependencies of [SwapInteractorImpl] as relaxed MockK mocks.
 * Extend this in every test class and override individual stubs in `@BeforeEach` or within tests.
 */
internal open class SwapInteractorImplTestBase {

    // region — mocked dependencies

    protected val repository: SwapRepository = mockk(relaxed = true)
    protected val allowPermissionsHandler: AllowPermissionsHandler = mockk(relaxed = true)
    private val cryptoCurrencyBalanceFetcher: CryptoCurrencyBalanceFetcher = mockk(relaxed = true)
    protected val sendTransactionUseCase: SendTransactionUseCase = mockk(relaxed = true)
    protected val createTransactionUseCase: CreateTransactionUseCase = mockk(relaxed = true)
    protected val createTransferTransactionUseCase: CreateTransferTransactionUseCase = mockk(relaxed = true)
    protected val createTransactionExtrasUseCase: CreateTransactionDataExtrasUseCase = mockk(relaxed = true)
    protected val isDemoCardUseCase: IsDemoCardUseCase = mockk(relaxed = true)
    protected val quotesRepository: QuotesRepository = mockk(relaxed = true)
    protected val multiQuoteStatusFetcher: MultiQuoteStatusFetcher = mockk(relaxed = true)
    protected val swapTransactionRepository: SwapTransactionRepository = mockk(relaxed = true)
    private val currencyChecksRepository: CurrencyChecksRepository = mockk(relaxed = true)
    private val appCurrencyRepository: AppCurrencyRepository = mockk(relaxed = true)
    protected val currenciesRepository: CurrenciesRepository = mockk(relaxed = true)
    protected val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier = mockk(relaxed = true)
    protected val validateTransactionUseCase: ValidateTransactionUseCase = mockk(relaxed = true)
    protected val estimateFeeUseCase: EstimateFeeUseCase = mockk(relaxed = true)
    protected val estimateFeeForTokenUseCase: EstimateFeeForTokenUseCase = mockk(relaxed = true)
    protected val estimateFeeForGaslessTxUseCase: EstimateFeeForGaslessTxUseCase = mockk(relaxed = true)
    private val getFeeForTokenUseCase: GetFeeForTokenUseCase = mockk(relaxed = true)
    protected val createAndSendGaslessTransactionUseCase: CreateAndSendGaslessTransactionUseCase =
        mockk(relaxed = true)
    protected val getFeeUseCase: GetFeeUseCase = mockk(relaxed = true)
    protected val getEthSpecificFeeUseCase: GetEthSpecificFeeUseCase = mockk(relaxed = true)
    protected val getCurrencyCheckUseCase: GetCurrencyCheckUseCase = mockk(relaxed = true)
    protected val getAssetRequirementsUseCase: GetAssetRequirementsUseCase = mockk(relaxed = true)
    protected val amountFormatter: AmountFormatter = mockk(relaxed = true)
    protected val rampStateManager: RampStateManager = mockk(relaxed = true)
    protected val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase =
        mockk(relaxed = true)
    protected val walletManagersFacade: WalletManagersFacade = mockk(relaxed = true)
    protected val getAllowanceInfoUseCase: GetAllowanceInfoUseCase = mockk(relaxed = true)
    protected val getSwapPairUseCase: GetSwapPairUseCase = mockk(relaxed = true)

    // endregion

    protected val sut: SwapInteractorImpl by lazy {
        SwapInteractorImpl(
            repository = repository,
            allowPermissionsHandler = allowPermissionsHandler,
            cryptoCurrencyBalanceFetcher = cryptoCurrencyBalanceFetcher,
            sendTransactionUseCase = sendTransactionUseCase,
            createTransactionUseCase = createTransactionUseCase,
            createTransferTransactionUseCase = createTransferTransactionUseCase,
            createTransactionExtrasUseCase = createTransactionExtrasUseCase,
            isDemoCardUseCase = isDemoCardUseCase,
            quotesRepository = quotesRepository,
            multiQuoteStatusFetcher = multiQuoteStatusFetcher,
            swapTransactionRepository = swapTransactionRepository,
            currencyChecksRepository = currencyChecksRepository,
            appCurrencyRepository = appCurrencyRepository,
            currenciesRepository = currenciesRepository,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            validateTransactionUseCase = validateTransactionUseCase,
            estimateFeeUseCase = estimateFeeUseCase,
            estimateFeeForTokenUseCase = estimateFeeForTokenUseCase,
            estimateFeeForGaslessTxUseCase = estimateFeeForGaslessTxUseCase,
            getFeeForTokenUseCase = getFeeForTokenUseCase,
            createAndSendGaslessTransactionUseCase = createAndSendGaslessTransactionUseCase,
            getFeeUseCase = getFeeUseCase,
            getEthSpecificFeeUseCase = getEthSpecificFeeUseCase,
            getCurrencyCheckUseCase = getCurrencyCheckUseCase,
            getAssetRequirementsUseCase = getAssetRequirementsUseCase,
            amountFormatter = amountFormatter,
            rampStateManager = rampStateManager,
            getFeePaidCryptoCurrencyStatusSyncUseCase = getFeePaidCryptoCurrencyStatusSyncUseCase,
            walletManagersFacade = walletManagersFacade,
            getAllowanceInfoUseCase = getAllowanceInfoUseCase,
            getSwapPairUseCase = getSwapPairUseCase,
        )
    }

    /**
     * Clears recorded calls and stubbed answers on all MockK mocks AND releases any
     * `mockkStatic` / `mockkObject` declarations between tests.
     *
     * - `clearAllMocks()` wipes recorded calls and stubbed answers; relaxed mocks remain relaxed
     *   (creation-time property). Each test must (re)stub any required behavior in its own
     *   `@BeforeEach` or test body.
     * - `unmockkAll()` releases static/object mocks set up inline by some tests
     *   (e.g. `mockkStatic(Base64::class)`, `mockkObject(SolanaTransactionHelper)`) so leaks
     *   do not propagate across tests within the same class.
     */
    @AfterEach
    open fun clearMocksAfterEachTest() {
        clearAllMocks()
        unmockkAll()
    }

    /**
     * Defensive shutdown hook — releases any remaining `mockkStatic` / `mockkObject` declarations
     * after the entire test class finishes, in case `@AfterEach` was bypassed (e.g. JVM shutdown
     * during a hard crash).
     *
     * Requires `@TestInstance(Lifecycle.PER_CLASS)` on every subclass — already the case across
     * all `SwapInteractorImpl*Test` classes.
     */
    @AfterAll
    open fun releaseStaticMocksAfterAllTests() {
        unmockkAll()
    }
}

// region — Test Builders

/**
 * Builds a [SwapCurrencyStatus] backed entirely by relaxed mocks.
 *
 * The [Network] mock is fully relaxed — [Network.rawId] is stubbed to return [networkRawId].
 * The extension function [com.tangem.blockchainsdk.utils.toBlockchain] is not stubbed here;
 * call-sites that need a specific Blockchain should use [io.mockk.mockkStatic] around the test.
 *
 * @param networkRawId raw network id — use `Blockchain.Ethereum.toNetworkId()` for EVM
 * @param contractAddress "0" for native coins, a real contract address for tokens
 * @param isCoin true to make the currency a [CryptoCurrency.Coin], false for [CryptoCurrency.Token]
 * @param amount token balance to expose via [CryptoCurrencyStatus.Value.amount]
 */
internal fun buildSwapCurrencyStatus(
    networkRawId: String = Blockchain.Ethereum.toNetworkId(),
    contractAddress: String = "0",
    isCoin: Boolean = true,
    amount: BigDecimal = BigDecimal("1"),
    decimals: Int = 18,
    userWalletId: UserWalletId = UserWalletId(stringValue = "deadbeef"),
    yieldSupplyActive: Boolean = false,
): SwapCurrencyStatus {
    val networkId = mockk<Network.ID>(relaxed = true) {
        every { rawId } returns Network.RawID(networkRawId)
    }
    val network = mockk<Network>(relaxed = true) {
        every { rawId } returns networkRawId
        every { id } returns networkId
        every { derivationPath } returns Network.DerivationPath.None
    }

    val currencyId = mockk<CryptoCurrency.ID>(relaxed = true) {
        every { rawCurrencyId } returns CryptoCurrency.RawID(contractAddress)
    }
    val currency: CryptoCurrency = if (isCoin) {
        mockk<CryptoCurrency.Coin>(relaxed = true) {
            every { this@mockk.network } returns network
            every { this@mockk.decimals } returns decimals
            every { this@mockk.id } returns currencyId
        }
    } else {
        mockk<CryptoCurrency.Token>(relaxed = true) {
            every { this@mockk.network } returns network
            every { this@mockk.decimals } returns decimals
            every { this@mockk.contractAddress } returns contractAddress
            every { this@mockk.id } returns currencyId
        }
    }

    val networkAddress = mockk<NetworkAddress>(relaxed = true) {
        every { defaultAddress } returns NetworkAddress.Address(
            value = "0xTestAddress",
            type = NetworkAddress.Address.Type.Primary,
        )
    }

    val maybeYield: YieldSupplyStatus? = if (yieldSupplyActive) {
        mockk<YieldSupplyStatus>(relaxed = true) {
            every { isActive } returns true
        }
    } else {
        null
    }

    val statusValue = mockk<CryptoCurrencyStatus.Loaded>(relaxed = true) {
        every { this@mockk.amount } returns amount
        every { this@mockk.networkAddress } returns networkAddress
        every { this@mockk.pendingTransactions } returns emptySet()
        every { this@mockk.yieldSupplyStatus } returns maybeYield
    }

    val cryptoCurrencyStatus = CryptoCurrencyStatus(
        currency = currency,
        value = statusValue,
    )

    val userWallet = mockk<UserWallet>(relaxed = true) {
        every { walletId } returns userWalletId
    }

    val account = mockk<Account>(relaxed = true) {
        every { accountId } returns mockk(relaxed = true)
    }

    return SwapCurrencyStatus(
        userWallet = userWallet,
        status = cryptoCurrencyStatus,
        account = account,
    )
}

/**
 * Builds a mocked [CryptoCurrency.Coin] with a stubbed network. Used where APIs require the concrete Coin subtype.
 */
internal fun buildCoinCurrency(
    networkRawId: String = Blockchain.Ethereum.toNetworkId(),
    decimals: Int = 18,
): CryptoCurrency.Coin {
    val networkId = mockk<Network.ID>(relaxed = true) {
        every { rawId } returns Network.RawID(networkRawId)
    }
    val network = mockk<Network>(relaxed = true) {
        every { rawId } returns networkRawId
        every { id } returns networkId
        every { derivationPath } returns Network.DerivationPath.None
    }
    val currencyId = mockk<CryptoCurrency.ID>(relaxed = true) {
        every { rawCurrencyId } returns CryptoCurrency.RawID("0")
    }
    return mockk<CryptoCurrency.Coin>(relaxed = true) {
        every { this@mockk.network } returns network
        every { this@mockk.decimals } returns decimals
        every { this@mockk.id } returns currencyId
    }
}

/**
 * Builds a [SwapProvider] for a given [ExchangeProviderType].
 */
internal fun buildSwapProvider(
    type: ExchangeProviderType = ExchangeProviderType.DEX,
    providerId: String = "test-provider-${type.name}",
): SwapProvider = SwapProvider(
    providerId = providerId,
    rateTypes = listOf(RateType.FLOAT),
    name = "TestProvider-${type.name}",
    type = type,
    imageLarge = "",
    termsOfUse = null,
    privacyPolicy = null,
    isRecommended = false,
    slippage = null,
    isExtraIdSupported = false,
)

/**
 * Builds a [TxFee.FeeComponent] wrapping a [Fee.Common] with the given fiat-equivalent amount.
 */
internal fun buildTxFee(
    feeValue: BigDecimal = BigDecimal("0.001"),
    selectedToken: CryptoCurrencyStatus? = null,
): TxFee.FeeComponent {
    val amount = mockk<Amount>(relaxed = true) {
        every { value } returns feeValue
    }
    val fee = mockk<Fee.Common>(relaxed = true) {
        every { this@mockk.amount } returns amount
    }
    return TxFee.FeeComponent(
        fee = fee,
        transactionFeeResult = TransactionFeeResult.Loaded(
            fee = mockk<TransactionFee.Single>(relaxed = true),
        ),
        selectedToken = selectedToken,
    )
}

/**
 * Builds a [TxFeeSealedState.Component] wrapping a [TxFee.FeeComponent].
 */
internal fun buildTxFeeSealedState(
    feeValue: BigDecimal = BigDecimal("0.001"),
    selectedToken: CryptoCurrencyStatus? = null,
): TxFeeSealedState = TxFeeSealedState.Component(
    txFee = buildTxFee(feeValue = feeValue, selectedToken = selectedToken),
)

/**
 * Builds a [SwapPairLeast] with matching from/to network+contract pairs.
 */
internal fun buildSwapPairLeast(
    fromNetwork: String = Blockchain.Ethereum.toNetworkId(),
    fromContract: String = "0",
    toNetwork: String = Blockchain.Bitcoin.toNetworkId(),
    toContract: String = "0",
    providers: List<SwapProvider> = listOf(buildSwapProvider()),
): SwapPairLeast = SwapPairLeast(
    from = LeastTokenInfo(contractAddress = fromContract, network = fromNetwork),
    to = LeastTokenInfo(contractAddress = toContract, network = toNetwork),
    providers = providers,
)

/**
 * Builds a [QuoteModel] with optional allowance contract.
 */
internal fun buildQuoteModel(
    toAmount: BigDecimal = BigDecimal("0.5"),
    decimals: Int = 18,
    allowanceContract: String? = null,
): QuoteModel = QuoteModel(
    toTokenAmount = SwapAmount(toAmount, decimals),
    allowanceContract = allowanceContract,
)

/**
 * Builds an [ExpressProvider] — used by [GetSwapPairUseCase] results.
 */
internal fun buildExpressProvider(
    providerId: String = "express-provider",
    type: ExpressProviderType = ExpressProviderType.DEX,
): ExpressProvider = ExpressProvider(
    providerId = providerId,
    rateTypes = listOf(ExpressRateType.Float),
    name = "ExpressProvider-${type.name}",
    type = type,
    imageLarge = "",
    termsOfUse = null,
    privacyPolicy = null,
    isRecommended = false,
    slippage = null,
    isExtraIdSupported = false,
)

/**
 * Builds a [SwapPairModel] — used as the result type of [GetSwapPairUseCase].
 */
internal fun buildSwapPairModel(
    fromNetworkRawId: String = Blockchain.Ethereum.toNetworkId(),
    fromContractAddress: String = "0",
    toNetworkRawId: String = Blockchain.Bitcoin.toNetworkId(),
    toContractAddress: String = "0",
    providers: List<ExpressProvider> = listOf(buildExpressProvider()),
): SwapPairModel {
    val fromCurrencyStatus = buildSwapCurrencyStatus(
        networkRawId = fromNetworkRawId,
        contractAddress = fromContractAddress,
    )
    val toCurrencyStatus = buildSwapCurrencyStatus(
        networkRawId = toNetworkRawId,
        contractAddress = toContractAddress,
    )
    return SwapPairModel(
        from = fromCurrencyStatus.status,
        to = toCurrencyStatus.status,
        providers = providers,
    )
}

// endregion