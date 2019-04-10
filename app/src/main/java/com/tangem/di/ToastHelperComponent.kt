package com.tangem.di

import com.tangem.ui.PrepareTransactionActivity
import com.tangem.ui.activity.EmptyWalletActivity
import com.tangem.ui.activity.LoadedWalletActivity
import com.tangem.ui.activity.MainActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [NavigatorModule::class])
interface ToastHelperComponent {

    fun inject(activity: MainActivity)

    fun inject(activity: LoadedWalletActivity)

    fun inject(activity: PrepareTransactionActivity)

    fun inject(activity: EmptyWalletActivity)

}