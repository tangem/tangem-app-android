package com.tangem.features.tangempay.common.kyc

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.modal.TangemModalActionScope
import com.tangem.core.ui.ds2.modal.tangemModalStatusUM
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.ModalMessage
import com.tangem.core.ui.message.modal.MessageModalUM
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_lightning_24
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.AccountType
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.CloseMethod
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.ContactSupportClicked
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.EntryPoint
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.NoticeShown
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.Placement
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.PopupClosed
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.PopupShown
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.ProvideInformationClicked
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.Source
import com.tangem.domain.tangempay.model.KycAdditionalInfoAccount
import com.tangem.domain.tangempay.repository.KycAdditionalInfoPopupStore
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class KycAdditionalInfoNoticeTest {

    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val onProvideInformation: () -> Unit = mockk(relaxed = true)
    private val onContactSupport: () -> Unit = mockk(relaxed = true)
    private val popupStore: KycAdditionalInfoPopupStore = mockk()
    private val userWalletId = UserWalletId("1234")

    @BeforeEach
    fun resetMocks() {
        clearMocks(analytics, uiMessageSender, onProvideInformation, onContactSupport, popupStore)
        coEvery { popupStore.tryMarkShown(any()) } returns true
        coEvery { popupStore.reset(any()) } returns Unit
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnStatus {

        @Test
        fun `GIVEN flag true first time AND popup allowed WHEN onStatus THEN notice and popup shown with modal`() =
            runTest {
                // Arrange
                val notice = createNotice()

                // Act
                notice.onStatus(isRequired = true, isPopupAllowed = true)

                // Assert
                verify(exactly = 1) { analytics.send(NoticeShown(AccountType.PAYMENT, Placement.ACCOUNT_BLOCK)) }
                verify(exactly = 1) { analytics.send(PopupShown(AccountType.PAYMENT, EntryPoint.ACCOUNT_ENTRY)) }
                verify(exactly = 2) { analytics.send(any()) }
                verify(exactly = 1) { uiMessageSender.send(any<ModalMessage>()) }
                coVerify(exactly = 1) {
                    popupStore.tryMarkShown(KycAdditionalInfoAccount(userWalletId, AccountType.PAYMENT))
                }
            }

        @Test
        fun `GIVEN popup already shown in session WHEN flag appears THEN notice sent and popup not shown`() = runTest {
            // Arrange
            coEvery { popupStore.tryMarkShown(any()) } returns false
            val notice = createNotice()

            // Act
            notice.onStatus(isRequired = true, isPopupAllowed = true)

            // Assert
            verify(exactly = 1) { analytics.send(NoticeShown(AccountType.PAYMENT, Placement.ACCOUNT_BLOCK)) }
            verify(exactly = 0) { analytics.send(ofType<PopupShown>()) }
            verify(exactly = 0) { uiMessageSender.send(any()) }
        }

        @Test
        fun `GIVEN popup not allowed WHEN flag appears THEN popup not marked as shown`() = runTest {
            // Arrange
            val notice = createNotice()

            // Act
            notice.onStatus(isRequired = true, isPopupAllowed = false)

            // Assert
            coVerify(exactly = 0) { popupStore.tryMarkShown(any()) }
        }

        @Test
        fun `GIVEN flag false WHEN onStatus THEN popup shown state reset for account`() = runTest {
            // Arrange
            val notice = createNotice(accountType = AccountType.MONEY)

            // Act
            notice.onStatus(isRequired = false, isPopupAllowed = true)

            // Assert
            coVerify(exactly = 1) { popupStore.reset(KycAdditionalInfoAccount(userWalletId, AccountType.MONEY)) }
        }

        @Test
        fun `GIVEN flag true first time AND popup not allowed WHEN onStatus THEN notice shown without popup`() =
            runTest {
                // Arrange
                val notice = createNotice()

                // Act
                notice.onStatus(isRequired = true, isPopupAllowed = false)

                // Assert
                verify(exactly = 1) { analytics.send(NoticeShown(AccountType.PAYMENT, Placement.ACCOUNT_BLOCK)) }
                verify(exactly = 0) { analytics.send(ofType<PopupShown>()) }
                verify(exactly = 0) { uiMessageSender.send(any()) }
            }

        @Test
        fun `GIVEN flag already true WHEN onStatus true again THEN nothing sent`() = runTest {
            // Arrange
            val notice = createNotice()
            notice.onStatus(isRequired = true, isPopupAllowed = true)
            clearMocks(analytics, uiMessageSender)

            // Act
            notice.onStatus(isRequired = true, isPopupAllowed = true)

            // Assert
            verify(exactly = 0) { analytics.send(any()) }
            verify(exactly = 0) { uiMessageSender.send(any()) }
        }

        @Test
        fun `GIVEN flag appeared without popup WHEN flag true again with popup allowed THEN popup not shown`() =
            runTest {
                // Arrange
                val notice = createNotice()
                notice.onStatus(isRequired = true, isPopupAllowed = false)
                clearMocks(analytics, uiMessageSender)

                // Act
                notice.onStatus(isRequired = true, isPopupAllowed = true)

                // Assert
                verify(exactly = 0) { analytics.send(any()) }
                verify(exactly = 0) { uiMessageSender.send(any()) }
            }

        @Test
        fun `GIVEN flag dropped WHEN flag returns THEN notice and popup sent again`() = runTest {
            // Arrange
            val notice = createNotice()
            notice.onStatus(isRequired = true, isPopupAllowed = true)
            notice.onStatus(isRequired = false, isPopupAllowed = true)

            // Act
            notice.onStatus(isRequired = true, isPopupAllowed = true)

            // Assert
            verify(exactly = 2) { analytics.send(NoticeShown(AccountType.PAYMENT, Placement.ACCOUNT_BLOCK)) }
            verify(exactly = 2) { analytics.send(PopupShown(AccountType.PAYMENT, EntryPoint.ACCOUNT_ENTRY)) }
            verify(exactly = 2) { uiMessageSender.send(any<ModalMessage>()) }
        }

        @Test
        fun `GIVEN flag false WHEN onStatus THEN nothing sent`() = runTest {
            // Arrange
            val notice = createNotice()

            // Act
            notice.onStatus(isRequired = false, isPopupAllowed = true)

            // Assert
            verify(exactly = 0) { analytics.send(any()) }
            verify(exactly = 0) { uiMessageSender.send(any()) }
        }

        @Test
        fun `GIVEN opened from push AND popup already shown in session WHEN flag appears THEN popup shown with push entry point`() =
            runTest {
                // Arrange
                coEvery { popupStore.tryMarkShown(any()) } returns false
                val notice = createNotice(isOpenedFromPush = true)

                // Act
                notice.onStatus(isRequired = true, isPopupAllowed = true)

                // Assert
                verify(exactly = 1) {
                    analytics.send(NoticeShown(accountType = AccountType.PAYMENT, placement = Placement.ACCOUNT_BLOCK))
                }
                verify(exactly = 1) {
                    analytics.send(PopupShown(accountType = AccountType.PAYMENT, entryPoint = EntryPoint.PUSH))
                }
                verify(exactly = 1) { uiMessageSender.send(any()) }
                coVerify(exactly = 1) {
                    popupStore.tryMarkShown(KycAdditionalInfoAccount(userWalletId, AccountType.PAYMENT))
                }
            }

        @Test
        fun `GIVEN popup from push shown AND flag dropped WHEN flag returns THEN popup uses account entry point`() =
            runTest {
                // Arrange
                val notice = createNotice(isOpenedFromPush = true)
                notice.onStatus(isRequired = true, isPopupAllowed = true)
                notice.onStatus(isRequired = false, isPopupAllowed = true)

                // Act
                notice.onStatus(isRequired = true, isPopupAllowed = true)

                // Assert
                verify(exactly = 1) { analytics.send(PopupShown(AccountType.PAYMENT, EntryPoint.PUSH)) }
                verify(exactly = 1) { analytics.send(PopupShown(AccountType.PAYMENT, EntryPoint.ACCOUNT_ENTRY)) }
            }

        @Test
        fun `GIVEN opened from push AND first status without flag WHEN flag appears later THEN popup uses push entry point`() =
            runTest {
                // Arrange
                coEvery { popupStore.tryMarkShown(any()) } returns false
                val notice = createNotice(isOpenedFromPush = true)
                notice.onStatus(isRequired = false, isPopupAllowed = true)

                // Act
                notice.onStatus(isRequired = true, isPopupAllowed = true)

                // Assert
                verify(exactly = 1) { analytics.send(PopupShown(AccountType.PAYMENT, EntryPoint.PUSH)) }
                verify(exactly = 1) { uiMessageSender.send(any()) }
            }

        @Test
        fun `GIVEN popup from push shown WHEN flag reported again THEN nothing sent again`() = runTest {
            // Arrange
            val notice = createNotice(isOpenedFromPush = true)
            notice.onStatus(isRequired = true, isPopupAllowed = true)

            // Act
            notice.onStatus(isRequired = true, isPopupAllowed = true)

            // Assert
            verify(exactly = 1) { analytics.send(PopupShown(AccountType.PAYMENT, EntryPoint.PUSH)) }
            verify(exactly = 2) { analytics.send(any()) }
            verify(exactly = 1) { uiMessageSender.send(any()) }
        }

        @Test
        fun `GIVEN opened from push AND popup not allowed WHEN flag appears THEN popup not shown`() = runTest {
            // Arrange
            val notice = createNotice(isOpenedFromPush = true)

            // Act
            notice.onStatus(isRequired = true, isPopupAllowed = false)

            // Assert
            verify(exactly = 0) { analytics.send(ofType<PopupShown>()) }
            verify(exactly = 0) { uiMessageSender.send(any()) }
            coVerify(exactly = 0) { popupStore.tryMarkShown(any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Popup {

        @Test
        fun `GIVEN modal WHEN content inspected THEN warning status with lightning and both buttons`() = runTest {
            // Act
            val popup = showPopup(createNotice())

            // Assert
            val expectedContent = tangemModalStatusUM {
                variant = TangemModalStatus.Variant.Warning
                glyph(Icons.ic_lightning_24)
                title = resourceReference(R.string.tangempay_kyc_additional_info_title)
                description = resourceReference(R.string.tangempay_kyc_additional_info_popup_description)
            }
            assertThat(popup.content).isEqualTo(expectedContent)
            assertThat(popup.title).isNull()
            assertThat(popup.buttons.map { it.variant to it.text }).containsExactly(
                TangemButton.Variant.Secondary to resourceReference(R.string.common_contact_support),
                TangemButton.Variant.Primary to resourceReference(R.string.tangempay_kyc_additional_info_button),
            ).inOrder()
        }

        @Test
        fun `GIVEN modal WHEN provide information clicked THEN event sent and callback invoked without popup closed`() =
            runTest {
                // Arrange
                val popup = showPopup(createNotice())
                val host = FakeModalHost(popup)
                clearMocks(analytics)

                // Act
                popup.provideInformationButton().onClick(host)

                // Assert
                verify(exactly = 1) {
                    analytics.send(
                        ProvideInformationClicked(AccountType.PAYMENT, Source.POPUP, EntryPoint.ACCOUNT_ENTRY),
                    )
                }
                verify(exactly = 0) { analytics.send(ofType<PopupClosed>()) }
                verify(exactly = 1) { analytics.send(any()) }
                verify(exactly = 1) { onProvideInformation() }
                verify(exactly = 0) { onContactSupport() }
                assertThat(host.dismissCount).isEqualTo(1)
            }

        @Test
        fun `GIVEN modal WHEN contact support clicked THEN event sent and callback invoked without popup closed`() =
            runTest {
                // Arrange
                val popup = showPopup(createNotice())
                val host = FakeModalHost(popup)
                clearMocks(analytics)

                // Act
                popup.contactSupportButton().onClick(host)

                // Assert
                verify(exactly = 1) {
                    analytics.send(ContactSupportClicked(AccountType.PAYMENT, EntryPoint.ACCOUNT_ENTRY))
                }
                verify(exactly = 0) { analytics.send(ofType<PopupClosed>()) }
                verify(exactly = 1) { analytics.send(any()) }
                verify(exactly = 1) { onContactSupport() }
                verify(exactly = 0) { onProvideInformation() }
                assertThat(host.dismissCount).isEqualTo(1)
            }

        @Test
        fun `GIVEN modal WHEN dismissed without action THEN popup closed sent with unknown method`() = runTest {
            // Arrange
            val popup = showPopup(createNotice())
            clearMocks(analytics)

            // Act
            popup.onDismiss()

            // Assert
            verify(exactly = 1) {
                analytics.send(PopupClosed(AccountType.PAYMENT, EntryPoint.ACCOUNT_ENTRY, CloseMethod.UNKNOWN))
            }
            verify(exactly = 1) { analytics.send(any()) }
            verify(exactly = 0) { onProvideInformation() }
            verify(exactly = 0) { onContactSupport() }
        }

        @Test
        fun `GIVEN action chosen in previous modal WHEN next modal dismissed without action THEN popup closed sent`() =
            runTest {
                // Arrange
                val notice = createNotice()
                val previousPopup = showPopup(notice)
                previousPopup.provideInformationButton().onClick(FakeModalHost(previousPopup))
                notice.onStatus(isRequired = false, isPopupAllowed = true)
                clearMocks(uiMessageSender)
                val nextPopup = showPopup(notice)
                clearMocks(analytics)

                // Act
                nextPopup.onDismiss()

                // Assert
                verify(exactly = 1) {
                    analytics.send(PopupClosed(AccountType.PAYMENT, EntryPoint.ACCOUNT_ENTRY, CloseMethod.UNKNOWN))
                }
            }

        @Test
        fun `GIVEN modal dismissed WHEN onDismiss invoked again THEN popup closed sent once`() = runTest {
            // Arrange
            val popup = showPopup(createNotice())
            clearMocks(analytics)
            popup.onDismiss()

            // Act
            popup.onDismiss()

            // Assert
            verify(exactly = 1) {
                analytics.send(PopupClosed(AccountType.PAYMENT, EntryPoint.ACCOUNT_ENTRY, CloseMethod.UNKNOWN))
            }
            verify(exactly = 1) { analytics.send(any()) }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN modal resolved WHEN another action THEN nothing sent and no callback invoked`(model: ResolvedModel) =
            runTest {
                // Arrange
                val popup = showPopup(createNotice())
                val host = FakeModalHost(popup)
                popup.perform(action = model.resolvedBy, host = host)
                clearMocks(analytics, onProvideInformation, onContactSupport)
                val dismissCountAfterResolution = host.dismissCount

                // Act
                popup.perform(action = model.followedBy, host = host)

                // Assert
                verify(exactly = 0) { analytics.send(any()) }
                verify(exactly = 0) { onProvideInformation() }
                verify(exactly = 0) { onContactSupport() }
                assertThat(host.dismissCount).isEqualTo(dismissCountAfterResolution)
            }

        private fun provideTestModels() = listOf(
            ResolvedModel(resolvedBy = PopupAction.PROVIDE_INFORMATION, followedBy = PopupAction.PROVIDE_INFORMATION),
            ResolvedModel(resolvedBy = PopupAction.PROVIDE_INFORMATION, followedBy = PopupAction.CONTACT_SUPPORT),
            ResolvedModel(resolvedBy = PopupAction.PROVIDE_INFORMATION, followedBy = PopupAction.DISMISS),
            ResolvedModel(resolvedBy = PopupAction.CONTACT_SUPPORT, followedBy = PopupAction.CONTACT_SUPPORT),
            ResolvedModel(resolvedBy = PopupAction.CONTACT_SUPPORT, followedBy = PopupAction.PROVIDE_INFORMATION),
            ResolvedModel(resolvedBy = PopupAction.CONTACT_SUPPORT, followedBy = PopupAction.DISMISS),
            ResolvedModel(resolvedBy = PopupAction.DISMISS, followedBy = PopupAction.PROVIDE_INFORMATION),
            ResolvedModel(resolvedBy = PopupAction.DISMISS, followedBy = PopupAction.CONTACT_SUPPORT),
        )

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        inner class OpenedFromPush {

            @ParameterizedTest
            @ProvideTestModels
            fun `GIVEN modal opened from push WHEN user acts THEN event carries push entry point`(model: PushActionModel) =
                runTest {
                    // Arrange
                    val popup = showPopup(createNotice(isOpenedFromPush = true))
                    clearMocks(analytics)

                    // Act
                    popup.perform(action = model.action, host = FakeModalHost(popup))

                    // Assert
                    verify(exactly = 1) { analytics.send(model.expectedEvent) }
                    verify(exactly = 1) { analytics.send(any()) }
                }

            private fun provideTestModels() = listOf(
                PushActionModel(
                    action = PopupAction.PROVIDE_INFORMATION,
                    expectedEvent = ProvideInformationClicked(AccountType.PAYMENT, Source.POPUP, EntryPoint.PUSH),
                ),
                PushActionModel(
                    action = PopupAction.CONTACT_SUPPORT,
                    expectedEvent = ContactSupportClicked(AccountType.PAYMENT, EntryPoint.PUSH),
                ),
                PushActionModel(
                    action = PopupAction.DISMISS,
                    expectedEvent = PopupClosed(AccountType.PAYMENT, EntryPoint.PUSH, CloseMethod.UNKNOWN),
                ),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnBannerProvideInformationClick {

        @Test
        fun `GIVEN notice WHEN banner provide information clicked THEN event with account block source and callback`() =
            runTest {
                // Arrange
                val notice = createNotice(accountType = AccountType.MONEY)

                // Act
                notice.onBannerProvideInformationClick()

                // Assert
                verify(exactly = 1) {
                    analytics.send(
                        ProvideInformationClicked(AccountType.MONEY, Source.ACCOUNT_BLOCK, EntryPoint.ACCOUNT_BLOCK),
                    )
                }
                verify(exactly = 1) { analytics.send(any()) }
                verify(exactly = 1) { onProvideInformation() }
                verify(exactly = 0) { uiMessageSender.send(any()) }
            }
    }

    private fun createNotice(
        accountType: AccountType = AccountType.PAYMENT,
        isOpenedFromPush: Boolean = false,
    ) = KycAdditionalInfoNotice(
        account = KycAdditionalInfoAccount(userWalletId = userWalletId, accountType = accountType),
        popupStore = popupStore,
        analytics = analytics,
        uiMessageSender = uiMessageSender,
        isOpenedFromPush = isOpenedFromPush,
        onProvideInformation = onProvideInformation,
        onContactSupport = onContactSupport,
    )

    private suspend fun showPopup(notice: KycAdditionalInfoNotice): MessageModalUM {
        val message = slot<ModalMessage>()
        notice.onStatus(isRequired = true, isPopupAllowed = true)
        verify { uiMessageSender.send(capture(message)) }
        return message.captured.messageModalUM
    }

    private fun MessageModalUM.provideInformationButton(): MessageModalUM.Button =
        buttons.single { it.variant == TangemButton.Variant.Primary }

    private fun MessageModalUM.contactSupportButton(): MessageModalUM.Button =
        buttons.single { it.variant == TangemButton.Variant.Secondary }

    private fun MessageModalUM.perform(action: PopupAction, host: FakeModalHost) {
        when (action) {
            PopupAction.PROVIDE_INFORMATION -> provideInformationButton().onClick(host)
            PopupAction.CONTACT_SUPPORT -> contactSupportButton().onClick(host)
            PopupAction.DISMISS -> onDismiss()
        }
    }

    private class FakeModalHost(private val popup: MessageModalUM) : TangemModalActionScope {

        var dismissCount = 0
            private set

        override fun dismiss() {
            dismissCount++
            popup.onDismiss()
        }
    }

    data class ResolvedModel(val resolvedBy: PopupAction, val followedBy: PopupAction)

    data class PushActionModel(val action: PopupAction, val expectedEvent: TangemPayKycAdditionalInfoAnalyticsEvents)

    enum class PopupAction {
        PROVIDE_INFORMATION,
        CONTACT_SUPPORT,
        DISMISS,
    }
}