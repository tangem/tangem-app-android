package com.tangem.features.storiesv2.impl.content.remote

import com.tangem.features.storiesv2.impl.analytics.StoryV2ErrorReason
import com.tangem.features.storiesv2.impl.content.StoryV2Action
import com.tangem.features.storiesv2.impl.content.StoryV2ContentMode
import com.tangem.features.storiesv2.impl.content.StoryV2EndBehavior
import com.tangem.features.storiesv2.impl.content.StoryV2Haptic

/**
 * A remote story that passed validation but still points at urls. It becomes a
 * [com.tangem.features.storiesv2.impl.content.StoryV2Composition] only once every file is on the device.
 */
internal data class RemoteStoryV2(
    val slug: String,
    val placements: Set<String>,
    val displayPolicy: DisplayPolicy,
    val endBehavior: StoryV2EndBehavior,
    val actions: List<StoryV2Action>,
    val slides: List<RemoteStoryV2Slide>,
) {

    enum class DisplayPolicy { ALWAYS, ONCE }
}

internal data class RemoteStoryV2Slide(
    val id: String,
    val title: String,
    val subtitle: String,
    val assetType: AssetType,
    val contentMode: StoryV2ContentMode,
    val durationMs: Long,
    val haptics: List<StoryV2Haptic>,
    val source: RemoteStoryV2File,
) {

    enum class AssetType { VIDEO, LOTTIE, IMAGE }
}

/** @property sizeBytes the size the downloaded file must have before it counts as ready */
internal data class RemoteStoryV2File(val url: String, val sizeBytes: Long)

internal sealed interface RemoteStoryV2ConversionResult {

    /** @property issues what was dropped on the way, reported to analytics but not fatal */
    data class Converted(val stories: List<RemoteStoryV2>, val issues: List<Issue>) : RemoteStoryV2ConversionResult

    /** The response breaks the contract as a whole, so none of it is trusted. */
    data class Rejected(val reason: StoryV2ErrorReason) : RemoteStoryV2ConversionResult

    data class Issue(val storyId: String?, val slideId: String?, val reason: StoryV2ErrorReason)
}