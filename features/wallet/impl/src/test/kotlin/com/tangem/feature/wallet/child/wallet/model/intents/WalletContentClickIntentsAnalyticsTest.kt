package com.tangem.feature.wallet.child.wallet.model.intents

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class WalletContentClickIntentsAnalyticsTest {

    private val stateHolder: WalletStateController = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val router: InnerWalletRouter = mockk(relaxed = true)

    private val userWalletId = UserWalletId(stringValue = "0123456789ABCDEF")

    private fun createImplementor(): WalletContentClickIntentsImplementor {
        every { stateHolder.getSelectedWalletId() } returns userWalletId

        val implementor = WalletContentClickIntentsImplementor(
            stateHolder = stateHolder,
            currencyActionsClickIntents = mockk(relaxed = true),
            onrampStatusFactory = mockk(relaxed = true),
            getUserWalletUseCase = mockk(relaxed = true),
            singleAccountStatusListSupplier = mockk(relaxed = true),
            getCryptoCurrencyActionsUseCase = mockk(relaxed = true),
            getExplorerTransactionUrlUseCase = mockk(relaxed = true),
            shouldShowMarketsTooltipUseCase = mockk(relaxed = true),
            dispatchers = mockk(relaxed = true),
            walletEventSender = mockk(relaxed = true),
            analyticsEventHandler = analyticsEventHandler,
            accountDependencies = mockk(relaxed = true),
            yieldSupplySetShouldShowMainPromoUseCase = mockk(relaxed = true),
            tokenListAnalyticsSender = mockk(relaxed = true),
            uiMessageSender = mockk(relaxed = true),
        )
        implementor.initialize(router = router, coroutineScope = TestScope())
        return implementor
    }

    @Test
    fun `WHEN onOrganizeTokensClick THEN sends ButtonAddManage event and opens bottom sheet`() =
        runTest {
            val implementor = createImplementor()
            val captured = slot<AnalyticsEvent>()

            implementor.onOrganizeTokensClick()

            verify(exactly = 1) { analyticsEventHandler.send(capture(captured)) }
            assertThat(captured.captured.category).isEqualTo("Portfolio")
            assertThat(captured.captured.event).isEqualTo("Button - Add Manage")
            assertThat(captured.captured.params).isEmpty()
            verify(exactly = 1) { router.openAddAndManageBottomSheet(userWalletId = userWalletId) }
            verify(exactly = 0) { router.openOrganizeTokensScreen(any()) }
        }
}