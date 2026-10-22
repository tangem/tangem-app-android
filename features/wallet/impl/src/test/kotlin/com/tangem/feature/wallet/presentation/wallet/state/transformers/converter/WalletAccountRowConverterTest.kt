package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.ds2.accountrow.TangemAccountRow
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.Account.Personal.Companion.createMainAccount
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import kotlin.reflect.KClass

internal class WalletAccountRowConverterTest {

    private val userWalletId = UserWalletId("00")
    private val currencyFactory = MockCryptoCurrencyFactory()

    private val converter = WalletAccountRowConverter(
        appCurrency = AppCurrency.Default,
        expandedAccounts = emptySet(),
        onAccountCollapseClick = {},
        onAccountExpandClick = {},
        onManageTokensClick = {},
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class State {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN account balance WHEN convert THEN matching row variant`(model: StateModel) {
            // Arrange
            val accountStatus = accountStatus(totalFiatBalance = model.totalFiatBalance, values = model.values)

            // Act
            val actual = converter.convert(value = accountStatus, tokenList = persistentListOf())

            // Assert
            assertThat(actual.state).isInstanceOf(model.expected.java)
        }

        private fun provideTestModels() = listOf(
            StateModel(
                totalFiatBalance = TotalFiatBalance.Loaded(BigDecimal.TEN, StatusSource.ACTUAL),
                values = listOf(loaded()),
                expected = TangemAccountRow.State.Content::class,
            ),
            StateModel(
                totalFiatBalance = TotalFiatBalance.Loading,
                values = listOf(CryptoCurrencyStatus.Loading),
                expected = TangemAccountRow.State.Shimmer::class,
            ),
            StateModel(
                totalFiatBalance = TotalFiatBalance.Failed,
                values = listOf(loaded(), unreachable()),
                expected = TangemAccountRow.State.Unreachable::class,
            ),
            StateModel(
                totalFiatBalance = TotalFiatBalance.Failed,
                values = listOf(unreachable(), missedDerivation()),
                expected = TangemAccountRow.State.BalanceError::class,
            ),
            StateModel(
                totalFiatBalance = TotalFiatBalance.Failed,
                values = listOf(missedDerivation()),
                expected = TangemAccountRow.State.BalanceError::class,
            ),
            StateModel(
                totalFiatBalance = TotalFiatBalance.Failed,
                values = listOf(loaded()),
                expected = TangemAccountRow.State.BalanceError::class,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AvatarColor {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN account color WHEN convert THEN avatar uses the agreed fill color`(model: ColorModel) {
            // Arrange
            val accountStatus = accountStatus(
                totalFiatBalance = TotalFiatBalance.Loaded(BigDecimal.TEN, StatusSource.ACTUAL),
                values = listOf(loaded()),
                color = model.color,
            )

            // Act
            val actual = converter.convert(value = accountStatus, tokenList = persistentListOf())

            // Assert
            assertThat((actual.avatar as TangemAvatar.UiState.Account).color).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ColorModel(CryptoPortfolioIcon.Color.Azure, TangemAvatar.FillColor.DarkBlue),
            ColorModel(CryptoPortfolioIcon.Color.CaribbeanBlue, TangemAvatar.FillColor.LightBlue),
            ColorModel(CryptoPortfolioIcon.Color.DullLavender, TangemAvatar.FillColor.Lavender),
            ColorModel(CryptoPortfolioIcon.Color.CandyGrapeFizz, TangemAvatar.FillColor.Violet),
            ColorModel(CryptoPortfolioIcon.Color.SweetDesire, TangemAvatar.FillColor.LightPurple),
            ColorModel(CryptoPortfolioIcon.Color.PalatinateBlue, TangemAvatar.FillColor.DarkBlue),
            ColorModel(CryptoPortfolioIcon.Color.FuchsiaNebula, TangemAvatar.FillColor.DarkPurple),
            ColorModel(CryptoPortfolioIcon.Color.MexicanPink, TangemAvatar.FillColor.Pink),
            ColorModel(CryptoPortfolioIcon.Color.Pelati, TangemAvatar.FillColor.Red),
            ColorModel(CryptoPortfolioIcon.Color.Pattypan, TangemAvatar.FillColor.Yellow),
            ColorModel(CryptoPortfolioIcon.Color.UFOGreen, TangemAvatar.FillColor.LightGreen),
            ColorModel(CryptoPortfolioIcon.Color.VitalGreen, TangemAvatar.FillColor.DarkGreen),
        )
    }

    private fun accountStatus(
        totalFiatBalance: TotalFiatBalance,
        values: List<CryptoCurrencyStatus.Value>,
        color: CryptoPortfolioIcon.Color? = null,
    ): AccountStatus.Personal {
        val mainAccount = createMainAccount(userWalletId)
        val account = color?.let {
            Account.Personal(
                accountId = mainAccount.accountId,
                accountName = mainAccount.accountName,
                icon = CryptoPortfolioIcon.ofCustomAccount(value = CryptoPortfolioIcon.Icon.Star, color = it),
                derivationIndex = mainAccount.derivationIndex,
            )
        } ?: mainAccount
        val coin = currencyFactory.createCoin(Blockchain.Ethereum)
        return AccountStatus.Personal(
            account = account,
            tokenList = TokenList.Ungrouped(
                currencies = values.map { CryptoCurrencyStatus(currency = coin, value = it) },
                totalFiatBalance = totalFiatBalance,
                sortedBy = TokensSortType.NONE,
            ),
            priceChangeLce = Unit.lceError(),
        )
    }

    private fun loaded() = CryptoCurrencyStatus.Loaded(
        amount = BigDecimal.ONE,
        fiatAmount = BigDecimal.TEN,
        fiatRate = BigDecimal.TEN,
        priceChange = BigDecimal.ZERO,
        yieldSupplyStatus = null,
        hasCurrentNetworkTransactions = false,
        pendingTransactions = emptySet(),
        networkAddress = NetworkAddress.Single(
            defaultAddress = NetworkAddress.Address(value = "addr", type = NetworkAddress.Address.Type.Primary),
        ),
        sources = CryptoCurrencyStatus.Sources(),
    )

    private fun unreachable() = CryptoCurrencyStatus.Unreachable(priceChange = null, fiatRate = null, networkAddress = null)

    private fun missedDerivation() = CryptoCurrencyStatus.MissedDerivation(priceChange = null, fiatRate = null)

    data class StateModel(
        val totalFiatBalance: TotalFiatBalance,
        val values: List<CryptoCurrencyStatus.Value>,
        val expected: KClass<out TangemAccountRow.State>,
    )

    data class ColorModel(val color: CryptoPortfolioIcon.Color, val expected: TangemAvatar.FillColor)
}