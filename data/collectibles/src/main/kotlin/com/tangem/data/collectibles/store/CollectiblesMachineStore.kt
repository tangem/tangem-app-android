package com.tangem.data.collectibles.store

import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.domain.collectibles.models.CollectibleGachaMachine

/**
 * In-memory machines read during this process, keyed by [CollectibleGachaMachine.externalId].
 *
 * The catalogue endpoint serves a complete machine per list entry, so a machine opened from the list is already
 * here and its details screen paints without a round trip.
 */
internal class CollectiblesMachineStore(
    private val store: RuntimeSharedStore<Map<String, CollectibleGachaMachine>>,
) {

    suspend fun getSyncOrNull(externalId: String): CollectibleGachaMachine? = store.getSyncOrNull()?.get(externalId)

    suspend fun store(machines: List<CollectibleGachaMachine>) {
        if (machines.isEmpty()) return

        store.update(default = emptyMap()) { stored ->
            stored + machines.associateBy(CollectibleGachaMachine::externalId)
        }
    }
}