package com.tangem.features.send.subcomponents.destination.model.converters

import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.send.subcomponents.destination.ui.state.DestinationWalletUM

internal object JointAccountDestinationFactory {

    fun create(
        accountStatus: AccountStatus.Joint,
        wallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
        supportedNetworks: List<Network.RawID>,
    ): List<DestinationWalletUM> {
        val activeValue = accountStatus.value as? JointAccountStatusValue.Active ?: return emptyList()
        if (cryptoCurrency.network.id.rawId !in supportedNetworks) return emptyList()

        return listOf(
            DestinationWalletUM(
                name = wallet.name,
                address = activeValue.safeAddress,
                cryptoCurrency = cryptoCurrency,
                userWalletId = wallet.walletId,
                account = accountStatus.account,
            ),
        )
    }
}