package com.tangem.features.commonfeatures.impl.addtoportfolio.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.commonfeatures.impl.addtoportfolio.model.AddToPortfolioModel
import com.tangem.features.commonfeatures.impl.addtoportfolio.model.AddToPortfolioPreselectedDataModel
import com.tangem.features.commonfeatures.impl.addtoportfolio.model.AddTokenModel
import com.tangem.features.commonfeatures.impl.addtoportfolio.model.ChooseNetworkModel
import com.tangem.features.commonfeatures.impl.addtoportfolio.model.TokenActionsModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface AddToPortfolioModelModule {

    @Binds
    @IntoMap
    @ClassKey(AddTokenModel::class)
    fun addTokenModel(model: AddTokenModel): Model

    @Binds
    @IntoMap
    @ClassKey(AddToPortfolioModel::class)
    fun addToPortfolioModel(model: AddToPortfolioModel): Model

    @Binds
    @IntoMap
    @ClassKey(AddToPortfolioPreselectedDataModel::class)
    fun addToPortfolioPreselectedDataModel(model: AddToPortfolioPreselectedDataModel): Model

    @Binds
    @IntoMap
    @ClassKey(TokenActionsModel::class)
    fun tokenActionsModel(model: TokenActionsModel): Model

    @Binds
    @IntoMap
    @ClassKey(ChooseNetworkModel::class)
    fun chooseNetworkModel(model: ChooseNetworkModel): Model
}