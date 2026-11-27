package com.tangem.features.polymarket.impl.crypto.model

import com.tangem.domain.polymarket.model.PolymarketCryptoPricePoint
import com.tangem.domain.polymarket.model.PolymarketCryptoWindowPhase
import com.tangem.domain.polymarket.model.PolymarketCryptoWinner
import com.tangem.domain.polymarket.model.PolymarketLiveQuote
import com.tangem.domain.polymarket.model.cryptoWinner

/** What the crypto Up/Down screen has to show, before any formatting. */
internal sealed interface PolymarketCryptoEventState {

    data object Loading : PolymarketCryptoEventState

    data object Failed : PolymarketCryptoEventState

    /** The event turned out not to be an Up/Down one: it belongs on the ordinary event screen. */
    data object NotUpDown : PolymarketCryptoEventState

    /**
     * @property nowMillis the backend's clock at the moment of this state; it moves on every second while the
     *  screen is in front
     * @property points the coin's recent prices, oldest first, none later than the end of the shown window
     * @property isPriceUnavailable no live price is to be had: the event names no feed, or the stream refused it
     * @property quotes live quotes of the shown market's outcomes by asset id; none once the window has ended
     */
    data class Content(
        val windows: PolymarketCryptoWindows,
        val nowMillis: Long,
        val points: List<PolymarketCryptoPricePoint>,
        val isPriceUnavailable: Boolean,
        val quotes: Map<String, PolymarketLiveQuote>,
    ) : PolymarketCryptoEventState {

        val phase: PolymarketCryptoWindowPhase get() = windows.phaseAt(nowMillis)

        /** Named only once the shown window has ended. */
        val winner: PolymarketCryptoWinner?
            get() = if (phase == PolymarketCryptoWindowPhase.ENDED) windows.shown.cryptoWinner() else null
    }
}