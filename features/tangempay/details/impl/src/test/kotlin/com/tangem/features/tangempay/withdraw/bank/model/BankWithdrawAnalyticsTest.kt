package com.tangem.features.tangempay.withdraw.bank.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.MoneyAccountFeatureToggles
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents.AmountErrorReason
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class BankWithdrawAnalyticsTest {

    private val analytics: AnalyticsEventHandler = mockk()
    private val sentEvents = mutableListOf<AnalyticsEvent>()
    private val mxnEnabled: MoneyAccountFeatureToggles = mockk { every { isMxnEnabled } returns true }
    private val mxnDisabled: MoneyAccountFeatureToggles = mockk { every { isMxnEnabled } returns false }

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
            moneyAccountFeatureToggles = mxnEnabled,
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
                report = { onRecipientSelected(rail = BankRail.ACH) },
                expectedEvents = listOf(TangemPayAnalyticsEvents.Offramp.SavedContactOnContactsScreenClicked()),
            ),
            PaymentAccountModel(
                step = "amount screen opened",
                report = { onAmountScreenOpened(rail = BankRail.ACH) },
                expectedEvents = emptyList(),
            ),
            PaymentAccountModel(
                step = "amount error shown",
                report = { onAmountErrorShown(rail = BankRail.ACH, reason = AmountErrorReason.BELOW_MINIMUM) },
                expectedEvents = emptyList(),
            ),
            PaymentAccountModel(
                step = "recipient form opened",
                report = { onRecipientFormOpened(newRecipientDraft(rail = BankRail.ACH)) },
                expectedEvents = emptyList(),
            ),
            PaymentAccountModel(
                step = "recipient saved",
                report = { onRecipientSaved(rail = BankRail.ACH, draft = newRecipientDraft(rail = BankRail.ACH)) },
                expectedEvents = emptyList(),
            ),
            PaymentAccountModel(
                step = "recipient save failed",
                report = { onRecipientSaveFailed(rail = BankRail.ACH, errorCode = FORBIDDEN_ERROR_CODE) },
                expectedEvents = emptyList(),
            ),
            PaymentAccountModel(
                step = "saved recipient picked",
                report = { onSavedRecipientPicked(rail = BankRail.ACH) },
                expectedEvents = emptyList(),
            ),
            PaymentAccountModel(
                step = "quote shown",
                report = { onQuoteShown(rail = BankRail.SPEI, amount = BigDecimal("50")) },
                expectedEvents = emptyList(),
            ),
            PaymentAccountModel(
                step = "confirmation screen opened",
                report = { onConfirmationScreenOpened(savedRecipientDraft()) },
                expectedEvents = listOf(TangemPayAnalyticsEvents.Offramp.ConfirmationScreenOpened()),
            ),
            PaymentAccountModel(
                step = "send clicked",
                report = { onSendClicked(savedRecipientDraft()) },
                expectedEvents = listOf(TangemPayAnalyticsEvents.Offramp.SendOnConfirmationClicked()),
            ),
            PaymentAccountModel(
                step = "send attempted",
                report = { onSendAttempted(savedRecipientDraft()) },
                expectedEvents = emptyList(),
            ),
            PaymentAccountModel(
                step = "send failed",
                report = { onSendFailed(errorCode = FORBIDDEN_ERROR_CODE, rail = BankRail.ACH) },
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
            moneyAccountFeatureToggles = mxnEnabled,
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
                step = "ACH recipient selected",
                report = { onRecipientSelected(rail = BankRail.ACH) },
                expectedEvents = listOf(transferEvent("VA Offramp Recipient Selected", mapOf("rail" to "ach"))),
            ),
            MoneyAccountModel(
                step = "SPEI recipient selected",
                report = { onRecipientSelected(rail = BankRail.SPEI) },
                expectedEvents = listOf(transferEvent("VA Offramp Recipient Selected", mapOf("rail" to "spei"))),
            ),
            MoneyAccountModel(
                step = "amount screen opened",
                report = { onAmountScreenOpened(rail = BankRail.WIRE) },
                expectedEvents = listOf(transferEvent("VA Offramp Amount Screen Opened", mapOf("rail" to "wire"))),
            ),
            MoneyAccountModel(
                step = "amount error shown",
                report = { onAmountErrorShown(rail = BankRail.SPEI, reason = AmountErrorReason.ABOVE_BALANCE) },
                expectedEvents = listOf(
                    transferEvent(
                        event = "VA Offramp Amount Error Shown",
                        params = mapOf("rail" to "spei", "reason" to "above_balance"),
                    ),
                ),
            ),
            MoneyAccountModel(
                step = "SPEI recipient form opened",
                report = { onRecipientFormOpened(newRecipientDraft(rail = BankRail.SPEI)) },
                expectedEvents = listOf(
                    transferEvent(
                        event = "VA Offramp Recipient Form Opened",
                        params = mapOf("rail" to "spei", "recipient_type" to "own"),
                    ),
                ),
            ),
            MoneyAccountModel(
                step = "SPEI recipient form opened after a someone else answer",
                report = { onRecipientFormOpened(newRecipientDraft(rail = BankRail.SPEI, isThirdParty = true)) },
                expectedEvents = listOf(
                    transferEvent(
                        event = "VA Offramp Recipient Form Opened",
                        params = mapOf("rail" to "spei", "recipient_type" to "own"),
                    ),
                ),
            ),
            MoneyAccountModel(
                step = "ACH recipient form opened for someone else",
                report = { onRecipientFormOpened(newRecipientDraft(rail = BankRail.ACH, isThirdParty = true)) },
                expectedEvents = listOf(
                    transferEvent(
                        event = "VA Offramp Recipient Form Opened",
                        params = mapOf("rail" to "ach", "recipient_type" to "someone_else"),
                    ),
                ),
            ),
            MoneyAccountModel(
                step = "ACH recipient form opened for own account",
                report = { onRecipientFormOpened(newRecipientDraft(rail = BankRail.ACH, isThirdParty = false)) },
                expectedEvents = listOf(
                    transferEvent(
                        event = "VA Offramp Recipient Form Opened",
                        params = mapOf("rail" to "ach", "recipient_type" to "own"),
                    ),
                ),
            ),
            MoneyAccountModel(
                step = "wire recipient form opened for a business",
                report = {
                    onRecipientFormOpened(
                        newRecipientDraft(rail = BankRail.WIRE, entry = BankWithdrawEntry.NewBusiness),
                    )
                },
                expectedEvents = listOf(
                    transferEvent(
                        event = "VA Offramp Recipient Form Opened",
                        params = mapOf("rail" to "wire", "recipient_type" to "business"),
                    ),
                ),
            ),
            MoneyAccountModel(
                step = "recipient form opened before a rail is picked",
                report = { onRecipientFormOpened(newRecipientDraft(rail = null)) },
                expectedEvents = emptyList(),
            ),
            MoneyAccountModel(
                step = "recipient saved",
                report = {
                    onRecipientSaved(
                        rail = BankRail.ACH,
                        draft = newRecipientDraft(rail = BankRail.ACH, isThirdParty = true),
                    )
                },
                expectedEvents = listOf(
                    transferEvent(
                        event = "VA Offramp Recipient Saved",
                        params = mapOf("rail" to "ach", "recipient_type" to "someone_else"),
                    ),
                ),
            ),
            MoneyAccountModel(
                step = "recipient save failed",
                report = { onRecipientSaveFailed(rail = BankRail.SPEI, errorCode = FORBIDDEN_ERROR_CODE) },
                expectedEvents = listOf(
                    transferEvent(
                        event = "VA Offramp Recipient Save Failed",
                        params = mapOf("rail" to "spei", "error_code" to "104120100"),
                    ),
                ),
            ),
            MoneyAccountModel(
                step = "saved recipient picked outside the list",
                report = { onSavedRecipientPicked(rail = BankRail.SPEI) },
                expectedEvents = listOf(transferEvent("VA Offramp Recipient Selected", mapOf("rail" to "spei"))),
            ),
            MoneyAccountModel(
                step = "quote shown",
                report = { onQuoteShown(rail = BankRail.SPEI, amount = BigDecimal("1000.01")) },
                expectedEvents = listOf(
                    transferEvent(
                        event = "VA Offramp Quote Shown",
                        params = mapOf("rail" to "spei", "amount_band" to ">1k"),
                    ),
                ),
            ),
            MoneyAccountModel(
                step = "confirmation screen opened",
                report = { onConfirmationScreenOpened(savedRecipientDraft()) },
                expectedEvents = listOf(
                    transferEvent("VA Offramp Confirmation Screen Opened", mapOf("rail" to "ach")),
                ),
            ),
            MoneyAccountModel(
                step = "send clicked",
                report = {
                    onSendClicked(savedRecipientDraft().copy(destination = offrampDestination(rail = BankRail.SPEI)))
                },
                expectedEvents = listOf(
                    transferEvent("VA Offramp Recipient Transfer Confirmed", mapOf("rail" to "spei")),
                ),
            ),
            MoneyAccountModel(
                step = "send attempted over ACH",
                report = { onSendAttempted(savedRecipientDraft()) },
                expectedEvents = listOf(
                    transferEvent(
                        event = "VA Send Confirmed",
                        params = mapOf("amount_usdc" to "500", "method" to "bank_transfer_ach"),
                    ),
                ),
            ),
            MoneyAccountModel(
                step = "send attempted over Wire",
                report = {
                    onSendAttempted(
                        savedRecipientDraft().copy(
                            amount = BigDecimal("12.5"),
                            destination = offrampDestination(rail = BankRail.WIRE),
                        ),
                    )
                },
                expectedEvents = listOf(
                    transferEvent(
                        event = "VA Send Confirmed",
                        params = mapOf("amount_usdc" to "12.5", "method" to "bank_transfer_wire"),
                    ),
                ),
            ),
            MoneyAccountModel(
                step = "send attempted over SPEI",
                report = {
                    onSendAttempted(savedRecipientDraft().copy(destination = offrampDestination(rail = BankRail.SPEI)))
                },
                expectedEvents = listOf(
                    transferEvent(
                        event = "VA Send Confirmed",
                        params = mapOf("amount_usdc" to "500", "method" to "bank_transfer_spei"),
                    ),
                ),
            ),
            MoneyAccountModel(
                step = "send attempted without a rail",
                report = { onSendAttempted(savedRecipientDraft().copy(destination = null)) },
                expectedEvents = emptyList(),
            ),
            MoneyAccountModel(
                step = "send attempted without an amount",
                report = { onSendAttempted(savedRecipientDraft().copy(amount = null)) },
                expectedEvents = emptyList(),
            ),
            MoneyAccountModel(
                step = "SPEI send failed with an error code",
                report = { onSendFailed(errorCode = FORBIDDEN_ERROR_CODE, rail = BankRail.SPEI) },
                expectedEvents = listOf(
                    transferEvent(
                        event = "VA Send failed",
                        params = mapOf("error_code" to "104120100", "method" to "bank_transfer_spei"),
                    ),
                ),
            ),
            MoneyAccountModel(
                step = "wire send failed without an error code",
                report = { onSendFailed(errorCode = null, rail = BankRail.WIRE) },
                expectedEvents = listOf(
                    transferEvent(event = "VA Send failed", params = mapOf("method" to "bank_transfer_wire")),
                ),
            ),
            MoneyAccountModel(
                step = "send failed without a rail",
                report = { onSendFailed(errorCode = null, rail = null) },
                expectedEvents = listOf(transferEvent("VA Send failed")),
            ),
            MoneyAccountModel(
                step = "ACH send succeeded",
                report = { onSendSucceeded(rail = BankRail.ACH) },
                expectedEvents = listOf(
                    transferEvent(event = "VA Send succesfull", params = mapOf("method" to "bank_transfer_ach")),
                ),
            ),
            MoneyAccountModel(
                step = "SPEI send succeeded",
                report = { onSendSucceeded(rail = BankRail.SPEI) },
                expectedEvents = listOf(
                    transferEvent(event = "VA Send succesfull", params = mapOf("method" to "bank_transfer_spei")),
                ),
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

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MoneyAccountWithMxnOff {

        private val withdrawAnalytics = BankWithdrawAnalytics(
            sourceAccount = BankWithdrawSourceAccount.Money(USER_WALLET_ID, productInstanceId = "instance-1"),
            analytics = analytics,
            moneyAccountFeatureToggles = mxnDisabled,
        )

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN MXN toggle off WHEN wizard step reported THEN events go out as before the MXN analytics`(
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
                step = "recipient selected",
                report = { onRecipientSelected(rail = BankRail.ACH) },
                expectedEvents = listOf(transferEvent("VA Offramp Recipient Selected")),
            ),
            MoneyAccountModel(
                step = "amount screen opened",
                report = { onAmountScreenOpened(rail = BankRail.WIRE) },
                expectedEvents = listOf(transferEvent("VA Offramp Amount Screen Opened")),
            ),
            MoneyAccountModel(
                step = "confirmation screen opened",
                report = { onConfirmationScreenOpened(savedRecipientDraft()) },
                expectedEvents = listOf(transferEvent("VA Offramp Confirmation Screen Opened")),
            ),
            MoneyAccountModel(
                step = "send clicked",
                report = { onSendClicked(savedRecipientDraft()) },
                expectedEvents = listOf(transferEvent("VA Offramp Recipient Transfer Confirmed")),
            ),
            MoneyAccountModel(
                step = "send attempted over ACH",
                report = { onSendAttempted(savedRecipientDraft()) },
                expectedEvents = listOf(
                    transferEvent(
                        event = "VA Send Confirmed",
                        params = mapOf("amount_usdc" to "500", "method" to "bank_transfer_ach"),
                    ),
                ),
            ),
            MoneyAccountModel(
                step = "send attempted over SPEI",
                report = {
                    onSendAttempted(savedRecipientDraft().copy(destination = offrampDestination(rail = BankRail.SPEI)))
                },
                expectedEvents = emptyList(),
            ),
            MoneyAccountModel(
                step = "send failed",
                report = { onSendFailed(errorCode = FORBIDDEN_ERROR_CODE, rail = BankRail.ACH) },
                expectedEvents = listOf(
                    transferEvent(event = "VA Send failed", params = mapOf("error_code" to "104120100")),
                ),
            ),
            MoneyAccountModel(
                step = "send succeeded",
                report = { onSendSucceeded(rail = BankRail.ACH) },
                expectedEvents = listOf(transferEvent("VA Send succesfull")),
            ),
            MoneyAccountModel(
                step = "new MXN-only events",
                report = {
                    onAmountErrorShown(rail = BankRail.ACH, reason = AmountErrorReason.BELOW_MINIMUM)
                    onRecipientFormOpened(newRecipientDraft(rail = BankRail.ACH))
                    onRecipientSaved(rail = BankRail.ACH, draft = newRecipientDraft(rail = BankRail.ACH))
                    onRecipientSaveFailed(rail = BankRail.ACH, errorCode = FORBIDDEN_ERROR_CODE)
                    onQuoteShown(rail = BankRail.SPEI, amount = BigDecimal("50"))
                    onSavedRecipientPicked(rail = BankRail.ACH)
                },
                expectedEvents = emptyList(),
            ),
        )

        private fun transferEvent(event: String, params: Map<String, String> = emptyMap()) =
            SentEvent(category = "VA Transfer", event = event, params = params)
    }

    @Test
    fun `GIVEN MXN toggle turned on after creation WHEN step reported THEN the rail is sent`() {
        // Arrange
        val toggles: MoneyAccountFeatureToggles = mockk { every { isMxnEnabled } returns false }
        val withdrawAnalytics = BankWithdrawAnalytics(
            sourceAccount = BankWithdrawSourceAccount.Money(USER_WALLET_ID, productInstanceId = "instance-1"),
            analytics = analytics,
            moneyAccountFeatureToggles = toggles,
        )
        every { toggles.isMxnEnabled } returns true

        // Act
        withdrawAnalytics.onAmountScreenOpened(rail = BankRail.SPEI)

        // Assert
        assertThat(sentEvents).containsExactly(MoneyAccountAnalyticsEvents.OfframpAmountScreenOpened(rail = BankRail.SPEI))
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

    private fun newRecipientDraft(
        rail: BankRail?,
        entry: BankWithdrawEntry = BankWithdrawEntry.NewPersonal,
        isThirdParty: Boolean? = null,
    ) = BankWithdrawDraft(
        entry = entry,
        newRecipient = NewRecipientDraft(isThirdParty = isThirdParty),
        amount = BigDecimal("500"),
        destination = rail?.let { offrampDestination(rail = it) },
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val FORBIDDEN_ERROR_CODE = 104_120_100
    }
}