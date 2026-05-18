package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.ds.message.TangemMessageEffect
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.tokens.model.warnings.DynamicAddressesWarnings
import com.tangem.domain.tokens.model.warnings.HederaWarnings
import com.tangem.domain.tokens.model.warnings.KaspaWarnings
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.AddFundsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM.TitleState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TransferUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.ZeroBalanceActionsUM
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class UpdateNotificationsTransformerTest {

    private val clickIntents: TokenDetailsClickIntents = mockk(relaxed = true)

    // region Mapping warnings to notifications

    @Test
    fun `GIVEN empty warnings WHEN transform THEN notifications are empty`() {
        // GIVEN
        val transformer = createTransformer(warnings = emptySet())

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.notifications).isEmpty()
    }

    @Test
    fun `GIVEN SomeNetworksUnreachable WHEN transform THEN notification with id networks_unreachable is created`() {
        // GIVEN
        val transformer = createTransformer(
            warnings = setOf(CryptoCurrencyWarning.SomeNetworksUnreachable),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.notifications).hasSize(1)
        assertThat(result.notifications.first().id).isEqualTo("networks_unreachable")
    }

    @Test
    fun `GIVEN BalanceNotEnoughForFee WHEN transform THEN notification with id balance_not_enough_for_fee is created`() {
        // GIVEN
        val tokenCurrency: CryptoCurrency = mockk(relaxed = true)
        val coinCurrency: CryptoCurrency = mockk(relaxed = true) {
            io.mockk.every { network } returns mockk(relaxed = true) {
                io.mockk.every { name } returns "Ethereum"
            }
            io.mockk.every { name } returns "Ethereum"
            io.mockk.every { symbol } returns "ETH"
        }
        val transformer = createTransformer(
            warnings = setOf(
                CryptoCurrencyWarning.BalanceNotEnoughForFee(
                    tokenCurrency = tokenCurrency,
                    coinCurrency = coinCurrency,
                ),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.notifications).hasSize(1)
        assertThat(result.notifications.first().id).isEqualTo("balance_not_enough_for_fee")
        assertThat(result.notifications.first().buttonsUM).hasSize(1)
    }

    @Test
    fun `GIVEN CustomTokenNotEnoughForFee with null feeCurrency WHEN transform THEN notification has no buttons`() {
        // GIVEN
        val currency: CryptoCurrency = mockk(relaxed = true)
        val transformer = createTransformer(
            warnings = setOf(
                CryptoCurrencyWarning.CustomTokenNotEnoughForFee(
                    currency = currency,
                    feeCurrency = null,
                    networkName = "Ethereum",
                    feeCurrencyName = "Ethereum",
                    feeCurrencySymbol = "ETH",
                ),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.notifications).hasSize(1)
        assertThat(result.notifications.first().id).isEqualTo("custom_token_not_enough_for_fee")
        assertThat(result.notifications.first().buttonsUM).isEmpty()
    }

    @Test
    fun `GIVEN BeaconChainShutdown WHEN transform THEN notification with id beacon_chain_shutdown is created`() {
        // GIVEN
        val transformer = createTransformer(
            warnings = setOf(CryptoCurrencyWarning.BeaconChainShutdown),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.notifications).hasSize(1)
        assertThat(result.notifications.first().id).isEqualTo("beacon_chain_shutdown")
    }

    @Test
    fun `GIVEN MigrationMaticToPol WHEN transform THEN notification with id migration_matic_pol is created`() {
        // GIVEN
        val transformer = createTransformer(
            warnings = setOf(CryptoCurrencyWarning.MigrationMaticToPol),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.notifications).hasSize(1)
        assertThat(result.notifications.first().id).isEqualTo("migration_matic_pol")
    }

    @Test
    fun `GIVEN MigrationClore WHEN transform THEN notification has button and id migration_clore`() {
        // GIVEN
        val transformer = createTransformer(
            warnings = setOf(CryptoCurrencyWarning.MigrationClore),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.notifications).hasSize(1)
        assertThat(result.notifications.first().id).isEqualTo("migration_clore")
        assertThat(result.notifications.first().buttonsUM).hasSize(1)
    }

    @Test
    fun `GIVEN HederaAssociateWarning WHEN transform THEN notification with button is created`() {
        // GIVEN
        val currency: CryptoCurrency = mockk(relaxed = true)
        val transformer = createTransformer(
            warnings = setOf(HederaWarnings.AssociateWarning(currency = currency)),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.notifications).hasSize(1)
        assertThat(result.notifications.first().id).isEqualTo("hedera_associate")
        assertThat(result.notifications.first().buttonsUM).hasSize(1)
    }

    @Test
    fun `GIVEN HederaAssociateWarningWithFee WHEN transform THEN notification with button is created`() {
        // GIVEN
        val currency: CryptoCurrency = mockk(relaxed = true)
        val transformer = createTransformer(
            warnings = setOf(
                HederaWarnings.AssociateWarningWithFee(
                    currency = currency,
                    fee = BigDecimal("0.05"),
                    feeCurrencySymbol = "HBAR",
                    feeCurrencyDecimals = 8,
                ),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.notifications).hasSize(1)
        assertThat(result.notifications.first().id).isEqualTo("hedera_associate_fee")
        assertThat(result.notifications.first().buttonsUM).hasSize(1)
    }

    @Test
    fun `GIVEN RequiredTrustline WHEN transform THEN notification with button is created`() {
        // GIVEN
        val currency: CryptoCurrency = mockk(relaxed = true)
        val transformer = createTransformer(
            warnings = setOf(
                CryptoCurrencyWarning.RequiredTrustline(
                    currency = currency,
                    currencySymbol = "XLM",
                    requiredAmount = BigDecimal("10"),
                    currencyDecimals = 7,
                ),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.notifications).hasSize(1)
        assertThat(result.notifications.first().id).isEqualTo("required_trustline")
        assertThat(result.notifications.first().buttonsUM).hasSize(1)
    }

    @Test
    fun `GIVEN KaspaIncompleteTransaction WHEN transform THEN notification with cancel and try again buttons is created`() {
        // GIVEN
        val currency: CryptoCurrency = mockk(relaxed = true)
        val transformer = createTransformer(
            warnings = setOf(
                KaspaWarnings.IncompleteTransaction(
                    currency = currency,
                    amount = BigDecimal("100"),
                    currencySymbol = "KAS",
                    currencyDecimals = 8,
                ),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.notifications).hasSize(1)
        assertThat(result.notifications.first().id).isEqualTo("kaspa_incomplete")
        assertThat(result.notifications.first().buttonsUM).hasSize(2)
        assertThat(result.notifications.first().onCloseClick).isNull()
        assertThat(result.notifications.first().messageEffect).isEqualTo(TangemMessageEffect.Warning)
    }

    // endregion

    // region Newly added warnings

    @Test
    fun `GIVEN ExistentialDeposit WHEN transform THEN notification with id existential_deposit is created`() {
        // GIVEN
        val transformer = createTransformer(
            warnings = setOf(
                CryptoCurrencyWarning.ExistentialDeposit(
                    currencyName = "Polkadot",
                    edStringValueWithSymbol = "1 DOT",
                ),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.notifications).hasSize(1)
        assertThat(result.notifications.first().id).isEqualTo("existential_deposit")
        assertThat(result.notifications.first().buttonsUM).isEmpty()
    }

    @Test
    fun `GIVEN Rent WHEN transform THEN notification with id rent_info and later button is created`() {
        // GIVEN
        val transformer = createTransformer(
            warnings = setOf(
                CryptoCurrencyWarning.Rent(
                    rent = BigDecimal("0.00001"),
                    exemptionAmount = BigDecimal("0.01"),
                    cryptoCurrency = mockk(relaxed = true),
                ),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.notifications).hasSize(1)
        assertThat(result.notifications.first().id).isEqualTo("rent_info")
        assertThat(result.notifications.first().onCloseClick).isNull()
        assertThat(result.notifications.first().buttonsUM).hasSize(1)
    }

    @Test
    fun `GIVEN Rent WHEN later clicked THEN onCloseRentInfoNotification is called`() {
        // GIVEN
        val transformer = createTransformer(
            warnings = setOf(
                CryptoCurrencyWarning.Rent(
                    rent = BigDecimal("0.00001"),
                    exemptionAmount = BigDecimal("0.01"),
                    cryptoCurrency = mockk(relaxed = true),
                ),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())
        result.notifications.first().buttonsUM.first().onClick()

        // THEN
        verify(exactly = 1) { clickIntents.onCloseRentInfoNotification() }
    }

    @Test
    fun `GIVEN SomeNetworksNoAccount WHEN transform THEN notification with id networks_no_account is created`() {
        // GIVEN
        val amountCurrency: CryptoCurrency = mockk(relaxed = true) {
            io.mockk.every { decimals } returns 7
            io.mockk.every { network } returns mockk(relaxed = true) {
                io.mockk.every { name } returns "Stellar"
                io.mockk.every { currencySymbol } returns "XLM"
            }
        }
        val transformer = createTransformer(
            warnings = setOf(
                CryptoCurrencyWarning.SomeNetworksNoAccount(
                    amountToCreateAccount = BigDecimal("1.0"),
                    amountCurrency = amountCurrency,
                ),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.notifications).hasSize(1)
        assertThat(result.notifications.first().id).isEqualTo("networks_no_account")
        assertThat(result.notifications.first().buttonsUM).isEmpty()
    }

    @Test
    fun `GIVEN TopUpWithoutReserve WHEN transform THEN notification with id top_up_without_reserve is created`() {
        // GIVEN
        val transformer = createTransformer(
            warnings = setOf(CryptoCurrencyWarning.TopUpWithoutReserve),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.notifications).hasSize(1)
        assertThat(result.notifications.first().id).isEqualTo("top_up_without_reserve")
        assertThat(result.notifications.first().buttonsUM).isEmpty()
    }

    @Test
    fun `GIVEN FeeResourceInfo WHEN transform THEN notification with id fee_resource_info is created`() {
        // GIVEN
        val transformer = createTransformer(
            warnings = setOf(
                CryptoCurrencyWarning.FeeResourceInfo(
                    amount = BigDecimal("50.0"),
                    maxAmount = BigDecimal("100.0"),
                ),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.notifications).hasSize(1)
        assertThat(result.notifications.first().id).isEqualTo("fee_resource_info")
        assertThat(result.notifications.first().buttonsUM).isEmpty()
    }

    @Test
    fun `GIVEN FeeResourceInfo with null maxAmount WHEN transform THEN notification is still created`() {
        // GIVEN
        val transformer = createTransformer(
            warnings = setOf(
                CryptoCurrencyWarning.FeeResourceInfo(
                    amount = BigDecimal("50.0"),
                    maxAmount = null,
                ),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.notifications).hasSize(1)
        assertThat(result.notifications.first().id).isEqualTo("fee_resource_info")
    }

    @Test
    fun `GIVEN UsedOutdatedDataWarning WHEN transform THEN notification with id used_outdated_data is created`() {
        // GIVEN
        val transformer = createTransformer(
            warnings = setOf(CryptoCurrencyWarning.UsedOutdatedDataWarning),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.notifications).hasSize(1)
        assertThat(result.notifications.first().id).isEqualTo("used_outdated_data")
    }

    // endregion

    // region Message effect

    @Test
    fun `GIVEN SomeNetworksUnreachable WHEN transform THEN messageEffect is None`() {
        // GIVEN
        val transformer = createTransformer(
            warnings = setOf(CryptoCurrencyWarning.SomeNetworksUnreachable),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.notifications.first().messageEffect).isEqualTo(TangemMessageEffect.None)
    }

    @Test
    fun `GIVEN MigrationClore WHEN transform THEN messageEffect is Warning`() {
        // GIVEN
        val transformer = createTransformer(
            warnings = setOf(CryptoCurrencyWarning.MigrationClore),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.notifications.first().messageEffect).isEqualTo(TangemMessageEffect.Warning)
    }

    // endregion

    // region Icon

    @Test
    fun `GIVEN any mapped warning WHEN transform THEN iconUM is not null`() {
        // GIVEN
        val transformer = createTransformer(
            warnings = setOf(CryptoCurrencyWarning.BeaconChainShutdown),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.notifications.first().iconUM).isNotNull()
    }

    // endregion

    // region Multiple warnings

    @Test
    fun `GIVEN multiple warnings WHEN transform THEN all are mapped to notifications`() {
        // GIVEN
        val transformer = createTransformer(
            warnings = setOf(
                CryptoCurrencyWarning.SomeNetworksUnreachable,
                CryptoCurrencyWarning.BeaconChainShutdown,
                CryptoCurrencyWarning.UsedOutdatedDataWarning,
                CryptoCurrencyWarning.TopUpWithoutReserve,
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.notifications).hasSize(4)
        assertThat(result.notifications.map { it.id }).containsExactly(
            "networks_unreachable",
            "beacon_chain_shutdown",
            "used_outdated_data",
            "top_up_without_reserve",
        )
    }

    // endregion

    // region Click callbacks

    @Test
    fun `GIVEN HederaAssociateWarning WHEN button clicked THEN onAssociateClick is called`() {
        // GIVEN
        val currency: CryptoCurrency = mockk(relaxed = true)
        val transformer = createTransformer(
            warnings = setOf(HederaWarnings.AssociateWarning(currency = currency)),
        )

        // WHEN
        val result = transformer.transform(initialState())
        result.notifications.first().buttonsUM.first().onClick()

        // THEN
        verify(exactly = 1) { clickIntents.onAssociateClick() }
    }

    @Test
    fun `GIVEN RequiredTrustline WHEN button clicked THEN onOpenTrustlineClick is called`() {
        // GIVEN
        val transformer = createTransformer(
            warnings = setOf(
                CryptoCurrencyWarning.RequiredTrustline(
                    currency = mockk(relaxed = true),
                    currencySymbol = "XLM",
                    requiredAmount = BigDecimal("10"),
                    currencyDecimals = 7,
                ),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())
        result.notifications.first().buttonsUM.first().onClick()

        // THEN
        verify(exactly = 1) { clickIntents.onOpenTrustlineClick() }
    }

    @Test
    fun `GIVEN KaspaIncompleteTransaction WHEN try again clicked THEN onRetryIncompleteTransactionClick is called`() {
        // GIVEN
        val transformer = createTransformer(
            warnings = setOf(
                KaspaWarnings.IncompleteTransaction(
                    currency = mockk(relaxed = true),
                    amount = BigDecimal("100"),
                    currencySymbol = "KAS",
                    currencyDecimals = 8,
                ),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())
        result.notifications.first().buttonsUM[1].onClick()

        // THEN
        verify(exactly = 1) { clickIntents.onRetryIncompleteTransactionClick() }
    }

    @Test
    fun `GIVEN KaspaIncompleteTransaction WHEN cancel clicked THEN onDismissIncompleteTransactionClick is called`() {
        // GIVEN
        val transformer = createTransformer(
            warnings = setOf(
                KaspaWarnings.IncompleteTransaction(
                    currency = mockk(relaxed = true),
                    amount = BigDecimal("100"),
                    currencySymbol = "KAS",
                    currencyDecimals = 8,
                ),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())
        result.notifications.first().buttonsUM.first().onClick()

        // THEN
        verify(exactly = 1) { clickIntents.onDismissIncompleteTransactionClick() }
    }

    @Test
    fun `GIVEN DynamicAddressesFundsFound WHEN transform THEN notification with learn more button is created`() {
        // GIVEN
        val transformer = createTransformer(
            warnings = setOf(DynamicAddressesWarnings.FundsFound),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.notifications).hasSize(1)
        assertThat(result.notifications.first().id).isEqualTo("dynamic_addresses_funds_found")
        assertThat(result.notifications.first().buttonsUM).hasSize(1)
    }

    @Test
    fun `GIVEN DynamicAddressesFundsFound WHEN button clicked THEN onDynamicAddressesFundsFoundLearnMoreClick is called`() {
        // GIVEN
        val transformer = createTransformer(
            warnings = setOf(DynamicAddressesWarnings.FundsFound),
        )

        // WHEN
        val result = transformer.transform(initialState())
        result.notifications.first().buttonsUM.first().onClick()

        // THEN
        verify(exactly = 1) { clickIntents.onDynamicAddressesFundsFoundLearnMoreClick() }
    }

    @Test
    fun `GIVEN MigrationClore WHEN button clicked THEN onCloreMigrationClick is called`() {
        // GIVEN
        val transformer = createTransformer(
            warnings = setOf(CryptoCurrencyWarning.MigrationClore),
        )

        // WHEN
        val result = transformer.transform(initialState())
        result.notifications.first().buttonsUM.first().onClick()

        // THEN
        verify(exactly = 1) { clickIntents.onCloreMigrationClick() }
    }

    @Test
    fun `GIVEN BalanceNotEnoughForFee WHEN buy button clicked THEN onBuyCoinClick is called`() {
        // GIVEN
        val coinCurrency: CryptoCurrency = mockk(relaxed = true) {
            io.mockk.every { network } returns mockk(relaxed = true) {
                io.mockk.every { name } returns "Ethereum"
            }
            io.mockk.every { name } returns "Ethereum"
            io.mockk.every { symbol } returns "ETH"
        }
        val transformer = createTransformer(
            warnings = setOf(
                CryptoCurrencyWarning.BalanceNotEnoughForFee(
                    tokenCurrency = mockk(relaxed = true),
                    coinCurrency = coinCurrency,
                ),
            ),
        )

        // WHEN
        val result = transformer.transform(initialState())
        result.notifications.first().buttonsUM.first().onClick()

        // THEN
        verify(exactly = 1) { clickIntents.onBuyCoinClick(coinCurrency) }
    }

    // endregion

    // region State preservation

    @Test
    fun `GIVEN any warnings WHEN transform THEN unrelated state fields are preserved`() {
        // GIVEN
        val state = initialState()
        val transformer = createTransformer(
            warnings = setOf(CryptoCurrencyWarning.SomeNetworksUnreachable),
        )

        // WHEN
        val result = transformer.transform(state)

        // THEN
        assertThat(result.topAppBarUM).isSameInstanceAs(state.topAppBarUM)
        assertThat(result.balanceBlockUM).isSameInstanceAs(state.balanceBlockUM)
        assertThat(result.marketPriceBlockState).isSameInstanceAs(state.marketPriceBlockState)
        assertThat(result.pullToRefreshConfig).isSameInstanceAs(state.pullToRefreshConfig)
        assertThat(result.isBalanceHidden).isEqualTo(state.isBalanceHidden)
        assertThat(result.isMarketPriceAvailable).isEqualTo(state.isMarketPriceAvailable)
    }

    // endregion

    private fun createTransformer(warnings: Set<CryptoCurrencyWarning>) = UpdateNotificationsTransformer(
        warnings = warnings,
        clickIntents = clickIntents,
    )

    private fun initialState(): TokenDetailsUM = TokenDetailsUM(
        topAppBarUM = TokenDetailsTopAppBarUM(
            titleState = TitleState.Simple(tokenName = "Tether"),
            subtitle = stringReference("ERC-20 in Ethereum network"),
            onBackClick = {},
            menuItems = persistentListOf(),
        ),
        balanceBlockUM = mockk<TokenDetailsBalanceBlockUM>(relaxed = true),
        notifications = persistentListOf(),
        marketPriceBlockState = mockk<MarketPriceBlockState>(relaxed = true),
        earnBlockState = null,
        pullToRefreshConfig = mockk<PullToRefreshConfig>(relaxed = true),
        isBalanceHidden = false,
        isMarketPriceAvailable = false,
        addFundsUM = AddFundsUM.Loading,
        transferUM = TransferUM.Loading,
        zeroBalanceActionsUM = ZeroBalanceActionsUM.Loading,
    )
}