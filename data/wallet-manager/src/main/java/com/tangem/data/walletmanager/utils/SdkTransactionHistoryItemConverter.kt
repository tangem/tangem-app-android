package com.tangem.data.walletmanager.utils

import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.walletmanager.model.SmartContractMethod
import com.tangem.utils.converter.Converter
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem as SdkTransactionHistoryItem

internal class SdkTransactionHistoryItemConverter(
    smartContractMethods: Map<String, SmartContractMethod>,
) : Converter<SdkTransactionHistoryItem, TxInfo> {

    private val typeConverter by lazy { SdkTransactionTypeConverter(smartContractMethods) }

    override fun convert(value: SdkTransactionHistoryItem): TxInfo = TxInfo(
        txHash = value.txHash,
        timestampInMillis = value.timestamp,
        isOutgoing = value.isOutgoing,
        destinationType = value.destinationType.toDomain(),
        sourceType = value.sourceType.toDomain(),
        interactionAddressType = value.extractInteractionAddressType(),
        status = when (value.status) {
            SdkTransactionHistoryItem.TransactionStatus.Confirmed -> TxInfo.TransactionStatus.Confirmed
            SdkTransactionHistoryItem.TransactionStatus.Failed -> TxInfo.TransactionStatus.Failed
            SdkTransactionHistoryItem.TransactionStatus.Unconfirmed -> TxInfo.TransactionStatus.Unconfirmed
        },
        type = typeConverter.convert(value.type),
        amount = requireNotNull(value.amount.value) { "Transaction amount value must not be null" },
    )

    private fun SdkTransactionHistoryItem.SourceType.toDomain(): TxInfo.SourceType = when (this) {
        is TransactionHistoryItem.SourceType.Single -> TxInfo.SourceType.Single(address)
        is TransactionHistoryItem.SourceType.Multiple -> TxInfo.SourceType.Multiple(addresses)
    }

    private fun SdkTransactionHistoryItem.DestinationType.toDomain(): TxInfo.DestinationType = when (this) {
        is SdkTransactionHistoryItem.DestinationType.Single -> TxInfo.DestinationType.Single(
            addressType.toDomain(),
        )
        is SdkTransactionHistoryItem.DestinationType.Multiple -> TxInfo.DestinationType.Multiple(
            addressTypes.map { it.toDomain() },
        )
    }

    private fun SdkTransactionHistoryItem.AddressType.toDomain(): TxInfo.AddressType = when (this) {
        is SdkTransactionHistoryItem.AddressType.Contract -> TxInfo.AddressType.Contract(address)
        is SdkTransactionHistoryItem.AddressType.User -> TxInfo.AddressType.User(address)
        is SdkTransactionHistoryItem.AddressType.Validator -> TxInfo.AddressType.Validator(address)
    }

    private fun SdkTransactionHistoryItem.extractInteractionAddressType(): TxInfo.InteractionAddressType? {
        return when (val transactionType = type) {
            SdkTransactionHistoryItem.TransactionType.Transfer -> if (isOutgoing) {
                mapToInteractionAddressType(destinationType = destinationType)
            } else {
                mapToInteractionAddressType(sourceType = sourceType)
            }

            is SdkTransactionHistoryItem.TransactionType.ContractMethod,
            is SdkTransactionHistoryItem.TransactionType.ContractMethodName,
            -> mapToInteractionAddressType(destinationType = destinationType)

            is SdkTransactionHistoryItem.TransactionType.TronStakingTransactionType.VoteWitnessContract -> {
                TxInfo.InteractionAddressType.Validator(address = transactionType.validatorAddress)
            }
            else -> null
        }
    }

    private fun mapToInteractionAddressType(
        destinationType: SdkTransactionHistoryItem.DestinationType,
    ): TxInfo.InteractionAddressType {
        return when (destinationType) {
            is TransactionHistoryItem.DestinationType.Multiple -> TxInfo.InteractionAddressType.Multiple(
                destinationType.addressTypes.map { it.address },
            )
            is TransactionHistoryItem.DestinationType.Single -> when (destinationType.addressType) {
                is TransactionHistoryItem.AddressType.Contract -> TxInfo.InteractionAddressType.Contract(
                    destinationType.addressType.address,
                )
                is TransactionHistoryItem.AddressType.User -> TxInfo.InteractionAddressType.User(
                    destinationType.addressType.address,
                )
                is TransactionHistoryItem.AddressType.Validator -> TxInfo.InteractionAddressType.Validator(
                    destinationType.addressType.address,
                )
            }
        }
    }

    private fun mapToInteractionAddressType(
        sourceType: SdkTransactionHistoryItem.SourceType,
    ): TxInfo.InteractionAddressType {
        return when (sourceType) {
            is TransactionHistoryItem.SourceType.Multiple -> TxInfo.InteractionAddressType.Multiple(
                sourceType.addresses,
            )
            is TransactionHistoryItem.SourceType.Single -> {
                TxInfo.InteractionAddressType.User(sourceType.address)
            }
        }
    }
}