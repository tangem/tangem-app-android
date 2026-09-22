package com.tangem.features.txhistory.model

import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.addressbook.usecase.GetContactAddressLookupUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Single source of the [TxHistoryLookupContext] used by both the history list and the details screen: combines the
 * account statuses (own-address map per network), the accounts-mode toggle, the wallet display info and the address
 * books of every wallet. Callers apply their own dispatcher / sharing — this only produces the cold combined flow.
 *
 * @param userWalletId the wallet whose history is being viewed — the priority wallet when one address is saved as a
 * contact in several address books.
 */
internal class TxHistoryOwnerLookupProducer @Inject constructor(
    private val multiAccountStatusListSupplier: MultiAccountStatusListSupplier,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val getWalletIconUseCase: GetWalletIconUseCase,
    private val walletIconUMConverter: WalletIconUMConverter,
    private val getContactAddressLookupUseCase: GetContactAddressLookupUseCase,
) {

    operator fun invoke(userWalletId: UserWalletId): Flow<TxHistoryLookupContext> {
        val ownAccountByNetwork = multiAccountStatusListSupplier()
            .map(::buildOwnAccountAddressMapAllNetworks)
            .distinctUntilChanged()

        val walletInfoById = userWalletsListRepository.userWallets
            .filterNotNull()
            .map { wallets ->
                wallets.associate { wallet ->
                    wallet.walletId to WalletInfo(
                        name = wallet.name,
                        deviceIconUM = walletIconUMConverter.convert(getWalletIconUseCase(wallet)),
                    )
                }
            }
            .distinctUntilChanged()

        return combine(
            flow = ownAccountByNetwork,
            flow2 = isAccountsModeEnabledUseCase(),
            flow3 = walletInfoById,
            flow4 = getContactAddressLookupUseCase(),
        ) { accounts, modeEnabled, walletInfo, contacts ->
            TxHistoryLookupContext(
                ownAccountByNetwork = accounts,
                isAccountsModeEnabled = modeEnabled,
                walletInfoById = walletInfo,
                contacts = contacts,
                viewedWalletId = userWalletId,
            )
        }
    }
}