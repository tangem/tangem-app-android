package com.tangem.spend.pushProvisioning.impl.di

import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner
import com.tangem.spend.pushProvisioning.PushProvisioningActivityBridge
import com.tangem.spend.pushProvisioning.impl.DefaultGoogleWalletProvisioner
import com.tangem.spend.pushProvisioning.impl.DefaultMeaWalletGooglePay
import com.tangem.spend.pushProvisioning.impl.DefaultPushProvisioningActivityBridge
import com.tangem.spend.pushProvisioning.impl.MeaWalletGooglePay
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface PushProvisioningModule {

    @Binds
    fun bindGoogleWalletProvisioner(impl: DefaultGoogleWalletProvisioner): GoogleWalletProvisioner

    @Binds
    fun bindMeaWalletGooglePay(impl: DefaultMeaWalletGooglePay): MeaWalletGooglePay

    @Binds
    fun bindPushProvisioningActivityBridge(impl: DefaultPushProvisioningActivityBridge): PushProvisioningActivityBridge
}