package com.tangem.feature.swap.ui.transfer

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.transaction.Fee
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
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TokenSwapInfo
import com.tangem.feature.swap.models.states.SwapNotificationUM
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapTransferNotificationsFactoryTest {

    private val sut = SwapTransferNotificationsFactory()

    private val userWalletId = UserWalletId(stringValue = "deadbeef")
    private val coldWallet: UserWallet.Cold = mockk(relaxed = true) {
        every { walletId } returns userWalletId
    }

    @Test
    fun `GIVEN clean state WHEN getNotifications THEN list is empty`() = runTest {
        val transferState = buildTransferState()

        val result = sut.getNotifications(
            transferState = transferState,
            feeCryptoCurrencyStatus = null,
            fee = null,
            onReduceByAmount = { _, _ -> },
            onReduceToAmount = {},
            onBuyClick = {},
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
            feeCryptoCurrencyStatus = null,
            fee = null,
            onReduceByAmount = { _, _ -> },
            onReduceToAmount = {},
            onBuyClick = {},
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
            val fee: Fee = mockk(relaxed = true) {
                every { amount.value } returns BigDecimal("0.4")
            }

            val result = sut.getNotifications(
                transferState = transferState,
                feeCryptoCurrencyStatus = null,
                fee = fee,
                onReduceByAmount = { _, _ -> },
                onReduceToAmount = {},
                onBuyClick = {},
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
            feeCryptoCurrencyStatus = null,
            fee = null,
            onReduceByAmount = { _, _ -> },
            onReduceToAmount = {},
            onBuyClick = {},
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
                feeCryptoCurrencyStatus = null,
                fee = null,
                onReduceByAmount = { _, _ -> },
                onReduceToAmount = {},
                onBuyClick = {},
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
            feeCryptoCurrencyStatus = null,
            fee = null,
            onReduceByAmount = { _, _ -> },
            onReduceToAmount = {},
            onBuyClick = {},
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
                feeCryptoCurrencyStatus = null,
                fee = null,
                onReduceByAmount = { _, _ -> },
                onReduceToAmount = {},
                onBuyClick = {},
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
            feeCryptoCurrencyStatus = null,
            fee = null,
            onReduceByAmount = { _, _ -> },
            onReduceToAmount = {},
            onBuyClick = {},
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
            feeCryptoCurrencyStatus = null,
            fee = null,
            onReduceByAmount = { _, _ -> },
            onReduceToAmount = {},
            onBuyClick = {},
        )

        assertThat(result.filterIsInstance<NotificationUM.Error.TokenExceedsBalance>()).hasSize(1)
    }

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
}