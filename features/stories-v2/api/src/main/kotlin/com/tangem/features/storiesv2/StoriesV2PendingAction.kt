package com.tangem.features.storiesv2

/**
 * An action tap waiting for the host's verdict, handed out through [StoriesV2Component.Params.onAction] with
 * playback already paused on the tapped slide. The story is alive until the tap is settled.
 *
 * The first [resume] or [end] wins; every later call on the same handle is ignored. Both are safe after the
 * player is gone — the viewer closed the story meanwhile, or the component was destroyed — and then do
 * nothing. Settle on the main thread, like every other call into the player.
 */
interface StoriesV2PendingAction {

    val actionId: String

    val target: StoryV2ActionTarget

    /** The tapped slide, for the story context the analytics spec wants on the host's own tap events. */
    val slideId: String

    /** One-based, matching how the analytics report slide positions. */
    val slideIndex: Int

    /**
     * The action turned out non-terminal — a scan was cancelled or failed, an overlay was closed: playback
     * continues on the same slide from the position it was paused at.
     */
    fun resume()

    /**
     * The action turned out terminal — a scan succeeded, navigation replaces the screen: the story ends and
     * [StoriesV2Component.Params.onResult] delivers [StoriesV2Result.ActionInvoked] for this action.
     */
    fun end()
}