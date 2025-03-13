package com.tangem.feature.qrscanning.di

import com.tangem.core.decompose.model.Model
import com.tangem.feature.qrscanning.DefaultQrScanningComponent
import com.tangem.feature.qrscanning.QrScanningComponent
import com.tangem.feature.qrscanning.model.QrScanningModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface QrScanningFeatureModule {

    @Binds
    fun bindComponentFactory(impl: DefaultQrScanningComponent.Factory): QrScanningComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(QrScanningModel::class)
    fun bindModel(model: QrScanningModel): Model
}