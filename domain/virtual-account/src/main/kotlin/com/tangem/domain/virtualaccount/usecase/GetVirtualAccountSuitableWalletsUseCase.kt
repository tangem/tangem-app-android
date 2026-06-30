package com.tangem.domain.virtualaccount.usecase

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.models.wallet.isTangemPayCompatible

class GetVirtualAccountSuitableWalletsUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
) {
    operator fun invoke(): List<UserWallet> {
        return userWalletsListRepository.userWallets.value
            .orEmpty()
            .filter { it.isMultiCurrency && !it.isLocked && it.isTangemPayCompatible }
    }
}