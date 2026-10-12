package com.tangem.data.walletmanager.utils

import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.domain.txhistory.TxHistoryFeatureToggles
import com.tangem.domain.txhistory.usecase.GetCachedTokenInfoUseCase
import com.tangem.domain.walletmanager.model.SmartContractMethod
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isNegative
import com.tangem.utils.extensions.isZero
import java.math.BigDecimal
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem as SdkTransactionHistoryItem

@Suppress("LongParameterList")
internal class SdkTransactionHistoryItemConverter(
    smartContractMethods: Map<String, SmartContractMethod>,
    yieldSupplyAddresses: Set<String>,
    gaslessFeeAddresses: Set<String>,
    currency: CryptoCurrency,
    networkTokens: Set<CryptoCurrency.Token>,
    getCachedTokenInfoUseCase: GetCachedTokenInfoUseCase,
    currencyAddresses: Collection<Address>,
    private val txHistoryFeatureToggles: TxHistoryFeatureToggles,
) : Converter<SdkTransactionHistoryItem, TxInfo> {

    private val typeConverter by lazy {
        SdkTransactionTypeConverter(
            smartContractMethods = smartContractMethods,
            yieldSupplyAddresses = yieldSupplyAddresses,
            gaslessFeeAddresses = gaslessFeeAddresses,
            currency = currency,
            networkTokens = networkTokens,
            getCachedTokenInfoUseCase = getCachedTokenInfoUseCase,
        )
    }

    private val ownAddresses: Set<String> = currencyAddresses
        .mapTo(mutableSetOf()) { it.value.lowercase() }

    override fun convertList(input: Collection<SdkTransactionHistoryItem>): List<TxInfo> {
        if (!txHistoryFeatureToggles.isNewTxHistoryEnabled) return input.map(::convert)

        return input.groupBy(SdkTransactionHistoryItem::txHash)
            .flatMap { (_, items) ->
                val transfers = items.map(::convert)

                transfers.singleOrNull()?.let { listOf(it) } ?: transfers.toMultiTransfer()
            }
    }

    private fun List<TxInfo.Single>.toMultiTransfer(): List<TxInfo> {
        val yieldExits = filter { it.type is TransactionType.YieldSupply.Exit }
        val yieldSends = filter { it.type is TransactionType.YieldSupply.Send }
        val merged = filterNot { it.isYieldExitOrSend() }

        return buildList {
            addAll(yieldExits)
            addAll(merged.mergeIfSeveral())
            addAll(yieldSends)
        }
    }

    private fun TxInfo.Single.isYieldExitOrSend(): Boolean =
        type is TransactionType.YieldSupply.Exit || type is TransactionType.YieldSupply.Send

    private fun List<TxInfo.Single>.mergeIfSeveral(): List<TxInfo> = when (size) {
        0, 1 -> this
        else -> listOf(mergeTransfers())
    }

    private fun List<TxInfo.Single>.mergeTransfers(): TxInfo.MultiTransfer {
        val withoutGaslessFee = filterNot { it.type is TransactionType.GaslessFee }
        val delta = withoutGaslessFee.fold(BigDecimal.ZERO) { sum, transfer -> sum + transfer.signedAmount() }

        return TxInfo.MultiTransfer(
            amount = delta.abs(),
            isOutgoing = delta.isNegative(),
            type = if (delta.isZero()) TransactionType.UnknownOperation else TransactionType.Transfer,
            destinationType = withoutGaslessFee.mergeDestinationType(),
            sourceType = withoutGaslessFee.mergeSourceType(),
            interactionAddressType = withoutGaslessFee.mergeInteractionAddressType(),
            transfers = this,
        )
    }

    private fun List<TxInfo.Single>.mergeSourceType(): TxInfo.SourceType {
        val addresses = flatMapTo(mutableSetOf()) { it.sourceType.addresses() }

        return addresses.singleOrNull()
            ?.let(TxInfo.SourceType::Single)
            ?: TxInfo.SourceType.Multiple(addresses.toList())
    }

    private fun List<TxInfo.Single>.mergeDestinationType(): TxInfo.DestinationType {
        val addressTypes = flatMapTo(mutableSetOf()) { it.destinationType.addressTypes() }

        return addressTypes.singleOrNull()
            ?.let(TxInfo.DestinationType::Single)
            ?: TxInfo.DestinationType.Multiple(addressTypes.toList())
    }

    private fun List<TxInfo.Single>.mergeInteractionAddressType(): TxInfo.InteractionAddressType? {
        val interactions = mapNotNullTo(mutableSetOf(), TxInfo.Single::interactionAddressType)

        return interactions.singleOrNull()
            ?: interactions.flatMapTo(mutableSetOf()) { it.addresses() }
                .takeIf(Set<String>::isNotEmpty)
                ?.let { TxInfo.InteractionAddressType.Multiple(it.toList()) }
    }

    private fun TxInfo.InteractionAddressType.addresses(): List<String> = when (this) {
        is TxInfo.InteractionAddressType.Contract -> listOf(address)
        is TxInfo.InteractionAddressType.User -> listOf(address)
        is TxInfo.InteractionAddressType.Validator -> listOf(address)
        is TxInfo.InteractionAddressType.Multiple -> addresses
    }

    private fun TxInfo.Single.signedAmount(): BigDecimal {
        val isFromOwn = sourceType.addresses().any { it.lowercase() in ownAddresses }
        val isToOwn = destinationType.addressTypes().any { it.address.lowercase() in ownAddresses }

        return when {
            isFromOwn == isToOwn -> BigDecimal.ZERO
            isFromOwn -> amount.negate()
            else -> amount
        }
    }

    private fun TxInfo.SourceType.addresses(): List<String> = when (this) {
        is TxInfo.SourceType.Single -> listOf(address)
        is TxInfo.SourceType.Multiple -> addresses
    }

    private fun TxInfo.DestinationType.addressTypes(): List<TxInfo.AddressType> = when (this) {
        is TxInfo.DestinationType.Single -> listOf(addressType)
        is TxInfo.DestinationType.Multiple -> addressTypes
    }

    override fun convert(value: SdkTransactionHistoryItem): TxInfo.Single = TxInfo.Single(
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
        type = typeConverter.convert(value),
        amount = requireNotNull(value.amount.value) { "Transaction amount value must not be null" },
        fee = value.fee.toDomain(),
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

    /**
     * The counterparty follows the direction for a contract call too: a token that arrives inside someone else's
     * contract call (a DEX payout, a withdrawal sent straight to the wallet) has the wallet itself as destination,
     * and naming it would show the transfer as coming from the user's own account.
     */
    private fun SdkTransactionHistoryItem.extractInteractionAddressType(): TxInfo.InteractionAddressType? {
        return when (val transactionType = type) {
            SdkTransactionHistoryItem.TransactionType.Transfer,
            is SdkTransactionHistoryItem.TransactionType.ContractMethod,
            is SdkTransactionHistoryItem.TransactionType.ContractMethodName,
            -> if (isOutgoing) {
                mapToInteractionAddressType(destinationType = destinationType)
            } else {
                mapToInteractionAddressType(sourceType = sourceType)
            }

            is SdkTransactionHistoryItem.TransactionType.SolanaStakingTransactionType.Stake -> {
                transactionType.validatorAddress?.let {
                    TxInfo.InteractionAddressType.Validator(address = it)
                }
            }
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