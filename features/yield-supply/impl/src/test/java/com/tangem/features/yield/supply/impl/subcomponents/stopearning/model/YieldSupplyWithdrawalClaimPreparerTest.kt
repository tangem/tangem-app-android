package com.tangem.features.yield.supply.impl.subcomponents.stopearning.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.yield.supply.YieldSupplyError
import com.tangem.domain.yield.supply.models.incentives.YieldClaimValidationError
import com.tangem.domain.yield.supply.models.incentives.YieldIncentivesError
import com.tangem.domain.yield.supply.models.incentives.YieldRewardsSummary
import com.tangem.domain.yield.supply.usecase.BuildYieldClaimTransactionUseCase
import com.tangem.domain.yield.supply.usecase.GetFreshYieldClaimCalldataUseCase
import com.tangem.domain.yield.supply.usecase.GetYieldRewardsSummaryUseCase
import com.tangem.domain.yield.supply.usecase.ValidateYieldClaimCalldataUseCase
import com.tangem.domain.yield.supply.usecase.YieldClaimValidationContext
import com.tangem.domain.yield.supply.usecase.YieldSupplyClaimTracker
import com.tangem.domain.yield.supply.usecase.YieldSupplyMarketFlowUseCase
import com.tangem.features.yield.supply.impl.incentives.claim.model.YieldSupplyClaimPreparer
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.yield.MockYieldSupply.CHAIN_ID
import com.tangem.test.mock.yield.MockYieldSupply.MODULE_ADDRESS
import com.tangem.test.mock.yield.MockYieldSupply.REWARD_TOKEN
import com.tangem.test.mock.yield.MockYieldSupply.USER_ADDRESS
import com.tangem.test.mock.yield.MockYieldSupply.calldata
import com.tangem.test.mock.yield.MockYieldSupply.claimableSummary
import com.tangem.test.mock.yield.MockYieldSupply.coin
import com.tangem.test.mock.yield.MockYieldSupply.deferredSummary
import com.tangem.test.mock.yield.MockYieldSupply.market
import com.tangem.test.mock.yield.MockYieldSupply.network
import com.tangem.test.mock.yield.MockYieldSupply.reward
import com.tangem.test.mock.yield.MockYieldSupply.token
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldSupplyWithdrawalClaimPreparerTest {

    private val marketFlowUseCase: YieldSupplyMarketFlowUseCase = mockk()
    private val getRewardsSummaryUseCase: GetYieldRewardsSummaryUseCase = mockk()
    private val getFreshCalldataUseCase: GetFreshYieldClaimCalldataUseCase = mockk()
    private val validateCalldataUseCase: ValidateYieldClaimCalldataUseCase = mockk()
    private val buildClaimTransactionUseCase: BuildYieldClaimTransactionUseCase = mockk()
    private val getFeeUseCase: GetFeeUseCase = mockk()
    private val claimTracker: YieldSupplyClaimTracker = mockk()

    private val userWallet: UserWallet = mockk(relaxed = true) {
        every { walletId } returns UserWalletId("abcdef012345")
    }

    private val preparer = YieldSupplyWithdrawalClaimPreparer(
        yieldSupplyMarketFlowUseCase = marketFlowUseCase,
        yieldSupplyClaimPreparer = YieldSupplyClaimPreparer(
            getYieldRewardsSummaryUseCase = getRewardsSummaryUseCase,
            getFreshYieldClaimCalldataUseCase = getFreshCalldataUseCase,
            validateYieldClaimCalldataUseCase = validateCalldataUseCase,
        ),
        buildYieldClaimTransactionUseCase = buildClaimTransactionUseCase,
        getFeeUseCase = getFeeUseCase,
        yieldSupplyClaimTracker = claimTracker,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(
            marketFlowUseCase,
            getRewardsSummaryUseCase,
            getFreshCalldataUseCase,
            validateCalldataUseCase,
            buildClaimTransactionUseCase,
            getFeeUseCase,
            claimTracker,
        )
        every { marketFlowUseCase(token = any()) } returns flowOf(market())
        every { claimTracker.isClaimInFlight(chainId = CHAIN_ID, userAddress = USER_ADDRESS) } returns flowOf(false)
        coEvery {
            getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS)
        } returns claimableSummary().right()
        coEvery {
            getFreshCalldataUseCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS)
        } returns calldata().right()
        coEvery { validateCalldataUseCase(calldata = any(), context = any()) } returns calldata().right()
        coEvery {
            buildClaimTransactionUseCase(
                userWalletId = any(),
                cryptoCurrencyStatus = any(),
                calldata = any(),
                fee = null,
            )
        } returns claimTransaction().right()
        coEvery {
            getFeeUseCase(userWallet = any(), network = any(), transactionData = any())
        } returns TransactionFee.Single(normal = fee(CLAIM_FEE)).right()
    }

    @Test
    fun `GIVEN recommended rewards and covered fees WHEN prepared THEN the claim transaction is ready`() = runTest {
        // Act
        val actual = preparer.prepare(
            userWallet = userWallet,
            cryptoCurrencyStatus = statusOf(token(), balance = BigDecimal.TEN),
            feeCryptoCurrencyStatus = statusOf(coin(), balance = BigDecimal.TEN),
            withdrawalFee = WITHDRAWAL_FEE,
        )

        // Assert
        assertThat(actual).isEqualTo(
            YieldPreClaim.Ready(
                transactionData = claimTransaction().copy(fee = fee(CLAIM_FEE)),
                chainId = CHAIN_ID,
                userAddress = USER_ADDRESS,
                token = token(),
                claimedRewards = listOf(reward()),
            ),
        )
    }

    @Test
    fun `GIVEN a fee paid in a token WHEN its balance covers both fees THEN the claim transaction is ready`() =
        runTest {
            // Arrange
            val feeToken = token(contractAddress = FEE_TOKEN_ADDRESS, symbol = "FEE")
            val tokenFee = fee(value = CLAIM_FEE, type = AmountType.Token(sdkToken(FEE_TOKEN_ADDRESS.uppercase())))
            coEvery {
                getFeeUseCase(userWallet = any(), network = any(), transactionData = any())
            } returns TransactionFee.Single(normal = tokenFee).right()

            // Act
            val actual = preparer.prepare(
                userWallet = userWallet,
                cryptoCurrencyStatus = statusOf(token(), balance = BigDecimal.TEN),
                feeCryptoCurrencyStatus = statusOf(feeToken, balance = BigDecimal.TEN),
                withdrawalFee = WITHDRAWAL_FEE,
            )

            // Assert
            assertThat(actual).isEqualTo(
                YieldPreClaim.Ready(
                    transactionData = claimTransaction().copy(fee = tokenFee),
                    chainId = CHAIN_ID,
                    userAddress = USER_ADDRESS,
                    token = token(),
                    claimedRewards = listOf(reward()),
                ),
            )
        }

    @Test
    fun `GIVEN a coin fee and the token status as the fee balance WHEN prepared THEN skipped after the estimate`() =
        runTest {
            // Act
            val actual = preparer.prepare(
                userWallet = userWallet,
                cryptoCurrencyStatus = statusOf(token(), balance = BigDecimal.TEN),
                feeCryptoCurrencyStatus = statusOf(token(), balance = BigDecimal.TEN),
                withdrawalFee = WITHDRAWAL_FEE,
            )

            // Assert
            assertThat(actual).isEqualTo(YieldPreClaim.Skip)
            coVerify(exactly = 1) { getFeeUseCase(userWallet = any(), network = any(), transactionData = any()) }
        }

    @Test
    fun `GIVEN a claimable summary WHEN prepared THEN the gate is given the module context of the summary`() =
        runTest {
            // Arrange
            val context = slot<YieldClaimValidationContext>()

            // Act
            preparer.prepare(
                userWallet = userWallet,
                cryptoCurrencyStatus = statusOf(token(), balance = BigDecimal.TEN),
                feeCryptoCurrencyStatus = statusOf(coin(), balance = BigDecimal.TEN),
                withdrawalFee = WITHDRAWAL_FEE,
            )

            // Assert
            coVerify(exactly = 1) { validateCalldataUseCase(calldata = any(), context = capture(context)) }
            assertThat(context.captured).isEqualTo(
                YieldClaimValidationContext(
                    userWalletId = userWallet.walletId,
                    network = token().network,
                    chainId = CHAIN_ID,
                    userAddress = USER_ADDRESS,
                    moduleAddress = MODULE_ADDRESS,
                    supportedRewardTokens = setOf(REWARD_TOKEN.lowercase()),
                ),
            )
        }

    @Test
    fun `GIVEN rewards are only pending WHEN prepared THEN calldata is never requested`() = runTest {
        // Arrange
        coEvery {
            getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS)
        } returns YieldRewardsSummary.PendingOnly(pending = listOf(reward())).right()

        // Act
        val actual = preparer.prepare(
            userWallet = userWallet,
            cryptoCurrencyStatus = statusOf(token(), balance = BigDecimal.TEN),
            feeCryptoCurrencyStatus = statusOf(coin(), balance = BigDecimal.TEN),
            withdrawalFee = WITHDRAWAL_FEE,
        )

        // Assert
        assertThat(actual).isEqualTo(YieldPreClaim.Skip)
        coVerify(exactly = 0) { getFreshCalldataUseCase(chainId = any(), userAddress = any()) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun skip(model: SkipModel) = runTest {
        // Arrange
        model.arrange()

        // Act
        val actual = preparer.prepare(
            userWallet = userWallet,
            cryptoCurrencyStatus = model.status,
            feeCryptoCurrencyStatus = statusOf(model.feeCurrency, balance = model.feeBalance),
            withdrawalFee = WITHDRAWAL_FEE,
        )

        // Assert
        assertThat(actual).isEqualTo(YieldPreClaim.Skip)
    }

    internal data class SkipModel(
        val reason: String,
        val status: CryptoCurrencyStatus,
        val feeBalance: BigDecimal,
        val feeCurrency: CryptoCurrency = coin(),
        val arrange: () -> Unit,
    ) {
        override fun toString(): String = reason
    }

    private fun provideTestModels() = listOf(
        SkipModel(reason = "currency is not a token", status = statusOf(coin(), BigDecimal.TEN), BigDecimal.TEN) {},
        SkipModel(reason = "no network address", status = statusWithoutAddress(), BigDecimal.TEN) {},
        SkipModel(reason = "token has no market entry", status = statusOf(token(), BigDecimal.TEN), BigDecimal.TEN) {
            every { marketFlowUseCase(token = any()) } returns flowOf(null)
        },
        SkipModel(reason = "markets cache cannot be read", status = statusOf(token(), BigDecimal.TEN), BigDecimal.TEN) {
            every { marketFlowUseCase(token = any()) } returns flow { throw IOException("DataStore read failed") }
        },
        SkipModel(reason = "incentives unavailable", status = statusOf(token(), BigDecimal.TEN), BigDecimal.TEN) {
            coEvery {
                getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS)
            } returns YieldIncentivesError.Unavailable.left()
        },
        SkipModel(reason = "claim is deferred", status = statusOf(token(), BigDecimal.TEN), BigDecimal.TEN) {
            coEvery {
                getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS)
            } returns deferredSummary().right()
        },
        SkipModel(reason = "calldata unavailable", status = statusOf(token(), BigDecimal.TEN), BigDecimal.TEN) {
            coEvery {
                getFreshCalldataUseCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS)
            } returns YieldIncentivesError.Unavailable.left()
        },
        SkipModel(reason = "calldata rejected by the gate", status = statusOf(token(), BigDecimal.TEN), BigDecimal.TEN) {
            coEvery {
                validateCalldataUseCase(calldata = any(), context = any())
            } returns YieldClaimValidationError.NotRecommended.left()
        },
        SkipModel(reason = "transaction cannot be built", status = statusOf(token(), BigDecimal.TEN), BigDecimal.TEN) {
            coEvery {
                buildClaimTransactionUseCase(
                    userWalletId = any(),
                    cryptoCurrencyStatus = any(),
                    calldata = any(),
                    fee = null,
                )
            } returns YieldSupplyError.DataError(Throwable()).left()
        },
        SkipModel(reason = "fee cannot be estimated", status = statusOf(token(), BigDecimal.TEN), BigDecimal.TEN) {
            coEvery {
                getFeeUseCase(userWallet = any(), network = any(), transactionData = any())
            } returns GetFeeError.UnknownError.left()
        },
        SkipModel(reason = "claim fee amount is unknown", status = statusOf(token(), BigDecimal.TEN), BigDecimal.TEN) {
            coEvery {
                getFeeUseCase(userWallet = any(), network = any(), transactionData = any())
            } returns TransactionFee.Single(normal = fee(value = null)).right()
        },
        SkipModel(reason = "a claim on this module is in flight", status = statusOf(token(), BigDecimal.TEN), BigDecimal.TEN) {
            every {
                claimTracker.isClaimInFlight(chainId = CHAIN_ID, userAddress = USER_ADDRESS)
            } returns flowOf(true)
        },
        SkipModel(
            reason = "the fee coin belongs to another network",
            status = statusOf(token(), BigDecimal.TEN),
            feeBalance = BigDecimal.TEN,
            feeCurrency = coin(network = network(id = "polygon", name = "Polygon", currencySymbol = "POL")),
        ) {},
        SkipModel(
            reason = "the token fee is paid in another token",
            status = statusOf(token(), BigDecimal.TEN),
            feeBalance = BigDecimal.TEN,
            feeCurrency = token(contractAddress = FEE_TOKEN_ADDRESS),
        ) {
            coEvery {
                getFeeUseCase(userWallet = any(), network = any(), transactionData = any())
            } returns TransactionFee.Single(
                normal = fee(value = CLAIM_FEE, type = AmountType.Token(sdkToken(TOKEN_ELSEWHERE_ADDRESS))),
            ).right()
        },
        SkipModel(
            reason = "the fee is paid with a fee resource",
            status = statusOf(token(), BigDecimal.TEN),
            feeBalance = BigDecimal.TEN,
        ) {
            coEvery {
                getFeeUseCase(userWallet = any(), network = any(), transactionData = any())
            } returns TransactionFee.Single(
                normal = fee(value = CLAIM_FEE, type = AmountType.FeeResource(name = "Mana")),
            ).right()
        },
        SkipModel(
            reason = "balance covers the claim but not the withdrawal after it",
            status = statusOf(token(), BigDecimal.TEN),
            feeBalance = CLAIM_FEE + WITHDRAWAL_FEE - BigDecimal("0.0001"),
        ) {},
    )

    private fun claimTransaction() = TransactionData.Uncompiled(
        fee = null,
        amount = Amount(currencySymbol = "ETH", value = BigDecimal.ZERO, decimals = 18, type = AmountType.Coin),
        contractAddress = null,
        sourceAddress = USER_ADDRESS,
        destinationAddress = MODULE_ADDRESS,
        extras = null,
    )

    private fun fee(value: BigDecimal?, type: AmountType = AmountType.Coin) = Fee.Ethereum.EIP1559(
        maxFeePerGas = BigInteger.valueOf(1_000_000_000L),
        priorityFee = BigInteger.ONE,
        gasLimit = BigInteger.valueOf(21_000),
        amount = Amount(currencySymbol = "ETH", value = value, decimals = 18, type = type),
    )

    private fun sdkToken(contractAddress: String) = Token(
        name = "FEE_TOKEN",
        symbol = "FEE",
        contractAddress = contractAddress,
        decimals = 6,
    )

    private fun statusOf(currency: CryptoCurrency, balance: BigDecimal) = CryptoCurrencyStatus(
        currency = currency,
        value = CryptoCurrencyStatus.NoQuote(
            amount = balance,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(
                    value = USER_ADDRESS,
                    type = NetworkAddress.Address.Type.Primary,
                ),
            ),
            yieldSupplyStatus = null,
            sources = CryptoCurrencyStatus.Sources(),
        ),
    )

    private fun statusWithoutAddress() = CryptoCurrencyStatus(
        currency = token(),
        value = CryptoCurrencyStatus.Loading,
    )

    private companion object {
        val CLAIM_FEE: BigDecimal = BigDecimal("0.001")
        val WITHDRAWAL_FEE: BigDecimal = BigDecimal("0.002")
        const val FEE_TOKEN_ADDRESS = "0xfee0000000000000000000000000000000000fee"
        const val TOKEN_ELSEWHERE_ADDRESS = "0xbad0000000000000000000000000000000000bad"
    }
}