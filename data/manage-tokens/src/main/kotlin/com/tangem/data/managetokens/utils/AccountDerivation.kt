package com.tangem.data.managetokens.utils

import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.network.Network

/** Where the networks of an account live: the account node of the card path, or the owner key path of a joint account */
internal sealed interface AccountDerivation {

    data class Personal(val index: DerivationIndex) : AccountDerivation

    data class Joint(val path: Network.DerivationPath.Joint) : AccountDerivation
}