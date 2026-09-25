package com.tangem.common.ui.account

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.OwnerKeyIndex
import com.tangem.domain.models.wallet.UserWalletId
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class AccountJointItemUMConverterTest {

    private val account = Account.Joint(
        accountId = AccountId.forJointAccount(userWalletId = UserWalletId("011"), value = "0".repeat(n = 64)).getOrNull()!!,
        accountName = AccountName("Family").getOrNull()!!,
        icon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
        ownerKeyIndex = OwnerKeyIndex(value = 0).getOrNull()!!,
        cryptoCurrencies = emptyList(),
    )
    private val appCurrency = AppCurrency(name = "US Dollar", code = "USD", symbol = "$")

    @Test
    fun `GIVEN a loaded balance WHEN convert THEN the row shows it in the app currency`() {
        // Arrange
        val balance = TotalFiatBalance.Loaded(amount = BigDecimal("12.5"), source = StatusSource.ACTUAL)

        // Act
        val actual = converter(balance).convert(account)

        // Assert
        assertThat(actual.balance).isEqualTo(
            UserWalletItemUM.Balance.Loaded(
                value = BigDecimal("12.5").format { fiat(appCurrency.code, appCurrency.symbol) },
                isFlickering = false,
            ),
        )
        assertThat(actual.titleBadge).isNotNull()
    }

    @Test
    fun `GIVEN a hidden balance WHEN convert THEN the row hides it`() {
        // Act
        val actual = converter(TotalFiatBalance.Loading, isBalanceHidden = true).convert(account)

        // Assert
        assertThat(actual.balance).isEqualTo(UserWalletItemUM.Balance.Hidden)
    }

    private fun converter(balance: TotalFiatBalance, isBalanceHidden: Boolean = false) = AccountJointItemUMConverter(
        onClick = {},
        appCurrency = appCurrency,
        accountBalance = balance,
        isBalanceHidden = isBalanceHidden,
    )
}