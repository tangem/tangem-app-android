package com.tangem.tap.domain.userWalletList.di

import com.tangem.tap.domain.userWalletList.UserWalletsListManager
import com.tangem.tap.domain.userWalletList.implementation.DummyUserWalletsListManager

fun UserWalletsListManager.Companion.provideDummyImplementation(): UserWalletsListManager {
    return DummyUserWalletsListManager()
}