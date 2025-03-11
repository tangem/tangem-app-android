package com.tangem.domain.managetokens

import arrow.core.EitherNel
import arrow.core.nonEmptyListOf
import arrow.core.raise.*
import com.tangem.domain.managetokens.model.AddCustomTokenForm
import com.tangem.domain.managetokens.model.exceptoin.CustomTokenFormValidationException
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.tokens.model.Network

class ValidateTokenFormUseCase(
    private val repository: CustomTokensRepository,
) {

    suspend operator fun invoke(
        networkId: Network.ID,
        formValues: AddCustomTokenForm.Raw,
    ): EitherNel<CustomTokenFormValidationException, AddCustomTokenForm.Validated> = either {
        if (formValues.name.isBlank() &&
            formValues.symbol.isBlank() &&
            formValues.decimals.isBlank()
        ) {
            if (formValues.contractAddress.isBlank()) {
                AddCustomTokenForm.Validated.Empty
            } else {
                val contractAddress = withError({ nonEmptyListOf(it) }) {
                    ensureIsContractAddressValid(formValues.contractAddress, networkId)
                }

                AddCustomTokenForm.Validated.ContractAddressOnly(contractAddress)
            }
        } else {
            zipOrAccumulate(
                { ensureIsContractAddressValid(formValues.contractAddress, networkId) },
                { ensureIsDecimalsValid(formValues.decimals) },
                { ensure(formValues.name.isNotBlank()) { CustomTokenFormValidationException.EmptyName } },
                { ensure(formValues.symbol.isNotBlank()) { CustomTokenFormValidationException.EmptySymbol } },
            ) { contractAddress, decimals, _, _ ->
                AddCustomTokenForm.Validated.All(
                    contractAddress = contractAddress,
                    symbol = formValues.symbol,
                    name = formValues.name,
                    decimals = decimals,
                )
            }
        }
    }

    private suspend fun Raise<CustomTokenFormValidationException>.ensureIsContractAddressValid(
        contractAddress: String,
        networkId: Network.ID,
    ): String {
        val isValid = catch({ repository.validateContractAddress(contractAddress, networkId) }) {
            raise(CustomTokenFormValidationException.DataError(it))
        }

        ensure(isValid) {
            CustomTokenFormValidationException.ContractAddress.Invalid
        }

        return contractAddress
    }

    private fun Raise<CustomTokenFormValidationException>.ensureIsDecimalsValid(decimals: String): Int {
        ensure(condition = decimals.isNotBlank()) {
            CustomTokenFormValidationException.Decimals.Empty
        }

        val decimalsInt = ensureNotNull(decimals.toIntOrNull()) {
            CustomTokenFormValidationException.Decimals.Invalid
        }
        ensure(condition = decimalsInt in MIN_DECIMALS..MAX_DECIMALS) {
            CustomTokenFormValidationException.Decimals.Invalid
        }

        return decimalsInt
    }

    companion object {
        const val MIN_DECIMALS = 1
        const val MAX_DECIMALS = 30
    }
}