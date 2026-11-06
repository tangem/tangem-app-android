package com.tangem.feature.referral.model

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.test.mock.MockAccounts
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class AccountAwardConverterTest {

    private val award = MockCryptoCurrencyFactory().createCoin(Blockchain.Polygon)

    @Test
    fun `GIVEN redesigned rows enabled AND award token held WHEN convert THEN DS3 row with balances`() {
        // Arrange
        val converter = createConverter(awardStatus = loadedStatus(), isRedesigned = true)

        // Act
        val row = converter.convert(Unit).tokenRow as TangemTokenRow.State.Network

        // Assert
        assertThat(row.id).isEqualTo(award.id.value)
        assertThat(row.network).isEqualTo(stringReference(award.symbol))
        assertThat(row.fiatBalance).isNotNull()
        assertThat(row.cryptoBalance).isNotNull()
    }

    @Test
    fun `GIVEN redesigned rows enabled AND award token not held WHEN convert THEN DS3 row without balances`() {
        // Arrange
        val converter = createConverter(awardStatus = null, isRedesigned = true)

        // Act
        val row = converter.convert(Unit).tokenRow as TangemTokenRow.State.Network

        // Assert
        assertThat(row.id).isEqualTo(award.id.value)
        assertThat(row.title).isEqualTo(stringReference(award.name))
        assertThat(row.network).isEqualTo(stringReference(award.symbol))
        assertThat(row.fiatBalance).isNull()
        assertThat(row.cryptoBalance).isNull()
    }

    @Test
    fun `GIVEN redesigned rows disabled WHEN convert THEN no DS3 row`() {
        // Arrange
        val converter = createConverter(awardStatus = loadedStatus(), isRedesigned = false)

        // Act
        val actual = converter.convert(Unit)

        // Assert
        assertThat(actual.tokenRow).isNull()
    }

    private fun createConverter(awardStatus: CryptoCurrencyStatus?, isRedesigned: Boolean) = AccountAwardConverter(
        isBalanceHidden = false,
        isSingleAccount = true,
        appCurrency = AppCurrency.Default,
        awardCryptoCurrency = award,
        accountAwardToken = awardStatus,
        cryptoPortfolio = AccountStatus.Personal(
            account = MockAccounts.createAccount(derivationIndex = 0),
            tokenList = TokenList.Ungrouped(
                currencies = listOfNotNull(awardStatus),
                totalFiatBalance = TotalFiatBalance.Loaded(BigDecimal.ONE, StatusSource.ACTUAL),
                sortedBy = TokensSortType.NONE,
            ),
            priceChangeLce = Unit.lceError(),
        ),
        onAccountClick = {},
        isRedesignedTokenRowsEnabled = isRedesigned,
    )

    private fun loadedStatus() = CryptoCurrencyStatus(
        currency = award,
        value = CryptoCurrencyStatus.Loaded(
            amount = BigDecimal.ONE,
            fiatAmount = BigDecimal.ONE,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = "addr", type = NetworkAddress.Address.Type.Primary),
            ),
            sources = CryptoCurrencyStatus.Sources(),
        ),
    )
}