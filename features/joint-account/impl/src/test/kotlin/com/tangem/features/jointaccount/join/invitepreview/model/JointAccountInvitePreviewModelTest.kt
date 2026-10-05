package com.tangem.features.jointaccount.join.invitepreview.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.jointaccount.model.JointAccountInvitePreview
import com.tangem.domain.jointaccount.model.JointAccountInvitePreviewError
import com.tangem.domain.jointaccount.usecase.GetJointAccountInvitePreviewUseCase
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.ErrorSource
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.JointAccountError
import com.tangem.features.jointaccount.join.invitepreview.state.JointAccountInvitePreviewStateController
import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import com.tangem.features.jointaccount.join.model.JointAccountJoinChildParams
import com.tangem.features.jointaccount.join.model.JointAccountJoinDraftHolder
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
internal class JointAccountInvitePreviewModelTest {

    private val router: Router = mockk(relaxed = true)
    private val getJointAccountInvitePreviewUseCase: GetJointAccountInvitePreviewUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val portfolioSelectorController: PortfolioSelectorController = mockk()
    private val portfolioFetcher: PortfolioFetcher = mockk(relaxed = true)
    private val portfolioFetcherFactory: PortfolioFetcher.Factory = mockk()
    private val clipboardManager: ClipboardManager = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    private val wallet: UserWallet = mockWallet(id = WALLET_ID, locked = false)
    private val portfolioData = MutableSharedFlow<PortfolioFetcher.Data>(replay = 1)

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            analyticsEventHandler,
            router,
            getJointAccountInvitePreviewUseCase,
            userWalletsListRepository,
            portfolioSelectorController,
            portfolioFetcherFactory,
        )
        every { portfolioFetcherFactory.create(any(), any()) } returns portfolioFetcher
        every { portfolioFetcher.data } returns portfolioData
        portfolioData.tryEmit(portfolioData(WALLET_ID, OTHER_WALLET_ID))
        every { portfolioSelectorController.selectedAccountWithData(any()) } returns MutableStateFlow(null)
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(null)
        every { userWalletsListRepository.selectedUserWallet } returns MutableStateFlow(null)
    }

    @Test
    fun `GIVEN wallets repository still loading WHEN model created THEN load waits instead of failing immediately`() =
        runTest {
            // Arrange — mirrors a cold start via deep link: the list is null until the repository finishes
            val wallets = MutableStateFlow<List<UserWallet>?>(value = null)
            every { userWalletsListRepository.userWallets } returns wallets
            coEvery { getJointAccountInvitePreviewUseCase(WALLET_ID, INVITE_ID) } returns preview().right()

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()
            val whileLoading = model.uiState.value
            wallets.value = listOf(wallet)
            advanceUntilIdle()

            // Assert
            assertThat(whileLoading).isInstanceOf(JointAccountInvitePreviewUM.Loading::class.java)
            assertThat(model.uiState.value).isInstanceOf(JointAccountInvitePreviewUM.Content::class.java)
            coVerify(exactly = 1) { getJointAccountInvitePreviewUseCase(WALLET_ID, INVITE_ID) }
            verify(exactly = 0) { analyticsEventHandler.send(ofType<JointAccountError>()) }
            model.onDestroy()
        }

    /**
     * The endpoint answers per wallet — 409 when *this* wallet already holds a slot — so a pick in the selector
     * has to be previewed again rather than reusing the first wallet's answer.
     */
    @Test
    fun `GIVEN another wallet is picked WHEN it changes THEN the invite is previewed for that wallet`() = runTest {
        // Arrange
        val other = mockWallet(id = OTHER_WALLET_ID, locked = false)
        val picked = MutableStateFlow<Pair<UserWallet, AccountStatus.CryptoPortfolio>?>(value = null)
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(wallet, other))
        every { portfolioSelectorController.selectedAccountWithData(any()) } returns picked
        coEvery { getJointAccountInvitePreviewUseCase(any(), INVITE_ID) } returns preview().right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()
        picked.value = other to mockk()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { getJointAccountInvitePreviewUseCase(WALLET_ID, INVITE_ID) }
        coVerify(exactly = 1) { getJointAccountInvitePreviewUseCase(OTHER_WALLET_ID, INVITE_ID) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN backend unreachable at first WHEN model created THEN the preview is retried and loads`() = runTest {
        // Arrange — a link opened right after a Quick Share transfer, the phone has no network for a few seconds
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(wallet))
        coEvery { getJointAccountInvitePreviewUseCase(WALLET_ID, INVITE_ID) } returnsMany listOf(
            JointAccountInvitePreviewError.Unreachable(cause = IOException()).left(),
            JointAccountInvitePreviewError.Unreachable(cause = IOException()).left(),
            preview().right(),
        )

        // Act
        val model = createModel(testScope = this)
        advanceTimeBy(2.seconds)
        val whileRetrying = model.uiState.value
        advanceUntilIdle()

        // Assert
        assertThat(whileRetrying).isInstanceOf(JointAccountInvitePreviewUM.Loading::class.java)
        assertThat(model.uiState.value).isInstanceOf(JointAccountInvitePreviewUM.Content::class.java)
        coVerify(exactly = 3) { getJointAccountInvitePreviewUseCase(WALLET_ID, INVITE_ID) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN backend stays unreachable WHEN model created THEN state is Failed after the retries`() = runTest {
        // Arrange
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(wallet))
        val cause = IOException()
        coEvery { getJointAccountInvitePreviewUseCase(WALLET_ID, INVITE_ID) } returns
            JointAccountInvitePreviewError.Unreachable(cause = cause).left()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(JointAccountInvitePreviewUM.Failed::class.java)
        coVerify(exactly = 4) { getJointAccountInvitePreviewUseCase(WALLET_ID, INVITE_ID) }
        // Only the failure the user ends up seeing is reported, not every retried attempt
        verify(exactly = 1) { analyticsEventHandler.send(JointAccountError(source = ErrorSource.Join, cause = cause)) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN server error WHEN model created THEN state is Failed without a retry`() = runTest {
        // Arrange
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(wallet))
        val cause = IllegalStateException()
        coEvery { getJointAccountInvitePreviewUseCase(WALLET_ID, INVITE_ID) } returns
            JointAccountInvitePreviewError.LoadFailed(cause = cause).left()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(JointAccountInvitePreviewUM.Failed::class.java)
        coVerify(exactly = 1) { getJointAccountInvitePreviewUseCase(WALLET_ID, INVITE_ID) }
        verify(exactly = 1) { analyticsEventHandler.send(JointAccountError(source = ErrorSource.Join, cause = cause)) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN a spent invite WHEN model created THEN the invalid link screen is reported as a join error`() =
        runTest {
            // Arrange
            val cause = IllegalStateException("Conflict")
            every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(wallet))
            coEvery { getJointAccountInvitePreviewUseCase(WALLET_ID, INVITE_ID) } returns
                JointAccountInvitePreviewError.InvalidLink(cause = cause).left()

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { analyticsEventHandler.send(JointAccountError(source = ErrorSource.Join, cause = cause)) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN selector data missing WHEN preview loaded THEN content waits for the data`() = runTest {
        // Arrange — a cold start through the invite link: the preview answers before the balances are read
        val selectorData = MutableSharedFlow<PortfolioFetcher.Data>(replay = 1)
        every { portfolioFetcher.data } returns selectorData
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(wallet))
        coEvery { getJointAccountInvitePreviewUseCase(WALLET_ID, INVITE_ID) } returns preview().right()

        // Act
        val model = createModel(testScope = this)
        advanceTimeBy(5.seconds)
        val beforeData = model.uiState.value
        selectorData.emit(portfolioData(WALLET_ID))
        advanceUntilIdle()

        // Assert
        assertThat(beforeData).isInstanceOf(JointAccountInvitePreviewUM.Loading::class.java)
        assertThat(model.uiState.value).isInstanceOf(JointAccountInvitePreviewUM.Content::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN selector data before the wallets WHEN wallets arrive THEN content waits for their data`() = runTest {
        // Arrange — the fetcher answers first with nothing to show, while the wallet list is still loading
        val selectorData = MutableSharedFlow<PortfolioFetcher.Data>(replay = 1)
        selectorData.tryEmit(portfolioData())
        val wallets = MutableStateFlow<List<UserWallet>?>(value = null)
        every { portfolioFetcher.data } returns selectorData
        every { userWalletsListRepository.userWallets } returns wallets
        coEvery { getJointAccountInvitePreviewUseCase(WALLET_ID, INVITE_ID) } returns preview().right()

        // Act
        val model = createModel(testScope = this)
        runCurrent()
        wallets.value = listOf(wallet)
        advanceTimeBy(5.seconds)
        val beforeData = model.uiState.value
        selectorData.emit(portfolioData(WALLET_ID))
        advanceUntilIdle()

        // Assert
        assertThat(beforeData).isInstanceOf(JointAccountInvitePreviewUM.Loading::class.java)
        assertThat(model.uiState.value).isInstanceOf(JointAccountInvitePreviewUM.Content::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN selector data never arrives WHEN the wait runs out THEN content is shown anyway`() = runTest {
        // Arrange
        every { portfolioFetcher.data } returns MutableSharedFlow<PortfolioFetcher.Data>(replay = 1)
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(wallet))
        coEvery { getJointAccountInvitePreviewUseCase(WALLET_ID, INVITE_ID) } returns preview().right()

        // Act
        val model = createModel(testScope = this)
        advanceTimeBy(9.seconds)
        val beforeTimeout = model.uiState.value
        advanceUntilIdle()

        // Assert
        assertThat(beforeTimeout).isInstanceOf(JointAccountInvitePreviewUM.Loading::class.java)
        assertThat(model.uiState.value).isInstanceOf(JointAccountInvitePreviewUM.Content::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN selector data missing WHEN preview fails THEN state is Failed at once`() = runTest {
        // Arrange
        every { portfolioFetcher.data } returns MutableSharedFlow<PortfolioFetcher.Data>(replay = 1)
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(wallet))
        coEvery { getJointAccountInvitePreviewUseCase(WALLET_ID, INVITE_ID) } returns
            JointAccountInvitePreviewError.LoadFailed(cause = IllegalStateException()).left()

        // Act
        val model = createModel(testScope = this)
        runCurrent()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(JointAccountInvitePreviewUM.Failed::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN no unlocked wallets after sync WHEN model created THEN state is Failed`() = runTest {
        // Arrange
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(emptyList())

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(JointAccountInvitePreviewUM.Failed::class.java)
        coVerify(exactly = 0) { getJointAccountInvitePreviewUseCase(any(), any()) }
        verify(exactly = 1) {
            analyticsEventHandler.send(JointAccountError(source = ErrorSource.Join, errorMessage = "No wallet"))
        }
        model.onDestroy()
    }

    private fun createModel(testScope: TestScope): JointAccountInvitePreviewModel = JointAccountInvitePreviewModel(
        paramsContainer = MutableParamsContainer(
            JointAccountJoinChildParams(inviteId = INVITE_ID, draftHolder = JointAccountJoinDraftHolder()),
        ),
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        router = router,
        stateController = JointAccountInvitePreviewStateController(),
        getJointAccountInvitePreviewUseCase = getJointAccountInvitePreviewUseCase,
        userWalletsListRepository = userWalletsListRepository,
        portfolioSelectorController = portfolioSelectorController,
        portfolioFetcherFactory = portfolioFetcherFactory,
        clipboardManager = clipboardManager,
        uiMessageSender = uiMessageSender,
        analyticsEventHandler = analyticsEventHandler,
    )

    private fun portfolioData(vararg walletIds: UserWalletId): PortfolioFetcher.Data = PortfolioFetcher.Data(
        appCurrency = AppCurrency.Default,
        isBalanceHidden = false,
        balances = walletIds.associateWith {
            mockk<PortfolioFetcher.PortfolioBalance> { every { accountsBalance.accountStatuses } returns emptyList() }
        },
    )

    private fun preview(): JointAccountInvitePreview = JointAccountInvitePreview(
        config = JointAccountConfig(
            name = "Family savings",
            icon = "Family",
            iconColor = "Azure",
            membersCount = 5,
            threshold = 2,
        ),
        creator = JointAccountInvitePreview.Creator(name = "Alice", address = "0xOwner"),
    )

    private fun mockWallet(id: UserWalletId, locked: Boolean): UserWallet = mockk<UserWallet.Cold> {
        every { walletId } returns id
        every { isLocked } returns locked
        every { name } returns "Wallet ${id.stringValue}"
    }

    private companion object {
        const val INVITE_ID = "invite-id"
        val WALLET_ID = UserWalletId("aabbcc")
        val OTHER_WALLET_ID = UserWalletId("ddeeff")
    }
}