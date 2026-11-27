package com.tangem.features.polymarket.impl.crypto.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketCryptoWindowPhase
import com.tangem.features.polymarket.impl.createPolymarketEvent
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class PolymarketCryptoWindowsTest {

    @Nested
    inner class Of {

        @Test
        fun `GIVEN event without a window WHEN created THEN there is nothing to show`() {
            // Act
            val actual = PolymarketCryptoWindows.of(createPolymarketEvent())

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN Up or Down event WHEN created THEN it is on screen with no switch under way`() {
            // Arrange
            val event = createCryptoEvent(index = 0)

            // Act
            val actual = requireNotNull(PolymarketCryptoWindows.of(event))

            // Assert
            assertThat(actual.shown).isEqualTo(event)
            assertThat(actual.window).isEqualTo(event.cryptoWindow)
            assertThat(actual.switchingTo).isNull()
            assertThat(actual.isGoingToLive).isFalse()
            assertThat(actual.phaseAt(duringCryptoWindow(0))).isEqualTo(PolymarketCryptoWindowPhase.LIVE)
        }
    }

    @Nested
    inner class RowAt {

        @Test
        fun `GIVEN live window on screen WHEN row is read THEN it holds the window and four upcoming ones`() {
            // Arrange
            val windows = windowsOf(index = 0)

            // Act
            val actual = windows.rowAt(duringCryptoWindow(0))

            // Assert
            assertThat(actual).isEqualTo(row(0, 1, 2, 3, 4, selected = 0))
        }

        @Test
        fun `GIVEN shown window has ended WHEN row is read THEN it stays chosen before the one that went live`() {
            // Arrange
            val windows = windowsOf(index = 0)

            // Act
            val actual = windows.rowAt(duringCryptoWindow(1))

            // Assert
            assertThat(actual).isEqualTo(row(0, 1, 2, 3, 4, 5, selected = 0))
        }

        @Test
        fun `GIVEN user moved on to the live window WHEN row is read THEN the window seen before it is kept`() {
            // Arrange
            val windows = windowsOf(index = 0).switchedTo(index = 1)

            // Act
            val actual = windows.rowAt(duringCryptoWindow(1))

            // Assert
            assertThat(actual).isEqualTo(row(0, 1, 2, 3, 4, 5, selected = 1))
        }

        @Test
        fun `GIVEN two windows were seen WHEN the next goes live THEN only the one before the live window is kept`() {
            // Arrange
            val windows = windowsOf(index = 0).switchedTo(index = 1)

            // Act
            val actual = windows.rowAt(duringCryptoWindow(2))

            // Assert
            assertThat(actual).isEqualTo(row(1, 2, 3, 4, 5, 6, selected = 1))
        }

        @Test
        fun `GIVEN window before the live one was never on screen WHEN row is read THEN it is not offered`() {
            // Arrange
            val windows = windowsOf(index = 0).switchedTo(index = 2)

            // Act
            val actual = windows.rowAt(duringCryptoWindow(2))

            // Assert
            assertThat(actual).isEqualTo(row(2, 3, 4, 5, 6, selected = 2))
        }

        @Test
        fun `GIVEN upcoming window on screen WHEN row is read THEN windows skipped on the way to it are kept`() {
            // Arrange
            val windows = windowsOf(index = 0).switchedTo(index = 3)

            // Act
            val actual = windows.rowAt(duringCryptoWindow(1))

            // Assert
            assertThat(actual).isEqualTo(row(0, 1, 2, 3, 4, 5, selected = 3))
        }

        @Test
        fun `GIVEN old window whose followers have all ended WHEN row is read THEN it is the only tab`() {
            // Arrange
            val windows = windowsOf(index = 0, following = 5)

            // Act
            val actual = windows.rowAt(duringCryptoWindow(40))

            // Assert
            assertThat(actual).isEqualTo(row(0, selected = 0))
        }

        @Test
        fun `GIVEN upcoming window opened with no live one known WHEN row is read THEN upcoming ones are offered`() {
            // Arrange
            val windows = windowsOf(index = 3)

            // Act
            val actual = windows.rowAt(duringCryptoWindow(1))

            // Assert
            assertThat(actual).isEqualTo(row(3, 4, 5, 6, selected = 3))
        }

        @Test
        fun `GIVEN more windows met than are remembered WHEN row is read THEN the latest stay and the earliest go`() {
            // Arrange
            val windows = windowsOf(index = 0).switchedTo(index = 20).switchedTo(index = 40).switchedTo(index = 60)

            // Act
            val whileShownIsLive = windows.rowAt(duringCryptoWindow(60))
            val backWhenFirstWasLive = windows.rowAt(duringCryptoWindow(1))

            // Assert
            assertThat(whileShownIsLive).isEqualTo(row(60, 61, 62, 63, 64, selected = 60))
            assertThat(backWhenFirstWasLive).isEqualTo(row(17, 18, 19, 20, 60, selected = 60))
        }

        @Test
        fun `GIVEN tab chosen WHEN its answer has not come THEN the tab is marked at once`() {
            // Arrange
            val now = duringCryptoWindow(0)
            val windows = windowsOf(index = 0).select(eventId = cryptoWindowId(2), nowMillis = now)

            // Act
            val actual = windows.rowAt(now)

            // Assert
            assertThat(actual).isEqualTo(row(0, 1, 2, 3, 4, selected = 2))
        }

        @Test
        fun `GIVEN switch to a window that is not a tab WHEN row is read THEN the shown window stays marked`() {
            // Arrange
            val now = duringCryptoWindow(40)
            val windows = windowsOf(index = 0, following = 5, liveEventId = cryptoWindowId(40)).goToLive(now)

            // Act
            val actual = windows.rowAt(now)

            // Assert
            assertThat(actual).isEqualTo(row(0, selected = 0))
        }

        private fun row(vararg indices: Int, selected: Int) = PolymarketCryptoWindowRow(
            tabs = indices.map { index ->
                val event = createCryptoEvent(index)
                val window = requireNotNull(event.cryptoWindow)
                PolymarketCryptoWindowTab(eventId = event.id, start = window.start, end = window.end)
            },
            selectedId = cryptoWindowId(selected),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class LiveTargetAt {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN shown window and the clock WHEN live target is read THEN it is the live window unless shown`(
            model: CryptoLiveTargetModel,
        ) {
            // Arrange
            val windows = windowsOf(index = model.shown, following = model.following, liveEventId = model.namedLive)

            // Act
            val actual = windows.liveTargetAt(duringCryptoWindow(model.liveNow))

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            CryptoLiveTargetModel(shown = 0, liveNow = 0, namedLive = cryptoWindowId(0), expected = null),
            CryptoLiveTargetModel(shown = 0, liveNow = 0, namedLive = cryptoWindowId(7), expected = null),
            CryptoLiveTargetModel(shown = 0, liveNow = 1, namedLive = cryptoWindowId(0), expected = cryptoWindowId(1)),
            CryptoLiveTargetModel(shown = 0, liveNow = 1, namedLive = cryptoWindowId(7), expected = cryptoWindowId(1)),
            CryptoLiveTargetModel(shown = 0, liveNow = 3, namedLive = null, expected = cryptoWindowId(3)),
            CryptoLiveTargetModel(shown = 3, liveNow = 1, namedLive = cryptoWindowId(1), expected = cryptoWindowId(1)),
            CryptoLiveTargetModel(
                shown = 0,
                following = 5,
                liveNow = 40,
                namedLive = cryptoWindowId(40),
                expected = cryptoWindowId(40),
            ),
            CryptoLiveTargetModel(
                shown = 0,
                following = 5,
                liveNow = 40,
                namedLive = cryptoWindowId(0),
                expected = null,
            ),
            CryptoLiveTargetModel(shown = 0, following = 5, liveNow = 40, namedLive = null, expected = null),
        )
    }

    @Nested
    inner class Select {

        @Test
        fun `GIVEN another window WHEN chosen THEN a switch to it is under way`() {
            // Arrange
            val windows = windowsOf(index = 0)

            // Act
            val actual = windows.select(eventId = cryptoWindowId(2), nowMillis = duringCryptoWindow(0))

            // Assert
            assertThat(actual.switchingTo).isEqualTo(cryptoWindowId(2))
            assertThat(actual.isGoingToLive).isFalse()
            assertThat(actual.shown).isEqualTo(windows.shown)
        }

        @Test
        fun `GIVEN ended window on screen WHEN the live tab is chosen THEN it counts as going to live`() {
            // Arrange
            val windows = windowsOf(index = 0)

            // Act
            val actual = windows.select(eventId = cryptoWindowId(1), nowMillis = duringCryptoWindow(1))

            // Assert
            assertThat(actual.switchingTo).isEqualTo(cryptoWindowId(1))
            assertThat(actual.isGoingToLive).isTrue()
        }

        @Test
        fun `GIVEN switch under way WHEN the same window is chosen again THEN nothing changes`() {
            // Arrange
            val now = duringCryptoWindow(0)
            val windows = windowsOf(index = 0).select(eventId = cryptoWindowId(2), nowMillis = now)

            // Act
            val actual = windows.select(eventId = cryptoWindowId(2), nowMillis = now)

            // Assert
            assertThat(actual).isSameInstanceAs(windows)
        }

        @Test
        fun `GIVEN switch under way WHEN the shown window is chosen THEN the switch is called off`() {
            // Arrange
            val now = duringCryptoWindow(1)
            val shown = windowsOf(index = 0)

            // Act
            val actual = shown.goToLive(now).select(eventId = cryptoWindowId(0), nowMillis = now)

            // Assert
            assertThat(actual).isEqualTo(shown)
        }

        @Test
        fun `GIVEN ended window on screen WHEN going to live THEN a switch to the live window is under way`() {
            // Arrange
            val windows = windowsOf(index = 0)

            // Act
            val actual = windows.goToLive(duringCryptoWindow(2))

            // Assert
            assertThat(actual.switchingTo).isEqualTo(cryptoWindowId(2))
            assertThat(actual.isGoingToLive).isTrue()
        }

        @Test
        fun `GIVEN live window on screen WHEN going to live THEN nothing changes`() {
            // Arrange
            val windows = windowsOf(index = 0)

            // Act
            val actual = windows.goToLive(duringCryptoWindow(0))

            // Assert
            assertThat(actual).isSameInstanceAs(windows)
        }
    }

    @Nested
    inner class Received {

        @Test
        fun `GIVEN answer for the shown window WHEN received THEN it is on screen and the switch goes on`() {
            // Arrange
            val now = duringCryptoWindow(0)
            val windows = windowsOf(index = 0).select(eventId = cryptoWindowId(2), nowMillis = now)
            val refreshed = createCryptoEvent(index = 0, title = "Renamed", priceToBeat = "100".toBigDecimal())

            // Act
            val actual = windows.received(refreshed)

            // Assert
            assertThat(actual.shown).isEqualTo(refreshed)
            assertThat(actual.window).isEqualTo(refreshed.cryptoWindow)
            assertThat(actual.switchingTo).isEqualTo(cryptoWindowId(2))
        }

        @Test
        fun `GIVEN prices on screen WHEN the next answer lacks them THEN they are kept`() {
            // Arrange
            val priced = createCryptoEvent(
                index = 0,
                priceToBeat = "100".toBigDecimal(),
                finalPrice = "105".toBigDecimal(),
            )
            val windows = requireNotNull(PolymarketCryptoWindows.of(priced))

            // Act
            val actual = windows.received(createCryptoEvent(index = 0, title = "Renamed"))

            // Assert
            assertThat(actual.shown).isEqualTo(priced.copy(title = "Renamed"))
            assertThat(actual.window).isEqualTo(priced.cryptoWindow)
        }

        @Test
        fun `GIVEN prices on screen WHEN the next answer brings other ones THEN the new ones are shown`() {
            // Arrange
            val windows = requireNotNull(
                PolymarketCryptoWindows.of(
                    createCryptoEvent(index = 0, priceToBeat = "100".toBigDecimal(), finalPrice = "105".toBigDecimal()),
                ),
            )
            val corrected = createCryptoEvent(
                index = 0,
                priceToBeat = "101".toBigDecimal(),
                finalPrice = "106".toBigDecimal(),
            )

            // Act
            val actual = windows.received(corrected)

            // Assert
            assertThat(actual.shown).isEqualTo(corrected)
            assertThat(actual.window).isEqualTo(corrected.cryptoWindow)
        }

        @Test
        fun `GIVEN answer for the shown window WHEN it is no longer Up or Down THEN it is dropped`() {
            // Arrange
            val windows = windowsOf(index = 0)

            // Act
            val actual = windows.received(createPolymarketEvent(id = cryptoWindowId(0)))

            // Assert
            assertThat(actual).isSameInstanceAs(windows)
        }

        @Test
        fun `GIVEN switch under way WHEN its answer comes THEN that window is on screen with its own prices`() {
            // Arrange
            val now = duringCryptoWindow(1)
            val windows = requireNotNull(
                PolymarketCryptoWindows.of(createCryptoEvent(index = 0, priceToBeat = "100".toBigDecimal())),
            ).goToLive(now)
            val live = createCryptoEvent(index = 1)

            // Act
            val actual = windows.received(live)

            // Assert
            assertThat(actual.shown).isEqualTo(live)
            assertThat(actual.window).isEqualTo(live.cryptoWindow)
            assertThat(actual.switchingTo).isNull()
            assertThat(actual.isGoingToLive).isFalse()
        }

        @Test
        fun `GIVEN switch under way WHEN its answer is not Up or Down THEN the shown window stays`() {
            // Arrange
            val shown = windowsOf(index = 0)
            val windows = shown.goToLive(duringCryptoWindow(1))

            // Act
            val actual = windows.received(createPolymarketEvent(id = cryptoWindowId(1)))

            // Assert
            assertThat(actual).isEqualTo(shown)
        }

        @Test
        fun `GIVEN windows chosen one after another WHEN answers come late THEN only the last choice is shown`() {
            // Arrange
            val now = duringCryptoWindow(0)
            val windows = windowsOf(index = 0)
                .select(eventId = cryptoWindowId(1), nowMillis = now)
                .select(eventId = cryptoWindowId(2), nowMillis = now)

            // Act
            val afterLate = windows.received(createCryptoEvent(index = 1))
            val afterChosen = afterLate.received(createCryptoEvent(index = 2))
            val afterStale = afterChosen.received(createCryptoEvent(index = 0, title = "Stale"))

            // Assert
            assertThat(afterLate).isSameInstanceAs(windows)
            assertThat(afterChosen.shown).isEqualTo(createCryptoEvent(index = 2))
            assertThat(afterStale).isSameInstanceAs(afterChosen)
        }
    }

    @Nested
    inner class Failed {

        @Test
        fun `GIVEN switch under way WHEN its answer fails THEN the shown window stays`() {
            // Arrange
            val shown = windowsOf(index = 0)
            val windows = shown.goToLive(duringCryptoWindow(1))

            // Act
            val actual = windows.failed(cryptoWindowId(1))

            // Assert
            assertThat(actual).isEqualTo(shown)
        }

        @Test
        fun `GIVEN switch under way WHEN the answer of an earlier choice fails THEN the switch goes on`() {
            // Arrange
            val now = duringCryptoWindow(0)
            val windows = windowsOf(index = 0)
                .select(eventId = cryptoWindowId(1), nowMillis = now)
                .select(eventId = cryptoWindowId(2), nowMillis = now)

            // Act
            val actual = windows.failed(cryptoWindowId(1))

            // Assert
            assertThat(actual).isSameInstanceAs(windows)
        }
    }

    private fun windowsOf(index: Int, following: Int = 20, liveEventId: String? = null): PolymarketCryptoWindows {
        return requireNotNull(
            PolymarketCryptoWindows.of(createCryptoEvent(index, following = following, liveEventId = liveEventId)),
        )
    }

    private fun PolymarketCryptoWindows.switchedTo(index: Int): PolymarketCryptoWindows {
        return select(eventId = cryptoWindowId(index), nowMillis = 0).received(createCryptoEvent(index))
    }
}

/**
 * @property shown index of the window on screen
 * @property following how many windows after it the backend named
 * @property liveNow index of the window the clock stands in
 * @property namedLive event the backend named live with the shown one
 */
internal data class CryptoLiveTargetModel(
    val shown: Int,
    val following: Int = 20,
    val liveNow: Int,
    val namedLive: String?,
    val expected: String?,
)