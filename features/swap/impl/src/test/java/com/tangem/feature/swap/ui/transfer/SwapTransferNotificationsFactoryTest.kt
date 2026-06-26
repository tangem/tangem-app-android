package com.tangem.feature.swap.ui.transfer

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TokenSwapInfo
import com.tangem.feature.swap.models.UiActions
import com.tangem.feature.swap.models.states.SwapNotificationUM
import com.tangem.features.send.api.entity.CustomFeeFieldUM
import com.tangem.features.send.api.entity.FeeItem
import com.tangem.features.send.api.entity.FeeSelectorUM
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapTransferNotificationsFactoryTest {

    private val sut = SwapTransferNotificationsFactory()

    private val actions: UiActions = mockk(relaxed = true)

    private val userWalletId = UserWalletId(stringValue = "deadbeef")
    private val coldWallet: UserWallet.Cold = mockk(relaxed = true) {
        every { walletId } returns userWalletId
    }

    @Test
    fun `GIVEN clean state WHEN getNotifications THEN list is empty`() = runTest {
        val transferState = buildTransferState()

        val result = sut.getNotifications(
            transferState = transferState,
            feeSelectorUM = null,
            feeCryptoCurrencyStatus = null,
            actions = actions,
        )

        assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN currencyCheck with rentWarning WHEN getNotifications THEN Solana RentInfo is added`() = runTest {
        val rentWarning = CryptoCurrencyWarning.Rent(
            rent = BigDecimal("0.01"),
            exemptionAmount = BigDecimal("1.0"),
            cryptoCurrency = buildCoin(),
        )
        val transferState = buildTransferState(
            currencyCheck = buildCurrencyCheck(rentWarning = rentWarning),
        )

        val result = sut.getNotifications(
            transferState = transferState,
            feeSelectorUM = null,
            feeCryptoCurrencyStatus = null,
            actions = actions,
        )

        assertThat(result.filterIsInstance<NotificationUM.Solana.RentInfo>()).hasSize(1)
    }

    @Test
    fun `GIVEN existential deposit greater than diff WHEN getNotifications THEN ExistentialDeposit is added`() =
        runTest {
            val fromStatus = buildCoinStatus(balance = BigDecimal("1.0"))
            val transferState = buildTransferState(
                fromTokenInfo = buildTokenInfo(
                    swapCurrencyStatus = fromStatus,
                    amount = BigDecimal("0.5"),
                ),
                currencyCheck = buildCurrencyCheck(existentialDeposit = BigDecimal("0.5")),
            )
            val feeSelectorUM = contentWithFee(feeValue = BigDecimal("0.4"))

            val result = sut.getNotifications(
                transferState = transferState,
                feeSelectorUM = feeSelectorUM,
                feeCryptoCurrencyStatus = null,
                actions = actions,
            )

            assertThat(result.filterIsInstance<NotificationUM.Error.ExistentialDeposit>()).hasSize(1)
        }

    @Test
    fun `GIVEN dust limit exceeded for coin WHEN getNotifications THEN MinimumAmountError is added`() = runTest {
        val fromStatus = buildCoinStatus(balance = BigDecimal("1.0"))
        val transferState = buildTransferState(
            fromTokenInfo = buildTokenInfo(
                swapCurrencyStatus = fromStatus,
                amount = BigDecimal("0.0001"),
            ),
            currencyCheck = buildCurrencyCheck(dustValue = BigDecimal("0.01")),
        )

        val result = sut.getNotifications(
            transferState = transferState,
            feeSelectorUM = null,
            feeCryptoCurrencyStatus = null,
            actions = actions,
        )

        assertThat(result.filterIsInstance<NotificationUM.Error.MinimumAmountError>()).hasSize(1)
    }

    @Test
    fun `GIVEN minAdaValue and no validationResult WHEN getNotifications THEN MinAdaValueCharged is added`() =
        runTest {
            val transferState = buildTransferState(
                minAdaValue = BigDecimal("1500000"),
            )

            val result = sut.getNotifications(
                transferState = transferState,
                feeSelectorUM = null,
                feeCryptoCurrencyStatus = null,
                actions = actions,
            )

            assertThat(result.filterIsInstance<NotificationUM.Cardano.MinAdaValueCharged>()).hasSize(1)
        }

    @Test
    fun `GIVEN transferState with isFeeCoverage true WHEN getNotifications THEN FeeCoverage is added`() = runTest {
        val fromStatus = buildCoinStatus(balance = BigDecimal("1.5"))
        val transferState = buildTransferState(
            fromTokenInfo = buildTokenInfo(
                swapCurrencyStatus = fromStatus,
                amount = BigDecimal("1.0"),
            ),
            isFeeCoverage = true,
            sendingAmount = BigDecimal("0.5"),
        )

        val result = sut.getNotifications(
            transferState = transferState,
            feeSelectorUM = null,
            feeCryptoCurrencyStatus = null,
            actions = actions,
        )

        assertThat(result.filterIsInstance<NotificationUM.Warning.FeeCoverageNotification>()).hasSize(1)
    }

    @Test
    fun `GIVEN toToken has NoAccount status with reserve gap WHEN getNotifications THEN NeedReserveToCreateAccount is added`() =
        runTest {
            val toStatus = buildNoAccountStatus(amountToCreateAccount = BigDecimal("2.0"))
            val transferState = buildTransferState(
                toTokenInfo = buildTokenInfo(
                    swapCurrencyStatus = toStatus,
                    amount = BigDecimal("0.5"),
                ),
            )

            val result = sut.getNotifications(
                transferState = transferState,
                feeSelectorUM = null,
                feeCryptoCurrencyStatus = null,
                actions = actions,
            )

            val reserve = result.filterIsInstance<SwapNotificationUM.Warning.NeedReserveToCreateAccount>()
            assertThat(reserve).hasSize(1)
            assertThat(reserve.first().receiveToken).isEqualTo(toStatus.currency.symbol)
        }

    @Test
    fun `GIVEN Tezos network with total balance amount WHEN getNotifications THEN ReduceAmount is added`() = runTest {
        val fromStatus = buildCoinStatus(rawNetworkId = "tezos", balance = BigDecimal("1.0"))
        val transferState = buildTransferState(
            fromTokenInfo = buildTokenInfo(
                swapCurrencyStatus = fromStatus,
                amount = BigDecimal("1.0"),
            ),
        )

        val result = sut.getNotifications(
            transferState = transferState,
            feeSelectorUM = null,
            feeCryptoCurrencyStatus = null,
            actions = actions,
        )

        assertThat(result.filterIsInstance<SwapNotificationUM.Warning.ReduceAmount>()).hasSize(1)
    }

    @Test
    fun `GIVEN BalanceNotEnoughForFee warning WHEN getNotifications THEN TokenExceedsBalance is added`() = runTest {
        val warning = CryptoCurrencyWarning.BalanceNotEnoughForFee(
            tokenCurrency = buildCoin(),
            coinCurrency = buildCoin(),
        )
        val transferState = buildTransferState(
            cryptoCurrencyWarning = warning,
        )

        val result = sut.getNotifications(
            transferState = transferState,
            feeSelectorUM = null,
            feeCryptoCurrencyStatus = null,
            actions = actions,
        )

        assertThat(result.filterIsInstance<NotificationUM.Error.TokenExceedsBalance>()).hasSize(1)
    }

    @Test
    fun `GIVEN Tron token and show count within limit WHEN getNotifications THEN Tron network fees Info is added`() =
        runTest {
            val transferState = buildTransferState(
                fromTokenInfo = buildTokenInfo(
                    swapCurrencyStatus = buildTronTokenStatus(),
                    amount = BigDecimal("10"),
                ),
                tronFeeNotificationShowCount = TRON_FEE_NOTIFICATION_MAX_SHOW_COUNT,
            )

            val result = sut.getNotifications(
                transferState = transferState,
                feeSelectorUM = null,
                feeCryptoCurrencyStatus = null,
                actions = actions,
            )

            assertThat(result.filterIsInstance<SwapNotificationUM.Info.TronTokenFee>()).hasSize(1)
        }

    @Test
    fun `GIVEN Tron token but show count exceeds limit WHEN getNotifications THEN no Tron network fees Info`() =
        runTest {
            val transferState = buildTransferState(
                fromTokenInfo = buildTokenInfo(
                    swapCurrencyStatus = buildTronTokenStatus(),
                    amount = BigDecimal("10"),
                ),
                tronFeeNotificationShowCount = TRON_FEE_NOTIFICATION_MAX_SHOW_COUNT + 1,
            )

            val result = sut.getNotifications(
                transferState = transferState,
                feeSelectorUM = null,
                feeCryptoCurrencyStatus = null,
                actions = actions,
            )

            assertThat(result.filterIsInstance<SwapNotificationUM.Info.TronTokenFee>()).isEmpty()
        }

    @Test
    fun `GIVEN Tron coin (not token) WHEN getNotifications THEN no Tron network fees Info`() = runTest {
        val transferState = buildTransferState(
            fromTokenInfo = buildTokenInfo(
                swapCurrencyStatus = buildCoinStatus(rawNetworkId = "tron"),
                amount = BigDecimal("10"),
            ),
            tronFeeNotificationShowCount = 0,
        )

        val result = sut.getNotifications(
            transferState = transferState,
            feeSelectorUM = null,
            feeCryptoCurrencyStatus = null,
            actions = actions,
        )

        assertThat(result.filterIsInstance<SwapNotificationUM.Info.TronTokenFee>()).isEmpty()
    }

    @Test
    fun `GIVEN UnknownError fee error and fee currency status WHEN getNotifications THEN NetworkFeeUnreachable is added`() =
        runTest {
            val transferState = buildTransferState()
            val feeCryptoCurrencyStatus = buildCoinStatus().status

            val result = sut.getNotifications(
                transferState = transferState,
                feeSelectorUM = errorSelector(GetFeeError.UnknownError),
                feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                actions = actions,
            )

            assertThat(result.filterIsInstance<NotificationUM.Warning.NetworkFeeUnreachable>()).hasSize(1)
        }

    @Test
    fun `GIVEN TronActivationError fee error and fee currency status WHEN getNotifications THEN TronAccountNotActivated is added`() =
        runTest {
            val transferState = buildTransferState()
            val feeCryptoCurrencyStatus = buildCoinStatus().status

            val result = sut.getNotifications(
                transferState = transferState,
                feeSelectorUM = errorSelector(GetFeeError.BlockchainErrors.TronActivationError),
                feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                actions = actions,
            )

            val notifications = result.filterIsInstance<NotificationUM.Warning.TronAccountNotActivated>()
            assertThat(notifications).hasSize(1)
            assertThat(notifications.first().tokenName).isEqualTo(feeCryptoCurrencyStatus.currency.name)
        }

    @Test
    fun `GIVEN fee error but null fee currency status WHEN getNotifications THEN no fee unreachable notification`() =
        runTest {
            val transferState = buildTransferState()

            val result = sut.getNotifications(
                transferState = transferState,
                feeSelectorUM = errorSelector(GetFeeError.UnknownError),
                feeCryptoCurrencyStatus = null,
                actions = actions,
            )

            assertThat(result.filterIsInstance<NotificationUM.Warning.NetworkFeeUnreachable>()).isEmpty()
        }

    @Test
    fun `GIVEN null fee error and fee currency status WHEN getNotifications THEN no fee unreachable notification`() =
        runTest {
            val transferState = buildTransferState()

            val result = sut.getNotifications(
                transferState = transferState,
                feeSelectorUM = null,
                feeCryptoCurrencyStatus = buildCoinStatus().status,
                actions = actions,
            )

            assertThat(result.filterIsInstance<NotificationUM.Warning.NetworkFeeUnreachable>()).isEmpty()
        }

    @Test
    fun `GIVEN custom fee below network minimum WHEN getNotifications THEN FeeTooLow is added`() = runTest {
        val transferState = buildTransferState()
        val feeSelectorUM = contentWithCustomFeeBelowMinimum(
            customFeeValue = "0.0001",
            minimumFeeValue = BigDecimal("0.001"),
        )

        val result = sut.getNotifications(
            transferState = transferState,
            feeSelectorUM = feeSelectorUM,
            feeCryptoCurrencyStatus = null,
            actions = actions,
        )

        assertThat(result.filterIsInstance<NotificationUM.Warning.FeeTooLow>()).hasSize(1)
    }

    @Test
    fun `GIVEN custom fee at network minimum WHEN getNotifications THEN no FeeTooLow`() = runTest {
        val transferState = buildTransferState()
        val feeSelectorUM = contentWithCustomFeeBelowMinimum(
            customFeeValue = "0.001",
            minimumFeeValue = BigDecimal("0.001"),
        )

        val result = sut.getNotifications(
            transferState = transferState,
            feeSelectorUM = feeSelectorUM,
            feeCryptoCurrencyStatus = null,
            actions = actions,
        )

        assertThat(result.filterIsInstance<NotificationUM.Warning.FeeTooLow>()).isEmpty()
    }

    /**
     * Builds a [FeeSelectorUM.Content] with a Custom fee whose [customFeeValue] is below the choosable
     * [minimumFeeValue], so
     * [com.tangem.features.send.api.subcomponents.feeSelector.utils.FeeCalculationUtils.checkIfCustomFeeTooLow]
     * reports the fee as too low. The choosable `priority` is left unstubbed (null) so the sibling
     * `checkIfCustomFeeTooHigh` short-circuits and does not add a spurious TooHigh notification.
     */
    private fun contentWithCustomFeeBelowMinimum(
        customFeeValue: String,
        minimumFeeValue: BigDecimal,
        decimals: Int = 8,
    ): FeeSelectorUM.Content {
        val customField: CustomFeeFieldUM = mockk(relaxed = true) {
            every { value } returns customFeeValue
            every { this@mockk.decimals } returns decimals
        }
        val customFeeItem: FeeItem.Custom = mockk(relaxed = true) {
            every { customValues } returns persistentListOf(customField)
        }
        val choosableFees: TransactionFee.Choosable = mockk(relaxed = true) {
            every { minimum.amount.value } returns minimumFeeValue
        }
        return mockk(relaxed = true) {
            every { selectedFeeItem } returns customFeeItem
            every { fees } returns choosableFees
        }
    }

    /**
     * Builds a [FeeSelectorUM.Content] whose selected fee carries [feeValue]. A non-Custom fee item is used so
     * [com.tangem.features.send.api.subcomponents.feeSelector.utils.FeeCalculationUtils.checkIfCustomFeeTooHigh]
     * short-circuits and does not add a spurious TooHigh notification.
     */
    private fun contentWithFee(feeValue: BigDecimal): FeeSelectorUM.Content {
        val fee: Fee = mockk(relaxed = true) {
            every { amount.value } returns feeValue
        }
        return mockk(relaxed = true) {
            every { selectedFeeItem } returns FeeItem.Market(fee = fee)
        }
    }

    private fun errorSelector(error: GetFeeError): FeeSelectorUM.Error = FeeSelectorUM.Error(error = error)

    @Suppress("LongParameterList")
    private fun buildTransferState(
        fromTokenInfo: TokenSwapInfo = buildTokenInfo(buildCoinStatus()),
        toTokenInfo: TokenSwapInfo = buildTokenInfo(buildCoinStatus()),
        cryptoCurrencyWarning: CryptoCurrencyWarning? = null,
        currencyCheck: CryptoCurrencyCheck? = null,
        validationResult: Throwable? = null,
        minAdaValue: BigDecimal? = null,
        isFeeCoverage: Boolean = false,
        sendingAmount: BigDecimal = fromTokenInfo.tokenAmount.value,
        tronFeeNotificationShowCount: Int = 0,
        isAmountSubtractAvailable: Boolean = false,
    ): SwapState.Transfer = SwapState.Transfer(
        userWallet = coldWallet,
        fromTokenInfo = fromTokenInfo,
        toTokenInfo = toTokenInfo,
        cryptoCurrencyWarning = cryptoCurrencyWarning,
        isInsufficientBalance = false,
        appCurrency = AppCurrency.Default,
        isBalanceHidden = false,
        isAccountsMode = false,
        isFeeCoverage = isFeeCoverage,
        sendingAmount = sendingAmount,
        tronFeeNotificationShowCount = tronFeeNotificationShowCount,
        isAmountSubtractAvailable = isAmountSubtractAvailable,
        currencyCheck = currencyCheck,
        validationResult = validationResult,
        minAdaValue = minAdaValue,
    )

    private fun buildTokenInfo(
        swapCurrencyStatus: SwapCurrencyStatus,
        amount: BigDecimal = BigDecimal("0.1"),
    ): TokenSwapInfo = TokenSwapInfo(
        tokenAmount = SwapAmount(value = amount, decimals = swapCurrencyStatus.currency.decimals),
        amountFiat = amount * BigDecimal("2000"),
        swapCurrencyStatus = swapCurrencyStatus,
    )

    private fun buildCurrencyCheck(
        existentialDeposit: BigDecimal? = null,
        dustValue: BigDecimal? = null,
        reserveAmount: BigDecimal? = null,
        rentWarning: CryptoCurrencyWarning.Rent? = null,
    ): CryptoCurrencyCheck = CryptoCurrencyCheck(
        dustValue = dustValue,
        reserveAmount = reserveAmount,
        minimumSendAmount = null,
        existentialDeposit = existentialDeposit,
        utxoAmountLimit = null,
        isAccountFunded = true,
        rentWarning = rentWarning,
    )

    private fun buildCoinStatus(
        rawNetworkId: String = "ethereum",
        balance: BigDecimal = BigDecimal("1.0"),
        fiatRate: BigDecimal = BigDecimal("2000"),
    ): SwapCurrencyStatus {
        val coin = buildCoin(rawNetworkId = rawNetworkId)
        val statusValue: CryptoCurrencyStatus.Loaded = mockk(relaxed = true) {
            every { amount } returns balance
            every { this@mockk.fiatRate } returns fiatRate
            every { fiatAmount } returns balance.multiply(fiatRate)
        }
        return SwapCurrencyStatus(
            userWallet = coldWallet,
            status = CryptoCurrencyStatus(currency = coin, value = statusValue),
            account = Account.CryptoPortfolio.createMainAccount(userWalletId),
        )
    }

    private fun buildNoAccountStatus(amountToCreateAccount: BigDecimal): SwapCurrencyStatus {
        val coin = buildCoin()
        val statusValue: CryptoCurrencyStatus.NoAccount = mockk(relaxed = true) {
            every { this@mockk.amountToCreateAccount } returns amountToCreateAccount
        }
        return SwapCurrencyStatus(
            userWallet = coldWallet,
            status = CryptoCurrencyStatus(currency = coin, value = statusValue),
            account = Account.CryptoPortfolio.createMainAccount(userWalletId),
        )
    }

    private fun buildCoin(rawNetworkId: String = "ethereum"): CryptoCurrency.Coin {
        return mockk(relaxed = true) {
            every { id } returns mockk(relaxed = true)
            every { network } returns mockk(relaxed = true) {
                every { rawId } returns rawNetworkId
                every { name } returns "Test Network"
            }
            every { name } returns "Test Coin"
            every { symbol } returns "TST"
            every { decimals } returns 18
        }
    }

    private fun buildTronTokenStatus(): SwapCurrencyStatus {
        val token = mockk<CryptoCurrency.Token>(relaxed = true) {
            every { id } returns mockk(relaxed = true)
            every { network } returns mockk(relaxed = true) {
                every { rawId } returns "tron"
                every { name } returns "Tron"
            }
            every { name } returns "Tether"
            every { symbol } returns "USDT"
            every { decimals } returns 6
        }
        val statusValue: CryptoCurrencyStatus.Loaded = mockk(relaxed = true) {
            every { amount } returns BigDecimal("100")
            every { fiatRate } returns BigDecimal.ONE
            every { fiatAmount } returns BigDecimal("100")
        }
        return SwapCurrencyStatus(
            userWallet = coldWallet,
            status = CryptoCurrencyStatus(currency = token, value = statusValue),
            account = Account.CryptoPortfolio.createMainAccount(userWalletId),
        )
    }

    private companion object {
        const val TRON_FEE_NOTIFICATION_MAX_SHOW_COUNT = 3
    }
}