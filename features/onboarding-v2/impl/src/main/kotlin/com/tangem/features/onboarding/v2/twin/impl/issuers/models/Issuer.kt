package com.tangem.features.onboarding.v2.twin.impl.issuers.models

import com.squareup.moshi.Json

internal data class Issuer(
    @Json(name = "privateKey") val privateKey: String,
    @Json(name = "publicKey") val publicKey: String,
)