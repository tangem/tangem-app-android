package com.tangem.features.tangempay.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.tangempay.closure.TangemPayCloseCardModel
import com.tangem.features.tangempay.limit.setup.TangemPayCardLimitSetupModel
import com.tangem.features.tangempay.model.*
import com.tangem.features.tangempay.tiers.current.TangemPayCurrentPlanModel
import com.tangem.features.tangempay.tiers.select.TangemPaySelectPlanModel
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
    @ClassKey(TangemPayAddToWalletModel::class)
    fun bindTangemPayAddToWalletModel(model: TangemPayAddToWalletModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayAddFundsModel::class)
    fun bindTangemPayAddFundsModel(model: TangemPayAddFundsModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayVirtualAccountDepositModel::class)
    fun bindTangemPayVirtualAccountDepositModel(model: TangemPayVirtualAccountDepositModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayViewPinModel::class)
    fun bindTangemPayViewPinModel(model: TangemPayViewPinModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayCardPageModel::class)
    fun bindTangemPayCardPageModel(model: TangemPayCardPageModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayEditDisplayNameModel::class)
    fun bindTangemPayEditDisplayNameModel(model: TangemPayEditDisplayNameModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayIssueAdditionalCardModel::class)
    fun bindTangemPayIssueAdditionalCardModel(model: TangemPayIssueAdditionalCardModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayReissueCardModel::class)
    fun bindTangemPayReissueCardModel(model: TangemPayReissueCardModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayCloseCardModel::class)
    fun bindTangemPayCloseCardModel(model: TangemPayCloseCardModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayCardLimitSetupModel::class)
    fun bindTangemPayCardLimitSetupModel(model: TangemPayCardLimitSetupModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayCurrentPlanModel::class)
    fun bindTangemPayCurrentPlanModel(model: TangemPayCurrentPlanModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPaySelectPlanModel::class)
    fun bindTangemPaySelectPlanModel(model: TangemPaySelectPlanModel): Model
}