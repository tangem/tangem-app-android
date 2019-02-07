package com.tangem.di

import com.tangem.presentation.activity.EmptyWalletActivity
import com.tangem.presentation.activity.LoadedWalletActivity
import com.tangem.presentation.activity.LogoActivity
import com.tangem.presentation.activity.MainActivity
import com.tangem.presentation.activity.PrepareCryptonitOtherApiWithdrawalActivity
import com.tangem.presentation.activity.PrepareKrakenWithdrawalActivity
import com.tangem.presentation.activity.PrepareTransactionActivity
import com.tangem.presentation.activity.PurgeActivity
import com.tangem.presentation.activity.VerifyCardActivity

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