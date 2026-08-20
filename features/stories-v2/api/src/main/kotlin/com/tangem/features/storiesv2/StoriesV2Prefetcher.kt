package com.tangem.features.storiesv2

/**
 * App-scoped warm-up for story assets.
 *
 * Call it the moment a story becomes known — a composition arrived, Main opened, a flow that can reach the story
 * started — and never on the tap that opens the player: by then the player must already have a local asset or at
 * least a poster.
 *
 * Fire-and-forget. Returns immediately, runs on an application scope, swallows failures into analytics, and skips
 * anything already warm.
 */
interface StoriesV2Prefetcher {

    fun prefetch(story: StoryV2Type)
}