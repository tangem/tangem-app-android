package com.tangem.domain.swap.models

import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.tokens.model.CryptoCurrencyStatus

/**
 * Domain layer representation of SwapPair data network model.
 *
 * @property from Short information about the token we want to change
 * @property to Short information about the token we want to exchange for
 * @property providers Exchange providers
 */
data class SwapPairModel(
    val from: CryptoCurrencyStatus,
    val to: CryptoCurrencyStatus,
    val providers: List<ExpressProvider>,
)