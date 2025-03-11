package com.tangem.features.onramp.selectcurrency.di

import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onramp.selectcurrency.model.OnrampSelectCurrencyModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(DecomposeComponent::class)
internal interface OnrampSelectCurrencyModelModule {

    @Binds
    @IntoMap
    @ClassKey(OnrampSelectCurrencyModel::class)
    fun bindOnrampSelectCurrencyModel(model: OnrampSelectCurrencyModel): Model
}