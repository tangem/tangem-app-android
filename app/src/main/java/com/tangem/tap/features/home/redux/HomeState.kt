package com.tangem.tap.features.home.redux

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.rekotlin.StateType

// todo refactor [REDACTED_TASK_KEY]
data class HomeState(
    val scanInProgress: Boolean = false,
    val stories: ImmutableList<Stories> = Stories.entries.toImmutableList(),
) : StateType {

    val firstStory: Stories get() = stories[0]

    fun stepOf(story: Stories): Int = stories.indexOf(story)
}

enum class Stories(val duration: Int = 6000) {
    TangemIntro,
    RevolutionaryWallet,
    UltraSecureBackup,
    Currencies,
    Web3,
    WalletForEveryone,
}