package com.tangem.features.home.impl.ui.state

import com.tangem.core.ui.components.stories.model.StoriesContentConfig
import com.tangem.core.ui.components.stories.model.StoryConfig
import kotlinx.collections.immutable.ImmutableList

internal data class HomeUM(
    val isScanInProgress: Boolean,
    val isStoriesContainerEnabled: Boolean,
    val stories: ImmutableList<Stories>,
    val storiesConfig: HomeStoriesConfig,
    val onGetStartedClick: () -> Unit,
) {
    val firstStory: Stories get() = stories[0]

    fun stepOf(story: Stories): Int = stories.indexOf(story)
}

/**
 * Config for the redesigned Home stories ([StoriesContainer]). The Home intro loops forever and is
 * not closable, so [isCloseButtonVisible] is `false` and [onClose] keeps its no-op default.
 */
internal data class HomeStoriesConfig(
    override val stories: ImmutableList<Stories>,
    override val isRestartable: Boolean = true,
    override val isCloseButtonVisible: Boolean = false,
) : StoriesContentConfig<Stories>

internal enum class Stories(override val duration: Int = 6000) : StoryConfig {
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
internal fun getRestrictedStories(): List<Stories> {
    return Stories.entries.filterNot { it == Stories.Currencies }
}