package com.tangem.domain.card.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.CriticalEvent
import com.tangem.core.analytics.models.getReferralParams

sealed class IntroductionProcess(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent("Introduction Process", event, params) {

    /**
     * Tracks the user opening the Introduction Process screen.
     *
     * @param entryReason why the screen appeared; mandatory on the redesigned Welcome screen, absent on the legacy one.
     */
    class ScreenOpened(
        entryReason: EntryReason? = null,
    ) : IntroductionProcess(
        event = "Introduction Process Screen Opened",
        params = buildMap { entryReason?.let { put(AnalyticsParam.ENTRY_REASON, it.value) } },
    ), CriticalEvent {
        enum class EntryReason(val value: String) {
            FirstLaunch("First Launch"),
            NoWallets("No Wallets"),
            LastWalletForgotten("Last Wallet Forgotten"),
        }
    }

    class SheetDismissed(
        sheet: Sheet,
        reason: DismissReason,
    ) : IntroductionProcess(
        event = "Sheet Dismissed",
        params = mapOf(
            AnalyticsParam.SHEET to sheet.value,
            AnalyticsParam.DISMISS_REASON to reason.value,
        ),
    ) {
        enum class Sheet(val value: String) {
            CreateWallet("Create Wallet"),
            IHaveWallet("I Have Wallet"),
            ImportExisting("Import Existing"),
        }

        enum class DismissReason(val value: String) {
            CloseButton("Close Button"),
            BackButton("Back Button"),
            Dismissed("Dismissed"),
        }
    }

    class LegalOpened(
        document: Document,
    ) : IntroductionProcess(
        event = "Legal Opened",
        params = mapOf(AnalyticsParam.DOCUMENT to document.value),
    ) {
        enum class Document(val value: String) {
            TermsOfService("Terms Of Service"),
            PrivacyPolicy("Privacy Policy"),
        }
    }

    class ScanCancelled(
        source: AnalyticsParam.ScreensSources,
    ) : IntroductionProcess(
        event = "Scan Cancelled",
        params = mapOf(AnalyticsParam.SOURCE to source.value),
    )

    class WelcomeMediaError(
        stage: Stage,
    ) : IntroductionProcess(
        event = "Welcome Media Error",
        params = mapOf(AnalyticsParam.STAGE to stage.value),
    ) {
        enum class Stage(val value: String) {
            Load("Load"),
            Playback("Playback"),
        }
    }

    /**
     * Tracks the user starting wallet creation from the Introduction screen.
     */
    class ButtonCreateNewWallet : IntroductionProcess("Button - Create New Wallet")
    class ButtonIHaveWallet : IntroductionProcess("Button - I Have Wallet")
    class ButtonBuyTangem : IntroductionProcess("Button - Buy Tangem")
    class ButtonHardwareWallet : IntroductionProcess("Button - Hardware Wallet")
    class ButtonSignInWithCard : IntroductionProcess("Button - Sign In With Card")
    class ButtonTokensList : IntroductionProcess("Button - Tokens List")
    class ButtonBuyCards : IntroductionProcess("Button - Buy Cards")
    class ButtonScanCardLegacy : IntroductionProcess("Button - Scan Card")

    /**
     * Carries its own Product Type: no mobile wallet context is installed when the I have a wallet sheet sends it, and
     * a card context left over from an abandoned card onboarding must not override it.
     */
    class ButtonImportExistingWallet : IntroductionProcess(
        event = "Button - Import Existing Wallet",
        params = mapOf(AnalyticsParam.PRODUCT_TYPE to AnalyticsParam.ProductType.MobileWallet.value),
    )

    /**
     * Tracks opening the Create Wallet introduction screen.
     *
     * @param screenType the wallet type the screen is about; `null` for the Create wallet sheet, which offers both.
     */
    class CreateWalletIntroScreenOpened(
        screenType: ScreenType?,
        referralId: String?,
    ) : IntroductionProcess(
        event = "Create Wallet Intro Screen Opened",
        params = buildMap {
            screenType?.let { put(AnalyticsParam.SCREEN_TYPE, it.value) }
            putAll(getReferralParams(referralId))
        },
    ), CriticalEvent {
        enum class ScreenType(val value: String) {
            Cold("Cold Wallet"),
            Hot("Mobile Wallet"),
        }
    }

    /**
     * Tracks opening the I have a wallet sheet with the Welcome screen button; going back to it from Import existing
     * wallet is not an opening.
     */
    class IHaveWalletIntroScreenOpened : IntroductionProcess("I Have Wallet Intro Screen Opened")

    class ButtonScanCard(
        val source: AnalyticsParam.ScreensSources,
    ) : IntroductionProcess(
        event = "Button - Scan Card",
        params = mapOf(
            AnalyticsParam.SOURCE to source.value,
        ),
    )
}