package com.tangem.features.txhistory.model

import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Single source of the [TxHistoryLookupContext] used by both the history list and the details screen: combines the
 * account statuses (own-address map per network), the accounts-mode toggle and the wallet display info. Callers apply
 * their own dispatcher / sharing — this only produces the cold combined flow.
 */
internal class TxHistoryOwnerLookupProducer @Inject constructor(
    private val multiAccountStatusListSupplier: MultiAccountStatusListSupplier,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val getWalletIconUseCase: GetWalletIconUseCase,
    private val walletIconUMConverter: WalletIconUMConverter,
) {

    operator fun invoke(): Flow<TxHistoryLookupContext> {
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
            ownAccountByNetwork,
            isAccountsModeEnabledUseCase(),
            walletInfoById,
        ) { accounts, modeEnabled, walletInfo ->
            TxHistoryLookupContext(
                ownAccountByNetwork = accounts,
                isAccountsModeEnabled = modeEnabled,
                walletInfoById = walletInfo,
            )
        }
    }
}