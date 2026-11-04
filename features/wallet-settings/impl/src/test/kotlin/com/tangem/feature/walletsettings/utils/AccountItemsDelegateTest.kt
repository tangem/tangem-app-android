package com.tangem.feature.walletsettings.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.walletsettings.component.WalletSettingsComponent
import com.tangem.feature.walletsettings.entity.WalletSettingsAccountsUM
import com.tangem.features.jointaccount.compromised.JointAccountCompromisedAlert
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** The accounts section of the wallet settings lists personal, joint and collectibles accounts in the document order */
internal class AccountItemsDelegateTest {

    private val userWalletId = MockAccounts.userWalletId
    private val wallet: UserWallet = mockk<UserWallet.Hot>()

    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val accountListSortingSaver: AccountListSortingSaver = mockk()
    private val jointAccountFeatureToggles: JointAccountFeatureToggles = mockk()

    private val main = MockAccounts.createAccount(derivationIndex = 0)
    private val savings = MockAccounts.createAccount(derivationIndex = 1)
    private val joint = MockAccounts.createJointAccount(ownerKeyIndex = 0)
    private val collectiblesAccount = Account.Collectibles(userWalletId = userWalletId)

    private val delegate = AccountItemsDelegate(
        paramsContainer = MutableParamsContainer(WalletSettingsComponent.Params(userWalletId = userWalletId)),
        messageSender = mockk<UiMessageSender>(),
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        getBalanceHidingSettingsUseCase = getBalanceHidingSettingsUseCase,
        getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
        accountListSortingSaver = accountListSortingSaver,
        analyticsEventHandler = mockk<AnalyticsEventHandler>(relaxed = true),
        jointAccountFeatureToggles = jointAccountFeatureToggles,
        jointAccountCompromisedAlert = mockk<JointAccountCompromisedAlert>(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            singleAccountStatusListSupplier,
            getBalanceHidingSettingsUseCase,
            getSelectedAppCurrencyUseCase,
            accountListSortingSaver,
            jointAccountFeatureToggles,
        )
        every { getBalanceHidingSettingsUseCase.isBalanceHidden() } returns flowOf(false)
        every { getSelectedAppCurrencyUseCase.invokeOrDefault() } returns flowOf(AppCurrency.Default)
        every { jointAccountFeatureToggles.isJointAccountEnabled } returns true
        every { accountListSortingSaver.accountsOrderFlow } returns MutableStateFlow(null)
    }

    @Test
    fun `GIVEN a joint account between personal ones and no saved order WHEN loadAccount THEN the document order is kept`() =
        runTest {
            // Arrange
            every { singleAccountStatusListSupplier(userWalletId) } returns flowOf(
                accountStatusList(personal(main), jointStatus(joint), personal(savings)),
            )

            // Act
            val actual = delegate.loadAccount(wallet, onAddAccountClick = { _, _ -> }, pushRoute = {}).first()

            // Assert
            assertThat(actual.accountIds()).containsExactly(
                main.accountId.value,
                joint.accountId.value,
                savings.accountId.value,
            ).inOrder()
        }

    @Test
    fun `GIVEN a saved order WHEN loadAccount THEN the rows follow it`() = runTest {
        // Arrange
        every { singleAccountStatusListSupplier(userWalletId) } returns flowOf(
            accountStatusList(personal(main), personal(savings), jointStatus(joint)),
        )
        every { accountListSortingSaver.accountsOrderFlow } returns
            MutableStateFlow<List<AccountId>?>(listOf(joint.accountId, main.accountId, savings.accountId))

        // Act
        val actual = delegate.loadAccount(wallet, onAddAccountClick = { _, _ -> }, pushRoute = {}).first()

        // Assert
        assertThat(actual.accountIds()).containsExactly(
            joint.accountId.value,
            main.accountId.value,
            savings.accountId.value,
        ).inOrder()
    }

    @Test
    fun `GIVEN a collectibles account WHEN loadAccount THEN it is listed in place and does not open`() = runTest {
        // Arrange
        every { singleAccountStatusListSupplier(userWalletId) } returns flowOf(
            accountStatusList(collectibles(CollectiblesAccountStatusValue.Loading), personal(main), personal(savings)),
        )

        // Act
        val actual = delegate.loadAccount(wallet, onAddAccountClick = { _, _ -> }, pushRoute = {}).first()

        // Assert
        assertThat(actual.accountIds()).containsExactly(
            collectiblesAccount.accountId.value,
            main.accountId.value,
            savings.accountId.value,
        ).inOrder()
        val row = actual.filterIsInstance<WalletSettingsAccountsUM.CollectiblesAccount>().single()
        assertThat(row.state.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN a collectibles account not onboarded WHEN loadAccount THEN it is not listed`() = runTest {
        // Arrange
        every { singleAccountStatusListSupplier(userWalletId) } returns flowOf(
            accountStatusList(collectibles(CollectiblesAccountStatusValue.NotOnboarded), personal(main)),
        )

        // Act
        val actual = delegate.loadAccount(wallet, onAddAccountClick = { _, _ -> }, pushRoute = {}).first()

        // Assert
        assertThat(actual.accountIds()).containsExactly(main.accountId.value)
    }

    private fun List<WalletSettingsAccountsUM>.accountIds(): List<String> = mapNotNull { item ->
        when (item) {
            is WalletSettingsAccountsUM.Account -> item.id
            is WalletSettingsAccountsUM.JointAccount -> item.id
            is WalletSettingsAccountsUM.CollectiblesAccount -> item.id
            is WalletSettingsAccountsUM.Header,
            is WalletSettingsAccountsUM.Footer,
            -> null
        }
    }

    private fun accountStatusList(vararg statuses: AccountStatus): AccountStatusList = AccountStatusList(
        userWalletId = userWalletId,
        accountStatuses = statuses.toList(),
        totalAccounts = statuses.count { it is AccountStatus.Personal },
        totalArchivedAccounts = 0,
        totalFiatBalance = TotalFiatBalance.Loading,
        sortType = TokensSortType.NONE,
        groupType = TokensGroupType.NONE,
    )

    private fun personal(account: Account.Personal) = AccountStatus.Personal(
        account = account,
        tokenList = TokenList.Empty,
        priceChangeLce = lceLoading(),
    )

    private fun collectibles(value: CollectiblesAccountStatusValue) = AccountStatus.Collectibles(
        account = collectiblesAccount,
        value = value,
    )

    private fun jointStatus(account: Account.Joint) = AccountStatus.Joint(
        account = account,
        value = JointAccountStatusValue.Active(
            source = StatusSource.ACTUAL,
            totalMembers = 2,
            safeAddress = "0x5afe",
            threshold = 2,
        ),
        tokenList = TokenList.Empty,
        priceChangeLce = lceLoading(),
    )
}