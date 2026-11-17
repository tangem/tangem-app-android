package com.tangem.domain.tangempay.repository

import com.tangem.domain.tangempay.model.KycAdditionalInfoAccount

/**
 * Remembers the accounts that already showed the CDD/EDD popup, for the lifetime of the process only: the popup
 * opens on the first visit of an account after app launch.
 */
interface KycAdditionalInfoPopupStore {

    /** Marks the popup of [account] as shown; returns `false` if it was already shown in this session. */
    suspend fun tryMarkShown(account: KycAdditionalInfoAccount): Boolean

    suspend fun reset(account: KycAdditionalInfoAccount)
}