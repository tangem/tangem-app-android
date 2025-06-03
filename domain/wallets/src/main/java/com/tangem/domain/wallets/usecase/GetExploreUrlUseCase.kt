package com.tangem.domain.wallets.usecase

import arrow.core.raise.catch
import com.tangem.blockchain.common.address.AddressType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
// [REDACTED_TODO_COMMENT]
class GetExploreUrlUseCase(private val walletsManagersFacade: WalletManagersFacade) {
// [REDACTED_TODO_COMMENT]
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
