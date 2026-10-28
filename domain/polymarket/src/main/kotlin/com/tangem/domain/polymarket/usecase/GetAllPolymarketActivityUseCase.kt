package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketActivityRepository
import com.tangem.domain.polymarket.model.PolymarketActivity
import com.tangem.domain.polymarket.model.PolymarketActivityError
import com.tangem.domain.polymarket.model.PolymarketActivityFilter
import com.tangem.domain.polymarket.model.PolymarketActivityListConfig
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class GetAllPolymarketActivityUseCase(
    private val repository: PolymarketActivityRepository,
    private val deriveAddresses: DerivePolymarketAddressesUseCase,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        filter: PolymarketActivityFilter,
    ): Either<PolymarketActivityError, List<PolymarketActivity>> = either {
        val addresses = ensureNotNull(deriveAddresses.stored(userWalletId)) {
            PolymarketActivityError.Unknown(null, null)
        }
        val config = PolymarketActivityListConfig(addresses, filter)
        val records = linkedMapOf<String, PolymarketActivity>()
        val cursors = mutableSetOf<String>()
        var cursor: String? = null
        do {
            currentCoroutineContext().ensureActive()
            val page = repository.getActivity(config, cursor, EXPORT_PAGE_SIZE).bind()
            currentCoroutineContext().ensureActive()
            page.items.forEach { records.putIfAbsent(it.id, it) }
            cursor = page.nextCursor
            cursor?.let {
                ensure(cursors.add(it)) { PolymarketActivityError.Unknown(null, "Repeated activity cursor") }
            }
        } while (cursor != null)
        records.values.toList()
    }

    private companion object {
        const val EXPORT_PAGE_SIZE = 100
    }
}