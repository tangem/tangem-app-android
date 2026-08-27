package com.tangem.features.tangempay.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.tangempay.account.TangemPayDetailsModel
import com.tangem.features.tangempay.addfunds.TangemPayAddFundsModel
import com.tangem.features.tangempay.addfunds.va.bank.TangemPayVaBankingDetailsErrorModel
import com.tangem.features.tangempay.addfunds.va.deposit.TangemPayVirtualAccountDepositModel
import com.tangem.features.tangempay.card.closure.TangemPayCloseCardModel
import com.tangem.features.tangempay.card.details.TangemPayCardPageModel
import com.tangem.features.tangempay.card.gpay.TangemPayAddToWalletModel
import com.tangem.features.tangempay.card.issue.TangemPayIssueAdditionalCardModel
import com.tangem.features.tangempay.card.limit.setup.TangemPayCardLimitSetupModel
import com.tangem.features.tangempay.card.name.TangemPayEditDisplayNameModel
import com.tangem.features.tangempay.card.pin.TangemPayChangePinModel
import com.tangem.features.tangempay.card.pin.TangemPayViewPinModel
import com.tangem.features.tangempay.card.reissue.TangemPayReissueCardModel
import com.tangem.features.tangempay.cashback.impl.model.TangemPayCashbackModel
import com.tangem.features.tangempay.multichain.choosenetwork.PaymentChooseNetworkModel
import com.tangem.features.tangempay.multichain.othernetworks.PaymentOtherNetworksModel
import com.tangem.features.tangempay.multichain.receive.PaymentReceiveModel
import com.tangem.features.tangempay.orderCard.impl.model.TangemPayOrderCardDataModel
import com.tangem.features.tangempay.orderCard.impl.model.TangemPayOrderCardModel
import com.tangem.features.tangempay.orderCard.impl.model.TangemPayOrderCardTypeModel
import com.tangem.features.tangempay.tiers.current.TangemPayCurrentPlanModel
import com.tangem.features.tangempay.tiers.select.TangemPaySelectPlanModel
import com.tangem.features.tangempay.txhistory.TangemPayTxHistoryModel
import com.tangem.features.tangempay.txhistory.details.TangemPayTxHistoryDetailsModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
@Suppress("TooManyFunctions")
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
    @ClassKey(TangemPayVaBankingDetailsErrorModel::class)
    fun bindTangemPayVaBankingDetailsErrorModel(model: TangemPayVaBankingDetailsErrorModel): Model

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

    @Binds
    @IntoMap
    @ClassKey(TangemPayCashbackModel::class)
    fun bindTangemPayCashbackModel(model: TangemPayCashbackModel): Model

    @Binds
    @IntoMap
    @ClassKey(PaymentChooseNetworkModel::class)
    fun bindPaymentChooseNetworkModel(model: PaymentChooseNetworkModel): Model

    @Binds
    @IntoMap
    @ClassKey(PaymentOtherNetworksModel::class)
    fun bindPaymentOtherNetworksModel(model: PaymentOtherNetworksModel): Model

    @Binds
    @IntoMap
    @ClassKey(PaymentReceiveModel::class)
    fun bindPaymentReceiveModel(model: PaymentReceiveModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayOrderCardTypeModel::class)
    fun bindTangemPayOrderCardTypeModel(model: TangemPayOrderCardTypeModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayOrderCardModel::class)
    fun bindTangemPayOrderCardModel(model: TangemPayOrderCardModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayOrderCardDataModel::class)
    fun bindTangemPayOrderCardDataModel(model: TangemPayOrderCardDataModel): Model
}