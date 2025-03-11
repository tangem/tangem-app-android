package com.tangem.domain.walletmanager.utils

import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.walletmanager.model.SmartContractMethod
import com.tangem.utils.converter.Converter
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem as SdkTransactionHistoryItem

internal class SdkTransactionHistoryItemConverter(
    smartContractMethods: Map<String, SmartContractMethod>,
) : Converter<SdkTransactionHistoryItem, TxHistoryItem> {

    private val typeConverter by lazy { SdkTransactionTypeConverter(smartContractMethods) }

    override fun convert(value: SdkTransactionHistoryItem): TxHistoryItem = TxHistoryItem(
        txHash = value.txHash,
        timestampInMillis = value.timestamp,
        isOutgoing = value.isOutgoing,
        destinationType = value.destinationType.toDomain(),
        sourceType = value.sourceType.toDomain(),
        interactionAddressType = value.extractInteractionAddressType(),
        status = when (value.status) {
            SdkTransactionHistoryItem.TransactionStatus.Confirmed -> TxHistoryItem.TransactionStatus.Confirmed
            SdkTransactionHistoryItem.TransactionStatus.Failed -> TxHistoryItem.TransactionStatus.Failed
            SdkTransactionHistoryItem.TransactionStatus.Unconfirmed -> TxHistoryItem.TransactionStatus.Unconfirmed
        },
        type = typeConverter.convert(value.type),
        amount = requireNotNull(value.amount.value) { "Transaction amount value must not be null" },
    )

    private fun SdkTransactionHistoryItem.SourceType.toDomain(): TxHistoryItem.SourceType = when (this) {
        is TransactionHistoryItem.SourceType.Single -> TxHistoryItem.SourceType.Single(address)
        is TransactionHistoryItem.SourceType.Multiple -> TxHistoryItem.SourceType.Multiple(addresses)
    }

    private fun SdkTransactionHistoryItem.DestinationType.toDomain(): TxHistoryItem.DestinationType = when (this) {
        is SdkTransactionHistoryItem.DestinationType.Single -> TxHistoryItem.DestinationType.Single(
            addressType.toDomain(),
        )
        is SdkTransactionHistoryItem.DestinationType.Multiple -> TxHistoryItem.DestinationType.Multiple(
            addressTypes.map { it.toDomain() },
        )
    }

    private fun SdkTransactionHistoryItem.AddressType.toDomain(): TxHistoryItem.AddressType = when (this) {
        is SdkTransactionHistoryItem.AddressType.Contract -> TxHistoryItem.AddressType.Contract(address)
        is SdkTransactionHistoryItem.AddressType.User -> TxHistoryItem.AddressType.User(address)
        is SdkTransactionHistoryItem.AddressType.Validator -> TxHistoryItem.AddressType.Validator(address)
    }

    private fun SdkTransactionHistoryItem.extractInteractionAddressType(): TxHistoryItem.InteractionAddressType? {
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
                TxHistoryItem.InteractionAddressType.Validator(address = transactionType.validatorAddress)
            }
            else -> null
        }
    }

    private fun mapToInteractionAddressType(
        destinationType: SdkTransactionHistoryItem.DestinationType,
    ): TxHistoryItem.InteractionAddressType {
        return when (destinationType) {
            is TransactionHistoryItem.DestinationType.Multiple -> TxHistoryItem.InteractionAddressType.Multiple(
                destinationType.addressTypes.map { it.address },
            )
            is TransactionHistoryItem.DestinationType.Single -> when (destinationType.addressType) {
                is TransactionHistoryItem.AddressType.Contract -> TxHistoryItem.InteractionAddressType.Contract(
                    destinationType.addressType.address,
                )
                is TransactionHistoryItem.AddressType.User -> TxHistoryItem.InteractionAddressType.User(
                    destinationType.addressType.address,
                )
                is TransactionHistoryItem.AddressType.Validator -> TxHistoryItem.InteractionAddressType.Validator(
                    destinationType.addressType.address,
                )
            }
        }
    }

    private fun mapToInteractionAddressType(
        sourceType: SdkTransactionHistoryItem.SourceType,
    ): TxHistoryItem.InteractionAddressType {
        return when (sourceType) {
            is TransactionHistoryItem.SourceType.Multiple -> TxHistoryItem.InteractionAddressType.Multiple(
                sourceType.addresses,
            )
            is TransactionHistoryItem.SourceType.Single -> {
                TxHistoryItem.InteractionAddressType.User(sourceType.address)
            }
        }
    }
}