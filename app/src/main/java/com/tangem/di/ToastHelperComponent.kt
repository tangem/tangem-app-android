package com.tangem.di

import com.tangem.ui.activity.MainActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [NavigatorModule::class])
interface ToastHelperComponent {

    fun inject(activity: MainActivity)

}