package com.tangem.tap.features.home.redux

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.rekotlin.StateType
// [REDACTED_TODO_COMMENT]
data class HomeState(
    val scanInProgress: Boolean = false,
    val isV2StoriesEnabled: Boolean = false,
    val stories: ImmutableList<Stories> = getRestrictedStories().toImmutableList(),
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

/**
 * For FCA restriction stories
 */
fun getRestrictedStories(): List<Stories> {
    return Stories.entries.filterNot { it == Stories.Currencies }
}
