package com.tangem.features.jointaccount.join.invitepreview.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.jointaccount.model.JointAccountInvitePreview
import com.tangem.domain.jointaccount.usecase.GetJointAccountInvitePreviewUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.jointaccount.join.invitepreview.state.JointAccountInvitePreviewStateController
import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import com.tangem.features.jointaccount.join.model.JointAccountJoinChildParams
import com.tangem.features.jointaccount.join.model.JointAccountJoinDraftHolder
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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

    private val wallet: UserWallet = mockWallet(id = WALLET_ID, locked = false)

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            getJointAccountInvitePreviewUseCase,
            userWalletsListRepository,
            portfolioSelectorController,
            portfolioFetcherFactory,
        )
        every { portfolioFetcherFactory.create(any(), any()) } returns portfolioFetcher
        every { portfolioSelectorController.selectedAccountWithData(any()) } returns MutableStateFlow(null)
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(null)
        every { userWalletsListRepository.selectedUserWallet } returns MutableStateFlow(null)
    }

    @Test
    fun `GIVEN wallets repository still loading WHEN model created THEN load waits instead of failing immediately`() =
        runTest {
            // Arrange — mirrors a cold start via deep link: the repository takes a moment to finish loading
            coEvery { userWalletsListRepository.userWalletsSync() } coAnswers {
                delay(1_000)
                listOf(wallet)
            }
            coEvery { getJointAccountInvitePreviewUseCase(WALLET_ID, INVITE_ID) } returns preview().right()

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isInstanceOf(JointAccountInvitePreviewUM.Content::class.java)
            coVerify(exactly = 1) { getJointAccountInvitePreviewUseCase(WALLET_ID, INVITE_ID) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN no unlocked wallets after sync WHEN model created THEN state is Failed`() = runTest {
        // Arrange
        coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(JointAccountInvitePreviewUM.Failed::class.java)
        coVerify(exactly = 0) { getJointAccountInvitePreviewUseCase(any(), any()) }
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
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }

    private companion object {
        const val INVITE_ID = "invite-id"
        val WALLET_ID = UserWalletId("aabbcc")
    }
}