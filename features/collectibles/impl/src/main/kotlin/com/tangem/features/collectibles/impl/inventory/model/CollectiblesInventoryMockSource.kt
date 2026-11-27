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
// TODO([REDACTED_TASK_KEY]): replace with GetCInventoryUseCase(listType), SearchCInventoryUseCase and RefreshCInventoryUseCase
internal class CollectiblesInventoryMockSource @Inject constructor() {

    data class Card(
        val id: String,
        val imageUrl: String,
        val backImageUrl: String,
        val title: String,
        val year: String,
        val cardName: String,
        val price: String,
        val grade: String,
        val gradingCompany: String,
        val gradeValue: String,
        val gradingId: String,
        val serialNumber: String,
        /** What is left of the buyback window; `null` for a card already in the collection */
        val timeLeft: Duration?,
        val buybackPrice: String?,
    )

    private val cardsByTab = MutableStateFlow<Map<CollectiblesInventoryTab, Either<Throwable, List<Card>>>?>(null)

    private var refreshCount = 0

    fun observe(tab: CollectiblesInventoryTab): Flow<Either<Throwable, List<Card>>> {
        return cardsByTab.filterNotNull().map { cards -> cards.getValue(tab) }.distinctUntilChanged()
    }

    /** Filters the cards already loaded into the tabs; a tab that is still loading or failed adds nothing. */
    fun search(query: String): Flow<List<Card>> {
        return cardsByTab
            .map { cards ->
                CollectiblesInventoryTab.entries
                    .flatMap { tab -> cards?.get(tab)?.getOrNull().orEmpty() }
                    .filter { card -> card.title.contains(query, ignoreCase = true) }
            }
            .distinctUntilChanged()
    }

    /** Reads only what the inventory already holds, the way the full view takes its card from the cache. */
    fun getCard(id: String): Card? = (BUYBACK + COLLECTION).firstOrNull { card -> card.id == id }

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

    private data class Nft(
        val name: String,
        val year: String,
        val cardName: String,
        val image: String,
        val backImage: String,
        val gradingCompany: String,
        val gradeValue: String,
        val price: String,
        val buybackPrice: String,
    )

    private companion object {

        val NFTS = listOf(
            Nft(
                name = "2002 Pokemon Neo Destiny Unown T",
                year = "2002",
                cardName = "Unown T",
                image = "https://arweave.net/wH0aDz0evnRMzOVG8xaIvML6qF97XGaVBZeQvI6Q5L0",
                backImage = "https://arweave.net/2tv5gmKJumtH-1QX0j6CenDUHi5YAiFmLJozMJklyXU",
                gradingCompany = "CGC",
                gradeValue = "9",
                price = "$13",
                buybackPrice = "$11.05",
            ),
            Nft(
                name = "2026 Pokemon Mega Evolution Prom",
                year = "2026",
                cardName = "Mega Evolution Prom",
                image = "https://arweave.net/HVWnaGt_wzTwa3FrEDFZwehyJ21w93didewd4BgyHqE",
                backImage = "https://arweave.net/eywh7DSr4_jGBoiz6fsu7n0LUUtMxVfn2353JUu6ons",
                gradingCompany = "CGC",
                gradeValue = "9.5",
                price = "$30",
                buybackPrice = "$25.50",
            ),
            Nft(
                name = "1999 #13 MUK-HOLO PSA 9 POKEMON",
                year = "1999",
                cardName = "MUK-HOLO",
                image = "https://arweave.net/1Bgy4YOi2pvuUd1yOAsJyAK571xA8aSSmweyAUdii5E",
                backImage = "https://arweave.net/71wSM75pN2VpJ9y4QkSqS-ZlMo-9QzkrhgJPgW4XtC8",
                gradingCompany = "PSA",
                gradeValue = "9",
                price = "$192",
                buybackPrice = "$163.20",
            ),
        )

        const val GRADING_ID_BASE = 4_812_000_000L

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
                backImageUrl = nft.backImage,
                title = nft.name,
                year = nft.year,
                cardName = nft.cardName,
                price = nft.price,
                grade = "${nft.gradingCompany} ${nft.gradeValue}",
                gradingCompany = nft.gradingCompany,
                gradeValue = nft.gradeValue,
                gradingId = (GRADING_ID_BASE + index).toString(),
                serialNumber = (index + 1).toString(),
                timeLeft = timeLeft,
                buybackPrice = nft.buybackPrice.takeIf { timeLeft != null },
            )
        }
    }
}