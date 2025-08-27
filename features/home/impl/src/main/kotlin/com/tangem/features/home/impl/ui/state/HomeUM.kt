package com.tangem.features.home.impl.ui.state

import kotlinx.collections.immutable.ImmutableList

data class HomeUM(
    val scanInProgress: Boolean,
    val stories: ImmutableList<Stories>,
    val onScanClick: () -> Unit,
    val onShopClick: () -> Unit,
    val onSearchTokensClick: () -> Unit,
    val onCreateNewWalletClick: () -> Unit,
    val onAddExistingWalletClick: () -> Unit,
) {
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