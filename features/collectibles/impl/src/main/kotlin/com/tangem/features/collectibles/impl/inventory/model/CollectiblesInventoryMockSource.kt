package com.tangem.features.collectibles.impl.inventory.model

import arrow.core.Either
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Walks the collection tab through every state, one per [refresh]: error on the first load, empty after a retry,
 * cards from then on. The buyback tab always has cards, shuffled on each refresh so the grid's item animations
 * are visible.
 */
// TODO([REDACTED_TASK_KEY]): replace with GetCInventoryUseCase(listType) and RefreshCInventoryUseCase
internal class CollectiblesInventoryMockSource @Inject constructor() {

    data class Card(
        val id: String,
        val imageUrl: String,
        val title: String,
        val price: String,
        val grade: String,
        /** What is left of the buyback window; `null` for a card already in the collection */
        val timeLeft: Duration?,
    )

    private val cardsByTab = MutableStateFlow<Map<CollectiblesInventoryTab, Either<Throwable, List<Card>>>?>(null)

    private var refreshCount = 0

    fun observe(tab: CollectiblesInventoryTab): Flow<Either<Throwable, List<Card>>> {
        return cardsByTab.filterNotNull().map { cards -> cards.getValue(tab) }.distinctUntilChanged()
    }

    suspend fun refresh() {
        delay(timeMillis = 1_000L)

        val collection = when (refreshCount++) {
            0 -> Either.Left(IllegalStateException("Mock inventory load error"))
            1 -> Either.Right(emptyList())
            else -> Either.Right(COLLECTION.shuffled())
        }
        cardsByTab.value = mapOf(
            CollectiblesInventoryTab.Buyback to Either.Right(BUYBACK.shuffled()),
            CollectiblesInventoryTab.Collection to collection,
        )
    }

    private data class Nft(val name: String, val image: String, val grade: String, val price: String)

    private companion object {

        val NFTS = listOf(
            Nft(
                name = "2002 Pokemon Neo Destiny Unown T",
                image = "https://arweave.net/wH0aDz0evnRMzOVG8xaIvML6qF97XGaVBZeQvI6Q5L0",
                grade = "CGC 9",
                price = "$13",
            ),
            Nft(
                name = "2026 Pokemon Mega Evolution Prom",
                image = "https://arweave.net/HVWnaGt_wzTwa3FrEDFZwehyJ21w93didewd4BgyHqE",
                grade = "CGC 9.5",
                price = "$30",
            ),
            Nft(
                name = "1999 #13 MUK-HOLO PSA 9 POKEMON",
                image = "https://arweave.net/1Bgy4YOi2pvuUd1yOAsJyAK571xA8aSSmweyAUdii5E",
                grade = "PSA 9",
                price = "$192",
            ),
        )

        val BUYBACK_TIMERS = listOf(
            Duration.ZERO,
            30.seconds,
            45.minutes,
            48.hours + 24.minutes,
            12.hours + 5.minutes,
        )

        val BUYBACK = List(size = 5) { index ->
            createCard(id = "buyback_$index", index = index, timeLeft = BUYBACK_TIMERS[index % BUYBACK_TIMERS.size])
        }

        val COLLECTION = List(size = 24) { index ->
            createCard(id = "collection_$index", index = index, timeLeft = null)
        }

        fun createCard(id: String, index: Int, timeLeft: Duration?): Card {
            val nft = NFTS[index % NFTS.size]
            return Card(
                id = id,
                imageUrl = nft.image,
                title = nft.name,
                price = nft.price,
                grade = nft.grade,
                timeLeft = timeLeft,
            )
        }
    }
}