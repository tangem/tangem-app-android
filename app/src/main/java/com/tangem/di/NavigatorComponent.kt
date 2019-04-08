package com.tangem.di

import com.tangem.ui.activity.EmptyWalletActivity
import com.tangem.ui.activity.LoadedWalletActivity
import com.tangem.ui.activity.LogoActivity
import com.tangem.ui.activity.MainActivity
import com.tangem.ui.activity.PrepareCryptonitOtherApiWithdrawalActivity
import com.tangem.ui.activity.PrepareKrakenWithdrawalActivity
import com.tangem.ui.PrepareTransactionActivity
import com.tangem.ui.activity.PurgeActivity
import com.tangem.ui.activity.VerifyCardActivity

import javax.inject.Singleton

import dagger.Component

@Singleton
@Component(modules = [NavigatorModule::class])
interface NavigatorComponent {

    fun inject(activity: LogoActivity)

    fun inject(activity: MainActivity)

    fun inject(activity: PurgeActivity)

    fun inject(activity: PrepareTransactionActivity)

    fun inject(activity: PrepareCryptonitOtherApiWithdrawalActivity)

    fun inject(activity: PrepareKrakenWithdrawalActivity)

    fun inject(activity: LoadedWalletActivity)

    fun inject(activity: VerifyCardActivity)

    fun inject(activity: EmptyWalletActivity)

}