package com.tangem.common.ui.account

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.ds2.accountrow.TangemAccountRow
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.Account.Personal.Companion.createMainAccount
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import kotlin.reflect.KClass

internal class AccountRowStateConverterTest {

    private val userWalletId = UserWalletId("00")
    private val currencyFactory = MockCryptoCurrencyFactory()

    private val converter = AccountRowStateConverter(appCurrency = AppCurrency.Default)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class State {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN account balance WHEN convert THEN matching row variant`(model: StateModel) {
            // Arrange
            val accountStatus = accountStatus(totalFiatBalance = model.totalFiatBalance, values = model.values)

            // Act
            val actual = converter.convert(value = accountStatus, onClick = {})

            // Assert
            assertThat(actual).isInstanceOf(model.expected.java)
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
            val actual = accountStatus.account.toAvatarState()

            // Assert
            assertThat((actual as TangemAvatar.UiState.Account).color).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ColorModel(CryptoPortfolioIcon.Color.Azure, TangemAvatar.FillColor.AzureBlue),
            ColorModel(CryptoPortfolioIcon.Color.CaribbeanBlue, TangemAvatar.FillColor.CaribbeanBlue),
            ColorModel(CryptoPortfolioIcon.Color.DullLavender, TangemAvatar.FillColor.DullLavender),
            ColorModel(CryptoPortfolioIcon.Color.CandyGrapeFizz, TangemAvatar.FillColor.CandyGrapeFizz),
            ColorModel(CryptoPortfolioIcon.Color.SweetDesire, TangemAvatar.FillColor.SweetDesire),
            ColorModel(CryptoPortfolioIcon.Color.PalatinateBlue, TangemAvatar.FillColor.PalatinateBlue),
            ColorModel(CryptoPortfolioIcon.Color.FuchsiaNebula, TangemAvatar.FillColor.FuchsiaNebula),
            ColorModel(CryptoPortfolioIcon.Color.MexicanPink, TangemAvatar.FillColor.MexicanPink),
            ColorModel(CryptoPortfolioIcon.Color.Pelati, TangemAvatar.FillColor.Pelati),
            ColorModel(CryptoPortfolioIcon.Color.Pattypan, TangemAvatar.FillColor.Pattypan),
            ColorModel(CryptoPortfolioIcon.Color.UFOGreen, TangemAvatar.FillColor.UfoGreen),
            ColorModel(CryptoPortfolioIcon.Color.VitalGreen, TangemAvatar.FillColor.VitalGreen),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AvatarGlyph {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN account icon and pending flag WHEN toAvatarState THEN avatar matches`(model: GlyphModel) {
            // Arrange
            val account = account(name = model.name, icon = model.icon)

            // Act
            val actual = account.toAvatarState(hasPendingOperation = model.hasPendingOperation)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            GlyphModel(
                name = AccountName.DefaultMain,
                icon = CryptoPortfolioIcon.Icon.Letter,
                hasPendingOperation = false,
                expected = TangemAvatar.UiState.Account(
                    letter = AccountNameUM.DefaultMain.value,
                    color = TangemAvatar.FillColor.AzureBlue,
                ),
            ),
            GlyphModel(
                name = AccountName("Savings").getOrNull()!!,
                icon = CryptoPortfolioIcon.Icon.Letter,
                hasPendingOperation = true,
                expected = TangemAvatar.UiState.Account(
                    letter = stringReference("Savings"),
                    color = TangemAvatar.FillColor.AzureBlue,
                    hasIndicator = true,
                ),
            ),
            GlyphModel(
                name = AccountName.DefaultMain,
                icon = CryptoPortfolioIcon.Icon.Star,
                hasPendingOperation = true,
                expected = TangemAvatar.UiState.Account(
                    imageVector = CryptoPortfolioIcon.Icon.Star.toAvatarIcon(),
                    color = TangemAvatar.FillColor.AzureBlue,
                    hasIndicator = true,
                ),
            ),
        )

        private fun account(name: AccountName, icon: CryptoPortfolioIcon.Icon): Account.Personal {
            val mainAccount = createMainAccount(userWalletId)
            return Account.Personal(
                accountId = mainAccount.accountId,
                accountName = name,
                icon = CryptoPortfolioIcon.ofCustomAccount(value = icon, color = CryptoPortfolioIcon.Color.Azure),
                derivationIndex = mainAccount.derivationIndex,
            )
        }
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

    data class GlyphModel(
        val name: AccountName,
        val icon: CryptoPortfolioIcon.Icon,
        val hasPendingOperation: Boolean,
        val expected: TangemAvatar.UiState,
    )

    data class ColorModel(val color: CryptoPortfolioIcon.Color, val expected: TangemAvatar.FillColor)
}