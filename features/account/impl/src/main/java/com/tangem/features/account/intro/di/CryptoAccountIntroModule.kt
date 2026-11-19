package com.tangem.features.account.intro.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.account.intro.model.CryptoAccountIntroModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface CryptoAccountIntroModule {

    @Binds
    @IntoMap
    @ClassKey(CryptoAccountIntroModel::class)
    fun bindCryptoAccountIntroModel(model: CryptoAccountIntroModel): Model
}