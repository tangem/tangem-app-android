package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account.Personal.Companion.createMainAccount
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.TokensListItemUM2
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListUM
import com.tangem.features.jointaccount.main.JointAccountMainUM
import com.tangem.test.mock.MockAccounts
import com.tangem.utils.StringsSigns
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WalletTokensListUMConverterTest {

    private val userWalletId = UserWalletId("00")

    @AfterEach
    fun tearDown() {
        unmockkStatic(ScanResponse::cardTypesResolver)
    }

    @ParameterizedTest
    @MethodSource("provideOrganizeButtonModels")
    fun `GIVEN wallet type WHEN convert THEN Add and Manage button visibility matches`(model: OrganizeButtonModel) {
        // Arrange
        val converter = createConverter(
            selectedWallet = mockColdWallet(
                isSingleCurrency = model.isSingleCurrency,
                isSingleWalletWithToken = model.isSingleWalletWithToken,
            ),
        )

        // Act
        val result = converter.convert(value = nonEmptyAccountList()) as WalletTokensListUM.Content

        // Assert
        assertThat(result.organizeButtonUM != null).isEqualTo(model.expectedButtonShown)
    }

    private fun createConverter(
        selectedWallet: UserWallet,
        clickIntents: WalletClickIntents = mockk(relaxed = true),
        isAccountsModeEnabled: Boolean = false,
    ): WalletTokensListUMConverter = WalletTokensListUMConverter(
        appCurrency = AppCurrency.Default,
        selectedWallet = selectedWallet,
        clickIntents = clickIntents,
        yieldModuleApyMap = emptyMap(),
        isAccountsModeEnabled = isAccountsModeEnabled,
        accountIdsWithInvites = emptySet(),
        expandedAccounts = emptySet(),
        stakingAvailabilityMap = emptyMap(),
        shouldShowMainPromo = false,
    )

    private fun mockColdWallet(isSingleCurrency: Boolean, isSingleWalletWithToken: Boolean): UserWallet.Cold {
        mockkStatic(ScanResponse::cardTypesResolver)
        val resolver = mockk<CardTypesResolver>(relaxed = true) {
            every { isSingleCurrency() } returns isSingleCurrency
            every { isSingleWalletWithToken() } returns isSingleWalletWithToken
        }
        val scanResponse = mockk<ScanResponse>()
        every { scanResponse.cardTypesResolver } returns resolver
        return mockk<UserWallet.Cold>(relaxed = true) {
            every { this@mockk.scanResponse } returns scanResponse
        }
    }

    @Test
    fun `GIVEN only a pending joint account WHEN convert THEN its row is shown instead of the empty state`() {
        // Arrange
        val converter = createConverter(selectedWallet = mockColdWallet(false, false), isAccountsModeEnabled = true)
        val accountList = accountListOf(pendingJointStatus())

        // Act
        val result = converter.convert(accountList)

        // Assert
        val content = result as WalletTokensListUM.Content
        val row = content.tokenList.single() as TokensListItemUM2.JointPending
        assertThat(row.state).isInstanceOf(JointAccountMainUM.WaitingMembers::class.java)
    }

    @Test
    fun `GIVEN personal and active joint accounts WHEN convert THEN rows follow the domain order`() {
        // Arrange
        val converter = createConverter(selectedWallet = mockColdWallet(false, false), isAccountsModeEnabled = true)
        val personal = AccountStatus.Personal(
            account = createMainAccount(userWalletId),
            tokenList = ungroupedTokenList(),
            priceChangeLce = Unit.lceError(),
        )
        val accountList = accountListOf(personal, activeJointStatus())

        // Act
        val result = converter.convert(accountList)

        // Assert
        val content = result as WalletTokensListUM.Content
        assertThat(content.tokenList).hasSize(2)
        val jointRow = content.tokenList[1] as TokensListItemUM2.Portfolio
        assertThat(jointRow.isCollapsable).isTrue()
        assertThat(jointRow.tokenList).hasSize(1)
        val subtitle = (jointRow.tokenRowUM as TangemTokenRowUM.Content).subtitleUM
        assertThat(subtitle).isEqualTo(
            TangemTokenRowUM.SubtitleUM.Content(
                text = combinedReference(
                    pluralReference(R.plurals.common_tokens_count, 1, wrappedList(1)),
                    stringReference(" ${StringsSigns.DOT} "),
                    pluralReference(R.plurals.joint_account_members_count, 3, wrappedList(3)),
                ),
            ),
        )
    }

    private fun jointAccount() = MockAccounts.createJointAccount(
        ownerKeyIndex = 0,
        cryptoCurrencies = listOf(createToken()),
        userWalletId = userWalletId,
    )

    private fun pendingJointStatus(): AccountStatus.Joint {
        return AccountStatus.Joint(
            account = jointAccount(),
            value = JointAccountStatusValue.Pending(StatusSource.ACTUAL, joinedMembers = 1, totalMembers = 3),
            tokenList = TokenList.Empty,
            priceChangeLce = Unit.lceError(),
        )
    }

    private fun activeJointStatus(): AccountStatus.Joint {
        return AccountStatus.Joint(
            account = jointAccount(),
            value = JointAccountStatusValue.Active(StatusSource.ACTUAL, totalMembers = 3, safeAddress = "0x5afe"),
            tokenList = ungroupedTokenList(),
            priceChangeLce = Unit.lceError(),
        )
    }

    private fun ungroupedTokenList(): TokenList.Ungrouped {
        return TokenList.Ungrouped(
            currencies = listOf(createLoadedStatus(createToken())),
            totalFiatBalance = TotalFiatBalance.Loaded(BigDecimal.ZERO, StatusSource.ACTUAL),
            sortedBy = TokensSortType.NONE,
        )
    }

    private fun accountListOf(vararg statuses: AccountStatus): AccountStatusList {
        return AccountStatusList(
            userWalletId = userWalletId,
            accountStatuses = statuses.toList(),
            totalAccounts = statuses.size,
            totalArchivedAccounts = 0,
            totalFiatBalance = TotalFiatBalance.Loaded(BigDecimal.ZERO, StatusSource.ACTUAL),
            sortType = TokensSortType.NONE,
            groupType = TokensGroupType.NONE,
        )
    }

    private fun nonEmptyAccountList(): AccountStatusList {
        val tokenList = TokenList.Ungrouped(
            currencies = listOf(createLoadedStatus(createToken())),
            totalFiatBalance = TotalFiatBalance.Loaded(BigDecimal.ZERO, StatusSource.ACTUAL),
            sortedBy = TokensSortType.NONE,
        )
        return AccountStatusList(
            userWalletId = userWalletId,
            accountStatuses = listOf(
                AccountStatus.Personal(
                    account = createMainAccount(userWalletId),
                    tokenList = tokenList,
                    priceChangeLce = Unit.lceError(),
                ),
            ),
            totalAccounts = 1,
            totalArchivedAccounts = 0,
            totalFiatBalance = TotalFiatBalance.Loaded(BigDecimal.ZERO, StatusSource.ACTUAL),
            sortType = TokensSortType.NONE,
            groupType = TokensGroupType.NONE,
        )
    }

    private fun createToken(): CryptoCurrency.Token {
        val network = Network(
            id = Network.ID(value = "ethereum", derivationPath = Network.DerivationPath.None),
            name = "ethereum",
            currencySymbol = "ETH",
            derivationPath = Network.DerivationPath.None,
            isTestnet = false,
            standardType = Network.StandardType.ERC20,
            hasFiatFeeRate = false,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
        return CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(rawId = "ethereum"),
                suffix = CryptoCurrency.ID.Suffix.ContractAddress(contractAddress = "0xABCDEF"),
            ),
            network = network,
            name = "Token",
            symbol = "TKN",
            decimals = 18,
            iconUrl = null,
            isCustom = false,
            contractAddress = "0xABCDEF",
        )
    }

    private fun createLoadedStatus(token: CryptoCurrency.Token): CryptoCurrencyStatus {
        val value = CryptoCurrencyStatus.Loaded(
            amount = BigDecimal.ONE,
            fiatAmount = BigDecimal.ZERO,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            stakingBalance = null,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(
                    value = "addr",
                    type = NetworkAddress.Address.Type.Primary,
                ),
            ),
            sources = CryptoCurrencyStatus.Sources(),
        )
        return CryptoCurrencyStatus(currency = token, value = value)
    }

    internal data class OrganizeButtonModel(
        val isSingleCurrency: Boolean,
        val isSingleWalletWithToken: Boolean,
        val expectedButtonShown: Boolean,
    )

    private fun provideOrganizeButtonModels() = listOf(
        // Multi-currency wallet (Wallet / Wallet2) — Add & Manage is shown.
        OrganizeButtonModel(isSingleCurrency = false, isSingleWalletWithToken = false, expectedButtonShown = true),
        // Single-currency wallet without token (e.g. S2C) — Add & Manage must be hidden ([REDACTED_TASK_KEY]).
        OrganizeButtonModel(isSingleCurrency = true, isSingleWalletWithToken = false, expectedButtonShown = false),
        // Single-currency wallet with token (e.g. NODL) — Add & Manage stays hidden.
        OrganizeButtonModel(isSingleCurrency = true, isSingleWalletWithToken = true, expectedButtonShown = false),
    )
}