package com.tangem.features.earn.flow.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.common.ui.amountScreen.converters.AmountStateConverter
import com.tangem.common.ui.amountScreen.converters.field.AmountFieldSetMaxAmountTransformer
import com.tangem.common.ui.amountScreen.models.AmountParameters
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.res.R
import com.tangem.core.ui.ds.button.TangemButtonIconPosition
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.TxInfo
import com.tangem.features.earn.api.ui.EarnHowItWorksUM
import com.tangem.features.earn.flow.api.EarnFlowLegal
import com.tangem.features.earn.flow.api.EarnFlowProduct
import com.tangem.features.earn.product.api.product.EarnHowItWorks
import com.tangem.features.earn.product.api.product.EarnTextBlock
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.product.api.session.EarnFlowAction
import com.tangem.features.earn.product.api.session.EarnFlowExit
import com.tangem.features.earn.product.api.session.EarnResultAsset
import com.tangem.features.earn.product.api.session.EarnResultButton
import com.tangem.features.earn.product.api.session.EarnStatusDetail
import com.tangem.features.earn.product.api.session.EarnTxStatus
import com.tangem.features.earn.product.api.session.Withdraw
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import com.tangem.core.ui.R as CoreUiR

internal class EarnFlowConverterTest {

    private val converter = EarnFlowConverter()

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Result {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN tx status WHEN result THEN screen variant follows it`(model: VariantModel) {
            // Act
            val actual = result(txStatus = model.status).variant

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            VariantModel(EarnTxStatus(TxInfo.TransactionStatus.Unconfirmed), TangemStatusScreen.Variant.Info),
            VariantModel(
                status = EarnTxStatus(
                    status = TxInfo.TransactionStatus.Confirmed,
                    detail = EarnStatusDetail.AwaitingSettlement(Instant.parse("2030-01-01T00:00:00Z")),
                ),
                expected = TangemStatusScreen.Variant.Info,
            ),
            VariantModel(EarnTxStatus(TxInfo.TransactionStatus.Confirmed), TangemStatusScreen.Variant.Success),
            VariantModel(EarnTxStatus(TxInfo.TransactionStatus.Failed), TangemStatusScreen.Variant.Error),
            VariantModel(
                status = EarnTxStatus(TxInfo.TransactionStatus.Confirmed, EarnStatusDetail.Returned),
                expected = TangemStatusScreen.Variant.Warning,
            ),
        )

        @Test
        fun `GIVEN no result buttons WHEN primary pressed THEN it is close with exit Done`() {
            // Arrange
            val exits = mutableListOf<EarnFlowExit>()
            val um = result(primary = null, onExit = exits::add)

            // Act
            um.primary?.onClick?.invoke()

            // Assert
            assertThat(um.primary?.text).isEqualTo(resourceReference(R.string.common_close))
            assertThat(um.secondary).isNull()
            assertThat(exits).containsExactly(EarnFlowExit.Done)
        }

        @Test
        fun `GIVEN only secondary result button WHEN result THEN no close button added`() {
            // Act
            val um = result(
                primary = null,
                secondary = EarnResultButton(stringReference("Token"), EarnFlowExit.ToToken),
            )

            // Assert
            assertThat(um.primary).isNull()
        }

        @Test
        fun `GIVEN button with ToYield exit WHEN pressed THEN exit is ToYield`() {
            // Arrange
            val exits = mutableListOf<EarnFlowExit>()
            val um = result(onExit = exits::add)

            // Act
            um.primary?.onClick?.invoke()

            // Assert
            assertThat(exits).containsExactly(EarnFlowExit.ToYield)
        }

        @ParameterizedTest
        @MethodSource("provideExplorerModels")
        fun `GIVEN explorer flag and link WHEN result THEN explorer buttons follow both`(model: ExplorerModel) {
            // Act
            val actual = result(isExplorerShown = model.isExplorerShown, explorerUrl = model.explorerUrl).explorer

            // Assert
            assertThat(actual != null).isEqualTo(model.expected)
        }

        private fun provideExplorerModels() = listOf(
            ExplorerModel(isExplorerShown = true, explorerUrl = TEST_EXPLORER_URL, expected = true),
            ExplorerModel(isExplorerShown = true, explorerUrl = null, expected = false),
            ExplorerModel(isExplorerShown = false, explorerUrl = TEST_EXPLORER_URL, expected = false),
        )

        @Test
        fun `GIVEN explorer shown and secondary button WHEN result THEN buttons kept without explorer`() {
            // Arrange
            val secondary = EarnResultButton(stringReference("Go to token"), EarnFlowExit.ToToken)

            // Act
            val actual = result(secondary = secondary, isExplorerShown = true, explorerUrl = TEST_EXPLORER_URL)

            // Assert
            assertThat(actual.explorer).isNull()
            assertThat(actual.secondary?.text).isEqualTo(secondary.text)
        }

        @Test
        fun `GIVEN explorer shown WHEN explore and share pressed THEN both get the link`() {
            // Arrange
            val explored = mutableListOf<String>()
            val shared = mutableListOf<String>()
            val um = result(
                isExplorerShown = true,
                explorerUrl = TEST_EXPLORER_URL,
                onExploreClick = explored::add,
                onShareClick = shared::add,
            )

            // Act
            um.explorer?.onExploreClick?.invoke()
            um.explorer?.onShareClick?.invoke()

            // Assert
            assertThat(explored).containsExactly(TEST_EXPLORER_URL)
            assertThat(shared).containsExactly(TEST_EXPLORER_URL)
        }

        @Test
        fun `GIVEN status asset WHEN result THEN asset passes through`() {
            // Arrange
            val asset = EarnResultAsset.Status(TangemStatusIcon.Kind.Checkmark)

            // Act
            val actual = result(asset = asset).asset

            // Assert
            assertThat(actual).isEqualTo(asset)
        }

        @Suppress("LongParameterList")
        private fun result(
            txStatus: EarnTxStatus = EarnTxStatus(TxInfo.TransactionStatus.Confirmed),
            primary: EarnResultButton? = testResult().primary,
            secondary: EarnResultButton? = null,
            asset: EarnResultAsset = EarnResultAsset.Logos,
            isExplorerShown: Boolean = false,
            explorerUrl: String? = null,
            onExit: (EarnFlowExit) -> Unit = {},
            onExploreClick: (String) -> Unit = {},
            onShareClick: (String) -> Unit = {},
        ) = converter.result(
            result = testResult(primary = primary, secondary = secondary)
                .copy(asset = asset, isExplorerShown = isExplorerShown),
            txStatus = txStatus,
            status = testStatus(),
            explorerUrl = explorerUrl,
            subtitle = null,
            product = testFlowProduct,
            callbacks = EarnResultCallbacks(
                onInfoClick = {},
                onExit = onExit,
                onExploreClick = onExploreClick,
                onShareClick = onShareClick,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Amount {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN notifications and fee readiness WHEN amount THEN primary enabled state follows them`(
            model: PrimaryEnabledModel,
        ) {
            // Act
            val actual = amount(isFeeReady = model.isFeeReady, notifications = model.notifications)

            // Assert
            assertThat(actual.primary.isEnabled).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            PrimaryEnabledModel(
                notifications = persistentListOf(NotificationUM.Error.TotalExceedsBalance),
                expected = false,
            ),
            PrimaryEnabledModel(
                notifications = persistentListOf(NotificationUM.Warning.NetworkFeeUnreachable {}),
                expected = true,
            ),
            PrimaryEnabledModel(isFeeReady = false, expected = false),
        )

        @Test
        fun `GIVEN product with how it works WHEN amount THEN info button opens it`() {
            // Arrange
            var opened = 0
            val product = testFlowProduct.copy(howItWorks = testHowItWorks())

            // Act
            amount(product = product, onHowItWorksClick = { opened++ }).onHowItWorksClick?.invoke()

            // Assert
            assertThat(opened).isEqualTo(1)
        }

        @Test
        fun `GIVEN product without how it works WHEN amount THEN no info button`() {
            // Act
            val actual = amount(product = testFlowProduct.copy(howItWorks = null))

            // Assert
            assertThat(actual.onHowItWorksClick).isNull()
        }

        @Test
        fun `GIVEN action with notice WHEN amount THEN notice shown`() {
            // Arrange
            val notice = stringReference("Arrives in 7 days")

            // Act
            val actual = amount(action = Withdraw(notice = notice))

            // Assert
            assertThat(actual.notice).isEqualTo(notice)
        }

        @Test
        fun `GIVEN withdrawal WHEN amount THEN no percent buttons and screen leads with back`() {
            // Act
            val actual = amount(action = Withdraw())

            // Assert
            assertThat(actual.percents).isEmpty()
            assertThat(actual.isFromPosition).isTrue()
        }

        @Test
        fun `GIVEN deposit WHEN amount THEN percent buttons shown`() {
            // Act
            val actual = amount(action = Deposit())

            // Assert
            assertThat(actual.percents.map { it.id }).containsExactly("25", "50", "75", "100").inOrder()
            assertThat(actual.isFromPosition).isFalse()
        }

        @Test
        fun `GIVEN deposit with loaded source WHEN amount THEN card names the source and the symbol`() {
            // Arrange
            val source = AccountTitleUM.Text(stringReference("From Main account"))

            // Act
            val actual = amount(action = Deposit(), source = source).amountState as AmountState.Data

            // Assert
            assertThat(actual.accountTitleUM).isEqualTo(source)
            assertThat(actual.tokenName).isEqualTo(stringReference(testEthereum.symbol))
        }

        @Test
        fun `GIVEN withdrawal WHEN amount THEN product heads the balance row`() {
            // Act
            val actual = amount(action = Withdraw()).amountState as AmountState.Data

            // Assert
            assertThat(actual.tokenName).isEqualTo(testFlowProduct.title)
        }

        @Test
        fun `GIVEN empty amount state WHEN amount THEN state stays empty`() {
            // Act
            val actual = amount(amountState = AmountState.Empty)

            // Assert
            assertThat(actual.amountState).isEqualTo(AmountState.Empty)
            assertThat(actual.primary.isEnabled).isFalse()
        }

        @Suppress("LongParameterList")
        private fun amount(
            amountState: AmountState = validAmountState(),
            isFeeReady: Boolean = true,
            notifications: ImmutableList<NotificationUM> = persistentListOf(),
            action: EarnFlowAction = Deposit(),
            product: EarnFlowProduct = testFlowProduct,
            onHowItWorksClick: () -> Unit = {},
            source: AccountTitleUM? = null,
        ) = converter.amount(
            action = action,
            product = product,
            amountState = amountState,
            balance = TEST_BALANCE,
            status = testStatus(),
            isFeeReady = isFeeReady,
            isPreparing = false,
            notifications = notifications,
            clickIntents = mockk<AmountScreenClickIntents>(),
            callbacks = EarnAmountCallbacks(onPercentClick = {}, onSubmit = {}, onHowItWorksClick = onHowItWorksClick),
            source = source,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Confirm {

        @Test
        fun `GIVEN executing WHEN confirm THEN primary is loading`() {
            // Act
            val actual = confirm(signing = EarnSigningState(stepLabel = null, isExecuting = true))

            // Assert
            assertThat(actual.primary.isLoading).isTrue()
        }

        @Test
        fun `GIVEN action without amount WHEN confirm THEN button label comes from the action`() {
            // Act
            val actual = confirm()

            // Assert
            assertThat(actual.primary.text).isEqualTo(TestClaim.label)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN plan and action notices WHEN confirm THEN plan notice wins`(model: NoticeModel) {
            // Act
            val actual = confirm(
                action = Withdraw(notice = model.actionNotice),
                plan = TestPlan(notice = model.planNotice),
            ).notice

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            NoticeModel(planNotice = PLAN_NOTICE, actionNotice = ACTION_NOTICE, expected = PLAN_NOTICE),
            NoticeModel(planNotice = null, actionNotice = ACTION_NOTICE, expected = ACTION_NOTICE),
            NoticeModel(planNotice = null, actionNotice = null, expected = null),
        )

        @Test
        fun `GIVEN status without rate WHEN confirm THEN fiat empty and crypto shown`() {
            // Act
            val noQuote = testStatus().copy(
                value = CryptoCurrencyStatus.NoQuote(
                    amount = TEST_BALANCE,
                    yieldSupplyStatus = null,
                    hasCurrentNetworkTransactions = false,
                    pendingTransactions = emptySet(),
                    networkAddress = mockk<NetworkAddress>(),
                    sources = CryptoCurrencyStatus.Sources(),
                ),
            )
            val actual = confirm(status = noQuote)

            // Assert
            assertThat(actual.amountFiat).isEqualTo(TextReference.EMPTY)
            assertThat(actual.amountCrypto).isNotEqualTo(TextReference.EMPTY)
        }

        @Test
        fun `GIVEN wallet icon WHEN confirm THEN primary ends with it`() {
            // Act
            val actual = confirm(walletIcon = CoreUiR.drawable.ic_tangem_24).primary

            // Assert
            assertThat(actual.tangemIconUM).isEqualTo(TangemIconUM.Icon(iconRes = CoreUiR.drawable.ic_tangem_24))
            assertThat(actual.iconPosition).isEqualTo(TangemButtonIconPosition.End)
        }

        @Test
        fun `GIVEN no wallet icon WHEN confirm THEN primary has no icon`() {
            // Act
            val actual = confirm(walletIcon = null).primary

            // Assert
            assertThat(actual.tangemIconUM).isNull()
        }

        @Test
        fun `GIVEN product with legal links WHEN confirm THEN legal opens each document`() {
            // Arrange
            val opened = mutableListOf<String>()
            val product = testFlowProduct.copy(legal = EarnFlowLegal(termsOfUseUrl = "tos", privacyPolicyUrl = "pp"))

            // Act
            val legal = requireNotNull(confirm(product = product, onLegalClick = { opened += it }).legal)
            legal.onClick(legal.termsOfUseUrl)
            legal.onClick(legal.privacyPolicyUrl)

            // Assert
            assertThat(opened).containsExactly("tos", "pp").inOrder()
        }

        @Test
        fun `GIVEN product without legal links WHEN confirm THEN no legal line`() {
            // Act
            val actual = confirm(product = testFlowProduct.copy(legal = null)).legal

            // Assert
            assertThat(actual).isNull()
        }

        private fun confirm(
            signing: EarnSigningState = EarnSigningState.Idle,
            status: CryptoCurrencyStatus = testStatus(),
            action: EarnFlowAction = TestClaim,
            plan: TestPlan = TestPlan(),
            product: EarnFlowProduct = testFlowProduct,
            walletIcon: Int? = null,
            onLegalClick: (String) -> Unit = {},
        ) = converter.confirm(
            action = action,
            product = product,
            plan = plan,
            status = status,
            appCurrency = AppCurrency.Default,
            signing = signing,
            isFeeReady = true,
            isPreparing = false,
            notifications = persistentListOf(),
            walletIcon = walletIcon,
            callbacks = EarnConfirmCallbacks(onInfoClick = {}, onConfirm = {}, onLegalClick = onLegalClick),
        )
    }

    @Nested
    inner class HowItWorks {

        @Test
        fun `GIVEN how it works WHEN converted THEN steps footnote and learn more follow it`() {
            // Arrange
            val opened = mutableListOf<String>()

            // Act
            val actual = converter.howItWorks(testHowItWorks(), onLearnMoreClick = opened::add)
            actual.onLearnMoreClick?.invoke()

            // Assert
            assertThat(actual.copy(onLearnMoreClick = null)).isEqualTo(
                EarnHowItWorksUM(
                    title = stringReference("How it works"),
                    steps = persistentListOf(
                        EarnHowItWorksUM.Step(title = stringReference("Supply"), description = stringReference("Lend")),
                    ),
                    footnote = stringReference("Audited"),
                    onLearnMoreClick = null,
                ),
            )
            assertThat(opened).containsExactly(TEST_LEARN_MORE_URL)
        }

        @Test
        fun `GIVEN no learn more link WHEN converted THEN no learn more button`() {
            // Act
            val actual = converter.howItWorks(testHowItWorks().copy(learnMoreUrl = null), onLearnMoreClick = {})

            // Assert
            assertThat(actual.onLearnMoreClick).isNull()
        }
    }

    data class VariantModel(val status: EarnTxStatus, val expected: TangemStatusScreen.Variant)

    data class ExplorerModel(val isExplorerShown: Boolean, val explorerUrl: String?, val expected: Boolean)

    data class NoticeModel(val planNotice: TextReference?, val actionNotice: TextReference?, val expected: TextReference?)

    data class PrimaryEnabledModel(
        val notifications: ImmutableList<NotificationUM> = persistentListOf(),
        val isFeeReady: Boolean = true,
        val expected: Boolean,
    )
}

private const val TEST_EXPLORER_URL = "https://etherscan.io/tx/0xhash"
private const val TEST_LEARN_MORE_URL = "https://aave.com"
private val PLAN_NOTICE = stringReference("From the plan")
private val ACTION_NOTICE = stringReference("From the action")

private fun testHowItWorks() = EarnHowItWorks(
    title = stringReference("How it works"),
    entries = listOf(EarnTextBlock(title = stringReference("Supply"), description = stringReference("Lend"))),
    footnote = stringReference("Audited"),
    learnMoreUrl = TEST_LEARN_MORE_URL,
)