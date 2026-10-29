package com.tangem.features.tangempay.withdraw.bank.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
import com.tangem.features.tangempay.withdraw.bank.offrampDestination
import com.tangem.features.tangempay.withdraw.bank.savedRecipientDraft
import com.tangem.test.core.ProvideTestModels
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class BankWithdrawAnalyticsTest {

    private val analytics: AnalyticsEventHandler = mockk()
    private val sentEvents = mutableListOf<AnalyticsEvent>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(analytics)
        sentEvents.clear()
        every { analytics.send(capture(sentEvents)) } just Runs
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PaymentAccount {

        private val withdrawAnalytics = BankWithdrawAnalytics(
            sourceAccount = BankWithdrawSourceAccount.Payment(USER_WALLET_ID),
            analytics = analytics,
        )

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN payment account WHEN wizard step reported THEN its offramp events sent unchanged`(
            model: PaymentAccountModel,
        ) {
            // Act
            model.report(withdrawAnalytics)

            // Assert
            assertThat(sentEvents).containsExactlyElementsIn(model.expectedEvents).inOrder()
        }

        private fun provideTestModels() = listOf(
            PaymentAccountModel(
                step = "recipient list opened",
                report = { onRecipientListOpened() },
                expectedEvents = listOf(TangemPayAnalyticsEvents.Offramp.SavedContactsScreenOpened()),
            ),
            PaymentAccountModel(
                step = "recipient selected",
                report = { onRecipientSelected() },
                expectedEvents = listOf(TangemPayAnalyticsEvents.Offramp.SavedContactOnContactsScreenClicked()),
            ),
            PaymentAccountModel(
                step = "amount screen opened",
                report = { onAmountScreenOpened() },
                expectedEvents = emptyList(),
            ),
            PaymentAccountModel(
                step = "confirmation screen opened",
                report = { onConfirmationScreenOpened() },
                expectedEvents = listOf(TangemPayAnalyticsEvents.Offramp.ConfirmationScreenOpened()),
            ),
            PaymentAccountModel(
                step = "send clicked",
                report = { onSendClicked(savedRecipientDraft()) },
                expectedEvents = listOf(TangemPayAnalyticsEvents.Offramp.SendOnConfirmationClicked()),
            ),
            PaymentAccountModel(
                step = "send failed",
                report = { onSendFailed(errorCode = FORBIDDEN_ERROR_CODE) },
                expectedEvents = listOf(TangemPayAnalyticsEvents.Offramp.SendErrorScreenOpened()),
            ),
            PaymentAccountModel(
                step = "send succeeded",
                report = { onSendSucceeded(rail = BankRail.WIRE) },
                expectedEvents = listOf(
                    TangemPayAnalyticsEvents.Offramp.SendSuccessScreenShowed(rail = BankRail.WIRE),
                ),
            ),
            PaymentAccountModel(
                step = "payment account only step",
                report = { sendPaymentAccountOnly(TangemPayAnalyticsEvents.Offramp.AmountEntered()) },
                expectedEvents = listOf(TangemPayAnalyticsEvents.Offramp.AmountEntered()),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MoneyAccount {

        private val withdrawAnalytics = BankWithdrawAnalytics(
            sourceAccount = BankWithdrawSourceAccount.Money(USER_WALLET_ID, productInstanceId = "instance-1"),
            analytics = analytics,
        )

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN money account WHEN wizard step reported THEN its VA Transfer events sent`(
            model: MoneyAccountModel,
        ) {
            // Act
            model.report(withdrawAnalytics)

            // Assert
            val actual = sentEvents.map { SentEvent(category = it.category, event = it.event, params = it.params) }
            assertThat(actual).containsExactlyElementsIn(model.expectedEvents).inOrder()
        }

        private fun provideTestModels() = listOf(
            MoneyAccountModel(
                step = "recipient list opened",
                report = { onRecipientListOpened() },
                expectedEvents = listOf(transferEvent("VA Offramp Recipient List Opened")),
            ),
            MoneyAccountModel(
                step = "recipient selected",
                report = { onRecipientSelected() },
                expectedEvents = listOf(transferEvent("VA Offramp Recipient Selected")),
            ),
            MoneyAccountModel(
                step = "amount screen opened",
                report = { onAmountScreenOpened() },
                expectedEvents = listOf(transferEvent("VA Offramp Amount Screen Opened")),
            ),
            MoneyAccountModel(
                step = "confirmation screen opened",
                report = { onConfirmationScreenOpened() },
                expectedEvents = listOf(transferEvent("VA Offramp Confirmation Screen Opened")),
            ),
            MoneyAccountModel(
                step = "send clicked over ACH",
                report = { onSendClicked(savedRecipientDraft()) },
                expectedEvents = listOf(
                    transferEvent("VA Offramp Recipient Transfer Confirmed"),
                    transferEvent(
                        event = "VA Send Confirmed",
                        params = mapOf("amount_usdc" to "500", "method" to "bank_transfer_ach"),
                    ),
                ),
            ),
            MoneyAccountModel(
                step = "send clicked over Wire",
                report = {
                    onSendClicked(
                        savedRecipientDraft().copy(
                            amount = BigDecimal("12.5"),
                            destination = offrampDestination(rail = BankRail.WIRE),
                        ),
                    )
                },
                expectedEvents = listOf(
                    transferEvent("VA Offramp Recipient Transfer Confirmed"),
                    transferEvent(
                        event = "VA Send Confirmed",
                        params = mapOf("amount_usdc" to "12.5", "method" to "bank_transfer_wire"),
                    ),
                ),
            ),
            MoneyAccountModel(
                step = "send clicked without an amount",
                report = { onSendClicked(savedRecipientDraft().copy(amount = null)) },
                expectedEvents = listOf(transferEvent("VA Offramp Recipient Transfer Confirmed")),
            ),
            MoneyAccountModel(
                step = "send failed with an error code",
                report = { onSendFailed(errorCode = FORBIDDEN_ERROR_CODE) },
                expectedEvents = listOf(
                    transferEvent(event = "VA Send failed", params = mapOf("error_code" to "104120100")),
                ),
            ),
            MoneyAccountModel(
                step = "send failed without an error code",
                report = { onSendFailed(errorCode = null) },
                expectedEvents = listOf(transferEvent("VA Send failed")),
            ),
            MoneyAccountModel(
                step = "send succeeded",
                report = { onSendSucceeded(rail = BankRail.ACH) },
                expectedEvents = listOf(transferEvent("VA Send succesfull")),
            ),
            MoneyAccountModel(
                step = "payment account only step",
                report = { sendPaymentAccountOnly(TangemPayAnalyticsEvents.Offramp.AmountEntered()) },
                expectedEvents = emptyList(),
            ),
        )

        private fun transferEvent(event: String, params: Map<String, String> = emptyMap()) =
            SentEvent(category = "VA Transfer", event = event, params = params)
    }

    data class PaymentAccountModel(
        val step: String,
        val report: BankWithdrawAnalytics.() -> Unit,
        val expectedEvents: List<AnalyticsEvent>,
    )

    data class MoneyAccountModel(
        val step: String,
        val report: BankWithdrawAnalytics.() -> Unit,
        val expectedEvents: List<SentEvent>,
    )

    data class SentEvent(val category: String, val event: String, val params: Map<String, String>)

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val FORBIDDEN_ERROR_CODE = 104_120_100
    }
}