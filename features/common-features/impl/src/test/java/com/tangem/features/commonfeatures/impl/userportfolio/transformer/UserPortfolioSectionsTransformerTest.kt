package com.tangem.features.commonfeatures.impl.userportfolio.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.ui.markets.tokenselector.TokenSelectorSectionUM
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.features.commonfeatures.api.addtoportfolio.AvailableToAddData
import com.tangem.features.commonfeatures.api.addtoportfolio.AvailableToAddWallet
import com.tangem.test.mock.MockAccounts
import io.mockk.mockk
import java.math.BigDecimal
import org.junit.jupiter.api.Test

/** The block lists every portfolio that holds the token: a joint account's tokens live on its Safe, not elsewhere */
internal class UserPortfolioSectionsTransformerTest {

    private val token = MockAccounts.createPaymentAccountToken()
    private val userWallet = MockUserWalletFactory.create()

    @Test
    fun `GIVEN a personal and an active joint account hold the token WHEN transform THEN both are listed`() {
        // Arrange
        val personal = AccountStatus.Personal(
            account = MockAccounts.createAccount(derivationIndex = 0, cryptoCurrencies = listOf(token)),
            tokenList = tokenList(),
            priceChangeLce = Unit.lceError(),
        )
        val joint = AccountStatus.Joint(
            account = MockAccounts.createJointAccount(cryptoCurrencies = listOf(token)),
            value = JointAccountStatusValue.Active(StatusSource.ACTUAL, totalMembers = 3, safeAddress = SAFE),
            tokenList = tokenList(),
            priceChangeLce = Unit.lceError(),
        )
        val transformer = transformer(accounts = listOf(personal, joint))

        // Act
        val actual = transformer.transform().content.sections
            .filterIsInstance<TokenSelectorSectionUM.TokenGroup>()
            .flatMap { group -> group.items.map { it.id } }

        // Assert
        assertThat(actual).containsExactly(
            "${userWallet.walletId.stringValue}_${personal.account.accountId.value}_${token.id.value}",
            "${userWallet.walletId.stringValue}_${joint.account.accountId.value}_${token.id.value}",
        )
    }

    private fun transformer(accounts: List<AccountStatus>) = UserPortfolioSectionsTransformer(
        availableData = AvailableToAddData(
            availableToAddWallets = mapOf(
                userWallet.walletId to AvailableToAddWallet(
                    userWallet = userWallet,
                    accounts = accounts,
                    availableNetworks = emptySet(),
                    availableToAddAccounts = emptyMap(),
                ),
            ),
        ),
        rawCurrencyId = requireNotNull(token.id.rawCurrencyId),
        appCurrency = AppCurrency.Default,
        isBalanceHidden = false,
        isAccountsModeEnabled = true,
        resolveWalletDeviceIcon = { mockk() },
        onTokenSelected = {},
    )

    private fun tokenList(): TokenList.Ungrouped = TokenList.Ungrouped(
        currencies = listOf(currencyStatus(token)),
        totalFiatBalance = TotalFiatBalance.Loaded(BigDecimal.ONE, StatusSource.ACTUAL),
        sortedBy = TokensSortType.NONE,
    )

    private fun currencyStatus(token: CryptoCurrency.Token): CryptoCurrencyStatus = CryptoCurrencyStatus(
        currency = token,
        value = CryptoCurrencyStatus.NoQuote(
            amount = BigDecimal.ONE,
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(type = NetworkAddress.Address.Type.Primary, value = SAFE),
            ),
            stakingBalance = null,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            sources = CryptoCurrencyStatus.Sources(),
        ),
    )

    private companion object {
        const val SAFE = "0x5afe00000000000000000000000000000000000001"
    }
}