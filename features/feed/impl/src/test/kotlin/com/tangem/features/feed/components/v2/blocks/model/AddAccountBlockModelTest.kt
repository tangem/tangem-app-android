package com.tangem.features.feed.components.v2.blocks.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.extension.isAccountsSupported
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.feed.components.v2.blocks.AddAccountBlockComponent.BottomSheetConfig
import com.tangem.features.feed.components.v2.createTestingCoroutineDispatcherProvider
import com.tangem.features.feed.components.v2.trackSlot
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class AddAccountBlockModelTest {

    private val router: Router = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val jointAccountFeatureToggles: JointAccountFeatureToggles = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val multiAccountListSupplier: MultiAccountListSupplier = mockk()
    private val portfolioSelectorController: PortfolioSelectorController = mockk(relaxed = true)
    private val portfolioFetcherFactory: PortfolioFetcher.Factory = mockk()

    private val walletAId = UserWalletId("aa")
    private val walletBId = UserWalletId("bb")
    private val accountAId: AccountId = mockk()
    private val accountBId: AccountId = mockk()

    private lateinit var wallets: MutableStateFlow<List<UserWallet>?>
    private lateinit var accountLists: MutableStateFlow<List<AccountList>>
    private lateinit var selectedAccounts: MutableStateFlow<Set<AccountId>>

    @BeforeEach
    fun setUp() {
        clearMocks(
            router,
            messageSender,
            jointAccountFeatureToggles,
            userWalletsListRepository,
            multiAccountListSupplier,
            portfolioSelectorController,
            portfolioFetcherFactory,
        )
        mockkStatic(UserWallet::isAccountsSupported)
        wallets = MutableStateFlow(emptyList())
        accountLists = MutableStateFlow(emptyList())
        selectedAccounts = MutableStateFlow(emptySet())
        every { userWalletsListRepository.userWallets } returns wallets
        every { multiAccountListSupplier() } returns accountLists
        every { portfolioSelectorController.selectedAccounts } returns selectedAccounts
        every { portfolioFetcherFactory.create(any(), any()) } returns mockk(relaxed = true)
        every { jointAccountFeatureToggles.isJointAccountEnabled } returns false
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(UserWallet::isAccountsSupported)
    }

    @Test
    fun `GIVEN no wallet can hold accounts WHEN the model is created THEN the block is hidden`() = runTest {
        // Arrange
        wallets.value = listOf(wallet(walletAId, isAccountsSupported = false))

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.isVisible.value).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN every accounts-capable wallet is locked WHEN the model is created THEN the block is hidden`() =
        runTest {
            // Arrange
            wallets.value = listOf(wallet(walletAId, isLocked = true))

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.isVisible.value).isFalse()
            model.onDestroy()
        }

    @Test
    fun `GIVEN an unlocked accounts-capable wallet WHEN the model is created THEN the block is shown`() = runTest {
        // Arrange
        wallets.value = listOf(wallet(walletAId))
        accountLists.value = listOf(accountList(walletAId))

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.isVisible.value).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN the account lists have not loaded WHEN the model is created THEN the block is hidden`() = runTest {
        // Arrange — the cap is unknown, so the card would be a tap that does nothing
        wallets.value = listOf(wallet(walletAId))

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.isVisible.value).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN an account list arrives late WHEN it is emitted THEN the block becomes shown`() = runTest {
        // Arrange
        wallets.value = listOf(wallet(walletAId))
        val model = createModel(testScope = this)
        advanceUntilIdle()
        assertThat(model.isVisible.value).isFalse()

        // Act
        accountLists.value = listOf(accountList(walletAId))
        advanceUntilIdle()

        // Assert
        assertThat(model.isVisible.value).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN accounts are not loaded yet WHEN the block is tapped THEN nothing happens`() = runTest {
        // Arrange — the cap is unknown, and claiming a limit here would be a lie
        wallets.value = listOf(wallet(walletAId))
        val model = createModel(testScope = this)
        val openedSheets = model.bottomSheetNavigation.trackSlot()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
        verify(exactly = 0) { messageSender.send(message = any()) }
        assertThat(openedSheets).isEmpty()
        model.onDestroy()
    }

    @Test
    fun `GIVEN joint accounts are off AND one wallet has room WHEN the block is tapped THEN creation opens`() =
        runTest {
            // Arrange
            wallets.value = listOf(wallet(walletAId))
            accountLists.value = listOf(accountList(walletAId, canAddCrypto = true))
            val model = createModel(testScope = this)
            val openedSheets = model.bottomSheetNavigation.trackSlot()
            advanceUntilIdle()

            // Act
            model.onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                router.push(route = AppRoute.CreateAccount(userWalletId = walletAId), onComplete = any())
            }
            // the path dismisses before navigating, which is a no-op on an empty slot — but no sheet opens
            assertThat(openedSheets.filterNotNull()).isEmpty()
            model.onDestroy()
        }

    @Test
    fun `GIVEN joint accounts are on AND one wallet has room WHEN the block is tapped THEN the type sheet opens`() =
        runTest {
            // Arrange
            every { jointAccountFeatureToggles.isJointAccountEnabled } returns true
            wallets.value = listOf(wallet(walletAId))
            accountLists.value = listOf(accountList(walletAId, canAddCrypto = true, canAddJoint = false))
            val model = createModel(testScope = this)
            val openedSheets = model.bottomSheetNavigation.trackSlot()
            advanceUntilIdle()

            // Act
            model.onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
            assertThat(openedSheets).containsExactly(
                BottomSheetConfig.AccountType(
                    userWalletId = walletAId,
                    isCryptoAccountLimitReached = false,
                    isJointAccountLimitReached = true,
                ),
            )
            model.onDestroy()
        }

    @Test
    fun `GIVEN a wallet at the crypto cap that can take a joint account WHEN tapped THEN it still qualifies`() =
        runTest {
            // Arrange
            every { jointAccountFeatureToggles.isJointAccountEnabled } returns true
            wallets.value = listOf(wallet(walletAId))
            accountLists.value = listOf(accountList(walletAId, canAddCrypto = false, canAddJoint = true))
            val model = createModel(testScope = this)
            val openedSheets = model.bottomSheetNavigation.trackSlot()
            advanceUntilIdle()

            // Act
            model.onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { messageSender.send(message = any()) }
            assertThat(openedSheets).containsExactly(
                BottomSheetConfig.AccountType(
                    userWalletId = walletAId,
                    isCryptoAccountLimitReached = true,
                    isJointAccountLimitReached = false,
                ),
            )
            model.onDestroy()
        }

    @Test
    fun `GIVEN joint accounts are off AND the only wallet is at the crypto cap WHEN tapped THEN the limit shows`() =
        runTest {
            // Arrange
            wallets.value = listOf(wallet(walletAId))
            accountLists.value = listOf(accountList(walletAId, canAddCrypto = false, canAddJoint = true))
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { messageSender.send(message = any()) }
            verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN several wallets have room WHEN the block is tapped THEN the wallet chooser opens`() = runTest {
        // Arrange
        wallets.value = listOf(wallet(walletAId), wallet(walletBId))
        accountLists.value = listOf(
            accountList(walletAId, canAddCrypto = true),
            accountList(walletBId, canAddCrypto = true),
        )
        val model = createModel(testScope = this)
        val openedSheets = model.bottomSheetNavigation.trackSlot()
        advanceUntilIdle()

        // Act
        model.onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
        assertThat(openedSheets).containsExactly(BottomSheetConfig.WalletSelector)
        model.onDestroy()
    }

    @Test
    fun `GIVEN several wallets but only one with room WHEN the block is tapped THEN creation opens for it`() =
        runTest {
            // Arrange
            wallets.value = listOf(wallet(walletAId), wallet(walletBId))
            accountLists.value = listOf(
                accountList(walletAId, canAddCrypto = false, canAddJoint = false),
                accountList(walletBId, canAddCrypto = true),
            )
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                router.push(route = AppRoute.CreateAccount(userWalletId = walletBId), onComplete = any())
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN the chooser is open WHEN a wallet is picked THEN creation opens for that wallet`() = runTest {
        // Arrange
        wallets.value = listOf(wallet(walletAId), wallet(walletBId))
        accountLists.value = listOf(
            accountList(walletAId, canAddCrypto = true, accountId = accountAId),
            accountList(walletBId, canAddCrypto = true, accountId = accountBId),
        )
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onClick()
        advanceUntilIdle()

        // Act
        selectedAccounts.value = setOf(accountBId)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            router.push(route = AppRoute.CreateAccount(userWalletId = walletBId), onComplete = any())
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the type sheet replaced the chooser WHEN the chooser reports dismiss THEN the type sheet stays`() =
        runTest {
            // Arrange
            every { jointAccountFeatureToggles.isJointAccountEnabled } returns true
            wallets.value = listOf(wallet(walletAId), wallet(walletBId))
            accountLists.value = listOf(
                accountList(walletAId, canAddCrypto = true, accountId = accountAId),
                accountList(walletBId, canAddCrypto = true, accountId = accountBId),
            )
            val model = createModel(testScope = this)
            val openedSheets = model.bottomSheetNavigation.trackSlot()
            advanceUntilIdle()
            model.onClick()
            advanceUntilIdle()
            selectedAccounts.value = setOf(accountBId)
            advanceUntilIdle()

            // Act
            model.portfolioSelectorCallback.onDismiss()
            model.portfolioSelectorCallback.onBack()

            // Assert
            assertThat(openedSheets.last()).isInstanceOf(BottomSheetConfig.AccountType::class.java)
            model.onDestroy()
        }

    @Test
    fun `GIVEN the chooser is open WHEN it is dismissed THEN the slot is cleared`() = runTest {
        // Arrange
        wallets.value = listOf(wallet(walletAId), wallet(walletBId))
        accountLists.value = listOf(
            accountList(walletAId, canAddCrypto = true),
            accountList(walletBId, canAddCrypto = true),
        )
        val model = createModel(testScope = this)
        val openedSheets = model.bottomSheetNavigation.trackSlot()
        advanceUntilIdle()
        model.onClick()
        advanceUntilIdle()

        // Act
        model.portfolioSelectorCallback.onDismiss()

        // Assert
        assertThat(openedSheets).containsExactly(BottomSheetConfig.WalletSelector, null).inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN the crypto limit is reached WHEN the option is tapped THEN the limit dialog shows`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()
            val config = BottomSheetConfig.AccountType(
                userWalletId = walletAId,
                isCryptoAccountLimitReached = true,
                isJointAccountLimitReached = false,
            )

            // Act
            model.onCryptoAccountClick(config)

            // Assert
            verify(exactly = 1) { messageSender.send(message = any()) }
            verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN the joint limit is not reached WHEN the option is tapped THEN joint creation opens`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        val config = BottomSheetConfig.AccountType(
            userWalletId = walletAId,
            isCryptoAccountLimitReached = true,
            isJointAccountLimitReached = false,
        )

        // Act
        model.onJointAccountClick(config)

        // Assert
        verify(exactly = 1) {
            router.push(route = AppRoute.JointAccountCreation(userWalletId = walletAId), onComplete = any())
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the joint limit is reached WHEN the option is tapped THEN the limit dialog shows`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        val config = BottomSheetConfig.AccountType(
            userWalletId = walletAId,
            isCryptoAccountLimitReached = false,
            isJointAccountLimitReached = true,
        )

        // Act
        model.onJointAccountClick(config)

        // Assert
        verify(exactly = 1) { messageSender.send(message = any()) }
        verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
        model.onDestroy()
    }

    private fun createModel(testScope: TestScope): AddAccountBlockModel = AddAccountBlockModel(
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        router = router,
        messageSender = messageSender,
        jointAccountFeatureToggles = jointAccountFeatureToggles,
        userWalletsListRepository = userWalletsListRepository,
        multiAccountListSupplier = multiAccountListSupplier,
        portfolioFetcherFactory = portfolioFetcherFactory,
        portfolioSelectorController = portfolioSelectorController,
    )

    private fun wallet(
        id: UserWalletId,
        isLocked: Boolean = false,
        isAccountsSupported: Boolean = true,
    ): UserWallet = mockk<UserWallet.Hot> {
        every { walletId } returns id
        every { this@mockk.isLocked } returns isLocked
        every { this@mockk.isAccountsSupported } returns isAccountsSupported
    }

    private fun accountList(
        userWalletId: UserWalletId,
        canAddCrypto: Boolean = true,
        canAddJoint: Boolean = false,
        accountId: AccountId? = null,
    ): AccountList = mockk {
        every { this@mockk.userWalletId } returns userWalletId
        every { canAddMoreCryptoAccounts } returns canAddCrypto
        every { canAddMoreJointAccounts } returns canAddJoint
        every { accounts } returns listOfNotNull(
            accountId?.let { id -> mockk<Account.Personal> { every { this@mockk.accountId } returns id } },
        )
    }
}