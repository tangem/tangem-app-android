package com.tangem.domain.features.addCustomToken

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.crypto.hdWallet.DerivationPath

/**
 * Created by Anton Zhilenkov on 07/04/2022.
 */
sealed class CustomCurrency(
    val network: Blockchain,
    val derivationPath: DerivationPath?,
) {

    @Deprecated("It will be removed in next releases")
    class CustomBlockchain(
        network: Blockchain,
        derivationPath: DerivationPath?,
    ) : CustomCurrency(network, derivationPath)

    @Deprecated("It will be removed in next releases")
    class CustomToken(
        val token: Token,
        network: Blockchain,
        derivationPath: DerivationPath?,
    ) : CustomCurrency(network, derivationPath)
}
