package com.tangem.domain.redux

import com.tangem.domain.models.wallet.UserWallet
import org.rekotlin.Action

interface ReduxStateHolder {

    fun dispatch(action: Action)

    suspend fun dispatchWithMain(action: Action)

    suspend fun onUserWalletSelected(userWallet: UserWallet)

    fun dispatchDialogShow(dialog: StateDialog)
}