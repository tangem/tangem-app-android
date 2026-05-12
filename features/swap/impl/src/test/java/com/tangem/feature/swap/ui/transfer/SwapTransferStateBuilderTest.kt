package com.tangem.feature.swap.ui.transfer

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.ui.extensions.resourceReference
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
import com.tangem.feature.swap.models.*
import com.tangem.feature.swap.models.states.ProviderState
import com.tangem.feature.swap.presentation.R
import com.tangem.feature.swap.utils.formatToUIRepresentation
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapTransferStateBuilderTest {

    private val actions: UiActions = mockk(relaxed = true)
    private val sut = SwapTransferStateBuilder()

    private val userWalletId = UserWalletId(stringValue = "deadbeef")
    private val coldWallet: UserWallet.Cold = mockk(relaxed = true) {
        every { walletId } returns userWalletId
    }
    private val fromCurrencyStatus: SwapCurrencyStatus = buildSwapCurrencyStatus(coldWallet)
    private val toCurrencyStatus: SwapCurrencyStatus = buildSwapCurrencyStatus(coldWallet)
    private val iconConverter = CryptoCurrencyToIconStateConverter()
    private val fromIcon = iconConverter.convert(fromCurrencyStatus.status)
    private val toIcon = iconConverter.convert(toCurrencyStatus.status)

    @Test
    fun `GIVEN accounts mode enabled WHEN createTransferState THEN cards expose Account titles for from and to`() {
        val transferState = buildTransferState(
            fromAmount = BigDecimal("1.5"),
            toAmount = BigDecimal("1.5"),
            isAccountsMode = true,
        )

        val result = sut.createTransferState(actions, transferState, baseStateHolder())

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
    }

    @Test
    fun `GIVEN accounts mode disabled WHEN createTransferState THEN cards fall back to Text titles for from and to`() {
        val transferState = buildTransferState(
            fromAmount = BigDecimal("2"),
            toAmount = BigDecimal("2"),
            isAccountsMode = false,
        )

        val result = sut.createTransferState(actions, transferState, baseStateHolder())

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
    }

    private fun assertSharedCardShape(
        result: SwapStateHolder,
        transferState: SwapState.Transfer,
    ) {
        val sendCard = result.sendCardData as SwapCardState.SwapCardData
        val receiveCard = result.receiveCardData as SwapCardState.SwapCardData
        val expectedFromText = transferState.fromTokenInfo.tokenAmount.formatToUIRepresentation()
        val expectedToText = transferState.toTokenInfo.tokenAmount.formatToUIRepresentation()
        assertThat(sendCard.amountTextFieldValue).isEqualTo(
            TextFieldValue(text = expectedFromText, selection = TextRange(index = expectedFromText.length)),
        )
        assertThat(receiveCard.amountTextFieldValue).isEqualTo(
            TextFieldValue(text = expectedToText, selection = TextRange(index = expectedToText.length)),
        )
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
            appCurrency = AppCurrency.Default,
            isBalanceHidden = false,
            isAccountsMode = isAccountsMode,
        )
    }

    private fun baseStateHolder(): SwapStateHolder = SwapStateHolder(
        sendCardData = SwapCardState.Loading(
            type = TransactionCardType.ReadOnly(
                accountTitleUM = AccountTitleUM.Text(resourceReference(R.string.swapping_from_title_v2)),
            ),
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