package com.tangem.features.swap.v2.impl.sendviaswap.success.model

import com.tangem.common.ui.navigationButtons.NavigationModelCallback
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.swap.models.SwapDataModel
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.entity.ConfirmUM
import com.tangem.features.swap.v2.impl.sendviaswap.entity.SendWithSwapUM
import com.tangem.features.swap.v2.impl.sendviaswap.success.SendWithSwapSuccessComponent
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class SendWithSwapSuccessModelTest {

    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val shareManager: ShareManager = mockk(relaxed = true)
    private val navigationCallback: NavigationModelCallback = mockk(relaxed = true)

    private val swapDataModel: SwapDataModel = mockk(relaxed = true)
    private val provider: ExpressProvider = mockk(relaxed = true)

    private val successConfirmUM = ConfirmUM.Success(
        transactionDate = TIMESTAMP,
        txUrl = TX_URL,
        swapDataModel = swapDataModel,
        provider = provider,
        amountUM = SwapAmountUM.Empty(swapDirection = SwapDirection.Direct),
        destinationUM = DestinationUM.Empty(),
        feeSelectorUM = FeeSelectorUM.Loading,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(urlOpener, shareManager)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnExploreClick {

        @Test
        fun `GIVEN confirmUM is Success at construction WHEN onExploreClick THEN opens tx url`() {
            // Arrange
            val model = createModel(flow = createStateFlow(confirmUM = successConfirmUM))

            // Act
            model.onExploreClick()

            // Assert
            verify(exactly = 1) { urlOpener.openUrl(TX_URL) }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN confirmUM is not Success at construction WHEN onExploreClick THEN no-ops`(
            case: NonSuccessConfirmUMCase,
        ) {
            // Arrange
            val model = createModel(flow = createStateFlow(confirmUM = case.confirmUM))

            // Act
            model.onExploreClick()

            // Assert
            verify(exactly = 0) { urlOpener.openUrl(any()) }
        }

        @Test
        fun `GIVEN confirmUM becomes Success after construction WHEN onExploreClick THEN still no-ops`() {
            // Arrange
            val flow = createStateFlow(confirmUM = ConfirmUM.Empty)
            val model = createModel(flow = flow)
            flow.value = flow.value.copy(confirmUM = successConfirmUM)

            // Act
            model.onExploreClick()

            // Assert
            verify(exactly = 0) { urlOpener.openUrl(any()) }
        }

        private fun provideTestModels(): List<NonSuccessConfirmUMCase> = nonSuccessConfirmUMCases()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnShareClick {

        @Test
        fun `GIVEN confirmUM is Success at construction WHEN onShareClick THEN shares tx url`() {
            // Arrange
            val model = createModel(flow = createStateFlow(confirmUM = successConfirmUM))

            // Act
            model.onShareClick()

            // Assert
            verify(exactly = 1) { shareManager.shareText(TX_URL) }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN confirmUM is not Success at construction WHEN onShareClick THEN no-ops`(
            case: NonSuccessConfirmUMCase,
        ) {
            // Arrange
            val model = createModel(flow = createStateFlow(confirmUM = case.confirmUM))

            // Act
            model.onShareClick()

            // Assert
            verify(exactly = 0) { shareManager.shareText(any()) }
        }

        @Test
        fun `GIVEN confirmUM becomes Success after construction WHEN onShareClick THEN still no-ops`() {
            // Arrange
            val flow = createStateFlow(confirmUM = ConfirmUM.Empty)
            val model = createModel(flow = flow)
            flow.value = flow.value.copy(confirmUM = successConfirmUM)

            // Act
            model.onShareClick()

            // Assert
            verify(exactly = 0) { shareManager.shareText(any()) }
        }

        private fun provideTestModels(): List<NonSuccessConfirmUMCase> = nonSuccessConfirmUMCases()
    }

    private fun createStateFlow(confirmUM: ConfirmUM): MutableStateFlow<SendWithSwapUM> = MutableStateFlow(
        SendWithSwapUM(
            amountUM = SwapAmountUM.Empty(swapDirection = SwapDirection.Direct),
            destinationUM = DestinationUM.Empty(),
            feeSelectorUM = FeeSelectorUM.Loading,
            confirmUM = confirmUM,
        ),
    )

    private fun createModel(flow: MutableStateFlow<SendWithSwapUM>): SendWithSwapSuccessModel {
        val params = SendWithSwapSuccessComponent.Params(
            sendWithSwapUMFlow = flow,
            analyticsCategoryName = ANALYTICS_CATEGORY,
            callback = navigationCallback,
        )
        return SendWithSwapSuccessModel(
            dispatchers = dispatchers,
            urlOpener = urlOpener,
            shareManager = shareManager,
            paramsContainer = MutableParamsContainer(value = params),
        )
    }

    private fun nonSuccessConfirmUMCases(): List<NonSuccessConfirmUMCase> = listOf(
        NonSuccessConfirmUMCase(description = "Empty", confirmUM = ConfirmUM.Empty),
        NonSuccessConfirmUMCase(
            description = "Content",
            confirmUM = ConfirmUM.Content(
                isPrimaryButtonEnabled = true,
                isTransactionInProcess = false,
                isShowTapHelp = false,
                sendingFooter = TextReference.EMPTY,
                notifications = persistentListOf(),
                tosUM = null,
            ),
        ),
    )

    data class NonSuccessConfirmUMCase(val description: String, val confirmUM: ConfirmUM) {
        override fun toString(): String = description
    }

    private companion object {
        const val TX_URL = "https://explorer.test/tx/abc"
        const val TIMESTAMP = 1_700_000_000_000L
        const val ANALYTICS_CATEGORY = "Send With Swap"
    }
}