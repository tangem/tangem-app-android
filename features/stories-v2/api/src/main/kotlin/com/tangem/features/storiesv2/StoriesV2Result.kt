package com.tangem.features.storiesv2

/** How the story ended. Exactly one result is delivered per player session. */
sealed interface StoriesV2Result {

    /** Close button, swipe down or system back. */
    data object Dismissed : StoriesV2Result

    /** Loop stories never end on their own and never report this. */
    data object Completed : StoriesV2Result

    /**
     * The story ended because of an action button: the viewer tapped it, the host performed it through
     * [StoriesV2Component.Params.onAction] and settled the tap with [StoriesV2PendingAction.end]. The action's
     * work is already done by the time this arrives — the host tears the player down, it does not act again.
     */
    data class ActionInvoked(val actionId: String, val target: StoryV2ActionTarget) : StoriesV2Result

    /**
     * Nothing showable was available, so no frame was ever drawn. Delivered before the first composition rather
     * than after a placeholder, and the host must continue its flow immediately.
     */
    data class Unavailable(val reason: Reason) : StoriesV2Result {

        enum class Reason {

            /** No composition for the requested type, or every slide was filtered out. */
            NO_CONTENT,

            /** A composition exists, but neither an asset nor a poster could produce a first frame. */
            NO_USABLE_FRAME,
        }
    }
}