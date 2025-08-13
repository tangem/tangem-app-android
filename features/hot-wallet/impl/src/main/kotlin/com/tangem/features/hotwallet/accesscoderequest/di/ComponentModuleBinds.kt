package com.tangem.features.hotwallet.accesscoderequest.di

import com.tangem.core.decompose.model.Model
import com.tangem.domain.wallets.hot.HotWalletPasswordRequester
import com.tangem.features.hotwallet.HotAccessCodeRequestComponent
import com.tangem.features.hotwallet.accesscoderequest.DefaultHotAccessCodeRequestComponent
import com.tangem.features.hotwallet.accesscoderequest.HotAccessCodeRequestModel
import com.tangem.features.hotwallet.accesscoderequest.proxy.HotWalletPasswordRequesterProxy
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ComponentModuleBinds {

    @Binds
    fun bindComponentFactory(impl: DefaultHotAccessCodeRequestComponent.Factory): HotAccessCodeRequestComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(HotAccessCodeRequestModel::class)
    fun bindCreateMobileWalletModel(model: HotAccessCodeRequestModel): Model

    @Binds
    @Singleton
    fun bindHotWalletPasswordRequester(impl: HotWalletPasswordRequesterProxy): HotWalletPasswordRequester
}