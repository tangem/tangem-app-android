package com.tangem.tap.features.scanfails.di

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.decompose.model.Model
import com.tangem.domain.card.ScanFailsCounter
import com.tangem.domain.card.ScanFailsRequester
import com.tangem.tap.domain.scanCard.DefaultScanFailsCounter
import com.tangem.tap.features.scanfails.ScanFailsComponent
import com.tangem.tap.features.scanfails.ScanFailsModel
import com.tangem.tap.features.scanfails.ScanFailsRequesterProxy
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ScanFailsModule {

    @Binds
    fun bindComponentFactory(impl: ScanFailsComponent.Factory): ComponentFactory<Unit, ScanFailsComponent>

    @Binds
    @IntoMap
    @ClassKey(ScanFailsModel::class)
    fun bindModel(model: ScanFailsModel): Model

    @Binds
    @Singleton
    fun bindRequester(impl: ScanFailsRequesterProxy): ScanFailsRequester

    @Binds
    @Singleton
    fun bindScanFailsCounter(impl: DefaultScanFailsCounter): ScanFailsCounter
}