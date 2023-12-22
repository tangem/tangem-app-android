package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.tokens.model.FoundToken
import com.tangem.domain.tokens.repository.TokensListRepository

class FindTokenByContractAddressUseCase(private val repository: TokensListRepository) {
    suspend operator fun invoke(contractAddress: String, networkId: String): Either<Throwable, FoundToken?> {
        return either {
            catch(
                block = { repository.findToken(contractAddress, networkId) },
                catch = { throwable -> raise(throwable) },
            )
        }
    }
}