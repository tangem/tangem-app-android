package com.tangem.common.ui.markets.tokenselector

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.ui.account.AccountNameUM
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class TokenSelectorContentConverterTest {

    private val wallet = MockUserWalletFactory.create()
    private val currencyFactory = MockCryptoCurrencyFactory()
    private val eth = currencyFactory.createCoin(Blockchain.Ethereum)
    private val btc = currencyFactory.createCoin(Blockchain.Bitcoin)

    private val mainAccount = Account.CryptoPortfolio.createMainAccount(USER_WALLET_ID)
    private val secondAccount = Account.CryptoPortfolio(
        accountId = AccountId.forCryptoPortfolio(USER_WALLET_ID, DerivationIndex(value = 1).getOrNull()!!),
        accountName = AccountName("Second").getOrNull()!!,
        icon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
        derivationIndex = DerivationIndex(value = 1).getOrNull()!!,
        cryptoCurrencies = listOf(eth),
    )

    @Test
    fun `GIVEN accounts mode disabled and single account WHEN convert THEN one group without headers`() {
        // Arrange
        val entries = listOf(entry(mainAccount, eth), entry(mainAccount, btc))

        // Act
        val result = converter().convert(entries)

        // Assert
        assertThat(result.sections).hasSize(1)
        val group = result.sections.single() as TokenSelectorSectionUM.TokenGroup
        assertThat(group.accountHeader).isNull()
        assertThat(group.items).hasSize(2)
    }

    @Test
    fun `GIVEN accounts mode enabled and token only in main WHEN convert THEN group has an account header`() {
        // Arrange
        val entries = listOf(entry(mainAccount, eth))

        // Act
        val result = converter(isAccountsModeEnabled = true).convert(entries)

        // Assert
        val group = result.sections.single() as TokenSelectorSectionUM.TokenGroup
        assertThat(group.accountHeader?.accountName).isEqualTo(AccountNameUM.DefaultMain.value)
    }

    @Test
    fun `GIVEN accounts mode disabled and two accounts WHEN convert THEN a group per account with a header`() {
        // Arrange
        val entries = listOf(entry(mainAccount, eth), entry(secondAccount, btc))

        // Act
        val result = converter().convert(entries)

        // Assert
        val groups = result.sections.filterIsInstance<TokenSelectorSectionUM.TokenGroup>()
        assertThat(result.sections).hasSize(2)
        assertThat(groups).hasSize(2)
        assertThat(groups.all { it.accountHeader != null }).isTrue()
        // Single wallet → no wallet headers.
        assertThat(result.sections.filterIsInstance<TokenSelectorSectionUM.WalletHeader>()).isEmpty()
    }

    @Test
    fun `GIVEN clicked item WHEN onClick invoked THEN its entry is reported back`() {
        // Arrange
        var clicked: TokenSelectorEntry? = null
        val entry = entry(mainAccount, eth)

        // Act
        val result = converter(onEntryClick = { clicked = it }).convert(listOf(entry))
        val item = (result.sections.single() as TokenSelectorSectionUM.TokenGroup).items.single()
        item.onClick()

        // Assert
        assertThat(clicked).isEqualTo(entry)
    }

    @Test
    fun `GIVEN no entries WHEN convert THEN sections are empty`() {
        // Act
        val result = converter().convert(emptyList())

        // Assert
        assertThat(result.sections).isEmpty()
    }

    private fun converter(
        isAccountsModeEnabled: Boolean = false,
        onEntryClick: (TokenSelectorEntry) -> Unit = {},
    ) = TokenSelectorContentConverter(
        appCurrency = AppCurrency.Default,
        isBalanceHidden = false,
        isAccountsModeEnabled = isAccountsModeEnabled,
        resolveWalletDeviceIcon = { mockk<DeviceIconUM>() },
        onEntryClick = onEntryClick,
    )

    private fun entry(account: Account.CryptoPortfolio, currency: CryptoCurrency) = TokenSelectorEntry(
        wallet = wallet,
        account = AccountStatus.CryptoPortfolio(
            account = account,
            tokenList = TokenList.Empty,
            priceChangeLce = lceLoading(),
        ),
        currencyStatus = CryptoCurrencyStatus(currency = currency, value = CryptoCurrencyStatus.Loading),
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
    }
}