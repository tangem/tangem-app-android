package com.tangem.features.jointaccount.deeplink

import arrow.core.Either
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.DeepLinkRoute
import com.tangem.common.routing.deeplink.DeeplinkConst.ACCOUNT_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.ENTRY_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.WALLET_ID_KEY
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.account.status.utils.MainExpandedAccountsHolder
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.ResolveAndSelectUserWalletUseCase
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import com.tangem.features.wallet.deeplink.WalletDeepLinkActionTrigger
import com.tangem.test.mock.MockAccounts
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultJointAccountDeepLinkHandlerTest {

    private val appRouter: AppRouter = mockk()
    private val resolveAndSelectUserWalletUseCase: ResolveAndSelectUserWalletUseCase = mockk()
    private val singleAccountSupplier: SingleAccountSupplier = mockk()
    private val singleAccountListFetcher: SingleAccountListFetcher = mockk()
    private val jointAccountListFetcher: SingleJointAccountListFetcher = mockk(relaxed = true)
    private val expandedAccountsHolder: MainExpandedAccountsHolder = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk()
    private val walletDeepLinkActionTrigger: WalletDeepLinkActionTrigger = mockk()
    private val jointAccountFeatureToggles: JointAccountFeatureToggles = mockk()

    private val userWalletId = UserWalletId("011")

    /** A well-formed (64-char hex) account id that never matches any created account. */
    private val notFoundAccountId = "f".repeat(64)

    @BeforeEach
    fun setUp() {
        every { jointAccountFeatureToggles.isJointAccountEnabled } returns true
    }

    @Test
    fun `GIVEN entry members WHEN handle deeplink THEN push JointAccountMembers`() = runTest {
        val jointAccount = MockAccounts.createJointAccount(userWalletId = userWalletId)
        mockUserWallet(userWalletId)
        every { appRouter.push(any(), any()) } answers {
            secondArg<(Boolean) -> Unit>().invoke(true)
        }
        every { appRouter.popTo(any<AppRoute>(), any()) } just Runs
        every { analyticsEventHandler.send(any()) } just Runs
        every { walletDeepLinkActionTrigger.selectWallet(any()) } just Runs
        every { singleAccountSupplier.filterJointAccount(jointAccount.accountId) } returns flowOf(jointAccount)

        createHandler(
            scope = this,
            queryParams = mapOf(
                WALLET_ID_KEY to "011",
                ACCOUNT_ID_KEY to jointAccount.accountId.value,
                ENTRY_KEY to DeepLinkRoute.JointAccount.ENTRY_MEMBERS,
            ),
        )
        advanceUntilIdle()

        verify {
            appRouter.push(
                route = AppRoute.JointAccountMembers(
                    userWalletId = userWalletId,
                    cryptoAccountId = jointAccount.accountId.value,
                ),
                onComplete = any(),
            )
        }
        verify { walletDeepLinkActionTrigger.selectWallet(userWalletId) }
    }

    @Test
    fun `GIVEN entry overview WHEN handle deeplink THEN expand account and pop to Wallet`() = runTest {
        val jointAccount = MockAccounts.createJointAccount(userWalletId = userWalletId)
        mockUserWallet(userWalletId)
        every { appRouter.popTo(any<AppRoute>(), any()) } answers {
            secondArg<(Boolean) -> Unit>().invoke(true)
        }
        every { analyticsEventHandler.send(any()) } just Runs
        every { expandedAccountsHolder.expandAccount(any()) } just Runs
        every { walletDeepLinkActionTrigger.selectWallet(any()) } just Runs
        every { singleAccountSupplier.filterJointAccount(jointAccount.accountId) } returns flowOf(jointAccount)

        createHandler(
            scope = this,
            queryParams = mapOf(
                WALLET_ID_KEY to "011",
                ACCOUNT_ID_KEY to jointAccount.accountId.value,
                ENTRY_KEY to DeepLinkRoute.JointAccount.ENTRY_OVERVIEW,
            ),
        )
        advanceUntilIdle()

        verify { appRouter.popTo(route = AppRoute.Wallet, onComplete = any()) }
        verify { expandedAccountsHolder.expandAccount(jointAccount.accountId) }
        verify { walletDeepLinkActionTrigger.selectWallet(userWalletId) }
    }

    @Test
    fun `GIVEN entry overview AND pop fails WHEN handle deeplink THEN account is not expanded`() = runTest {
        val jointAccount = MockAccounts.createJointAccount(userWalletId = userWalletId)
        mockUserWallet(userWalletId)
        every { appRouter.popTo(any<AppRoute>(), any()) } answers {
            secondArg<(Boolean) -> Unit>().invoke(false)
        }
        every { analyticsEventHandler.send(any()) } just Runs
        every { singleAccountSupplier.filterJointAccount(jointAccount.accountId) } returns flowOf(jointAccount)

        createHandler(
            scope = this,
            queryParams = mapOf(
                WALLET_ID_KEY to "011",
                ACCOUNT_ID_KEY to jointAccount.accountId.value,
                ENTRY_KEY to DeepLinkRoute.JointAccount.ENTRY_OVERVIEW,
            ),
        )
        advanceUntilIdle()

        verify(exactly = 0) { expandedAccountsHolder.expandAccount(any()) }
        verify(exactly = 0) { walletDeepLinkActionTrigger.selectWallet(any()) }
    }

    @Test
    fun `GIVEN unknown account_id WHEN handle deeplink THEN pop to Wallet`() = runTest {
        mockUserWallet(userWalletId)
        every { appRouter.popTo(any<AppRoute>(), any()) } just Runs
        every { singleAccountSupplier.filterJointAccount(any<AccountId>()) } returns emptyFlow()

        createHandler(
            scope = this,
            queryParams = mapOf(
                WALLET_ID_KEY to "011",
                ACCOUNT_ID_KEY to notFoundAccountId,
                ENTRY_KEY to DeepLinkRoute.JointAccount.ENTRY_OVERVIEW,
            ),
        )
        advanceUntilIdle()

        verify { appRouter.popTo(route = AppRoute.Wallet, onComplete = any()) }
        verify(exactly = 0) { expandedAccountsHolder.expandAccount(any()) }
        verify(exactly = 0) { walletDeepLinkActionTrigger.selectWallet(any()) }
    }

    @Test
    fun `GIVEN unknown entry WHEN handle deeplink THEN analytics is not sent`() = runTest {
        // Arrange
        val jointAccount = MockAccounts.createJointAccount(userWalletId = userWalletId)
        mockUserWallet(userWalletId)
        every { singleAccountSupplier.filterJointAccount(jointAccount.accountId) } returns flowOf(jointAccount)

        // Act
        createHandler(
            scope = this,
            queryParams = mapOf(
                WALLET_ID_KEY to "011",
                ACCOUNT_ID_KEY to jointAccount.accountId.value,
                ENTRY_KEY to "unknown_entry",
            ),
        )
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
        verify(exactly = 0) { appRouter.push(any(), any()) }
        verify(exactly = 0) { appRouter.popTo(any<AppRoute>(), any()) }
    }

    @Test
    fun `GIVEN cold start AND account missing WHEN handle deeplink THEN redirect to main without refreshing`() =
        runTest {
            // Arrange
            mockUserWallet(userWalletId)
            every { appRouter.popTo(any<AppRoute>(), any()) } just Runs
            every {
                singleAccountSupplier.filterJointAccount(any<AccountId>())
            } returns emptyFlow()

            // Act
            createHandler(
                scope = this,
                queryParams = mapOf(
                    WALLET_ID_KEY to "011",
                    ACCOUNT_ID_KEY to notFoundAccountId,
                    ENTRY_KEY to DeepLinkRoute.JointAccount.ENTRY_OVERVIEW,
                ),
                isFromOnNewIntent = false,
            )
            advanceUntilIdle()

            // Assert
            verify { appRouter.popTo(route = AppRoute.Wallet, onComplete = any()) }
            coVerify(exactly = 0) { singleAccountListFetcher.invoke(any()) }
            coVerify(exactly = 0) { jointAccountListFetcher.invoke(params = any()) }
        }

    @Test
    fun `GIVEN account appears only after refresh WHEN handle deeplink THEN expand account`() = runTest {
        // Arrange
        val jointAccount = MockAccounts.createJointAccount(userWalletId = userWalletId)
        mockUserWallet(userWalletId)
        every { appRouter.popTo(any<AppRoute>(), any()) } answers {
            secondArg<(Boolean) -> Unit>().invoke(true)
        }
        every { analyticsEventHandler.send(any()) } just Runs
        every { expandedAccountsHolder.expandAccount(any()) } just Runs
        every { walletDeepLinkActionTrigger.selectWallet(any()) } just Runs

        val accountFlow = MutableSharedFlow<Account.Joint>(replay = 1)
        every { singleAccountSupplier.filterJointAccount(jointAccount.accountId) } returns accountFlow
        coEvery { singleAccountListFetcher.invoke(SingleAccountListFetcher.Params(userWalletId)) } coAnswers {
            accountFlow.emit(jointAccount)
            Either.Right(Unit)
        }

        // Act
        createHandler(
            scope = this,
            queryParams = mapOf(
                WALLET_ID_KEY to "011",
                ACCOUNT_ID_KEY to jointAccount.accountId.value,
                ENTRY_KEY to DeepLinkRoute.JointAccount.ENTRY_OVERVIEW,
            ),
            isFromOnNewIntent = true,
        )
        advanceUntilIdle()

        // Assert
        verify { expandedAccountsHolder.expandAccount(jointAccount.accountId) }
    }

    /**
     * The push announces a changed composition, so the joint list is refreshed next to the account list — and
     * a failure there must not cost the user the navigation the tap asked for.
     */
    @Test
    fun `GIVEN the joint refresh fails WHEN handle deeplink THEN it ran for the resolved wallet and the account opens`() =
        runTest {
            // Arrange
            val jointAccount = MockAccounts.createJointAccount(userWalletId = userWalletId)
            mockUserWallet(userWalletId)
            every { appRouter.popTo(any<AppRoute>(), any()) } answers {
                secondArg<(Boolean) -> Unit>().invoke(true)
            }
            every { analyticsEventHandler.send(any()) } just Runs
            every { expandedAccountsHolder.expandAccount(any()) } just Runs
            every { walletDeepLinkActionTrigger.selectWallet(any()) } just Runs
            every { singleAccountSupplier.filterJointAccount(jointAccount.accountId) } returns flowOf(jointAccount)
            coEvery { singleAccountListFetcher.invoke(any()) } returns Either.Right(Unit)
            coEvery { jointAccountListFetcher.invoke(params = any()) } returns
                Either.Left(IllegalStateException("service unavailable"))

            // Act
            createHandler(
                scope = this,
                queryParams = mapOf(
                    WALLET_ID_KEY to "011",
                    ACCOUNT_ID_KEY to jointAccount.accountId.value,
                    ENTRY_KEY to DeepLinkRoute.JointAccount.ENTRY_OVERVIEW,
                ),
                isFromOnNewIntent = true,
            )
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                jointAccountListFetcher.invoke(params = SingleJointAccountListFetcher.Params(userWalletId))
            }
            verify { expandedAccountsHolder.expandAccount(jointAccount.accountId) }
        }

    @Test
    fun `GIVEN refresh failed AND account missing WHEN handle deeplink THEN redirect to main`() = runTest {
        // Arrange
        mockUserWallet(userWalletId)
        every { appRouter.popTo(any<AppRoute>(), any()) } just Runs
        every { singleAccountSupplier.filterJointAccount(any<AccountId>()) } returns emptyFlow()
        coEvery {
            singleAccountListFetcher.invoke(SingleAccountListFetcher.Params(userWalletId))
        } returns Either.Left(IllegalStateException("service unavailable"))

        // Act
        createHandler(
            scope = this,
            queryParams = mapOf(
                WALLET_ID_KEY to "011",
                ACCOUNT_ID_KEY to notFoundAccountId,
                ENTRY_KEY to DeepLinkRoute.JointAccount.ENTRY_OVERVIEW,
            ),
            isFromOnNewIntent = true,
        )
        advanceUntilIdle()

        // Assert
        verify { appRouter.popTo(route = AppRoute.Wallet, onComplete = any()) }
    }

    @Test
    fun `GIVEN feature toggle disabled WHEN handle deeplink THEN redirect to main without resolving wallet`() =
        runTest {
            // Arrange
            every { jointAccountFeatureToggles.isJointAccountEnabled } returns false
            every { appRouter.popTo(any<AppRoute>(), any()) } just Runs

            // Act
            createHandler(
                scope = this,
                queryParams = mapOf(
                    WALLET_ID_KEY to "011",
                    ACCOUNT_ID_KEY to "account123",
                    ENTRY_KEY to DeepLinkRoute.JointAccount.ENTRY_OVERVIEW,
                ),
            )
            advanceUntilIdle()

            // Assert
            verify { appRouter.popTo(route = AppRoute.Wallet, onComplete = any()) }
            coVerify(exactly = 0) { resolveAndSelectUserWalletUseCase(any()) }
        }

    private fun mockUserWallet(userWalletId: UserWalletId) {
        val userWallet: UserWallet = mockk { every { walletId } returns userWalletId }
        coEvery { resolveAndSelectUserWalletUseCase(userWalletId) } returns userWallet
    }

    private fun createHandler(
        scope: CoroutineScope,
        queryParams: Map<String, String>,
        isFromOnNewIntent: Boolean = false,
    ) {
        DefaultJointAccountDeepLinkHandler(
            scope = scope,
            queryParams = queryParams,
            isFromOnNewIntent = isFromOnNewIntent,
            appRouter = appRouter,
            resolveAndSelectUserWalletUseCase = resolveAndSelectUserWalletUseCase,
            singleAccountSupplier = singleAccountSupplier,
            singleAccountListFetcher = singleAccountListFetcher,
            jointAccountListFetcher = jointAccountListFetcher,
            expandedAccountsHolder = expandedAccountsHolder,
            analyticsEventHandler = analyticsEventHandler,
            walletDeepLinkActionTrigger = walletDeepLinkActionTrigger,
            jointAccountFeatureToggles = jointAccountFeatureToggles,
        )
    }
}