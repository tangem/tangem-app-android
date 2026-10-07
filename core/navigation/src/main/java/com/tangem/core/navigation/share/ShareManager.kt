package com.tangem.core.navigation.share

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface ShareManager {

    /** The texts shared by [shareTrackedText], each emitted once the user picks where to send it */
    val sharedTexts: Flow<String>
        get() = emptyFlow()

    fun shareText(text: String)

    /**
     * Shares [text] like [shareText] and reports it to [sharedTexts] once the user picks where to send it; dismissing
     * the share sheet reports nothing. The system reports the sheet's own "Copy" action only from Android 15.
     */
    fun shareTrackedText(text: String) = shareText(text)
}