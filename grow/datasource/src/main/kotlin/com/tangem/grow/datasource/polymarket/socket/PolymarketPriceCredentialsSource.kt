package com.tangem.grow.datasource.polymarket.socket

import kotlinx.coroutines.flow.StateFlow

interface PolymarketPriceCredentialsSource {

    /** What the live-data socket authorizes with; it stays closed while there is nothing. */
    val credentials: StateFlow<PolymarketPriceAuthDto.Credentials?>
}