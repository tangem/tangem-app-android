package com.tangem.features.tangempay.eligibility

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.tangempay.eligibility.country.ResidencyListModel
import com.tangem.features.tangempay.eligibility.country.CountrySelectorModel
import com.tangem.features.tangempay.eligibility.waitlist.TangemPayWaitlistModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface TangemPayEligibilityModelsModule {

    @Binds
    @IntoMap
    @ClassKey(ResidencyListModel::class)
    fun bindResidencyListModel(model: ResidencyListModel): Model

    @Binds
    @IntoMap
    @ClassKey(CountrySelectorModel::class)
    fun bindCountrySelectorModel(model: CountrySelectorModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayWaitlistModel::class)
    fun bindWaitlistModel(model: TangemPayWaitlistModel): Model
}