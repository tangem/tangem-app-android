package com.tangem.tap.features.home.domain

import com.tangem.datasource.api.tangemTech.models.GeoResponse

/** Repository for Home feature */
interface HomeRepository {

    /** Get user's country code */
    suspend fun getUserCountryCode(): GeoResponse
}
