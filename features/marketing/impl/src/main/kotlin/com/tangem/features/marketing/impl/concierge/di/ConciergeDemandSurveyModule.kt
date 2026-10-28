package com.tangem.features.marketing.impl.concierge.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.marketing.api.concierge.ConciergeDemandSurveyComponent
import com.tangem.features.marketing.impl.concierge.DefaultConciergeDemandSurveyComponent
import com.tangem.features.marketing.impl.concierge.model.ConciergeDemandSurveyModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ConciergeDemandSurveyComponentModule {

    @Binds
    @Singleton
    fun bindConciergeDemandSurveyComponentFactory(
        impl: DefaultConciergeDemandSurveyComponent.Factory,
    ): ConciergeDemandSurveyComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface ConciergeDemandSurveyModelModule {

    @Binds
    @IntoMap
    @ClassKey(ConciergeDemandSurveyModel::class)
    fun bindConciergeDemandSurveyModel(model: ConciergeDemandSurveyModel): Model
}