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
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.TxInfo
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.product.api.session.EarnResultButton
import com.tangem.features.earn.product.api.session.EarnFlowExit
import com.tangem.features.earn.product.api.session.EarnStatusDetail
import com.tangem.features.earn.product.api.session.EarnTxStatus
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

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

        private fun result(
            txStatus: EarnTxStatus = EarnTxStatus(TxInfo.TransactionStatus.Confirmed),
            primary: EarnResultButton? = testResult().primary,
            secondary: EarnResultButton? = null,
            onExit: (EarnFlowExit) -> Unit = {},
        ) = converter.result(
            result = testResult(primary = primary, secondary = secondary),
            txStatus = txStatus,
            status = testStatus(),
            product = testFlowProduct,
            callbacks = EarnResultCallbacks(onInfoClick = {}, onExit = onExit),
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
        fun `GIVEN empty amount state WHEN amount THEN state stays empty`() {
            // Act
            val actual = amount(amountState = AmountState.Empty)

            // Assert
            assertThat(actual.amountState).isEqualTo(AmountState.Empty)
            assertThat(actual.primary.isEnabled).isFalse()
        }

        private fun amount(
            amountState: AmountState = validAmountState(),
            isFeeReady: Boolean = true,
            notifications: ImmutableList<NotificationUM> = persistentListOf(),
        ) = converter.amount(
            action = Deposit(),
            product = testFlowProduct,
            amountState = amountState,
            balance = TEST_BALANCE,
            status = testStatus(),
            isFeeReady = isFeeReady,
            isPreparing = false,
            notifications = notifications,
            clickIntents = mockk<AmountScreenClickIntents>(),
            callbacks = EarnAmountCallbacks(onPercentClick = {}, onSubmit = {}),
        )
    }

    @Nested
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

        private fun confirm(
            signing: EarnSigningState = EarnSigningState.Idle,
            status: CryptoCurrencyStatus = testStatus(),
        ) = converter.confirm(
            action = TestClaim,
            product = testFlowProduct,
            plan = TestPlan(),
            status = status,
            appCurrency = AppCurrency.Default,
            signing = signing,
            isFeeReady = true,
            isPreparing = false,
            notifications = persistentListOf(),
            callbacks = EarnConfirmCallbacks(onInfoClick = {}, onConfirm = {}),
        )
    }

    /** An entered 0.5 ETH: the shared transformers mark it valid. */
    private fun validAmountState(): AmountState {
        val status = testStatus()
        val boundary = EnterAmountBoundary(amount = BigDecimal("0.5"), fiatRate = status.value.fiatRate)
        val initial = AmountStateConverter(
            clickIntents = mockk(relaxed = true),
            appCurrency = AppCurrency.Default,
            cryptoCurrencyStatus = status,
            maxEnterAmount = EnterAmountBoundary(amount = TEST_BALANCE, fiatRate = status.value.fiatRate),
            iconStateConverter = CryptoCurrencyToIconStateConverter(),
            isBalanceHidden = false,
            accountTitleUM = AccountTitleUM.Text(TextReference.EMPTY),
        ).convert(AmountParameters(title = TextReference.EMPTY, value = ""))
        return AmountFieldSetMaxAmountTransformer(status, boundary, minAmount = null).transform(initial)
    }

    data class VariantModel(val status: EarnTxStatus, val expected: TangemStatusScreen.Variant)

    data class PrimaryEnabledModel(
        val notifications: ImmutableList<NotificationUM> = persistentListOf(),
        val isFeeReady: Boolean = true,
        val expected: Boolean,
    )
}