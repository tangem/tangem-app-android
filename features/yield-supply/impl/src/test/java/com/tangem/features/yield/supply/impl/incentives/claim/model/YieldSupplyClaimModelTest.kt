package com.tangem.features.yield.supply.impl.incentives.claim.model

import arrow.core.left
import arrow.core.none
import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.TransactionData
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.extensions.iconResId
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.getTintForTokenIcon
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.tryGetBackgroundForTokenIcon
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.yield.supply.YieldModuleAddressProvider
import com.tangem.domain.yield.supply.models.incentives.YieldIncentivesError
import com.tangem.domain.yield.supply.models.incentives.YieldReward
import com.tangem.domain.yield.supply.models.incentives.YieldRewardDestination
import com.tangem.domain.yield.supply.models.incentives.YieldRewardsSummary
import com.tangem.domain.yield.supply.usecase.BuildYieldClaimTransactionUseCase
import com.tangem.domain.yield.supply.usecase.GetFreshYieldClaimCalldataUseCase
import com.tangem.domain.yield.supply.usecase.GetYieldRewardsSummaryUseCase
import com.tangem.domain.yield.supply.usecase.ValidateYieldClaimCalldataUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyClaimTracker
import com.tangem.domain.yield.supply.usecase.YieldSupplyMarketFlowUseCase
import com.tangem.features.yield.supply.impl.incentives.claim.YieldSupplyClaimComponent
import com.tangem.features.yield.supply.impl.incentives.claim.entity.YieldSupplyClaimUM
import com.tangem.features.yield.supply.impl.incentives.claim.result.entity.YieldSupplyClaimResultUM
import com.tangem.features.yield.supply.impl.subcomponents.YieldSupplyActionModelTestBase
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.mock.MockAccounts
import com.tangem.test.mock.yield.MockYieldSupply
import com.tangem.utils.StringsSigns.DASH_SIGN
import io.mockk.called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Locale
import com.tangem.core.res.R as CoreResR

@OptIn(ExperimentalCoroutinesApi::class)
internal class YieldSupplyClaimModelTest : YieldSupplyActionModelTestBase() {

    private val marketFlowUseCase: YieldSupplyMarketFlowUseCase = mockk()
    private val getRewardsSummaryUseCase: GetYieldRewardsSummaryUseCase = mockk()
    private val getFreshClaimCalldataUseCase: GetFreshYieldClaimCalldataUseCase = mockk()
    private val buildClaimTransactionUseCase: BuildYieldClaimTransactionUseCase = mockk()
    private val moduleAddressProvider: YieldModuleAddressProvider = mockk()
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase = mockk {
        coEvery { invokeSync(userWalletId = any(), currency = any()) } returns none()
    }
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase = mockk {
        coEvery { invokeSync() } returns false
    }

    private var originalLocale: Locale = Locale.getDefault()
    private var isSheetDismissed = false
    private val submittedResults = mutableListOf<YieldSupplyClaimResultUM>()
    private val claimTracker: YieldSupplyClaimTracker = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        isSheetDismissed = false
        submittedResults.clear()
        clearMocks(claimTracker)

        every { marketFlowUseCase(any()) } returns flowOf(market())
        coEvery { moduleAddressProvider.getOrFetch(any(), any()) } returns MODULE_ADDRESS
        coEvery { getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = SOURCE_ADDRESS) } returns
            summaryOf(owner()).right()
        coEvery { getFreshClaimCalldataUseCase(chainId = CHAIN_ID, userAddress = SOURCE_ADDRESS) } returns
            calldata().right()
        coEvery { buildClaimTransactionUseCase(any(), any(), any(), any()) } returns uncompiledTx().right()
        coEvery { getFeeUseCase(userWallet = any(), network = any(), transactionData = any()) } returns
            transactionFee(BigDecimal("2")).right()
        coEvery { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) } returns TX_HASH.right()
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `GIVEN every destination WHEN sheet opens THEN groups are ordered and unsupported rewards are listed gross`() = runTest {
        // Arrange
        coEvery { getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = SOURCE_ADDRESS) } returns
            YieldRewardsSummary.Claimable(
                moduleAddress = MODULE_ADDRESS,
                claimable = listOf(module(), owner(), aave()),
                pending = emptyList(),
                unsupported = listOf(unsupported()),
                supportedRewardTokens = SUPPORTED_REWARD_TOKENS,
                totalNetUsd = BigDecimal("30.00"),
            ).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.total).isEqualTo(stringReference("\$30.00"))
        assertThat(model.uiState.value.groups).isEqualTo(
            persistentListOf(
                group(CoreResR.string.yield_module_claim_destination_owner_title, netRow(owner())),
                group(CoreResR.string.yield_module_claim_destination_aave_title, netRow(aave())),
                group(CoreResR.string.yield_module_claim_destination_module_title, netRow(module())),
                group(CoreResR.string.yield_module_claim_destination_unsupported_title, grossRow(unsupported())),
            ),
        )

        model.onDestroy()
    }

    @Test
    fun `GIVEN fresh calldata WHEN sheet opens THEN both fees are shown and the rate rides in the service fee`() =
        runTest {
            // Act
            val model = createModel()
            advanceUntilIdle()

            // Assert
            val state = model.uiState.value
            assertThat(state.serviceFee).isEqualTo(stringReference("(15%)$NBSP\$0.15"))
            assertThat(state.networkFee).isEqualTo(stringReference("\$2.00"))
            assertThat(state.isServiceFeeVisible).isTrue()
            assertThat(state.warning).isNull()
            assertThat(state.isClaimEnabled).isTrue()

            model.onDestroy()
        }

    @Test
    fun `GIVEN rewards share no single rate WHEN sheet opens THEN the service fee carries no rate`() = runTest {
        // Arrange
        coEvery { getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = SOURCE_ADDRESS) } returns
            summaryOf(owner(), aave(serviceFeeRateBps = 2000)).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.serviceFee).isEqualTo(stringReference("\$0.15"))

        model.onDestroy()
    }

    @Test
    fun `GIVEN the claim gate rejects the payload WHEN sheet opens THEN no fee is estimated`() = runTest {
        // Arrange
        coEvery { getFreshClaimCalldataUseCase(chainId = CHAIN_ID, userAddress = SOURCE_ADDRESS) } returns
            calldata(isClaimRecommended = false).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        val state = model.uiState.value
        assertThat(state.warning).isEqualTo(YieldSupplyClaimUM.Warning.RewardsUnavailable)
        assertThat(state.serviceFee).isEqualTo(stringReference(DASH_SIGN))
        assertThat(state.networkFee).isEqualTo(stringReference(DASH_SIGN))
        assertThat(state.isClaimEnabled).isFalse()
        coVerify(exactly = 0) { getFeeUseCase(userWallet = any(), network = any(), transactionData = any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN incentives unavailable WHEN sheet opens THEN rewards are reported as unavailable`() = runTest {
        // Arrange
        coEvery { getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = SOURCE_ADDRESS) } returns
            YieldIncentivesError.Unavailable.left()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.warning).isEqualTo(YieldSupplyClaimUM.Warning.RewardsUnavailable)
        assertThat(model.uiState.value.groups).isEmpty()
        coVerify(exactly = 0) { getFreshClaimCalldataUseCase(any(), any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN the balance cannot cover the fee WHEN notified THEN the service fee row goes away`() = runTest {
        // Arrange
        val cannotCoverFee = MutableStateFlow(false)
        every { notificationsUpdateTrigger.cannotCoverFeeFlow } returns cannotCoverFee

        // Act
        val model = createModel()
        advanceUntilIdle()
        val visibleBefore = model.uiState.value.isServiceFeeVisible
        cannotCoverFee.value = true
        advanceUntilIdle()

        // Assert
        assertThat(visibleBefore).isTrue()
        assertThat(model.uiState.value.isServiceFeeVisible).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN a notification error WHEN notified THEN claim is disabled and the service fee row stays`() = runTest {
        // Arrange - a fee that merely failed to load leaves the service fee a known number, so the design keeps it
        val hasError = MutableStateFlow(false)
        every { notificationsUpdateTrigger.hasErrorFlow } returns hasError

        // Act
        val model = createModel()
        advanceUntilIdle()
        hasError.value = true
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isServiceFeeVisible).isTrue()
        assertThat(model.uiState.value.isClaimEnabled).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN the fee cannot be estimated WHEN sheet opens THEN the fee is dashed and claim is disabled`() = runTest {
        // Arrange
        coEvery { getFeeUseCase(userWallet = any(), network = any(), transactionData = any()) } returns
            mockk<GetFeeError>(relaxed = true).left()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.networkFee).isEqualTo(stringReference(DASH_SIGN))
        assertThat(model.uiState.value.isClaimEnabled).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN a reload while the first run is in flight WHEN both finish THEN only the newest result shows`() =
        runTest {
            // Arrange
            coEvery { getFreshClaimCalldataUseCase(chainId = CHAIN_ID, userAddress = SOURCE_ADDRESS) } coAnswers {
                delay(SLOW_CALL_MILLIS)
                calldata().right()
            } andThen calldata(isClaimRecommended = false).right()

            // Act
            val model = createModel()
            advanceTimeBy(SLOW_CALL_MILLIS / 2)
            model.onRetryClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.warning).isEqualTo(YieldSupplyClaimUM.Warning.RewardsUnavailable)
            assertThat(model.uiState.value.isClaimEnabled).isFalse()
            coVerify(exactly = 0) { getFeeUseCase(userWallet = any(), network = any(), transactionData = any()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN the markets cache never warms up WHEN sheet opens THEN it stops waiting and offers a retry`() =
        runTest {
            // Arrange
            every { marketFlowUseCase(any()) } returns MutableStateFlow(null)

            // Act
            val model = createModel()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.warning).isEqualTo(YieldSupplyClaimUM.Warning.RewardsUnavailable)
            assertThat(model.uiState.value.networkFee).isEqualTo(stringReference(DASH_SIGN))
            coVerify(exactly = 0) { getRewardsSummaryUseCase(any(), any()) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN a dust reward WHEN sheet opens THEN its row is not a plain zero`() = runTest {
        // Arrange — a billionth of an eighteen-decimal token, which is still something after the service fee
        val dust = reward(
            symbol = "WETH",
            destination = YieldRewardDestination.Owner,
            claimableAmount = BigInteger("1000000000"),
        )
        coEvery { getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = SOURCE_ADDRESS) } returns
            summaryOf(dust).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        val row = model.uiState.value.groups.single().rewards.single()
        assertThat(row.amount).isEqualTo(stringReference("<${NBSP}WETH${NBSP}0.00000001"))

        model.onDestroy()
    }

    @Test
    fun `GIVEN an unroutable reward with nothing to claim WHEN sheet opens THEN it is not listed`() = runTest {
        // Arrange
        val pendingOnly = unsupported().copy(claimableAmount = BigInteger.ZERO, pendingAmount = BigInteger.TEN)
        coEvery { getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = SOURCE_ADDRESS) } returns
            YieldRewardsSummary.Claimable(
                moduleAddress = MODULE_ADDRESS,
                claimable = listOf(owner()),
                pending = emptyList(),
                unsupported = listOf(pendingOnly),
                supportedRewardTokens = SUPPORTED_REWARD_TOKENS,
                totalNetUsd = BigDecimal("30.00"),
            ).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.groups).isEqualTo(
            persistentListOf(group(CoreResR.string.yield_module_claim_destination_owner_title, netRow(owner()))),
        )

        model.onDestroy()
    }

    @Test
    fun `GIVEN a prepared claim WHEN confirmed twice THEN one transaction is sent on freshly fetched calldata`() =
        runTest {
            // Arrange
            val sentTransaction = slot<TransactionData>()
            val model = createModel()
            advanceUntilIdle()

            // Act
            model.onClaimClick()
            model.onClaimClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                sendTransactionUseCase(txData = capture(sentTransaction), userWallet = any(), network = any())
            }
            coVerify(exactly = 2) { getFreshClaimCalldataUseCase(chainId = CHAIN_ID, userAddress = SOURCE_ADDRESS) }
            assertThat(sentTransaction.captured).isEqualTo(uncompiledTx().copy(fee = ethFee(BigDecimal("2"))))
            assertThat(submittedResults).hasSize(1)

            model.onDestroy()
        }

    @Test
    fun `GIVEN a sent claim WHEN confirmed THEN the signed figures are handed over and the claim is followed`() =
        runTest {
            // Arrange
            val model = createModel()
            advanceUntilIdle()
            val state = model.uiState.value

            // Act
            model.onClaimClick()
            advanceUntilIdle()

            // Assert
            assertThat(isSheetDismissed).isFalse()
            assertThat(submittedResults).containsExactly(
                YieldSupplyClaimResultUM(
                    status = YieldSupplyClaimResultUM.Status.Submitted,
                    total = state.total,
                    serviceFee = state.serviceFee,
                    networkFee = state.networkFee,
                ),
            )
            verify(exactly = 1) {
                claimTracker.track(
                    userWalletId = userWalletId,
                    cryptoCurrency = token,
                    chainId = CHAIN_ID,
                    userAddress = SOURCE_ADDRESS,
                    transactionHash = TX_HASH,
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN sending fails WHEN confirmed THEN nothing is handed over and no claim is followed`() = runTest {
        // Arrange
        coEvery { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) } returns
            SendTransactionError.UnknownError().left()
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClaimClick()
        advanceUntilIdle()

        // Assert
        assertThat(submittedResults).isEmpty()
        verify { claimTracker wasNot called }

        model.onDestroy()
    }

    @Test
    fun `GIVEN the claim gate rejects the newest calldata WHEN confirmed THEN nothing is signed`() = runTest {
        // Arrange
        coEvery { getFreshClaimCalldataUseCase(chainId = CHAIN_ID, userAddress = SOURCE_ADDRESS) } returns
            calldata().right() andThen calldata(isClaimRecommended = false).right()
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClaimClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) }
        assertThat(model.uiState.value.warning).isEqualTo(YieldSupplyClaimUM.Warning.RewardsUnavailable)
        assertThat(model.uiState.value.isClaimInProgress).isFalse()
        assertThat(isSheetDismissed).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN the fee outgrew the balance WHEN confirmed THEN the card is not asked to sign`() = runTest {
        // Arrange
        coEvery { getFeeUseCase(userWallet = any(), network = any(), transactionData = any()) } returns
            transactionFee(BigDecimal("100")).right()
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClaimClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) }
        assertThat(model.uiState.value.isClaimInProgress).isFalse()
        assertThat(model.uiState.value.isClaimEnabled).isFalse()
        assertThat(isSheetDismissed).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN sending fails WHEN confirmed THEN the error is raised and the claim stops`() = runTest {
        // Arrange
        coEvery { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) } returns
            SendTransactionError.UnknownError().left()
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClaimClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            alertFactory.getSendTransactionErrorState(error = any(), popBack = any(), onFailedTxEmailClick = any())
        }
        assertThat(model.uiState.value.isClaimInProgress).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN a claim in flight WHEN reloaded THEN the prepared payload is not replaced under it`() = runTest {
        // Arrange
        coEvery { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) } coAnswers {
            delay(SLOW_CALL_MILLIS)
            TX_HASH.right()
        }
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onClaimClick()
        advanceTimeBy(SLOW_CALL_MILLIS / 2)
        model.onRetryClick()
        val isClaimInProgress = model.uiState.value.isClaimInProgress
        advanceUntilIdle()

        // Assert
        assertThat(isClaimInProgress).isTrue()
        coVerify(exactly = 2) { getFreshClaimCalldataUseCase(chainId = CHAIN_ID, userAddress = SOURCE_ADDRESS) }
        coVerify(exactly = 1) { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) }
        assertThat(submittedResults).hasSize(1)

        model.onDestroy()
    }

    @Test
    fun `GIVEN a claim in flight WHEN the close icon is tapped THEN the sheet stays open`() = runTest {
        // Arrange
        coEvery { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) } coAnswers {
            delay(SLOW_CALL_MILLIS)
            TX_HASH.right()
        }
        val model = createModel()
        advanceUntilIdle()
        model.onClaimClick()
        advanceTimeBy(SLOW_CALL_MILLIS / 2)

        // Act
        model.onCloseClick()

        // Assert
        assertThat(isSheetDismissed).isFalse()

        advanceUntilIdle()
        model.onDestroy()
    }

    @Test
    fun `GIVEN no claim in flight WHEN the close icon is tapped THEN the sheet is dismissed`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.onCloseClick()

        // Assert
        assertThat(isSheetDismissed).isTrue()

        model.onDestroy()
    }

    @Test
    fun `GIVEN accounts mode WHEN the sheet loads THEN only the owner group names the account`() = runTest {
        // Arrange - the other destinations do not land in an account of the user's, so they stay unnamed
        val account = MockAccounts.createAccount(derivationIndex = 0, name = "Main Account")
        coEvery { isAccountsModeEnabledUseCase.invokeSync() } returns true
        coEvery { getAccountCurrencyStatusUseCase.invokeSync(userWalletId = any(), currency = any()) } returns
            mockk<AccountCryptoCurrencyStatus> { every { this@mockk.account } returns account }.some()
        coEvery { getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = SOURCE_ADDRESS) } returns
            summaryOf(owner(), aave()).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        val groups = model.uiState.value.groups
        assertThat(groups.first().storage).isEqualTo(
            YieldSupplyClaimUM.StorageUM(
                name = account.accountName.toUM().value,
                icon = CryptoPortfolioIconConverter.convert(account.icon),
            ),
        )
        assertThat(groups.last().storage).isNull()

        model.onDestroy()
    }

    private fun TestScope.createModel() = YieldSupplyClaimModel(
        paramsContainer = MutableParamsContainer(
            YieldSupplyClaimComponent.Params(
                userWallet = userWallet,
                cryptoCurrencyStatusFlow = cryptoCurrencyStatusFlow,
                onSubmitted = { submittedResults += it },
                onDismiss = { isSheetDismissed = true },
            ),
        ),
        dispatchers = createTestingCoroutineDispatcherProvider(),
        yieldSupplyMarketFlowUseCase = marketFlowUseCase,
        getYieldRewardsSummaryUseCase = getRewardsSummaryUseCase,
        getFreshYieldClaimCalldataUseCase = getFreshClaimCalldataUseCase,
        validateYieldClaimCalldataUseCase = ValidateYieldClaimCalldataUseCase(moduleAddressProvider),
        buildYieldClaimTransactionUseCase = buildClaimTransactionUseCase,
        getFeeUseCase = getFeeUseCase,
        getFeePaidCryptoCurrencyStatusSyncUseCase = getFeePaidCryptoCurrencyStatusSyncUseCase,
        getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
        sendTransactionUseCase = sendTransactionUseCase,
        yieldSupplyNotificationsUpdateTrigger = notificationsUpdateTrigger,
        yieldSupplyAlertFactory = alertFactory,
        yieldSupplyClaimTracker = claimTracker,
        getAccountCurrencyStatusUseCase = getAccountCurrencyStatusUseCase,
        isAccountsModeEnabledUseCase = isAccountsModeEnabledUseCase,
    )

    private fun summaryOf(vararg claimable: YieldReward) = YieldRewardsSummary.Claimable(
        moduleAddress = MODULE_ADDRESS,
        claimable = claimable.toList(),
        pending = emptyList(),
        unsupported = emptyList(),
        supportedRewardTokens = SUPPORTED_REWARD_TOKENS,
        totalNetUsd = BigDecimal("30.00"),
    )

    private fun group(titleResId: Int, vararg rewards: YieldSupplyClaimUM.RewardUM) = YieldSupplyClaimUM.RewardGroupUM(
        title = resourceReference(titleResId),
        rewards = persistentListOf(*rewards),
    )

    /** Net of the service fee: `1_000_000_000_000_000_000 * (10_000 - 1_500) / 10_000`. */
    private fun netRow(reward: YieldReward) = row(reward, amount = "0.85")

    private fun grossRow(reward: YieldReward) = row(reward, amount = "1.00", fiatAmount = null)

    private fun row(reward: YieldReward, amount: String, fiatAmount: String? = "\$10.00") = YieldSupplyClaimUM.RewardUM(
        icon = CurrencyIconState.TokenIcon(
            url = null,
            topBadgeIconResId = token.network.iconResId,
            isGrayscale = false,
            shouldShowCustomBadge = false,
            fallbackTint = getTintForTokenIcon(tryGetBackgroundForTokenIcon(reward.rewardToken)),
            fallbackBackground = tryGetBackgroundForTokenIcon(reward.rewardToken),
        ),
        symbol = reward.symbol,
        networkName = token.network.name,
        amount = stringReference("${reward.symbol}$NBSP$amount"),
        fiatAmount = fiatAmount?.let(::stringReference),
    )

    private fun owner() = reward(symbol = "WETH", destination = YieldRewardDestination.Owner)

    private fun aave(serviceFeeRateBps: Int = SERVICE_FEE_BPS) =
        reward(symbol = "AAVE", destination = YieldRewardDestination.Aave, serviceFeeRateBps = serviceFeeRateBps)

    private fun module() = reward(symbol = "GHO", destination = YieldRewardDestination.Module)

    private fun unsupported() = reward(symbol = "stkGHO", destination = YieldRewardDestination.Unsupported)

    private fun reward(
        symbol: String,
        destination: YieldRewardDestination,
        serviceFeeRateBps: Int = SERVICE_FEE_BPS,
        claimableAmount: BigInteger = BigInteger("1000000000000000000"),
    ) = MockYieldSupply.reward(
        rewardToken = "0x$symbol",
        symbol = symbol,
        claimableAmount = claimableAmount,
        usdValue = BigDecimal("12.00"),
        estimatedNetValueUsd = BigDecimal("10.00"),
        estimatedServiceFeeUsd = BigDecimal("2.00"),
        serviceFeeRateBps = serviceFeeRateBps,
        finalDestination = destination,
    )

    private fun calldata(isClaimRecommended: Boolean = true) = MockYieldSupply.calldata(
        chainId = CHAIN_ID,
        userAddress = SOURCE_ADDRESS,
        moduleAddress = MODULE_ADDRESS,
        to = MODULE_ADDRESS,
        totalEstimatedServiceFeeUsd = BigDecimal("0.15"),
        isClaimRecommended = isClaimRecommended,
    )

    private fun market() = MockYieldSupply.market(
        chainId = CHAIN_ID,
        apy = BigDecimal("4.0"),
        campaign = MockYieldSupply.campaign(),
    )

    private companion object {
        const val CHAIN_ID = 1
        const val MODULE_ADDRESS = "0x3333333333333333333333333333333333333333"
        val SUPPORTED_REWARD_TOKENS = setOf("0xweth", "0xaave", "0xgho")
        const val SERVICE_FEE_BPS = 1500
        const val NBSP = ' '
        const val SLOW_CALL_MILLIS = 4_000L
        const val TX_HASH = "0xclaimhash"
    }
}