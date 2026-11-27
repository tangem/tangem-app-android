package com.tangem.features.allservices.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.allservices.impl.entries.model.CryptoAccountEntryModel
import com.tangem.features.allservices.impl.entries.model.EarnOfferEntryModel
import com.tangem.features.allservices.impl.entries.model.JointAccountEntryModel
import com.tangem.features.allservices.impl.model.AllServicesModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface ModelModule {

    @Binds
    @IntoMap
    @ClassKey(AllServicesModel::class)
    fun bindsAllServicesModel(model: AllServicesModel): Model

    @Binds
    @IntoMap
    @ClassKey(CryptoAccountEntryModel::class)
    fun bindsCryptoAccountEntryModel(model: CryptoAccountEntryModel): Model

    @Binds
    @IntoMap
    @ClassKey(EarnOfferEntryModel::class)
    fun bindsEarnOfferEntryModel(model: EarnOfferEntryModel): Model

    @Binds
    @IntoMap
    @ClassKey(JointAccountEntryModel::class)
    fun bindsJointAccountEntryModel(model: JointAccountEntryModel): Model
}