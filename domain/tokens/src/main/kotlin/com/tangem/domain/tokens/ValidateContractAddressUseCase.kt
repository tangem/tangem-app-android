package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.AddCustomTokenError
import com.tangem.domain.tokens.repository.TokensListRepository

class ValidateContractAddressUseCase(private val tokensListRepository: TokensListRepository) {
    operator fun invoke(address: String, networkId: String): Either<AddCustomTokenError, Unit> {
        return either {
            catch(
                block = {
                    if (address.isEmpty()) raise(AddCustomTokenError.FieldIsEmpty)

                    if (!tokensListRepository.validateAddress(networkId, address)) {
                        raise(AddCustomTokenError.InvalidContractAddress)
                    }
                },
                catch = {
                    AddCustomTokenError.InvalidContractAddress
                },
            )
        }
    }
}