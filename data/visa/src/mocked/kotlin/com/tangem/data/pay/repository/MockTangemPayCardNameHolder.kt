package com.tangem.data.pay.repository

import arrow.core.getOrElse
import com.tangem.domain.models.account.CardDisplayName
import javax.inject.Inject
import javax.inject.Singleton

/** Shared, mutable mock card display name so rename is exercisable end-to-end in the MOCK env. */
@Singleton
internal class MockTangemPayCardNameHolder @Inject constructor() {

    @Volatile
    var displayName: CardDisplayName? = CardDisplayName(DEFAULT_NAME).getOrElse { null }

    private companion object {
        const val DEFAULT_NAME = "My Card"
    }
}