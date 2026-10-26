package com.tangem.feature.swap.domain

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.TransferERC20TokenCallData
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.transaction.models.AllowanceInfo
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.feature.swap.domain.models.domain.SwapBalanceStatus
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.test.core.ProvideTestModels
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Tests for `loadDexSwapDataNoFee` — the replacement for the legacy `loadDexSwapData`.
 *
 * Verifies:
 *  - `dexSwapFeeCalculator.calculate` is NEVER called during quote loading (fee is owned by
 *    the fee selector now).
 *  - The returned `preparedSwapConfigState.balanceStatus` is [SwapBalanceStatus.Pending].
 *  - `swapDataModel` is populated from the Express response so `applySwapFee` (and
 *    `FeeSelectorRepository.loadFeeExtended`) can consume it later.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplLoadDexSwapDataNoFeeTest : SwapInteractorImplTestBase() {

    private val ethNetwork = Blockchain.Ethereum.toNetworkId()

    @BeforeEach
    fun setup() {
        coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
        coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal("10")
        coEvery {
            getCurrencyCheckUseCase.invoke(
                userWalletId = any(),
                currencyStatus = any(),
                feeCurrencyStatus = any(),
                amount = any(),
                fee = any(),
                feeCurrencyBalanceAfterTransaction = any(),
                recipientAddress = any(),
            )
        } returns CryptoCurrencyCheck(
            dustValue = null,
            reserveAmount = null,
            minimumSendAmount = null,
            existentialDeposit = null,
            utxoAmountLimit = null,
            isAccountFunded = true,
            rentWarning = null,
            isMemoRequired = false,
        )
        coEvery {
            validateTransactionUseCase.invoke(
                amount = any(),
                fee = any(),
                memo = any(),
                destination = any(),
                userWalletId = any(),
                network = any(),
            )
        } returns Unit.right()
        coEvery { quotesRepository.getMultiQuoteSyncOrNull(any()) } returns emptySet()
        coEvery { multiQuoteStatusFetcher.invoke(any()) } returns Unit.right()
        coEvery { getFeePaidCryptoCurrencyStatusSyncUseCase.invoke(any(), any()) } returns null.right()
        coEvery { currenciesRepository.createCoinCurrency(any()) } returns buildCoinCurrency()
        coEvery {
            getAllowanceInfoUseCase.invoke(
                userWalletId = any(),
                cryptoCurrency = any(),
                spenderAddress = any(),
                requiredAmount = any(),
            )
        } returns (AllowanceInfo.Enough(allowance = BigDecimal("100")) as AllowanceInfo).right()
    }

    @Test
    fun `DEX findBestQuote returns QuotesLoadedState without invoking DexSwapFeeCalculator`() = runTest {
        val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
        val from = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true, amount = BigDecimal("10"))
        val to = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        val quoteModel = buildQuoteModel(allowanceContract = null)
        val swapDataModel = SwapDataModel(
            toTokenAmount = SwapAmount(BigDecimal("0.5"), 18),
            transaction = ExpressTransactionModel.DEX(
                fromAmount = SwapAmount(BigDecimal("1.0"), 18),
                toAmount = SwapAmount(BigDecimal("0.5"), 18),
                txValue = "1000000000000000000",
                txId = "tx-id",
                txTo = "0xToAddress",
                txExtraId = null,
                txFrom = "0xFromAddress",
                txData = "0xdata",
                otherNativeFeeWei = null,
                gas = BigInteger.valueOf(21_000L),
                allowanceContract = null,
            ),
        )

        coEvery {
            repository.findBestQuote(
                userWallet = any(),
                fromContractAddress = any(),
                fromNetwork = any(),
                toContractAddress = any(),
                toNetwork = any(),
                fromAmount = any(),
                fromDecimals = any(),
                toDecimals = any(),
                providerId = dexProvider.providerId,
                rateType = any(),
            )
        } returns quoteModel.right()

        coEvery {
            repository.getExchangeData(
                userWallet = any(),
                fromContractAddress = any(),
                fromNetwork = any(),
                toContractAddress = any(),
                fromAddress = any(),
                toNetwork = any(),
                fromAmount = any(),
                fromDecimals = any(),
                toDecimals = any(),
                providerId = dexProvider.providerId,
                rateType = any(),
                toAddress = any(),
                expressOperationType = any(),
                refundAddress = any(),
            )
        } returns swapDataModel.right()

        val result = sut.findBestQuote(
            fromSwapCurrencyStatus = from,
            toSwapCurrencyStatus = to,
            providers = listOf(dexProvider),
            amountToSwap = "1.0",
            reduceBalanceBy = BigDecimal.ZERO,
        )

        assertThat(result).hasSize(1)
        val state = result[dexProvider]
        assertThat(state).isInstanceOf(SwapState.QuotesLoadedState::class.java)
        val quotesState = state as SwapState.QuotesLoadedState
        // Fee not computed yet — balanceStatus is Pending until applySwapFee patches the state.
        assertThat(quotesState.preparedSwapConfigState.balanceStatus)
            .isInstanceOf(SwapBalanceStatus.Pending::class.java)
        // swapDataModel is propagated so the fee selector can later call loadSwapFee with it.
        assertThat(quotesState.swapDataModel).isEqualTo(swapDataModel)
        // Fee calculator must not be invoked during quote loading.
        coVerify(exactly = 0) { dexSwapFeeCalculator.calculate(any(), any(), any()) }
    }

    // region permission-state selection on AllowanceInfo.NotEnough

    @Test
    fun `GIVEN NotEnough allowance AND integrated active THEN permissionState is PermissionSettings`() = runTest {
        stubAllowance(AllowanceInfo.NotEnough(allowance = BigDecimal.ZERO, requiredAmount = BigDecimal.ONE))

        val state = runFindBestQuoteForToken()

        val permission = state.permissionState
        assertThat(permission).isInstanceOf(PermissionDataState.PermissionSettings::class.java)
        assertThat((permission as PermissionDataState.PermissionSettings).spenderAddress).isEqualTo(SPENDER)
    }

    @Test
    fun `GIVEN Enough allowance THEN permissionState is Empty`() = runTest {
        stubAllowance(AllowanceInfo.Enough(allowance = BigDecimal("100")))

        val state = runFindBestQuoteForToken()

        assertThat(state.permissionState).isEqualTo(PermissionDataState.Empty)
    }

    @Test
    fun `GIVEN yield swap AND NotEnough allowance AND integrated active THEN permissionState is not integrated`() =
        runTest {
            // [REDACTED_TASK_KEY] / iOS parity: yield swaps must never use the integrated approve+swap path.
            // The yield-module proxy allowance is granted at enrollment, so no in-flow approval is shown.
            coEvery { yieldModuleAddressProvider.getOrFetch(any(), any()) } returns YIELD_PROXY
            coEvery { walletManagersFacade.isSwapSpenderAllowed(any(), any(), any()) } returns true
            stubAllowance(AllowanceInfo.NotEnough(allowance = BigDecimal.ZERO, requiredAmount = BigDecimal.ONE))

            val dexProvider = stubDexQuoteAndExchangeData()
            val result = invokeFindBestQuote(dexProvider, yieldSupplyActive = true)
            val state = result[dexProvider] as SwapState.QuotesLoadedState

            assertThat(state.permissionState)
                .isNotInstanceOf(PermissionDataState.PermissionSettings::class.java)
            assertThat(state.permissionState).isEqualTo(PermissionDataState.Empty)
        }

    // endregion

    // region from / refund address

    @Test
    fun `GIVEN yield swap WHEN findBestQuote THEN exchange data is requested for the module with a wallet refund`() =
        runTest {
            // Arrange
            coEvery { yieldModuleAddressProvider.getOrFetch(any(), any()) } returns YIELD_PROXY
            coEvery { walletManagersFacade.isSwapSpenderAllowed(any(), any(), any()) } returns true
            val dexProvider = stubDexQuoteAndExchangeData()

            // Act
            invokeFindBestQuote(dexProvider, yieldSupplyActive = true)

            // Assert
            verifyExchangeDataRequested(fromAddress = YIELD_PROXY, refundAddress = WALLET_ADDRESS)
        }

    @Test
    fun `GIVEN non-yield swap WHEN findBestQuote THEN both from and refund address are the wallet address`() = runTest {
        // Arrange
        val dexProvider = stubDexQuoteAndExchangeData()

        // Act
        invokeFindBestQuote(dexProvider)

        // Assert
        verifyExchangeDataRequested(fromAddress = WALLET_ADDRESS, refundAddress = WALLET_ADDRESS)
    }

    @Test
    fun `GIVEN yield swap AND no module address WHEN findBestQuote THEN wallet address is used as from address`() =
        runTest {
            // Arrange
            coEvery { yieldModuleAddressProvider.getOrFetch(any(), any()) } returns null
            coEvery { walletManagersFacade.isSwapSpenderAllowed(any(), any(), any()) } returns true
            val dexProvider = stubDexQuoteAndExchangeData()

            // Act
            invokeFindBestQuote(dexProvider, yieldSupplyActive = true)

            // Assert
            verifyExchangeDataRequested(fromAddress = WALLET_ADDRESS, refundAddress = WALLET_ADDRESS)
        }

    private fun verifyExchangeDataRequested(fromAddress: String, refundAddress: String) {
        coVerify(exactly = 1) {
            repository.getExchangeData(
                userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                toContractAddress = any(), fromAddress = fromAddress, toNetwork = any(),
                fromAmount = any(), fromDecimals = any(), toDecimals = any(),
                providerId = any(), rateType = any(), toAddress = any(),
                expressOperationType = any(), refundAddress = refundAddress,
            )
        }
    }

    // endregion

    // region MCP intent

    @Test
    fun `GIVEN intent WHEN findBestQuote with DEX provider THEN exchange data is requested for the intent`() = runTest {
        // Arrange
        val dexProvider = stubDexQuoteAndExchangeData()

        // Act
        invokeFindBestQuote(dexProvider, intentId = INTENT_ID)

        // Assert
        verifyExchangeDataRequestedForIntent(INTENT_ID)
    }

    @Test
    fun `GIVEN no intent WHEN findBestQuote with DEX provider THEN exchange data is requested without an intent`() =
        runTest {
            // Arrange
            val dexProvider = stubDexQuoteAndExchangeData()

            // Act
            invokeFindBestQuote(dexProvider)

            // Assert
            verifyExchangeDataRequestedForIntent(null)
        }

    private fun verifyExchangeDataRequestedForIntent(intentId: String?) {
        coVerify(exactly = 1) {
            repository.getExchangeData(
                userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                toContractAddress = any(), fromAddress = any(), toNetwork = any(),
                fromAmount = any(), fromDecimals = any(), toDecimals = any(),
                providerId = any(), rateType = any(), toAddress = any(),
                expressOperationType = any(), refundAddress = any(), intentId = intentId,
            )
        }
    }

    // endregion

    // region yield swap registry gate

    @Test
    fun `GIVEN router quote AND transfer payload WHEN findBestQuote THEN provider kept without a registry check`() =
        runTest {
            // Arrange
            stubYieldSwapWithTransferDetection()
            val dexProvider = stubDexQuoteAndExchangeData(
                quoteAllowanceContract = "0xRouterQuotedOnly",
                txTo = YIELD_TOKEN,
                txData = TRANSFER_TX_DATA,
                dataAllowanceContract = null,
            )

            // Act
            val result = invokeFindBestQuote(dexProvider, yieldSupplyActive = true)

            // Assert
            assertThat(result[dexProvider]).isInstanceOf(SwapState.QuotesLoadedState::class.java)
            coVerify(exactly = 0) { walletManagersFacade.isSwapSpenderAllowed(any(), any(), any()) }
            coVerify(exactly = 0) { walletManagersFacade.isSwapTargetAllowed(any(), any(), any()) }
        }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN router swap payload WHEN findBestQuote THEN provider kept only if registry allows spender and target`(
        model: RegistryGateModel,
    ) = runTest {
        // Arrange
        stubYieldSwapWithTransferDetection()
        coEvery { walletManagersFacade.isSwapSpenderAllowed(any(), any(), model.router) } returns model.isSpenderAllowed
        coEvery { walletManagersFacade.isSwapTargetAllowed(any(), any(), model.router) } returns model.isTargetAllowed
        val dexProvider = stubDexQuoteAndExchangeData(
            quoteAllowanceContract = model.quoteAllowanceContract,
            txTo = model.router,
            txData = SWAP_TX_DATA,
            dataAllowanceContract = model.router,
        )

        // Act
        val result = invokeFindBestQuote(dexProvider, yieldSupplyActive = true)

        // Assert
        assertThat(result.containsKey(dexProvider)).isEqualTo(model.isProviderKept)
    }

    @Test
    fun `GIVEN toggle off AND router quote outside registry WHEN findBestQuote THEN skipped before exchange data`() =
        runTest {
            // Arrange
            coEvery { yieldModuleAddressProvider.getOrFetch(any(), any()) } returns YIELD_PROXY
            every { swapFeatureToggles.isYieldDexTransferEnabled } returns false
            coEvery { walletManagersFacade.isSwapSpenderAllowed(any(), any(), any()) } returns false
            val dexProvider = stubDexQuoteAndExchangeData(
                quoteAllowanceContract = "0xUnregisteredRouter",
                txTo = YIELD_TOKEN,
                txData = TRANSFER_TX_DATA,
                dataAllowanceContract = null,
            )

            // Act
            val result = invokeFindBestQuote(dexProvider, yieldSupplyActive = true)

            // Assert
            assertThat(result).doesNotContainKey(dexProvider)
            coVerify(exactly = 0) {
                repository.getExchangeData(
                    userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                    toContractAddress = any(), fromAddress = any(), toNetwork = any(),
                    fromAmount = any(), fromDecimals = any(), toDecimals = any(),
                    providerId = any(), rateType = any(), toAddress = any(),
                    expressOperationType = any(), refundAddress = any(), intentId = any(),
                )
            }
        }

    data class RegistryGateModel(
        val router: String,
        val quoteAllowanceContract: String?,
        val isSpenderAllowed: Boolean,
        val isTargetAllowed: Boolean,
        val isProviderKept: Boolean,
    )

    // Each case uses its own router: the interactor caches allowed addresses for the session.
    private fun provideTestModels() = listOf(
        RegistryGateModel(
            router = "0xRouterAllowed",
            quoteAllowanceContract = "0xRouterAllowed",
            isSpenderAllowed = true,
            isTargetAllowed = true,
            isProviderKept = true,
        ),
        RegistryGateModel(
            router = "0xRouterTargetDenied",
            quoteAllowanceContract = "0xRouterTargetDenied",
            isSpenderAllowed = true,
            isTargetAllowed = false,
            isProviderKept = false,
        ),
        RegistryGateModel(
            router = "0xRouterUnquoted",
            quoteAllowanceContract = null,
            isSpenderAllowed = false,
            isTargetAllowed = false,
            isProviderKept = false,
        ),
    )

    private fun stubYieldSwapWithTransferDetection() {
        coEvery { yieldModuleAddressProvider.getOrFetch(any(), any()) } returns YIELD_PROXY
        every { swapFeatureToggles.isYieldDexTransferEnabled } returns true
    }

    // endregion

    private fun stubAllowance(info: AllowanceInfo) {
        coEvery {
            getAllowanceInfoUseCase.invoke(
                userWalletId = any(),
                cryptoCurrency = any(),
                spenderAddress = any(),
                requiredAmount = any(),
            )
        } returns info.right()
    }

    /** Runs findBestQuote for a token from-currency whose quote carries [SPENDER] as allowanceContract. */
    private suspend fun runFindBestQuoteForToken(): SwapState.QuotesLoadedState {
        val dexProvider = stubDexQuoteAndExchangeData()
        val result = invokeFindBestQuote(dexProvider)
        return result[dexProvider] as SwapState.QuotesLoadedState
    }

    private fun stubDexQuoteAndExchangeData(
        quoteAllowanceContract: String? = SPENDER,
        txTo: String = "0xToAddress",
        txData: String = "0xdata",
        dataAllowanceContract: String? = SPENDER,
    ): com.tangem.feature.swap.domain.models.domain.SwapProvider {
        val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
        val quoteModel = buildQuoteModel(allowanceContract = quoteAllowanceContract)
        val swapDataModel = SwapDataModel(
            toTokenAmount = SwapAmount(BigDecimal("0.5"), 18),
            transaction = ExpressTransactionModel.DEX(
                fromAmount = SwapAmount(BigDecimal("1.0"), 18),
                toAmount = SwapAmount(BigDecimal("0.5"), 18),
                txValue = "1000000000000000000",
                txId = "tx-id",
                txTo = txTo,
                txExtraId = null,
                txFrom = "0xFromAddress",
                txData = txData,
                otherNativeFeeWei = null,
                gas = BigInteger.valueOf(21_000L),
                allowanceContract = dataAllowanceContract,
            ),
        )
        coEvery {
            repository.findBestQuote(
                userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                toContractAddress = any(), toNetwork = any(), fromAmount = any(),
                fromDecimals = any(), toDecimals = any(),
                providerId = dexProvider.providerId, rateType = any(),
            )
        } returns quoteModel.right()
        coEvery {
            repository.getExchangeData(
                userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                toContractAddress = any(), fromAddress = any(), toNetwork = any(),
                fromAmount = any(), fromDecimals = any(), toDecimals = any(),
                providerId = dexProvider.providerId, rateType = any(), toAddress = any(),
                expressOperationType = any(), refundAddress = any(), intentId = any(),
            )
        } returns swapDataModel.right()
        return dexProvider
    }

    private suspend fun invokeFindBestQuote(
        dexProvider: com.tangem.feature.swap.domain.models.domain.SwapProvider,
        yieldSupplyActive: Boolean = false,
        intentId: String? = null,
    ) = sut.findBestQuote(
        fromSwapCurrencyStatus = buildSwapCurrencyStatus(
            networkRawId = ethNetwork,
            isCoin = false,
            contractAddress = YIELD_TOKEN,
            amount = BigDecimal("10"),
            yieldSupplyActive = yieldSupplyActive,
        ),
        toSwapCurrencyStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork),
        providers = listOf(dexProvider),
        amountToSwap = "1.0",
        reduceBalanceBy = BigDecimal.ZERO,
        intentId = intentId,
    )

    private companion object {
        const val SPENDER = "0xSpender"
        const val YIELD_PROXY = "0xYieldProxy"
        const val WALLET_ADDRESS = "0xTestAddress"
        const val INTENT_ID = "intent-id"
        const val YIELD_TOKEN = "0xToken"
        const val WORD_HEX_LENGTH = 64

        private val RECIPIENT_WORD = "742d35cc6634c0532925a3b844bc454e4438f44e".padStart(WORD_HEX_LENGTH, '0')
        private val AMOUNT_WORD = BigInteger.valueOf(1_000_000L).toString(16).padStart(WORD_HEX_LENGTH, '0')

        val TRANSFER_TX_DATA = TransferERC20TokenCallData.METHOD_ID + RECIPIENT_WORD + AMOUNT_WORD
        val SWAP_TX_DATA = "0x12aa3caf$RECIPIENT_WORD$AMOUNT_WORD"
    }
}