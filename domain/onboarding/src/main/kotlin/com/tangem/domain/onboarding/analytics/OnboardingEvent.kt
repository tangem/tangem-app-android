package com.tangem.domain.onboarding.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.PRODUCT_TYPE
import com.tangem.domain.models.currency.CryptoCurrency

open class OnboardingEvent(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category, event, params) {

    data object Started : OnboardingEvent("Onboarding", "Onboarding Started")
    data object Finished : OnboardingEvent("Onboarding", "Onboarding Finished")
    data object ButtonMobileWallet : OnboardingEvent("Onboarding", "Button - Mobile Wallet")

    data class ButtonBuy(
        val source: AnalyticsParam.ScreensSources,
    ) : OnboardingEvent(
        category = "Onboarding",
        event = "Button - Buy",
        params = mapOf(AnalyticsParam.SOURCE to source.value),
    )

    sealed class CreateWallet(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : OnboardingEvent("Onboarding / Create Wallet", event, params) {

        data object ScreenOpened : CreateWallet("Create Wallet Screen Opened")
        data class ButtonCreateWallet(
            private val overridingProductType: AnalyticsParam.ProductType? = null,
        ) : CreateWallet(
            event = "Button - Create Wallet",
            params = buildMap {
                overridingProductType?.let { put(PRODUCT_TYPE, it.value) }
            },
        )
        data class ButtonOtherOptions(
            private val overridingProductType: AnalyticsParam.ProductType? = null,
        ) : CreateWallet(
            event = "Button - Other Options",
            params = buildMap {
                overridingProductType?.let { put(PRODUCT_TYPE, it.value) }
            },
        )
        data object ButtonStartUpgrade : CreateWallet("Button - Start upgrade")
        data object UpgradeWalletScreenOpened : CreateWallet("Upgrade Wallet Screen Opened")
        class WalletCreatedSuccessfully(
            creationType: WalletCreationType = WalletCreationType.PrivateKey,
            seedPhraseLength: Int? = null,
            passphrase: AnalyticsParam.EmptyFullState = AnalyticsParam.EmptyFullState.Empty,
            overridingProductType: AnalyticsParam.ProductType? = null,
        ) : CreateWallet(
            event = "Wallet Created Successfully",
            params = buildMap {
                put("Creation Type", creationType.value)
                if (seedPhraseLength != null) {
                    put("Seed Phrase Length", seedPhraseLength.toString())
                }
                put("Passphrase", passphrase.value)
                overridingProductType?.let { put(PRODUCT_TYPE, it.value) }
            },
        )

        sealed class WalletCreationType(val value: String) {
            data object PrivateKey : WalletCreationType(value = "Private Key")
            data object NewSeed : WalletCreationType(value = "New Seed")
            data object SeedImport : WalletCreationType(value = "Seed Import")
        }
    }

    sealed class Topup(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : OnboardingEvent("Onboarding / Top Up", event, params) {

        object ScreenOpened : Topup("Activation Screen Opened")

        object ButtonShowWalletAddress : Topup("Button - Show the Wallet Address")

        class ButtonBuyCrypto(currency: CryptoCurrency) : Topup(
            event = "Button - Buy Crypto",
            params = mapOf(AnalyticsParam.CURRENCY to currency.symbol),
        )
    }

    sealed class Backup(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : OnboardingEvent("Onboarding / Backup", event, params) {

        data object ScreenOpened : Backup("Backup Screen Opened")
        data object Started : Backup("Backup Started")
        data object Skipped : Backup("Backup Skipped")
        data object SettingAccessCodeStarted : Backup("Setting Access Code Started")
        data object AccessCodeEntered : Backup("Access Code Entered")
        data object AccessCodeReEntered : Backup("Access Code Re-entered")
        data object SeedPhraseInfo : Backup("Seed Phrase Info")
        data object SeedCheckingScreenOpened : Backup("Seed Checking Screen Opened")
        data object AccessCodeSkipped : Backup("Access Code Skipped")

        class Finished(cardsCount: Int) : Backup(
            event = "Backup Finished",
            params = mapOf("Cards count" to "$cardsCount"),
        )

        data object ResetCancelEvent : Backup(
            event = "Reset Card Notification",
            params = mapOf("Option" to "Cancel"),
        )

        data object ResetPerformEvent : Backup(
            event = "Reset Card Notification",
            params = mapOf("Option" to "Reset"),
        )

        data object ResumeInterruptedBackup : Backup(
            event = "Notice - Backup Canceled",
            params = mapOf("Action" to "Resume"),
        )

        data object CancelInterruptedBackup : Backup(
            event = "Notice - Backup Canceled",
            params = mapOf("Action" to "Cancel"),
        )
    }

    sealed class Twins(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : OnboardingEvent("Onboarding / Twins", event, params) {

        data object ScreenOpened : Twins("Twinning Screen Opened")
        data object SetupStarted : Twins("Twin Setup Started")
        data object SetupFinished : Twins("Twin Setup Finished")
    }

    data class OfflineAttestationFailed(
        val source: AnalyticsParam.ScreensSources,
    ) : OnboardingEvent(
        category = "Error",
        event = "Offline Attestation Failed",
        params = mapOf(AnalyticsParam.SOURCE to source.value),
    )

    sealed class SeedPhrase(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : OnboardingEvent("Onboarding / Seed Phrase", event, params) {

        data object ImportWalletScreenOpened : SeedPhrase("Import Wallet Screen Opened")
        data class ButtonImportWallet(
            private val overridingProductType: AnalyticsParam.ProductType? = null,
        ) : SeedPhrase(
            event = "Button - Import Wallet",
            params = buildMap {
                overridingProductType?.let { put(PRODUCT_TYPE, it.value) }
            },
        )
        data class ImportSeedPhraseScreenOpened(
            private val overridingProductType: AnalyticsParam.ProductType? = null,
        ) : SeedPhrase(
            event = "Import Seed Phrase Screen Opened",
            params = buildMap {
                overridingProductType?.let { put(PRODUCT_TYPE, it.value) }
            },
        )
        data class ButtonImport(
            private val overridingProductType: AnalyticsParam.ProductType? = null,
        ) : SeedPhrase(
            event = "Button - Import",
            params = buildMap {
                overridingProductType?.let { put(PRODUCT_TYPE, it.value) }
            },
        )
    }
}