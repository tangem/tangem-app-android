package com.tangem.features.storiesv2.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

/**
 * The four events every story reports, whatever its content and wherever it was opened from. A new story does not
 * add event names — it arrives as new values of [STORY_ID] and [AnalyticsParam.SOURCE].
 */
internal sealed class StoriesV2Events(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Stories", event = event, params = params) {

    /** Sent once, after the decision to open the player and before the first slide is shown. */
    data class StoryOpened(
        private val storyId: String,
        private val storyVersion: Int,
        private val slidesTotal: Int,
        private val contentOrigin: String,
        private val source: String,
    ) : StoriesV2Events(
        event = "Story Opened",
        params = mapOf(
            STORY_ID to storyId,
            STORY_VERSION to storyVersion.toString(),
            SLIDES_TOTAL to slidesTotal.toString(),
            CONTENT_ORIGIN to contentOrigin,
            AnalyticsParam.SOURCE to source,
        ),
    )

    /**
     * @property watchedMs      time the slide was actually on screen, excluding pause and background
     * @property fallbackReason absent unless the slide was degraded to its poster
     */
    data class SlideViewed(
        private val storyId: String,
        private val storyVersion: Int,
        private val slideId: String,
        private val slideIndex: Int,
        private val assetType: String,
        private val assetDurationMs: Long,
        private val watchedMs: Long,
        private val isSlideCompleted: Boolean,
        private val advanceReason: String,
        private val fallbackReason: String?,
        private val source: String,
    ) : StoriesV2Events(
        event = "Slide Viewed",
        params = buildMap {
            put(STORY_ID, storyId)
            put(STORY_VERSION, storyVersion.toString())
            put(SLIDE_ID, slideId)
            put(SLIDE_INDEX, slideIndex.toString())
            put(ASSET_TYPE, assetType)
            put(ASSET_DURATION_MS, assetDurationMs.toString())
            put(WATCHED_MS, watchedMs.toString())
            put(SLIDE_COMPLETED, isSlideCompleted.toString())
            put(ADVANCE_REASON, advanceReason)
            fallbackReason?.let { put(FALLBACK_REASON, it) }
            put(AnalyticsParam.SOURCE, source)
        },
    )

    /** Sent once on any exit path. */
    @Suppress("LongParameterList")
    data class StoryClosed(
        private val storyId: String,
        private val storyVersion: Int,
        private val slidesTotal: Int,
        private val watched: Int,
        private val loopsWatched: Int,
        private val isStoryCompleted: Boolean,
        private val exitReason: String,
        private val actionId: String?,
        private val actionTargetType: String?,
        private val actionDestination: String?,
        private val fallbackSlides: Int,
        private val source: String,
    ) : StoriesV2Events(
        event = "Story Closed",
        params = buildMap {
            put(STORY_ID, storyId)
            put(STORY_VERSION, storyVersion.toString())
            put(SLIDES_TOTAL, slidesTotal.toString())
            put(WATCHED, watched.toString())
            put(LOOPS_WATCHED, loopsWatched.toString())
            put(STORY_COMPLETED, isStoryCompleted.toString())
            put(EXIT_REASON, exitReason)
            actionId?.let { put(ACTION_ID, it) }
            actionTargetType?.let { put(ACTION_TARGET_TYPE, it) }
            actionDestination?.let { put(ACTION_DESTINATION, it) }
            put(FALLBACK_SLIDES, fallbackSlides.toString())
            put(AnalyticsParam.SOURCE, source)
        },
    )

    /** Sent on every distinguishable failure, terminal or recoverable. */
    data class StoryErrorOccurred(
        private val storyId: String?,
        private val slideId: String?,
        private val errorStage: String,
        private val errorReason: String,
        private val source: String?,
    ) : StoriesV2Events(
        event = "Story Error Occurred",
        params = buildMap {
            storyId?.let { put(STORY_ID, it) }
            slideId?.let { put(SLIDE_ID, it) }
            put(ERROR_STAGE, errorStage)
            put(ERROR_REASON, errorReason)
            source?.let { put(AnalyticsParam.SOURCE, it) }
        },
    )
}

internal enum class StoryV2AdvanceReason(val analyticsValue: String) {
    AUTO(analyticsValue = "auto"),
    TAP_FORWARD(analyticsValue = "tap_forward"),
    TAP_BACK(analyticsValue = "tap_back"),
    LOOP_RESTART(analyticsValue = "loop_restart"),
    STORY_CLOSED(analyticsValue = "story_closed"),
}

internal enum class StoryV2ExitReason(val analyticsValue: String) {
    COMPLETED(analyticsValue = "completed"),
    CLOSE_BUTTON(analyticsValue = "close_button"),
    SWIPE_DOWN(analyticsValue = "swipe_down"),
    ACTION(analyticsValue = "action"),
}

/**
 * Why a slide fell back to its poster. Reduce Motion is deliberately a separate value from the two technical ones:
 * it is a deliberate viewer setting and must not count against the playback guardrail.
 */
internal enum class StoryV2FallbackReason(val analyticsValue: String) {
    ASSET_NOT_READY(analyticsValue = "asset_not_ready"),
    PLAYBACK_FAILED(analyticsValue = "playback_failed"),
    REDUCE_MOTION(analyticsValue = "reduce_motion"),
}

internal enum class StoryV2ErrorStage(val analyticsValue: String) {
    REQUEST(analyticsValue = "request"),
    CONTENT_VALIDATION(analyticsValue = "content_validation"),
    ASSET_PREFETCH(analyticsValue = "asset_prefetch"),
    ASSET_PLAYBACK(analyticsValue = "asset_playback"),
    ACTION_OPEN(analyticsValue = "action_open"),
}

internal enum class StoryV2ErrorReason(val analyticsValue: String) {
    NETWORK(analyticsValue = "network"),
    HTTP_FAILURE(analyticsValue = "http_failure"),
    TIMEOUT(analyticsValue = "timeout"),
    NOT_FOUND(analyticsValue = "not_found"),
    DECODE_FAILURE(analyticsValue = "decode_failure"),
    UNKNOWN_SCHEMA(analyticsValue = "unknown_schema"),
    UNKNOWN_ASSET(analyticsValue = "unknown_asset"),
    UNKNOWN_PLACEMENT(analyticsValue = "unknown_placement"),
    UNKNOWN_POLICY(analyticsValue = "unknown_policy"),
    UNKNOWN_TARGET(analyticsValue = "unknown_target"),
    FORBIDDEN_TARGET(analyticsValue = "forbidden_target"),
    NO_COMPATIBLE_SOURCE(analyticsValue = "no_compatible_source"),
    INTEGRITY_MISMATCH(analyticsValue = "integrity_mismatch"),
    NO_USABLE_FRAME(analyticsValue = "no_usable_frame"),
}

private const val STORY_ID = "Story Id"
private const val STORY_VERSION = "Story Version"
private const val SLIDE_ID = "Slide Id"
private const val SLIDE_INDEX = "Slide Index"
private const val SLIDES_TOTAL = "Slides Total"
private const val CONTENT_ORIGIN = "Content Origin"
private const val ASSET_TYPE = "Asset Type"
private const val ASSET_DURATION_MS = "Asset Duration Ms"
private const val WATCHED_MS = "Watched Ms"
private const val WATCHED = "Watched"
private const val LOOPS_WATCHED = "Loops Watched"
private const val SLIDE_COMPLETED = "Slide Completed"
private const val STORY_COMPLETED = "Story Completed"
private const val ADVANCE_REASON = "Advance Reason"
private const val FALLBACK_REASON = "Fallback Reason"
private const val FALLBACK_SLIDES = "Fallback Slides"
private const val EXIT_REASON = "Exit Reason"
private const val ACTION_ID = "Action Id"
private const val ACTION_TARGET_TYPE = "Action Target Type"
private const val ACTION_DESTINATION = "Action Destination"
private const val ERROR_STAGE = "Error Stage"
private const val ERROR_REASON = "Error Reason"