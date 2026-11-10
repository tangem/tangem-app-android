package com.tangem.domain.tangempay.model

data class TangemPayAuthChallenge(
    val challenge: String,
    val session: TangemPayAuthSession,
)