package com.tangem.spend.pushProvisioning.noop.di

import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner
import com.tangem.spend.pushProvisioning.PushProvisioningActivityBridge
import com.tangem.spend.pushProvisioning.noop.NoopGoogleWalletProvisioner
import com.tangem.spend.pushProvisioning.noop.NoopPushProvisioningActivityBridge
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface NoopPushProvisioningModule {

    @Binds
    fun bindGoogleWalletProvisioner(impl: NoopGoogleWalletProvisioner): GoogleWalletProvisioner

    @Binds
    fun bindPushProvisioningActivityBridge(impl: NoopPushProvisioningActivityBridge): PushProvisioningActivityBridge
}