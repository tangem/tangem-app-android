package com.tangem.tap.features.home.redux

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import org.rekotlin.StateType
import java.util.Locale

@Immutable
data class HomeState(
    val scanInProgress: Boolean = false,
    val stories: List<Stories> = initDefaultStories(),
) : StateType {

    val firstStory: Stories
        get() = stories[0]

    fun stepOf(story: Stories): Int = stories.indexOf(story)

    fun onCountryCodeUpdate(homeState: HomeState, countryCode: String) {
        val isNewWalletAvailable = !(countryCode == RUSSIA_COUNTRY_CODE || countryCode == BELARUS_COUNTRY_CODE)
        homeState.stories.forEach {
            it.isNewWalletAvailable.value = isNewWalletAvailable
        }
    }

    companion object {
        private const val RUSSIA_COUNTRY_CODE = "ru"
        private const val BELARUS_COUNTRY_CODE = "by"
        fun initDefaultStories(): List<Stories> = listOf(
            Stories.TangemIntro,
            Stories.RevolutionaryWallet,
            Stories.UltraSecureBackup,
            Stories.Currencies,
            Stories.Web3,
            Stories.WalletForEveryone,
        )

        fun isNewWalletAvailableInit(): Boolean {
            val locale = Locale.getDefault().language
            return !(locale == RUSSIA_COUNTRY_CODE || locale == BELARUS_COUNTRY_CODE)
        }
    }
}

sealed class Stories(
    val duration: Int,
    val isNewWalletAvailable: MutableState<Boolean> = mutableStateOf(HomeState.isNewWalletAvailableInit()),
) {
    object TangemIntro : Stories(duration = 6000)
    object RevolutionaryWallet : Stories(duration = 6000)
    object UltraSecureBackup : Stories(duration = 6000)
    object Currencies : Stories(duration = 6000)
    object Web3 : Stories(duration = 6000)
    object WalletForEveryone : Stories(duration = 6000)
}
