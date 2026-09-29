package com.tangem.spend.pushProvisioning.mock.di

import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner
import com.tangem.spend.pushProvisioning.PushProvisioningActivityBridge
import com.tangem.spend.pushProvisioning.mock.MockedGoogleWalletProvisioner
import com.tangem.spend.pushProvisioning.mock.MockedPushProvisioningActivityBridge
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface MockedPushProvisioningModule {

    @Binds
    fun bindGoogleWalletProvisioner(impl: MockedGoogleWalletProvisioner): GoogleWalletProvisioner

    @Binds
    fun bindPushProvisioningActivityBridge(impl: MockedPushProvisioningActivityBridge): PushProvisioningActivityBridge
}