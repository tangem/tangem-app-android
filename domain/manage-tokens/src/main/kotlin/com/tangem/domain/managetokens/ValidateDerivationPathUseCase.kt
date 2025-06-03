package com.tangem.domain.managetokens

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.managetokens.model.exceptoin.DerivationPathValidationException
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.models.network.Network

class ValidateDerivationPathUseCase(
    private val repository: CustomTokensRepository,
) {

    operator fun invoke(rawValue: String): Either<DerivationPathValidationException, Network.DerivationPath> {
        return either {
            ensure(rawValue.isNotEmpty()) { DerivationPathValidationException.Empty }

            catch({ repository.createDerivationPath(rawValue) }) {
                raise(DerivationPathValidationException.Invalid)
            }
        }
    }
}