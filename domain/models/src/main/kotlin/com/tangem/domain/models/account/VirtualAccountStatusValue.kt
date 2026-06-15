package com.tangem.domain.models.account

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Represents the various states a virtual account (VA) can have, encapsulating different information based on
 * the state. Mirrors [PaymentAccountStatusValue] but carries VA-specific states (no card-related variants).
 *
 * @property source The source of the status information.
 */
@Serializable
sealed class VirtualAccountStatusValue {
    abstract val source: StatusSource

    /** The total fiat balance associated with this status. */
    val totalFiatBalance: TotalFiatBalance
        get() = when (this) {
            is Empty,
            is NotCreated,
            is UnderReview,
            is Provisioning,
            is CountryNotSupported,
            is Error,
            -> TotalFiatBalance.Loaded(amount = SerializedBigDecimal.ZERO, source = source)
            is Loading -> TotalFiatBalance.Loading
            is Active -> {
                val rate = fiatRate ?: return TotalFiatBalance.Failed
                TotalFiatBalance.Loaded(amount = fiatBalance.availableBalance.multiply(rate), source = source)
            }
        }

    /**
     * Copies the status with a new [source].
     *
     * @param source The new source of the status information.
     */
    fun copySealed(source: StatusSource): VirtualAccountStatusValue {
        return when (this) {
            is UnderReview -> copy(source = source)
            is Provisioning -> copy(source = source)
            is Active -> copy(source = source)
            is Loading,
            is Empty,
            is NotCreated,
            is CountryNotSupported,
            is Error,
            -> this
        }
    }

    /** Represents an empty virtual account status when no specific state is available. */
    @Serializable
    data object Empty : VirtualAccountStatusValue() {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    /** Represents the Loading state of a virtual account, typically while fetching its details. */
    @Serializable
    data object Loading : VirtualAccountStatusValue() {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    /** Represents a state where the virtual account has not been created yet. */
    @Serializable
    data object NotCreated : VirtualAccountStatusValue() {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    /**
     * Represents a state where the virtual account is under review (KYC).
     *
     * @property source The source of the status information.
     * @property kycStatus The current KYC status.
     * @property customerId The unique identifier of the customer.
     */
    @Serializable
    data class UnderReview(
        override val source: StatusSource,
        val kycStatus: KycStatus,
        val customerId: String,
    ) : VirtualAccountStatusValue()

    /**
     * Represents a state where the virtual account is being provisioned on the backend (e.g. via Rain),
     * after KYC approval and terms acceptance.
     *
     * @property source The source of the status information.
     */
    @Serializable
    data class Provisioning(override val source: StatusSource) : VirtualAccountStatusValue()

    /** Represents a state where the user's country is not eligible for a virtual account. */
    @Serializable
    data object CountryNotSupported : VirtualAccountStatusValue() {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    /**
     * Represents a state where the virtual account is successfully loaded with complete information.
     *
     * @property source The source of the status information.
     * @property customerId The unique identifier of the customer.
     * @property currencyCode The code of the currency.
     * @property depositAddress The on-chain address for deposits, if available.
     * @property fiatBalance The fiat balance details.
     * @property cryptoBalance The crypto balance details.
     * @property availableForWithdrawal The crypto amount currently available for withdrawal/swap.
     * @property cryptoCurrency The crypto currency held in the account (e.g. USDC).
     * @property fiatRate Exchange rate of [cryptoCurrency] to the app's selected fiat currency,
     *                    or `null` if the quote is not yet available. When `null`,
     *                    [totalFiatBalance] resolves to [TotalFiatBalance.Failed].
     */
    @Serializable
    data class Active(
        override val source: StatusSource,
        val customerId: String,
        val currencyCode: String,
        val depositAddress: String?,
        val fiatBalance: FiatBalance,
        val cryptoBalance: CryptoBalance,
        val availableForWithdrawal: SerializedBigDecimal,
        val cryptoCurrency: CryptoCurrency.Token,
        val fiatRate: SerializedBigDecimal?,
    ) : VirtualAccountStatusValue() {
        val cryptoCurrencyStatus: CryptoCurrencyStatus = CryptoCurrencyStatus(
            currency = cryptoCurrency,
            value = buildCryptoCurrencyStatusValue(
                amount = availableForWithdrawal,
                fiatAmount = fiatBalance.availableBalance,
                fiatRate = fiatRate,
                depositAddress = cryptoBalance.depositAddress,
            ),
        )
    }

    /** Represents an error state for the virtual account status. */
    @Serializable
    sealed class Error : VirtualAccountStatusValue() {
        /** Error state indicating the device is exposed. */
        @Serializable
        data object ExposedDevice : Error() {
            override val source: StatusSource = StatusSource.ACTUAL
        }

        /** Error state indicating the account is unavailable. */
        @Serializable
        data object Unavailable : Error() {
            override val source: StatusSource = StatusSource.ACTUAL
        }

        /** Error state indicating the account data is not synced. */
        @Serializable
        data object NotSynced : Error() {
            override val source: StatusSource = StatusSource.ACTUAL
        }
    }

    /**
     * Represents the fiat balance of the virtual account.
     *
     * @property availableBalance The amount of available balance in fiat.
     * @property currency The currency of the balance.
     */
    @Serializable
    data class FiatBalance(val availableBalance: SerializedBigDecimal, val currency: String)

    /**
     * Represents the crypto balance of the virtual account.
     *
     * @property id The unique identifier of the crypto asset.
     * @property chainId The identifier of the blockchain network.
     * @property depositAddress The on-chain address for deposits.
     * @property tokenContractAddress The contract address of the token.
     * @property balance The amount of the crypto balance.
     */
    @Serializable
    data class CryptoBalance(
        val id: String,
        val chainId: Long,
        val depositAddress: String,
        val tokenContractAddress: String,
        val balance: SerializedBigDecimal,
    )
}

private fun buildCryptoCurrencyStatusValue(
    amount: SerializedBigDecimal,
    fiatAmount: SerializedBigDecimal,
    fiatRate: SerializedBigDecimal?,
    depositAddress: String,
): CryptoCurrencyStatus.Value {
    val networkAddress = NetworkAddress.Single(
        defaultAddress = NetworkAddress.Address(
            type = NetworkAddress.Address.Type.Primary,
            value = depositAddress,
        ),
    )
    return if (fiatRate != null) {
        CryptoCurrencyStatus.Loaded(
            amount = amount,
            fiatAmount = fiatAmount,
            fiatRate = fiatRate,
            priceChange = BigDecimal.ZERO,
            networkAddress = networkAddress,
            sources = CryptoCurrencyStatus.Sources(),
            pendingTransactions = emptySet(),
            stakingBalance = null,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
        )
    } else {
        CryptoCurrencyStatus.NoQuote(
            amount = amount,
            networkAddress = networkAddress,
            stakingBalance = null,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            sources = CryptoCurrencyStatus.Sources(),
        )
    }
}