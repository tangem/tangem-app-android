package com.tangem.features.tangempay.withdraw.hub.state

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.domain.pay.model.recents.RecentTokenTransfer
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawOptions
import com.tangem.features.tangempay.withdraw.hub.bankAccount
import com.tangem.features.tangempay.withdraw.hub.model.OptionsState
import com.tangem.features.tangempay.withdraw.hub.model.RecentsState
import com.tangem.features.tangempay.withdraw.hub.model.TokenRecentsState
import com.tangem.features.tangempay.withdraw.hub.model.WithdrawHubState
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.BankSectionUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.RecentRecipientUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.RecentsUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.TokenRecentsUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayWithdrawHubUMFactoryTest {

    private val onBackClick: () -> Unit = mockk(relaxed = true)
    private val onWithinPortfolioClick: () -> Unit = mockk(relaxed = true)
    private val onToPersonalAccountClick: () -> Unit = mockk(relaxed = true)
    private val onToBusinessAccountClick: () -> Unit = mockk(relaxed = true)
    private val onRecentRecipientClick: (String) -> Unit = mockk(relaxed = true)
    private val onSeeAllClick: () -> Unit = mockk(relaxed = true)
    private val onRetryRecentsClick: () -> Unit = mockk(relaxed = true)
    private val onRecentTokenClick: (String) -> Unit = mockk(relaxed = true)

    private val factory = TangemPayWithdrawHubUMFactory(
        callbacks = TangemPayWithdrawHubUMFactory.Callbacks(
            onBackClick = onBackClick,
            onWithinPortfolioClick = onWithinPortfolioClick,
            onToPersonalAccountClick = onToPersonalAccountClick,
            onToBusinessAccountClick = onToBusinessAccountClick,
            onRecentRecipientClick = onRecentRecipientClick,
            onSeeAllClick = onSeeAllClick,
            onRetryRecentsClick = onRetryRecentsClick,
            onRecentTokenClick = onRecentTokenClick,
        ),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            onBackClick,
            onWithinPortfolioClick,
            onToPersonalAccountClick,
            onToBusinessAccountClick,
            onRecentRecipientClick,
            onSeeAllClick,
            onRetryRecentsClick,
            onRecentTokenClick,
        )
    }

    @Test
    fun `GIVEN factory WHEN create THEN within portfolio card has its title`() {
        // Act
        val actual = create()

        // Assert
        assertThat(actual.swapSection.withinPortfolio.title)
            .isEqualTo(resourceReference(R.string.tangempay_withdraw_hub_within_portfolio))
    }

    @Test
    fun `GIVEN factory WHEN create THEN bank cards titled and recents loading`() {
        // Act
        val actual = create()

        // Assert
        assertThat((actual.bankSection as BankSectionUM.Content).toPersonalAccount.title)
            .isEqualTo(resourceReference(R.string.tangempay_withdraw_hub_to_personal_account))
        assertThat((actual.bankSection as BankSectionUM.Content).toBusinessAccount.title)
            .isEqualTo(resourceReference(R.string.tangempay_withdraw_hub_to_business_account))
        assertThat((actual.bankSection as BankSectionUM.Content).recents).isEqualTo(RecentsUM.Loading)
    }

    @Test
    fun `GIVEN created state WHEN within portfolio clicked THEN callback invoked`() {
        // Arrange
        val state = create()

        // Act
        state.swapSection.withinPortfolio.onClick()

        // Assert
        verify(exactly = 1) { onWithinPortfolioClick() }
    }

    @Test
    fun `GIVEN created state WHEN back clicked THEN callback invoked`() {
        // Arrange
        val state = create()

        // Act
        state.onBackClick()

        // Assert
        verify(exactly = 1) { onBackClick() }
    }

    @Test
    fun `GIVEN created state WHEN to personal account clicked THEN callback invoked`() {
        // Arrange
        val state = create()

        // Act
        (state.bankSection as BankSectionUM.Content).toPersonalAccount.onClick()

        // Assert
        verify(exactly = 1) { onToPersonalAccountClick() }
    }

    @Test
    fun `GIVEN created state WHEN to business account clicked THEN callback invoked`() {
        // Arrange
        val state = create()

        // Act
        (state.bankSection as BankSectionUM.Content).toBusinessAccount.onClick()

        // Assert
        verify(exactly = 1) { onToBusinessAccountClick() }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN beneficiary WHEN createRecents THEN short name and initial`(model: NameModel) {
        // Arrange
        val account = bankAccount(id = "acc-1", beneficiary = model.beneficiary, accountNumber = "1234567890")

        // Act
        val actual = createRecents(accounts = listOf(account)) as RecentsUM.Content

        // Assert
        assertThat(actual.recipients.single()).isEqualTo(
            RecentRecipientUM(
                id = "acc-1",
                initial = model.expectedInitial,
                name = model.expectedName,
                accountNumber = "1234567890",
            ),
        )
    }

    @Test
    fun `GIVEN no saved accounts WHEN createRecents THEN empty`() {
        // Act
        val actual = createRecents(accounts = emptyList())

        // Assert
        assertThat(actual).isEqualTo(RecentsUM.Empty)
    }

    @Test
    fun `GIVEN every saved account shown WHEN createRecents THEN no see all`() {
        // Act
        val actual = createRecents(accounts = listOf(bankAccount()), hasMoreAccounts = false) as RecentsUM.Content

        // Assert
        assertThat(actual.onSeeAllClick).isNull()
    }

    @Test
    fun `GIVEN accounts WHEN createRecents THEN order preserved`() {
        // Arrange
        val accounts = listOf(
            bankAccount(id = "acc-2", beneficiary = Beneficiary.Business(name = "Raxanax")),
            bankAccount(id = "acc-1"),
        )

        // Act
        val actual = createRecents(accounts = accounts) as RecentsUM.Content

        // Assert
        assertThat(actual.recipients.map { it.id }).containsExactly("acc-2", "acc-1").inOrder()
    }

    @Test
    fun `GIVEN content WHEN recipient clicked THEN callback receives account id`() {
        // Arrange
        val content = createRecents(accounts = listOf(bankAccount(id = "acc-7"))) as RecentsUM.Content

        // Act
        content.onRecipientClick("acc-7")

        // Assert
        verify(exactly = 1) { onRecentRecipientClick("acc-7") }
    }

    @Test
    fun `GIVEN more saved accounts than shown WHEN see all clicked THEN callback invoked`() {
        // Arrange
        val content = createRecents(accounts = listOf(bankAccount()), hasMoreAccounts = true) as RecentsUM.Content

        // Act
        requireNotNull(content.onSeeAllClick).invoke()

        // Assert
        verify(exactly = 1) { onSeeAllClick() }
    }

    @Test
    fun `GIVEN error state WHEN retry clicked THEN callback invoked`() {
        // Arrange
        val error = createRecents(recents = RecentsState.Failed) as RecentsUM.Error

        // Act
        error.onRetryClick()

        // Assert
        verify(exactly = 1) { onRetryRecentsClick() }
    }

    @Test
    fun `GIVEN options loading WHEN create THEN bank section loading`() {
        // Act
        val actual = create(options = OptionsState.Loading)

        // Assert
        assertThat(actual.bankSection).isEqualTo(BankSectionUM.Loading)
    }

    @Test
    fun `GIVEN options ready WHEN create THEN bank section available`() {
        // Act
        val actual = create().bankSection as BankSectionUM.Content

        // Assert
        assertThat(actual.isAvailable).isTrue()
    }

    @Test
    fun `GIVEN options unavailable WHEN create THEN bank section unavailable and keeps its recents`() {
        // Arrange
        val recents = RecentsState.Loaded(accounts = listOf(bankAccount(id = "acc-1")), hasMoreAccounts = false)

        // Act
        val actual = create(options = OptionsState.Unavailable, recents = recents).bankSection as BankSectionUM.Content

        // Assert
        assertThat(actual.isAvailable).isFalse()
        assertThat((actual.recents as RecentsUM.Content).recipients.map { it.id }).containsExactly("acc-1")
    }

    @Test
    fun `GIVEN token recents loading WHEN create THEN swap recents loading`() {
        // Act
        val actual = create()

        // Assert
        assertThat(actual.swapSection.recents).isEqualTo(TokenRecentsUM.Loading)
    }

    @Test
    fun `GIVEN no token recents WHEN create THEN swap recents empty`() {
        // Act
        val actual = create(tokenRecents = TokenRecentsState.Loaded(transfers = emptyList()))

        // Assert
        assertThat(actual.swapSection.recents).isEqualTo(TokenRecentsUM.Empty)
    }

    @Test
    fun `GIVEN a recent transfer WHEN create THEN the tile carries the token and the account`() {
        // Arrange
        val currency = recentCurrency(name = "Polygon")
        val account = MockAccounts.createAccount(
            derivationIndex = 1,
            name = "Main account",
            userWalletId = MockAccounts.userWalletId,
            cryptoCurrencies = listOf(currency),
        )
        val state = TokenRecentsState.Loaded(
            transfers = listOf(RecentTokenTransfer(account = account, currency = currency)),
        )

        // Act
        val actual = create(tokenRecents = state).swapSection.recents as TokenRecentsUM.Content

        // Assert
        assertThat(actual.tokens.single()).isEqualTo(
            TangemPayWithdrawHubUM.RecentTokenUM(
                id = currency.id.value,
                icon = CryptoCurrencyToIconStateConverter(shouldGrayscaleTestnet = false).convert(currency),
                name = "Polygon",
                accountName = account.accountName.toUM().value,
                accountIcon = CryptoPortfolioIconConverter.convert(account.icon),
            ),
        )
    }

    private fun create(
        options: OptionsState = OptionsState.Ready(bankWithdrawOptions()),
        recents: RecentsState = RecentsState.Loading,
        tokenRecents: TokenRecentsState = TokenRecentsState.Loading,
    ): TangemPayWithdrawHubUM = factory.create(
        WithdrawHubState(options = options, recents = recents, tokenRecents = tokenRecents),
    )

    private fun createRecents(
        accounts: List<BankAccount> = emptyList(),
        hasMoreAccounts: Boolean = false,
        recents: RecentsState = RecentsState.Loaded(accounts = accounts, hasMoreAccounts = hasMoreAccounts),
    ): RecentsUM = (create(recents = recents).bankSection as BankSectionUM.Content).recents

    private fun recentCurrency(name: String): CryptoCurrency.Coin = CryptoCurrency.Coin(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(rawId = "polygon-pos"),
            suffix = CryptoCurrency.ID.Suffix.RawID(rawId = "matic-network"),
        ),
        network = Network(
            id = Network.ID(value = "polygon-pos", derivationPath = Network.DerivationPath.None),
            name = "Polygon",
            currencySymbol = "POL",
            derivationPath = Network.DerivationPath.None,
            isTestnet = false,
            standardType = Network.StandardType.ERC20,
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        ),
        name = name,
        symbol = "POL",
        decimals = 18,
        iconUrl = null,
        isCustom = false,
    )

    data class NameModel(
        val beneficiary: Beneficiary,
        val expectedName: String,
        val expectedInitial: String,
    )

    private fun provideTestModels() = listOf(
        NameModel(
            beneficiary = Beneficiary.Individual(firstName = "Andrei", lastName = "Melnikov"),
            expectedName = "Andrei M.",
            expectedInitial = "A",
        ),
        NameModel(
            beneficiary = Beneficiary.Individual(firstName = "Johnny", lastName = ""),
            expectedName = "Johnny",
            expectedInitial = "J",
        ),
        NameModel(
            beneficiary = Beneficiary.Individual(firstName = "Pablo", lastName = " escobar"),
            expectedName = "Pablo E.",
            expectedInitial = "P",
        ),
        NameModel(
            beneficiary = Beneficiary.Business(name = "raxanax LLC"),
            expectedName = "raxanax LLC",
            expectedInitial = "R",
        ),
    )
}