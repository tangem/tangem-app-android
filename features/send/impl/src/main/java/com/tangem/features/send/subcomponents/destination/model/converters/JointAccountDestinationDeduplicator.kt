package com.tangem.features.send.subcomponents.destination.model.converters

import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.send.subcomponents.destination.ui.state.DestinationWalletUM

/**
 * Every local wallet that is a member of a joint account holds its own copy of it, all pointing at one Safe, so the
 * same address would be offered once per member. Keeps one entry per Safe address — the sender's own copy when it is
 * a member, otherwise the first — at the position of its first occurrence.
 */
internal object JointAccountDestinationDeduplicator {

    fun deduplicate(destinations: List<DestinationWalletUM>, senderWalletId: UserWalletId): List<DestinationWalletUM> {
        val keptBySafe = destinations
            .filter { it.account is Account.Joint }
            .groupBy { it.safeKey() }
            .mapValues { (_, members) -> members.firstOrNull { it.userWalletId == senderWalletId } ?: members.first() }

        val offeredSafes = mutableSetOf<String>()
        return destinations.mapNotNull { destination ->
            if (destination.account !is Account.Joint) return@mapNotNull destination

            val safe = destination.safeKey()
            if (offeredSafes.add(safe)) keptBySafe.getValue(safe) else null
        }
    }

    private fun DestinationWalletUM.safeKey(): String = address.lowercase()
}