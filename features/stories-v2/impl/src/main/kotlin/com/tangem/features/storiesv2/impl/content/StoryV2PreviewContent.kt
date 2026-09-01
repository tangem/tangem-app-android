package com.tangem.features.storiesv2.impl.content

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Composition the player shows instead of the one its type would resolve to. Set only by the tester showcase, so it
 * can drive the real player through compositions no story type maps to, and the player keeps one content entry
 * point instead of gaining a demo-shaped parameter.
 */
@Singleton
internal class StoryV2PreviewContent @Inject constructor() {

    @Volatile
    var composition: StoryV2Composition? = null
}