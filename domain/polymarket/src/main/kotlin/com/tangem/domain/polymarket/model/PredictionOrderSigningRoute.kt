package com.tangem.domain.polymarket.model

sealed interface PredictionOrderSigningRoute {

    data object Owner : PredictionOrderSigningRoute

    data class Session(val identity: PolymarketClobIdentity.Session) : PredictionOrderSigningRoute
}