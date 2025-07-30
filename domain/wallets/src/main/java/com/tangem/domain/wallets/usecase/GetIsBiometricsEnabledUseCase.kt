package com.tangem.domain.wallets.usecase

import com.tangem.domain.wallets.legacy.UserWalletsListManager
import javax.inject.Inject

class GetIsBiometricsEnabledUseCase @Inject constructor(
    private val userWalletsListManager: UserWalletsListManager,
) {

    operator fun invoke(): Boolean = userWalletsListManager as? UserWalletsListManager.Lockable != null
}