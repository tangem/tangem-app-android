package com.tangem.tap.data.converter

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.offramp.model.PendingOfframp
import com.tangem.tap.data.model.PendingOfframpEntry
import com.tangem.utils.converter.Converter

/**
 * Converts a persisted [PendingOfframpEntry] into the domain [PendingOfframp].
 */
internal class PendingOfframpEntryConverter : Converter<PendingOfframpEntry, PendingOfframp> {

    override fun convert(value: PendingOfframpEntry): PendingOfframp = PendingOfframp(
        requestId = value.requestId,
        userWalletId = UserWalletId(stringValue = value.userWalletId),
        currencyId = value.currencyId,
        createdAt = value.createdAt,
    )
}