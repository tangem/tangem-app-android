package com.tangem.feature.swap.ui.transfer

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.feature.swap.buildSwapCurrencyStatus
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.ui.PriceImpact
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TokenSwapInfo
import com.tangem.feature.swap.model.SwapProcessDataState
import com.tangem.feature.swap.models.*
import com.tangem.feature.swap.models.states.ProviderState
import com.tangem.feature.swap.presentation.R
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapTransferStateBuilderTest {

    private val actions: UiActions = mockk(relaxed = true)
    private val notificationsFactory: SwapTransferNotificationsFactory = mockk(relaxed = true) {
        coEvery {
            getNotifications(
                transferState = any(),
                feeCryptoCurrencyStatus = any(),
                fee = any(),
                onReduceByAmount = any(),
                onReduceToAmount = any(),
            )
        } returns persistentListOf()
    }
    private val sut = SwapTransferStateBuilder(notificationsFactory = notificationsFactory)

    private val userWalletId = UserWalletId(stringValue = "deadbeef")
    private val coldWallet: UserWallet.Cold = mockk(relaxed = true) {
        every { walletId } returns userWalletId
    }
    private val fromCurrencyStatus: SwapCurrencyStatus = buildSwapCurrencyStatus(coldWallet)
    private val toCurrencyStatus: SwapCurrencyStatus = buildSwapCurrencyStatus(coldWallet)
    private val iconConverter = CryptoCurrencyToIconStateConverter()
    private val fromIcon = iconConverter.convert(fromCurrencyStatus.status)
    private val toIcon = iconConverter.convert(toCurrencyStatus.status)
    private val initialAmountTextFieldValue = TextFieldValue(
        text = "0.5",
        selection = TextRange(index = 3),
    )

    @Test
    fun `GIVEN accounts mode enabled WHEN createTransferState THEN cards expose Account titles for from and to`() =
        runTest {
            val transferState = buildTransferState(
                fromAmount = BigDecimal("1.5"),
                toAmount = BigDecimal("1.5"),
                isAccountsMode = true,
            )
            val uiState = baseStateHolder()

            val result = sut.createTransferState(
                actions = actions,
                transferState = transferState,
                uiStateHolder = uiState,
                feePaidCryptoCurrencyStatus = null,
                fee = null,
            )

            val portfolioAccount = fromCurrencyStatus.account as Account.CryptoPortfolio
            val expectedAccountIcon = CryptoPortfolioIconConverter.convert(portfolioAccount.icon)
            val expectedAccountName = portfolioAccount.accountName.toUM().value
            val sendType = (result.sendCardData as SwapCardState.SwapCardData).type as TransactionCardType.Inputtable
            val receiveType = (result.receiveCardData as SwapCardState.SwapCardData).type as TransactionCardType.ReadOnly
            assertThat(sendType.accountTitleUM).isEqualTo(
                AccountTitleUM.Account(
                    prefixText = resourceReference(R.string.swapping_from_account_title),
                    name = expectedAccountName,
                    icon = expectedAccountIcon,
                ),
            )
            assertThat(receiveType.accountTitleUM).isEqualTo(
                AccountTitleUM.Account(
                    prefixText = resourceReference(R.string.swapping_to_account_title),
                    name = expectedAccountName,
                    icon = expectedAccountIcon,
                ),
            )
            assertSharedCardShape(
                result = result,
                transferState = transferState,
            )
            coVerify(exactly = 1) {
                notificationsFactory.getNotifications(
                    transferState = transferState,
                    feeCryptoCurrencyStatus = null,
                    fee = null,
                    onReduceByAmount = any(),
                    onReduceToAmount = any(),
                )
            }
        }

    @Test
    fun `GIVEN accounts mode disabled WHEN createTransferState THEN cards fall back to Text titles for from and to`() =
        runTest {
            val transferState = buildTransferState(
                fromAmount = BigDecimal("2"),
                toAmount = BigDecimal("2"),
                isAccountsMode = false,
            )
            val uiState = baseStateHolder()

            val result = sut.createTransferState(
                actions = actions,
                transferState = transferState,
                uiStateHolder = uiState,
                feePaidCryptoCurrencyStatus = null,
                fee = null,
            )

            val sendType = (result.sendCardData as SwapCardState.SwapCardData).type as TransactionCardType.Inputtable
            val receiveType = (result.receiveCardData as SwapCardState.SwapCardData).type as TransactionCardType.ReadOnly
            assertThat(sendType.accountTitleUM).isEqualTo(
                AccountTitleUM.Text(resourceReference(R.string.swapping_from_title_v2)),
            )
            assertThat(receiveType.accountTitleUM).isEqualTo(
                AccountTitleUM.Text(resourceReference(R.string.swapping_to_title)),
            )
            assertSharedCardShape(
                result = result,
                transferState = transferState,
            )
            coVerify(exactly = 1) {
                notificationsFactory.getNotifications(
                    transferState = transferState,
                    feeCryptoCurrencyStatus = null,
                    fee = null,
                    onReduceByAmount = any(),
                    onReduceToAmount = any(),
                )
            }
        }

    @Test
    fun `GIVEN insufficient balance and accounts mode disabled WHEN createTransferState THEN from card shows insufficient funds title and error and swap is disabled`() =
        runTest {
            val transferState = buildTransferState(
                fromAmount = BigDecimal("10"),
                toAmount = BigDecimal("10"),
                isAccountsMode = false,
                isInsufficientBalance = true,
            )
            val uiState = baseStateHolder()

            val result = sut.createTransferState(
                actions = actions,
                transferState = transferState,
                uiStateHolder = uiState,
                feePaidCryptoCurrencyStatus = null,
                fee = null,
            )

            val sendType = (result.sendCardData as SwapCardState.SwapCardData).type as TransactionCardType.Inputtable
            val receiveType = (result.receiveCardData as SwapCardState.SwapCardData).type as TransactionCardType.ReadOnly
            assertThat(sendType.accountTitleUM).isEqualTo(
                AccountTitleUM.Text(resourceReference(R.string.swapping_insufficient_funds)),
            )
            assertThat(sendType.inputError).isEqualTo(TransactionCardType.InputError.InsufficientFunds)
            assertThat(receiveType.accountTitleUM).isEqualTo(
                AccountTitleUM.Text(resourceReference(R.string.swapping_to_title)),
            )
            assertThat(result.isInsufficientFunds).isTrue()
            assertThat(result.swapButton.isEnabled).isFalse()
            assertThat(result.swapButton.mode).isEqualTo(SwapButton.Mode.TRANSFER)
            coVerify(exactly = 1) {
                notificationsFactory.getNotifications(
                    transferState = transferState,
                    feeCryptoCurrencyStatus = null,
                    fee = null,
                    onReduceByAmount = any(),
                    onReduceToAmount = any(),
                )
            }
        }

    @Test
    fun `GIVEN insufficient balance and accounts mode enabled WHEN createTransferState THEN from card overrides Account title with insufficient funds text`() =
        runTest {
            val transferState = buildTransferState(
                fromAmount = BigDecimal("10"),
                toAmount = BigDecimal("10"),
                isAccountsMode = true,
                isInsufficientBalance = true,
            )
            val uiState = baseStateHolder()

            val result = sut.createTransferState(
                actions = actions,
                transferState = transferState,
                uiStateHolder = uiState,
                feePaidCryptoCurrencyStatus = null,
                fee = null,
            )

            val portfolioAccount = toCurrencyStatus.account as Account.CryptoPortfolio
            val expectedAccountIcon = CryptoPortfolioIconConverter.convert(portfolioAccount.icon)
            val expectedAccountName = portfolioAccount.accountName.toUM().value
            val sendType = (result.sendCardData as SwapCardState.SwapCardData).type as TransactionCardType.Inputtable
            val receiveType = (result.receiveCardData as SwapCardState.SwapCardData).type as TransactionCardType.ReadOnly
            assertThat(sendType.accountTitleUM).isEqualTo(
                AccountTitleUM.Text(resourceReference(R.string.swapping_insufficient_funds)),
            )
            assertThat(sendType.inputError).isEqualTo(TransactionCardType.InputError.InsufficientFunds)
            assertThat(receiveType.accountTitleUM).isEqualTo(
                AccountTitleUM.Account(
                    prefixText = resourceReference(R.string.swapping_to_account_title),
                    name = expectedAccountName,
                    icon = expectedAccountIcon,
                ),
            )
            assertThat(result.isInsufficientFunds).isTrue()
            assertThat(result.swapButton.isEnabled).isFalse()
            coVerify(exactly = 1) {
                notificationsFactory.getNotifications(
                    transferState = transferState,
                    feeCryptoCurrencyStatus = null,
                    fee = null,
                    onReduceByAmount = any(),
                    onReduceToAmount = any(),
                )
            }
        }

    @Test
    fun `GIVEN content uiState WHEN createTransferInProgressState THEN swap button is disabled in TRANSFER_PROGRESSING mode`() {
        val initialButton = SwapButton(
            walletInteractionIcon = null,
            isEnabled = true,
            mode = SwapButton.Mode.TRANSFER,
            onClick = {},
        )
        val uiState = baseStateHolder().copy(swapButton = initialButton)

        val result = sut.createTransferInProgressState(uiState)

        assertThat(result.swapButton.isEnabled).isFalse()
        assertThat(result.swapButton.mode).isEqualTo(SwapButton.Mode.TRANSFER_PROGRESSING)
        assertThat(result.swapButton.walletInteractionIcon).isEqualTo(initialButton.walletInteractionIcon)
        assertThat(result.swapButton.onClick).isEqualTo(initialButton.onClick)
    }

    @Test
    fun `GIVEN no blocking notifications and non-null fee WHEN updateTransferButtonEnableState THEN swap button becomes enabled`() =
        runTest {
            val transferState = buildTransferState(
                fromAmount = BigDecimal("1"),
                toAmount = BigDecimal("1"),
                isAccountsMode = false,
            )
            val fee: Fee = mockk(relaxed = true)
            val uiState = baseStateHolder().copy(
                swapButton = SwapButton(
                    walletInteractionIcon = null,
                    isEnabled = false,
                    mode = SwapButton.Mode.TRANSFER,
                    onClick = {},
                ),
            )
            coEvery {
                notificationsFactory.getNotifications(
                    transferState = transferState,
                    feeCryptoCurrencyStatus = null,
                    fee = fee,
                    onReduceByAmount = any(),
                    onReduceToAmount = any(),
                )
            } returns persistentListOf()

            val result = sut.updateTransferButtonEnableState(
                transferState = transferState,
                actions = actions,
                uiStateHolder = uiState,
                feePaidCryptoCurrencyStatus = null,
                fee = fee,
            )

            assertThat(result.swapButton.isEnabled).isTrue()
            assertThat(result.swapButton.mode).isEqualTo(SwapButton.Mode.TRANSFER)
            assertThat(result.notifications).isEmpty()
            coVerify(exactly = 1) {
                notificationsFactory.getNotifications(
                    transferState = transferState,
                    feeCryptoCurrencyStatus = null,
                    fee = fee,
                    onReduceByAmount = any(),
                    onReduceToAmount = any(),
                )
            }
        }

    @Test
    fun `GIVEN dataState with from-to currencies WHEN createSuccessState THEN success holder is built in transfer mode with given fee and txUrl`() {
        val appCurrency = AppCurrency(code = "USD", name = "US Dollar", symbol = "$")
        val amount = BigDecimal("1.5")
        val dataState = SwapProcessDataState(
            fromSwapCurrencyStatus = fromCurrencyStatus,
            toSwapCurrencyStatus = toCurrencyStatus,
            amount = amount.toPlainString(),
        )
        val fee: TextReference = stringReference("0.001 ETH")
        val txUrl = "https://explorer.example/tx/0xabc"
        val timestamp = 1_700_000_000_000L

        val result = sut.createSuccessState(
            uiState = baseStateHolder(),
            dataState = dataState,
            appCurrency = appCurrency,
            isAccountsMode = true,
            txUrl = txUrl,
            timestamp = timestamp,
            fee = fee,
        )

        val success = requireNotNull(result.successState)
        assertThat(success.isTransferMode).isTrue()
        assertThat(success.shouldShowStatusButton).isFalse()
        assertThat(success.timestamp).isEqualTo(timestamp)
        assertThat(success.txUrl).isEqualTo(txUrl)
        assertThat(success.fee).isEqualTo(fee)
        assertThat(success.providerName).isEqualTo(TextReference.EMPTY)
        assertThat(success.providerType).isEqualTo(TextReference.EMPTY)
        assertThat(success.providerIcon).isEmpty()
        assertThat(success.rate).isEqualTo(TextReference.EMPTY)
        assertThat(success.fromTokenIconState).isEqualTo(fromIcon)
        assertThat(success.toTokenIconState).isEqualTo(toIcon)

        val portfolioAccount = fromCurrencyStatus.account as Account.CryptoPortfolio
        val expectedIcon = CryptoPortfolioIconConverter.convert(portfolioAccount.icon)
        val expectedName = portfolioAccount.accountName.toUM().value
        assertThat(success.fromTitle).isEqualTo(
            AccountTitleUM.Account(
                prefixText = resourceReference(R.string.swapping_from_account_title),
                name = expectedName,
                icon = expectedIcon,
            ),
        )
        assertThat(success.toTitle).isEqualTo(
            AccountTitleUM.Account(
                prefixText = resourceReference(R.string.swapping_to_account_title),
                name = expectedName,
                icon = expectedIcon,
            ),
        )
    }

    private fun assertSharedCardShape(
        result: SwapStateHolder,
        transferState: SwapState.Transfer,
    ) {
        val sendCard = result.sendCardData as SwapCardState.SwapCardData
        val receiveCard = result.receiveCardData as SwapCardState.SwapCardData
        assertThat(sendCard.amountTextFieldValue).isEqualTo(initialAmountTextFieldValue)
        assertThat(receiveCard.amountTextFieldValue).isEqualTo(initialAmountTextFieldValue)
        assertThat(sendCard.currencyIconState).isEqualTo(fromIcon)
        assertThat(receiveCard.currencyIconState).isEqualTo(toIcon)
        assertThat(sendCard.isBalanceHidden).isEqualTo(transferState.isBalanceHidden)
        assertThat(receiveCard.isBalanceHidden).isEqualTo(transferState.isBalanceHidden)
        assertThat((sendCard.type is TransactionCardType.Inputtable)).isTrue()
        assertThat(receiveCard.type).isInstanceOf(TransactionCardType.ReadOnly::class.java)
        assertThat(result.swapButton).isEqualTo(
            SwapButton(
                walletInteractionIcon = walletInterationIcon(transferState.userWallet),
                isEnabled = false,
                mode = SwapButton.Mode.TRANSFER,
                onClick = actions.onTransferClick,
            ),
        )
    }

    private fun buildTransferState(
        fromAmount: BigDecimal,
        toAmount: BigDecimal,
        isAccountsMode: Boolean,
        isInsufficientBalance: Boolean = false,
    ): SwapState.Transfer {
        val fromInfo = TokenSwapInfo(
            tokenAmount = SwapAmount(value = fromAmount, decimals = fromCurrencyStatus.currency.decimals),
            amountFiat = fromAmount * QUOTE,
            swapCurrencyStatus = fromCurrencyStatus,
        )
        val toInfo = TokenSwapInfo(
            tokenAmount = SwapAmount(value = toAmount, decimals = toCurrencyStatus.currency.decimals),
            amountFiat = toAmount * QUOTE,
            swapCurrencyStatus = toCurrencyStatus,
        )
        return SwapState.Transfer(
            userWallet = coldWallet,
            fromTokenInfo = fromInfo,
            toTokenInfo = toInfo,
            isInsufficientBalance = isInsufficientBalance,
            appCurrency = AppCurrency.Default,
            isBalanceHidden = false,
            isAccountsMode = isAccountsMode,
            isFeeCoverage = false,
            sendingAmount = fromAmount,
        )
    }

    private fun baseStateHolder(): SwapStateHolder = SwapStateHolder(
        sendCardData = SwapCardState.SwapCardData(
            type = TransactionCardType.Inputtable(
                onAmountChanged = {},
                onFocusChanged = {},
                inputError = TransactionCardType.InputError.Empty,
                accountTitleUM = AccountTitleUM.Text(resourceReference(R.string.swapping_from_title_v2)),
                isEnabled = true,
            ),
            currencyIconState = fromIcon,
            tokenSymbol = stringReference(""),
            amountEquivalent = TextReference.EMPTY,
            amountTextFieldValue = initialAmountTextFieldValue,
            balance = "",
            isBalanceHidden = false,
        ),
        receiveCardData = SwapCardState.Loading(
            type = TransactionCardType.ReadOnly(
                accountTitleUM = AccountTitleUM.Text(resourceReference(R.string.swapping_to_title)),
            ),
        ),
        isInsufficientFunds = false,
        changeCardsButtonState = ChangeCardsButtonState.ENABLED,
        providerState = ProviderState.Empty(),
        priceImpact = PriceImpact.Empty,
        swapButton = SwapButton(walletInteractionIcon = null, isEnabled = false, onClick = {}),
        shouldShowMaxAmount = false,
        onRefresh = {},
        onBackClicked = {},
        onChangeCardsClicked = {},
        onSelectTokenClick = {},
        onSuccess = {},
    )

    private companion object {
        val QUOTE: BigDecimal = BigDecimal("2000")
    }
}