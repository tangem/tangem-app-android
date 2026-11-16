package com.tangem.features.commonfeatures.impl.portfolioselector

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.commonfeatures.api.portfolioselector.AccountWalletPicker.Pick
import com.tangem.features.commonfeatures.api.portfolioselector.AccountWalletPicker.Resolution
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.commonfeatures.impl.R
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultAccountWalletPickerTest {

    private val getWalletsUseCase: GetWalletsUseCase = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val portfolioFetcherFactory: PortfolioFetcher.Factory = mockk()
    private val portfolioSelectorController: PortfolioSelectorController = mockk()

    private val selectedAccounts = MutableSharedFlow<Set<AccountId>>()
    private val isEnabled = MutableStateFlow<(UserWallet, AccountStatus) -> Boolean>({ _, _ -> true })
    private val walletBadge = MutableStateFlow<(UserWallet) -> TextReference?>({ null })

    @BeforeEach
    fun resetMocks() {
        clearMocks(getWalletsUseCase, singleAccountListSupplier, portfolioFetcherFactory, portfolioSelectorController)
        every { portfolioSelectorController.selectedAccounts } returns selectedAccounts
        every { portfolioSelectorController.isEnabled } returns isEnabled
        every { portfolioSelectorController.walletBadge } returns walletBadge
        every { getWalletsUseCase() } returns flowOf(listOf(hotWallet(WALLET_A), hotWallet(WALLET_B)))
    }

    @Test
    fun `GIVEN only one wallet with room WHEN resolve THEN single`() = runTest {
        // Arrange
        every { getWalletsUseCase() } returns flowOf(listOf(hotWallet(WALLET_A)))
        stubAccountLists(accountList(WALLET_A, activeAccounts = 1))

        // Act
        val resolution = createPicker().resolve(hasRoom = AccountList::canAddMoreCryptoAccounts)

        // Assert
        assertThat(resolution).isEqualTo(Resolution.Single(WALLET_A))
    }

    @Test
    fun `GIVEN only one wallet at the cap WHEN resolve THEN limit reached`() = runTest {
        // Arrange
        every { getWalletsUseCase() } returns flowOf(listOf(hotWallet(WALLET_A)))
        stubAccountLists(accountList(WALLET_A, activeAccounts = 20))

        // Act
        val resolution = createPicker().resolve(hasRoom = AccountList::canAddMoreCryptoAccounts)

        // Assert
        assertThat(resolution).isEqualTo(Resolution.LimitReached)
    }

    @Test
    fun `GIVEN several wallets WHEN resolve THEN choose with the full ones`() = runTest {
        // Arrange
        stubAccountLists(accountList(WALLET_A, activeAccounts = 1), accountList(WALLET_B, activeAccounts = 20))

        // Act
        val resolution = createPicker().resolve(hasRoom = AccountList::canAddMoreCryptoAccounts)

        // Assert
        assertThat(resolution).isEqualTo(Resolution.Choose(fullWalletIds = setOf(WALLET_B)))
    }

    @Test
    fun `GIVEN one wallet list never loads WHEN resolve THEN the loaded wallet is used after the timeout`() = runTest {
        // Arrange
        stubAccountLists(accountList(WALLET_A, activeAccounts = 1))
        every { singleAccountListSupplier(WALLET_B) } returns emptyFlow<AccountList>()

        // Act
        val resolution = createPicker().resolve(hasRoom = AccountList::canAddMoreCryptoAccounts)

        // Assert
        assertThat(resolution).isEqualTo(Resolution.Single(WALLET_A))
    }

    @Test
    fun `GIVEN no wallet list loads WHEN resolve THEN unavailable`() = runTest {
        // Arrange
        every { singleAccountListSupplier(any<UserWalletId>()) } returns emptyFlow<AccountList>()

        // Act
        val resolution = createPicker().resolve(hasRoom = AccountList::canAddMoreCryptoAccounts)

        // Assert
        assertThat(resolution).isEqualTo(Resolution.Unavailable)
    }

    @Test
    fun `GIVEN full wallet marked WHEN chooser opened THEN only it gets the full badge`() = runTest {
        // Arrange
        stubAccountLists(accountList(WALLET_A, activeAccounts = 1), accountList(WALLET_B, activeAccounts = 20))
        val picker = createPicker()
        picker.resolve(hasRoom = AccountList::canAddMoreCryptoAccounts)

        // Act
        picker.openChooser(fullWalletIds = setOf(WALLET_B))

        // Assert
        val status: AccountStatus = mockk()
        assertThat(isEnabled.value(hotWallet(WALLET_A), status)).isTrue()
        assertThat(isEnabled.value(hotWallet(WALLET_B), status)).isTrue()
        assertThat(walletBadge.value(hotWallet(WALLET_A))).isNull()
        val fullBadge = resourceReference(R.string.account_add_limit_badge)
        assertThat(walletBadge.value(hotWallet(WALLET_B))).isEqualTo(fullBadge)
    }

    @Test
    fun `GIVEN a wallet whose list never loaded WHEN chooser opened THEN it is disabled`() = runTest {
        // Arrange
        stubAccountLists(accountList(WALLET_A, activeAccounts = 1))
        every { singleAccountListSupplier(WALLET_B) } returns emptyFlow<AccountList>()
        val picker = createPicker()
        picker.resolve(hasRoom = AccountList::canAddMoreCryptoAccounts)

        // Act
        picker.openChooser(fullWalletIds = emptySet())

        // Assert
        assertThat(isEnabled.value(hotWallet(WALLET_B), mockk())).isFalse()
    }

    @Test
    fun `GIVEN chooser open WHEN wallets picked THEN full one reports full and the other its id`() = runTest {
        // Arrange
        stubAccountLists(accountList(WALLET_A, activeAccounts = 1), accountList(WALLET_B, activeAccounts = 20))
        val picker = createPicker()
        picker.resolve(hasRoom = AccountList::canAddMoreCryptoAccounts)
        picker.openChooser(fullWalletIds = setOf(WALLET_B))

        // Act
        val fullPick = async { picker.picks.first() }
        advanceUntilIdle()
        selectedAccounts.emit(setOf(Account.Personal.createMainAccount(WALLET_B).accountId))
        val walletPick = async { picker.picks.first() }
        advanceUntilIdle()
        selectedAccounts.emit(setOf(Account.Personal.createMainAccount(WALLET_A).accountId))

        // Assert
        assertThat(fullPick.await()).isEqualTo(Pick.Full)
        assertThat(walletPick.await()).isEqualTo(Pick.Wallet(WALLET_A))
    }

    @Test
    fun `GIVEN picker created WHEN nothing used THEN no wallet data read`() = runTest {
        // Act
        createPicker()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { getWalletsUseCase() }
        verify(exactly = 0) { singleAccountListSupplier(any<UserWalletId>()) }
        verify(exactly = 0) { portfolioFetcherFactory.create(any(), any()) }
    }

    private fun stubAccountLists(vararg accountLists: AccountList) {
        accountLists.forEach { list -> every { singleAccountListSupplier(list.userWalletId) } returns flowOf(list) }
    }

    private fun accountList(walletId: UserWalletId, activeAccounts: Int) =
        MockAccounts.createAccountList(activeAccounts = activeAccounts, userWalletId = walletId)

    private fun TestScope.createPicker() = DefaultAccountWalletPicker(
        scope = backgroundScope,
        getWalletsUseCase = getWalletsUseCase,
        singleAccountListSupplier = singleAccountListSupplier,
        portfolioFetcherFactory = portfolioFetcherFactory,
        portfolioSelectorController = portfolioSelectorController,
    )

    private fun hotWallet(walletId: UserWalletId) = UserWallet.Hot(
        name = "Hot",
        walletId = walletId,
        hotWalletId = HotWalletId(value = "hot", authType = HotWalletId.AuthType.NoPassword),
        wallets = emptyList(),
        isBackedUp = false,
    )

    private companion object {
        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("022")
    }
}