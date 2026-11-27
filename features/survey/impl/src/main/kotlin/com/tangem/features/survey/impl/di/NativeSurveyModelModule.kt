package com.tangem.features.survey.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.survey.impl.nativesurvey.screen.model.NativeSurveyModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface NativeSurveyModelModule {

    @Binds
    @IntoMap
    @ClassKey(NativeSurveyModel::class)
    fun bindNativeSurveyModel(model: NativeSurveyModel): Model
}