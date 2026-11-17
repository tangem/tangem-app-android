package com.tangem.domain.polymarket.model

fun List<PolymarketSessionKey>.newestFor(address: String): PolymarketSessionKey? =
    filter { it.address.equals(address, ignoreCase = true) }.maxByOrNull { it.createdAt }

fun List<PolymarketSessionKey>.newestPerAddress(): List<PolymarketSessionKey> =
    groupBy { it.address.lowercase() }.values.map { records -> records.maxBy { it.createdAt } }