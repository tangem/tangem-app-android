package com.tangem.tap.features.details.ui.details

import com.tangem.domain.common.TapWorkarounds.isSaltPay
import com.tangem.domain.common.TapWorkarounds.isStart2Coin
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
                SettingsElement.Chat -> if (state.scanResponse?.card?.isSaltPay != true) it else null
                SettingsElement.LinkMoreCards -> {
                    // if (state.createBackupAllowed) it else null
// [REDACTED_TODO_COMMENT]
                    if (state.createBackupAllowed && state.scanResponse?.card?.isSaltPay != true) it else null
                }
                SettingsElement.PrivacyPolicy -> {
                    if (state.privacyPolicyUrl != null) it else null
                }
                SettingsElement.AppSettings -> null // TODO: until we implement settings from this screen
                SettingsElement.AppCurrency -> if (state.scanResponse?.card?.isMultiwalletAllowed != true) it else null
                SettingsElement.TermsOfUse -> if (state.scanResponse?.card?.isStart2Coin == true) it else null
                else -> it
            }
        }

        return DetailsScreenState(
            settings,
            tangemLinks = getSocialLinks(),
            tangemVersion = getTangemAppVersion(),
            appCurrency = state.appCurrency.name,
            onItemsClick = { handleClickingSettingsItem(it) },
            onSocialNetworkClick = { handleSocialNetworkClick(it) },
        )
    }

    private fun handleSocialNetworkClick(url: String) {
        store.dispatch(NavigationAction.OpenUrl(url))
    }

    private fun handleClickingSettingsItem(item: SettingsElement) {
        when (item) {
            SettingsElement.WalletConnect -> {
                store.dispatch(NavigationAction.NavigateTo(AppScreen.WalletConnectSessions))
            }
            SettingsElement.Chat -> {
                store.dispatch(GlobalAction.OpenChat(SupportInfo()))
            }
            SettingsElement.SendFeedback -> {
                store.dispatch(GlobalAction.SendEmail(FeedbackEmail()))
            }
            SettingsElement.CardSettings -> {
                store.dispatch(NavigationAction.NavigateTo(AppScreen.CardSettings))
            }
            SettingsElement.AppCurrency -> {
                store.dispatch(WalletAction.AppCurrencyAction.ChooseAppCurrency)
            }
            SettingsElement.AppSettings -> {
                store.dispatch(NavigationAction.NavigateTo(AppScreen.AppSettings)) //TODO: To be available later
            }
            SettingsElement.LinkMoreCards -> {
                store.dispatch(DetailsAction.CreateBackup)
            }
            SettingsElement.TermsOfService -> {
                store.dispatch(DisclaimerAction.Show())
            }
            SettingsElement.TermsOfUse -> {
                store.dispatch(DetailsAction.ShowDisclaimer)
            }
            SettingsElement.PrivacyPolicy -> {
// [REDACTED_TODO_COMMENT]
            }
        }
    }

    private fun getSocialLinks(): List<TangemLink> {
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
