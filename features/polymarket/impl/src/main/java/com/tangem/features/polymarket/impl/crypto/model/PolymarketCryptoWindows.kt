package com.tangem.features.polymarket.impl.crypto.model

import com.tangem.domain.polymarket.model.PolymarketCryptoWindow
import com.tangem.domain.polymarket.model.PolymarketCryptoWindowPhase
import com.tangem.domain.polymarket.model.PolymarketEvent

/**
 * The windows of one Up/Down series as the screen knows them, and the one it shows. Built with [of].
 *
 * The shown window changes by the user's choice only: one that has ended stays on screen while the next goes live
 * in the tabs. The event on screen is always one whole answer, and an answer is taken only for the window it was
 * asked for, so a late one cannot put another window's market under the shown header.
 *
 * @property shown event on screen
 * @property window window of [shown]
 * @property switchingTo window the user has chosen and whose answer is awaited
 * @property isGoingToLive whether [switchingTo] is where the "go to live" action leads
 * @property known windows met in the answers, earliest first
 * @property seenIds windows that have been on screen
 */
internal data class PolymarketCryptoWindows(
    val shown: PolymarketEvent,
    val window: PolymarketCryptoWindow,
    val switchingTo: String? = null,
    val isGoingToLive: Boolean = false,
    private val known: List<PolymarketCryptoWindowTab> = emptyList(),
    private val seenIds: Set<String> = emptySet(),
) {

    fun phaseAt(nowMillis: Long): PolymarketCryptoWindowPhase = window.phaseAt(nowMillis)

    /**
     * The live window, up to [UPCOMING_TABS] upcoming ones, the window before the live one when it has been on
     * screen, and the shown one wherever it stands. The window being switched to is marked as chosen at once,
     * before its answer.
     */
    fun rowAt(nowMillis: Long): PolymarketCryptoWindowRow {
        val live = liveAt(nowMillis)
        val previous = live?.let { anchor -> known.find { it.end == anchor.start && it.eventId in seenIds } }
        val upcoming = known
            .filter { it.phaseAt(nowMillis) == PolymarketCryptoWindowPhase.UPCOMING }
            .take(UPCOMING_TABS)
        val tabs = (listOfNotNull(previous, live) + upcoming + window.toTab(shown.id))
            .distinctBy(PolymarketCryptoWindowTab::eventId)
            .sortedBy(PolymarketCryptoWindowTab::start)

        return PolymarketCryptoWindowRow(
            tabs = tabs,
            selectedId = switchingTo?.takeIf { id -> tabs.any { it.eventId == id } } ?: shown.id,
        )
    }

    /**
     * Window the "go to live" action leads to; none while the shown one is live. It is the window live by the
     * clock, and when the answers have not reached one, the window the backend named live with the shown event.
     */
    fun liveTargetAt(nowMillis: Long): String? {
        if (phaseAt(nowMillis) == PolymarketCryptoWindowPhase.LIVE) return null

        return liveAt(nowMillis)?.eventId ?: window.liveEventId?.takeIf { it != shown.id }
    }

    /** The user's choice of a window. Choosing the shown one calls off the switch under way. */
    fun select(eventId: String, nowMillis: Long): PolymarketCryptoWindows = when (eventId) {
        shown.id -> withoutSwitch()
        switchingTo -> this
        else -> copy(switchingTo = eventId, isGoingToLive = eventId == liveTargetAt(nowMillis))
    }

    fun goToLive(nowMillis: Long): PolymarketCryptoWindows {
        return liveTargetAt(nowMillis)?.let { select(eventId = it, nowMillis = nowMillis) } ?: this
    }

    /**
     * An answer for the shown window refreshes it, keeping a price the answer lacks; one for the window being
     * switched to puts that window on screen. Any other answer came late and is dropped, and so is an answer that
     * is not an Up/Down event.
     */
    fun received(event: PolymarketEvent): PolymarketCryptoWindows {
        val received = event.cryptoWindow

        return when (event.id) {
            shown.id -> if (received == null) this else showing(event, received.keepingPricesOf(window))
            switchingTo -> if (received == null) withoutSwitch() else showing(event, received).withoutSwitch()
            else -> this
        }
    }

    /**
     * The answer for [eventId] did not come: the shown window stays. Answers are told apart by the event alone, so
     * a load given up for a newer choice must not be reported here.
     */
    fun failed(eventId: String): PolymarketCryptoWindows = if (eventId == switchingTo) withoutSwitch() else this

    fun withoutSwitch(): PolymarketCryptoWindows = copy(switchingTo = null, isGoingToLive = false)

    private fun liveAt(nowMillis: Long): PolymarketCryptoWindowTab? {
        return known.find { it.phaseAt(nowMillis) == PolymarketCryptoWindowPhase.LIVE }
    }

    private fun showing(event: PolymarketEvent, window: PolymarketCryptoWindow): PolymarketCryptoWindows {
        val answered = listOf(window.toTab(event.id)) + window.nextWindows.map {
            PolymarketCryptoWindowTab(eventId = it.eventId, start = it.start, end = it.end)
        }
        val known = (answered + known)
            .distinctBy(PolymarketCryptoWindowTab::eventId)
            .sortedBy(PolymarketCryptoWindowTab::start)
            .takeLast(MAX_KNOWN_WINDOWS)

        return copy(
            shown = event.copy(cryptoWindow = window),
            window = window,
            known = known,
            seenIds = (seenIds + event.id).intersect(known.mapTo(HashSet(), PolymarketCryptoWindowTab::eventId)),
        )
    }

    companion object {
        const val UPCOMING_TABS = 4

        /** Keeps a long stay on the screen from piling windows up; the row needs only the latest of them. */
        private const val MAX_KNOWN_WINDOWS = 64

        /** Null for an event that is not an Up/Down one. */
        fun of(event: PolymarketEvent): PolymarketCryptoWindows? {
            val window = event.cryptoWindow ?: return null

            return PolymarketCryptoWindows(shown = event, window = window).showing(event, window)
        }
    }
}

private fun PolymarketCryptoWindow.toTab(eventId: String) = PolymarketCryptoWindowTab(eventId, start, end)

private fun PolymarketCryptoWindow.keepingPricesOf(shown: PolymarketCryptoWindow) = copy(
    priceToBeat = priceToBeat ?: shown.priceToBeat,
    finalPrice = finalPrice ?: shown.finalPrice,
)