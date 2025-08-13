package com.tangem.features.send.v2.feeselector.model.transformers

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.v2.api.entity.*
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.send.v2.feeselector.model.FeeSelectorIntents
import com.tangem.lib.crypto.BlockchainUtils.isTron
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList

@Suppress("LongParameterList")
internal class FeeSelectorLoadedTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
    private val appCurrency: AppCurrency,
    private val fees: TransactionFee,
    private val feeStateConfiguration: FeeSelectorParams.FeeStateConfiguration,
    private val isFeeApproximate: Boolean,
    private val feeSelectorIntents: FeeSelectorIntents,
) : Transformer<FeeSelectorUM> {

    private val feeItemsConverter = FeeItemConverter(
        feeStateConfiguration = feeStateConfiguration,
        normalFee = fees.normal,
        feeSelectorIntents = feeSelectorIntents,
        appCurrency = appCurrency,
        cryptoCurrencyStatus = feeCryptoCurrencyStatus,
    )

    override fun transform(prevState: FeeSelectorUM): FeeSelectorUM {
        val prevCustomFee = if (prevState is FeeSelectorUM.Content) {
            prevState.feeItems.find { it is FeeItem.Custom } as? FeeItem.Custom
        } else {
            null
        }
        val feeItems: ImmutableList<FeeItem> = feeItemsConverter.convert(FeeItemConverter.Input(fees, prevCustomFee))

        val selectedFee = when (prevState) {
            is FeeSelectorUM.Content -> feeItems.first { it.isSameClass(prevState.selectedFeeItem) }
            is FeeSelectorUM.Error,
            FeeSelectorUM.Loading,
            -> feeItems.find { it is FeeItem.Suggested } ?: feeItems.first { it is FeeItem.Market }
        }

        val nonce = ((prevState as? FeeSelectorUM.Content)?.feeNonce as? FeeNonce.Nonce)?.nonce

        return FeeSelectorUM.Content(
            isPrimaryButtonEnabled = true,
            fees = fees,
            feeItems = feeItems,
            selectedFeeItem = selectedFee,
            feeExtraInfo = FeeExtraInfo(
                isFeeApproximate = isFeeApproximate,
                isFeeConvertibleToFiat = feeCryptoCurrencyStatus.currency.network.hasFiatFeeRate,
                isTronToken = cryptoCurrencyStatus.currency is CryptoCurrency.Token &&
                    isTron(cryptoCurrencyStatus.currency.network.rawId),
            ),
            feeFiatRateUM = feeCryptoCurrencyStatus.value.fiatRate?.let { rate ->
                FeeFiatRateUM(
                    rate = rate,
                    appCurrency = appCurrency,
                )
            },
            feeNonce = if (fees.normal is Fee.Ethereum) {
                FeeNonce.Nonce(
                    nonce = nonce,
                    onNonceChange = feeSelectorIntents::onNonceChange,
                )
            } else {
                FeeNonce.None
            },
        )
    }
}