package com.tangem.features.foryou.impl.model.converter.earnOpportunities

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.R
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.foryou.impl.entity.ForYouEarnOpportunitiesType
import com.tangem.features.foryou.impl.entity.EarnOpportunitiesUM
import com.tangem.features.foryou.impl.entity.ForYouWalletHeaderUM
import com.tangem.test.mock.MockAccounts
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class ForYouEarnOpportunitiesPotentialRewardsConverterTest {

    private val appCurrency: AppCurrency = AppCurrency.Default

    @Test
    fun `GIVEN accounts mode off WHEN convert THEN one flat row per earn currency`() {
        // Arrange
        val earnData = createEarnOpportunities(
            earnCurrencies = listOf("token-a", "token-b").associate { currencyId ->
                createStatus(
                    createEarnCurrency(tokenId = currencyId, currencyId = currencyId),
                    createRowLoadedValue(),
                ) to createEarnApyInfo(isActive = false)
            },
        )
        val converter = createConverter(isAccountsModeEnabled = false)

        // Act
        val result = converter.convert(listOf(earnData)) as EarnOpportunitiesUM.Content

        // Assert — flat, non-expandable token rows in a single header-less group
        assertThat(result.tokenList.map { it.header }).containsExactly(null)
        assertThat(result.items.map { it.tokenRowUM.id }).containsExactly("token-a", "token-b").inOrder()
        assertThat(result.items.map { it.isExpandable }).containsExactly(false, false)
        assertThat(result.items.map { it.isExpanded }).containsExactly(false, false)
        assertThat(result.items.flatMap { it.tokenList }).isEmpty()
    }

    @Test
    fun `GIVEN accounts mode on WHEN convert THEN one expandable account row with token children`() {
        // Arrange
        val account = MockAccounts.createAccount(derivationIndex = 1, name = "Earn account")
        val earnData = createEarnOpportunities(
            account = account,
            earnCurrencies = listOf("token-a", "token-b").associate { currencyId ->
                createStatus(
                    createEarnCurrency(tokenId = currencyId, currencyId = currencyId),
                    createRowLoadedValue(),
                ) to createEarnApyInfo(isActive = false)
            },
        )
        val converter = createConverter(isAccountsModeEnabled = true)

        // Act
        val result = converter.convert(listOf(earnData)) as EarnOpportunitiesUM.Content

        // Assert — a single account row hosting both token rows as children
        val item = result.items.single()
        assertThat(item.tokenRowUM.id).isEqualTo(account.accountId.value)
        assertThat(item.isExpandable).isTrue()
        assertThat(item.isExpanded).isFalse()
        assertThat(item.tokenList.map { it.id }).containsExactly("token-a", "token-b")
    }

    @Test
    fun `GIVEN account row clicked WHEN convert THEN expand callback receives account id`() {
        // Arrange
        val account = MockAccounts.createAccount(derivationIndex = 1)
        val earnData = createEarnOpportunities(
            account = account,
            earnCurrencies = mapOf(
                createStatus(createEarnCurrency(), createRowLoadedValue()) to createEarnApyInfo(isActive = false),
            ),
        )
        var clickedAssetId: String? = null
        val converter = createConverter(isAccountsModeEnabled = true, expandClick = { clickedAssetId = it })

        // Act
        val result = converter.convert(listOf(earnData)) as EarnOpportunitiesUM.Content
        (result.items.single().tokenRowUM as TangemTokenRowUM.Content).onItemClick?.invoke()

        // Assert
        assertThat(clickedAssetId).isEqualTo(account.accountId.value)
    }

    @Test
    fun `GIVEN token row clicked WHEN convert THEN token callback receives wallet currency and earn type`() {
        // Arrange
        val currency = createEarnCurrency()
        val earnType = ForYouEarnOpportunitiesType.YieldSupply(apy = "7.5")
        val walletId = UserWalletId("01")
        val earnData = createEarnOpportunities(
            userWalletId = walletId,
            earnCurrencies = mapOf(
                createStatus(currency, createRowLoadedValue()) to createEarnApyInfo(isActive = false, type = earnType),
            ),
        )
        var clicked: Triple<UserWalletId?, CryptoCurrency, ForYouEarnOpportunitiesType>? = null
        val converter = createConverter(
            isAccountsModeEnabled = false,
            onTokenClick = { id, clickedCurrency, type -> clicked = Triple(id, clickedCurrency, type) },
        )

        // Act
        val result = converter.convert(listOf(earnData)) as EarnOpportunitiesUM.Content
        (result.items.single().tokenRowUM as TangemTokenRowUM.Content).onItemClick?.invoke()

        // Assert
        assertThat(clicked).isEqualTo(Triple(walletId, currency, earnType))
    }

    @Test
    fun `GIVEN several accounts WHEN convert THEN header reward is the sum across accounts`() {
        // Arrange
        val first = createEarnOpportunities(
            account = MockAccounts.createAccount(derivationIndex = 1),
            accountPotentialReward = BigDecimal("10"),
        )
        val second = createEarnOpportunities(
            account = MockAccounts.createAccount(derivationIndex = 2),
            accountPotentialReward = BigDecimal("2.5"),
        )
        val converter = createConverter(isAccountsModeEnabled = false)

        // Act
        val result = converter.convert(listOf(first, second)) as EarnOpportunitiesUM.Content

        // Assert — mirrors the production per-year fiat rendering of the 12.5 total
        val expectedTotal = BigDecimal("12.5").format {
            fiat(fiatCurrencySymbol = appCurrency.symbol, fiatCurrencyCode = appCurrency.code)
        }
        assertThat(result.potentialReward)
            .isEqualTo(resourceReference(R.string.for_you_earn_per_year, wrappedList(expectedTotal)))
        assertThat(result.subtitleRes).isEqualTo(R.string.for_you_earn_opportunities_tokens_rewards)
    }

    @Test
    fun `GIVEN one wallet WHEN convert THEN single group has no header`() {
        // Arrange
        val earnData = createEarnOpportunities(userWalletId = UserWalletId("01"))
        val converter = createConverter(
            isAccountsModeEnabled = false,
            walletHeaders = mapOf(UserWalletId("01") to createWalletHeader(UserWalletId("01"))),
        )

        // Act
        val result = converter.convert(listOf(earnData)) as EarnOpportunitiesUM.Content

        // Assert — a single wallet is rendered flat, without a header
        assertThat(result.tokenList).hasSize(1)
        assertThat(result.tokenList.single().header).isNull()
    }

    @Test
    fun `GIVEN two wallets WHEN convert THEN one headed group per wallet in reward order`() {
        // Arrange — second wallet earns more, so it must lead
        val walletA = UserWalletId("0a")
        val walletB = UserWalletId("0b")
        val fromA = createEarnOpportunities(
            userWalletId = walletA,
            account = MockAccounts.createAccount(derivationIndex = 1),
            accountPotentialReward = BigDecimal("10"),
        )
        val fromB = createEarnOpportunities(
            userWalletId = walletB,
            account = MockAccounts.createAccount(derivationIndex = 2),
            accountPotentialReward = BigDecimal("25"),
        )
        val headerA = createWalletHeader(walletA, name = "Wallet A")
        val headerB = createWalletHeader(walletB, name = "Wallet B")
        val converter = createConverter(
            isAccountsModeEnabled = true,
            walletHeaders = mapOf(walletA to headerA, walletB to headerB),
        )

        // Act — pass reward-descending, matching how the parent converter sorts
        val result = converter.convert(listOf(fromB, fromA)) as EarnOpportunitiesUM.Content

        // Assert — one group per wallet, each with its header, highest-earning wallet first
        assertThat(result.tokenList.map { it.header }).containsExactly(headerB, headerA).inOrder()
    }

    private fun createConverter(
        isAccountsModeEnabled: Boolean,
        expandClick: (String) -> Unit = {},
        onTokenClick: (UserWalletId?, CryptoCurrency, ForYouEarnOpportunitiesType) -> Unit = { _, _, _ -> },
        walletHeaders: Map<UserWalletId, ForYouWalletHeaderUM> = emptyMap(),
    ) = ForYouEarnOpportunitiesPotentialRewardsConverter(
        appCurrency = appCurrency,
        isAccountsModeEnabled = isAccountsModeEnabled,
        expandClick = expandClick,
        onTokenClick = onTokenClick,
        onAllEarnTokensClick = {},
        walletHeaders = walletHeaders,
    )
}