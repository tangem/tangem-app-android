package com.tangem.features.feed.ui.v2.state

import androidx.compose.runtime.Immutable

/**
 * The Earn card.
 *
 * Pitch and target are independent: a user who already earns but has no token worth naming gets the
 * onboarding wording while still being taken to For You.
 *
 * @property pitch what the card says
 * @property target where a tap goes
 */
@Immutable
internal data class EarnBlockUM(
    val pitch: Pitch = Pitch.Plain,
    val target: Target = Target.EarnTab,
) {

    sealed interface Pitch {

        /** No rate known yet — the card pitches earning without naming one. */
        data object Plain : Pitch

        /** Nothing earning yet: the best rate Earn offers at all. */
        data class Onboarding(val apy: String) : Pitch

        /** Already earning: the best rate among the user's own holdings, and the token it belongs to. */
        data class Growth(val apy: String, val tokenSymbol: String) : Pitch
    }

    enum class Target {

        /** The feed's own Earn tab, below the blocks. */
        EarnTab,

        /** The For You screen, where the user sees what they already earn. */
        ForYou,
    }
}