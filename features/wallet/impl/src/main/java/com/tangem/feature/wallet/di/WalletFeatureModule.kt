package com.tangem.feature.wallet.di

import com.tangem.core.decompose.model.Model
import com.tangem.feature.wallet.DefaultWalletEntryComponent
import com.tangem.feature.wallet.child.organizetokens.model.OrganizeTokensModel
import com.tangem.feature.wallet.child.wallet.model.WalletModel
import com.tangem.feature.wallet.utils.DefaultUserWalletImageFetcher
import com.tangem.feature.wallet.utils.DefaultUserWalletsFetcher
import com.tangem.features.wallet.WalletEntryComponent
import com.tangem.features.wallet.utils.UserWalletImageFetcher
import com.tangem.features.wallet.utils.UserWalletsFetcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface WalletFeatureModule {

    @Binds
    fun bindComponentFactory(impl: DefaultWalletEntryComponent.Factory): WalletEntryComponent.Factory

    @Binds
    fun bindUserWalletsFetcher(impl: DefaultUserWalletsFetcher.Factory): UserWalletsFetcher.Factory

    @Binds
    @Singleton
    fun bindUserWalletImageFetcher(impl: DefaultUserWalletImageFetcher): UserWalletImageFetcher

    @Binds
    @IntoMap
    @ClassKey(WalletModel::class)
    fun bindWalletModel(model: WalletModel): Model

    @Binds
    @IntoMap
    @ClassKey(OrganizeTokensModel::class)
    fun bindOrganizeTokensModel(model: OrganizeTokensModel): Model
}