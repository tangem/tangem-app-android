package com.tangem.data.collectibles.account.converter

import com.tangem.data.collectibles.account.entity.CollectiblesAccountStatusValueDTO
import com.tangem.data.collectibles.account.entity.CollectiblesVaultBalanceDTO
import com.tangem.domain.collectibles.account.CollectiblesVaultCurrencyFactory
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.account.CollectiblesVaultBalance
import javax.inject.Inject

/**
 * Maps between [CollectiblesAccountStatusValue] and its storage DTO.
 *
 * [convert] returns `null` for every state that does not mean "there is an account" — that `null` is what makes
 * the store refuse the write, so a loading or failed read cannot be recorded as the account itself.
 *
 * [convertBack] restores the value as [StatusSource.CACHE]: it has not been re-read from the chain in this
 * session, whatever it was when it was written. [CollectiblesAccountStatusValue.MissedDerivation] is the exception —
 * it says the owner key is missing on this device, which no chain read is needed to know.
 */
internal class CollectiblesAccountStatusValueDTOConverter @Inject constructor(
    private val currencyFactory: CollectiblesVaultCurrencyFactory,
) {

    fun convert(value: CollectiblesAccountStatusValue): CollectiblesAccountStatusValueDTO? = when (value) {
        is CollectiblesAccountStatusValue.Onboarded -> CollectiblesAccountStatusValueDTO.Onboarded(
            balance = value.vaultBalance?.toDTO(),
        )
        is CollectiblesAccountStatusValue.UpdateRequired -> CollectiblesAccountStatusValueDTO.UpdateRequired(
            balance = value.vaultBalance?.toDTO(),
        )
        is CollectiblesAccountStatusValue.MissedDerivation -> CollectiblesAccountStatusValueDTO.MissedDerivation
        is CollectiblesAccountStatusValue.Loading,
        is CollectiblesAccountStatusValue.NotOnboarded,
        is CollectiblesAccountStatusValue.Error,
        -> null
    }

    fun convertBack(value: CollectiblesAccountStatusValueDTO): CollectiblesAccountStatusValue = when (value) {
        is CollectiblesAccountStatusValueDTO.Onboarded -> CollectiblesAccountStatusValue.Onboarded(
            source = StatusSource.CACHE,
            vaultBalance = value.balance?.toDomain(),
        )
        is CollectiblesAccountStatusValueDTO.UpdateRequired -> CollectiblesAccountStatusValue.UpdateRequired(
            source = StatusSource.CACHE,
            vaultBalance = value.balance?.toDomain(),
        )
        is CollectiblesAccountStatusValueDTO.MissedDerivation -> CollectiblesAccountStatusValue.MissedDerivation
    }

    private fun CollectiblesVaultBalance.toDTO() = CollectiblesVaultBalanceDTO(
        usdcAmount = usdcAmount,
        usdcMint = usdcToken.contractAddress,
    )

    private fun CollectiblesVaultBalanceDTO.toDomain() = CollectiblesVaultBalance(
        usdcToken = currencyFactory.create(mint = usdcMint),
        usdcAmount = usdcAmount,
    )
}