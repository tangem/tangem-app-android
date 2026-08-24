package com.tangem.features.storiesv2.impl.content

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.storiesv2.impl.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stories that ship inside the apk: available before the first response of any api, and working offline from the
 * moment the app is installed. Same [StoryV2Composition] a remote story is mapped to, so behaviour is identical.
 */
@Singleton
internal class BundledStoryV2ContentSource @Inject constructor() {

    fun onboarding(): StoryV2Composition = StoryV2Composition(
        id = ONBOARDING_ID,
        version = ONBOARDING_VERSION,
        origin = StoryV2Origin.BUNDLED,
        // A step of the wallet creation flow, not a screen to sit through: it loops until the viewer acts.
        endBehavior = StoryV2EndBehavior.LOOP,
        actions = listOf(
            StoryV2Action(
                id = "learn_more",
                label = resourceReference(R.string.stories_onboarding_action_learn_more),
                style = StoryV2Action.Style.SECONDARY,
                target = StoryV2Action.Target.Web(url = BUY_URL),
            ),
            StoryV2Action(
                id = "scan_wallet",
                label = resourceReference(R.string.stories_onboarding_action_scan),
                style = StoryV2Action.Style.PRIMARY,
                target = StoryV2Action.Target.Screen(key = SCAN_WALLET_SCREEN),
                icon = StoryV2MediaRef.DrawableResource(R.drawable.ic_tangem_24),
            ),
        ),
        slides = listOf(
            StoryV2Slide(
                id = "one_tap",
                title = resourceReference(R.string.stories_onboarding_slide_1_title),
                subtitle = resourceReference(R.string.stories_onboarding_slide_1_subtitle),
                asset = StoryV2Asset.Video(
                    source = StoryV2MediaRef.RawResource(R.raw.stories_onboarding_slide_1),
                    poster = StoryV2MediaRef.DrawableResource(R.drawable.img_stories_onboarding_poster_1),
                    durationMs = SLIDE_1_DURATION_MS,
                ),
            ),
            StoryV2Slide(
                id = "multiple_cards",
                title = resourceReference(R.string.stories_onboarding_slide_2_title),
                subtitle = resourceReference(R.string.stories_onboarding_slide_2_subtitle),
                asset = StoryV2Asset.Video(
                    source = StoryV2MediaRef.RawResource(R.raw.stories_onboarding_slide_2),
                    poster = StoryV2MediaRef.DrawableResource(R.drawable.img_stories_onboarding_poster_2),
                    durationMs = SLIDE_2_DURATION_MS,
                ),
            ),
            StoryV2Slide(
                id = "your_keys",
                title = resourceReference(R.string.stories_onboarding_slide_3_title),
                subtitle = resourceReference(R.string.stories_onboarding_slide_3_subtitle),
                asset = StoryV2Asset.Video(
                    source = StoryV2MediaRef.RawResource(R.raw.stories_onboarding_slide_3),
                    poster = StoryV2MediaRef.DrawableResource(R.drawable.img_stories_onboarding_poster_3),
                    durationMs = SLIDE_3_DURATION_MS,
                ),
            ),
            // An image until the fourth video is delivered, which keeps the mixed video+image path exercised.
            StoryV2Slide(
                id = "trusted_by_millions",
                title = resourceReference(R.string.stories_onboarding_slide_4_title),
                subtitle = resourceReference(R.string.stories_onboarding_slide_4_subtitle),
                asset = StoryV2Asset.Image(
                    source = StoryV2MediaRef.DrawableResource(R.drawable.img_stories_onboarding_slide_4),
                    durationMs = SLIDE_4_DURATION_MS,
                ),
            ),
        ),
    )

    private companion object {
        const val ONBOARDING_ID = "onboarding"
        const val ONBOARDING_VERSION = 1
        const val BUY_URL = "https://buy.tangem.com"

        /** Resolved by the host: onboarding starts the card scan from here. */
        const val SCAN_WALLET_SCREEN = "scan_wallet"

        // Exactly the length of each bundled file: duration is authoritative for progress, so a value that
        // disagrees with the media makes the bar and the picture disagree too.
        const val SLIDE_1_DURATION_MS = 7_000L
        const val SLIDE_2_DURATION_MS = 6_000L
        const val SLIDE_3_DURATION_MS = 5_000L
        const val SLIDE_4_DURATION_MS = 5_000L
    }
}