package com.tangem.features.tangempay.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.tangempay.model.TangemPayAddFundsModel
import com.tangem.features.tangempay.model.TangemPayAddToWalletModel
import com.tangem.features.tangempay.model.TangemPayCardDetailsBlockModel
import com.tangem.features.tangempay.model.TangemPayChangePinModel
import com.tangem.features.tangempay.model.TangemPayDetailsModel
import com.tangem.features.tangempay.model.TangemPayTxHistoryDetailsModel
import com.tangem.features.tangempay.model.TangemPayTxHistoryModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface TangemPayModelModule {

    @Binds
    @IntoMap
    @ClassKey(TangemPayDetailsModel::class)
    fun bindTangemPayDetailsModel(model: TangemPayDetailsModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayTxHistoryModel::class)
    fun bindTangemPayTxHistoryModel(model: TangemPayTxHistoryModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayTxHistoryDetailsModel::class)
    fun bindTangemPayTxHistoryDetailsModel(model: TangemPayTxHistoryDetailsModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayChangePinModel::class)
    fun bindTangemPayChangePinModel(model: TangemPayChangePinModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayCardDetailsBlockModel::class)
    fun bindTangemPayCardDetailsBlockModel(model: TangemPayCardDetailsBlockModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayAddToWalletModel::class)
    fun bindTangemPayAddToWalletModel(model: TangemPayAddToWalletModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayAddFundsModel::class)
    fun bindTangemPayAddFundsModel(model: TangemPayAddFundsModel): Model
}