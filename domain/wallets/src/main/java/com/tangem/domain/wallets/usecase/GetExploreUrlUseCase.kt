package com.tangem.domain.wallets.usecase

import arrow.core.raise.catch
import com.tangem.blockchain.common.address.AddressType
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId

// TODO: Add tests
class GetExploreUrlUseCase(private val walletsManagersFacade: WalletManagersFacade) {

    // FIXME: Handle error
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        addressType: AddressType = AddressType.Default,
    ): String {
        return catch(
            block = {
                walletsManagersFacade.getExploreUrl(
                    userWalletId = userWalletId,
                    network = currency.network,
                    addressType = addressType,
                    contractAddress = (currency as? CryptoCurrency.Token)?.contractAddress,
                )
            },
            catch = { "" },
        )
    }
}