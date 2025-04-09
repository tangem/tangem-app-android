package com.tangem.features.send.v2.sendnft.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.send.v2.sendnft.confirm.model.NFTSendConfirmModel
import com.tangem.features.send.v2.sendnft.model.NFTSendModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface NFTSendModelModule {

    @Binds
    @IntoMap
    @ClassKey(NFTSendModel::class)
    fun provideNFTSendModel(model: NFTSendModel): Model

    @Binds
    @IntoMap
    @ClassKey(NFTSendConfirmModel::class)
    fun provideNFTSendConfirmModel(model: NFTSendConfirmModel): Model
}