package com.tangem.tap.features.home.redux

import com.tangem.tap.common.entities.IndeterminateProgressButton
import com.tangem.tap.features.send.redux.states.ButtonState
import com.tangem.tap.features.wallet.redux.ProgressState
import org.rekotlin.StateType

data class HomeState(
    val scanInProgress: Boolean = false,
    val btnScanState: IndeterminateProgressButton = IndeterminateProgressButton(ButtonState.ENABLED),
    val stories: List<Stories> = initDefaultStories(),
) : StateType {

    val firstStory: Stories
        get() = stories[0]

    val btnScanStateInProgress: Boolean
        get() = btnScanState.progressState == ProgressState.Loading

    fun stepOf(story: Stories): Int = stories.indexOf(story)

    companion object {
        fun initDefaultStories(): List<Stories> = listOf(
            Stories.TangemIntro,
            Stories.RevolutionaryWallet,
            Stories.UltraSecureBackup,
            Stories.Currencies,
            Stories.Web3,
            Stories.WalletForEveryone,
        )
    }
}

sealed class Stories(
    val isDarkBackground: Boolean,
    val duration: Int,
) {
    object OneInchPromo : Stories(true, duration = 8000)
    object TangemIntro : Stories(true, duration = 8000)
    object RevolutionaryWallet : Stories(true, duration = 6000)
    object UltraSecureBackup : Stories(false, duration = 6000)
    object Currencies : Stories(false, duration = 6000)
    object Web3 : Stories(false, duration = 6000)
    object WalletForEveryone : Stories(true, duration = 6000)
}