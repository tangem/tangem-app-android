package com.tangem.features.walletconnect.transaction.utils

const val ADDRESS_FIRST_PART_LENGTH = 7
const val ADDRESS_SECOND_PART_LENGTH = 4

internal fun String.toShortAddressText() =
    "${take(ADDRESS_FIRST_PART_LENGTH)}...${takeLast(ADDRESS_SECOND_PART_LENGTH)}"