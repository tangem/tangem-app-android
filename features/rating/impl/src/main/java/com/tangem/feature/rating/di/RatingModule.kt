package com.tangem.feature.rating.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.feature.rating.DefaultRatingComponent
import com.tangem.feature.rating.model.RatingModel
import com.tangem.features.rating.RatingComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
internal interface RatingFeatureModule {

    @Binds
    @Singleton
    fun bindFactory(factory: DefaultRatingComponent.Factory): RatingComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface RatingModelModule {

    @Binds
    @IntoMap
    @ClassKey(RatingModel::class)
    fun bindModel(model: RatingModel): Model
}