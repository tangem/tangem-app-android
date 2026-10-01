package com.tangem.domain.jointaccount.gas

import arrow.core.Either
import com.tangem.domain.jointaccount.model.SafeGasToken
import com.tangem.domain.models.network.Network

/** The creation and the co-signing paths price a gas token from the same source, so both apply the same tolerance */
fun interface JointGasTokenResolver {

    /** `null` when [address] is neither the coin nor a Smart Gas token of [network] */
    suspend fun resolve(network: Network, address: String): Either<Throwable, SafeGasToken?>
}