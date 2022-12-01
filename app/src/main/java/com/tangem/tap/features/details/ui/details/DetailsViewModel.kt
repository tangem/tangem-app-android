package com.tangem.tap.features.details.ui.details

import com.tangem.domain.common.TapWorkarounds.isSaltPay
import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.tap.common.analytics.Analytics
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.common.feedback.FeedbackEmail
import com.tangem.tap.common.feedback.SupportInfo
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.extensions.isMultiwalletAllowed
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.features.disclaimer.redux.DisclaimerAction
import com.tangem.tap.features.home.LocaleRegionProvider
import com.tangem.tap.features.home.RUSSIA_COUNTRY_CODE
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.wallet.BuildConfig
import org.rekotlin.Store

class DetailsViewModel(private val store: Store<AppState>) {
    fun updateState(state: DetailsState): DetailsScreenState {
        val settings = SettingsElement.values().mapNotNull {
            when (it) {
                SettingsElement.WalletConnect -> {
                    if (state.scanResponse?.card?.isMultiwalletAllowed == true) it else null
                }
                SettingsElement.SendFeedback -> if (state.scanResponse?.card?.isSaltPay != true) it else null
                SettingsElement.LinkMoreCards -> {
                    // if (state.createBackupAllowed) it else null
                    // TODO: SaltPay: temporary excluding backup process for Visa cards
                    if (state.createBackupAllowed && state.scanResponse?.card?.isSaltPay != true) it else null
                }
                SettingsElement.PrivacyPolicy -> {
                    if (state.privacyPolicyUrl != null) it else null
                }
                SettingsElement.AppSettings -> if (state.isBiometricsAvailable) it else null
                SettingsElement.AppCurrency -> if (state.scanResponse?.card?.isMultiwalletAllowed != true) it else null
                SettingsElement.TermsOfUse -> if (state.scanResponse?.card?.isStart2Coin == true) it else null
                else -> it
            }
        }

        return DetailsScreenState(
            elements = settings,
            tangemLinks = getSocialLinks(),
            tangemVersion = getTangemAppVersion(),
            appCurrency = state.appCurrency.name,
            onItemsClick = { handleClickingSettingsItem(it) },
            onSocialNetworkClick = { handleSocialNetworkClick(it) },
        )
    }

    private fun handleSocialNetworkClick(link: SocialNetworkLink) {
        Analytics.send(Settings.ButtonSocialNetwork(link.network))
        store.dispatch(NavigationAction.OpenUrl(link.url))
    }

    private fun handleClickingSettingsItem(item: SettingsElement) {
        when (item) {
            SettingsElement.WalletConnect -> {
                store.dispatch(NavigationAction.NavigateTo(AppScreen.WalletConnectSessions))
            }
            SettingsElement.Chat -> {
                Analytics.send(Settings.ButtonChat())
                store.dispatch(GlobalAction.OpenChat(SupportInfo()))
            }
            SettingsElement.SendFeedback -> {
                Analytics.send(Settings.ButtonSendFeedback())
                store.dispatch(GlobalAction.SendEmail(FeedbackEmail()))
            }
            SettingsElement.CardSettings -> {
                Analytics.send(Settings.ButtonCardSettings())
                store.dispatch(NavigationAction.NavigateTo(AppScreen.CardSettings))
            }
            SettingsElement.AppCurrency -> {
                store.dispatch(WalletAction.AppCurrencyAction.ChooseAppCurrency)
            }
            SettingsElement.AppSettings -> {
                Analytics.send(Settings.ButtonAppSettings())
                store.dispatch(NavigationAction.NavigateTo(AppScreen.AppSettings))
            }
            SettingsElement.LinkMoreCards -> {
                Analytics.send(Settings.ButtonCreateBackup())
                store.dispatch(DetailsAction.CreateBackup)
            }
            SettingsElement.TermsOfService -> {
                store.dispatch(DisclaimerAction.Show())
            }
            SettingsElement.TermsOfUse -> {
                store.dispatch(DetailsAction.ShowDisclaimer)
            }
            SettingsElement.PrivacyPolicy -> {
                // TODO: To be available later
            }
            // SettingsElement.ReferralProgram -> {
            //     store.dispatch(NavigationAction.NavigateTo(AppScreen.ReferralProgram))
            // }
        }
    }

    private fun getSocialLinks(): List<SocialNetworkLink> {
        val locale = LocaleRegionProvider().getRegion()
        return if (locale.lowercase() == RUSSIA_COUNTRY_CODE) {
            TangemSocialAccounts.accountsRu
        } else {
            TangemSocialAccounts.accountsEn
        }
    }

    private fun getTangemAppVersion(): String {
        val versionCode: Int = BuildConfig.VERSION_CODE
        val versionName: String = BuildConfig.VERSION_NAME
        return "$versionName ($versionCode)"
    }
}
