package com.tangem.features.onboarding.v2.twin.impl.issuers

import com.tangem.features.onboarding.v2.twin.impl.issuers.models.Issuer

/**
 * Storage for list of Twins [Issuer]
 */
internal interface IssuersConfigStorage {

    /** Get config */
    suspend fun getConfig(): List<Issuer>
}