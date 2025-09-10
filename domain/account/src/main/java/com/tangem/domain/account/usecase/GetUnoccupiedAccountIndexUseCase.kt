package com.tangem.domain.account.usecase

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Use case for retrieving the next unoccupied account index
 *
 * @property crudRepository repository for performing CRUD operations on accounts
 *
[REDACTED_AUTHOR]
 */
class GetUnoccupiedAccountIndexUseCase(
    private val crudRepository: AccountsCRUDRepository,
) {

    /**
     * Invokes the use case to calculate the next unoccupied account index
     *
     * @param userWalletId the unique identifier of the user wallet
     */
    suspend operator fun invoke(userWalletId: UserWalletId): Either<Error, DerivationIndex> = either {
        val totalAccountsCount = getTotalAccountsCount(userWalletId = userWalletId)

        DerivationIndex(totalAccountsCount + 1).getOrElse {
            raise(Error.InvalidDerivationIndex(it))
        }
    }

    private suspend fun Raise<Error>.getTotalAccountsCount(userWalletId: UserWalletId): Int {
        return catch(
            block = { crudRepository.getTotalAccountsCount(userWalletId = userWalletId) },
            catch = { raise(Error.DataOperationFailed(cause = it)) },
        )
            .getOrElse { raise(Error.DataNotFound) }
    }

    /**
     * Represents possible errors that can occur in the use case
     */
    sealed interface Error {

        val tag: String
            get() = this::class.simpleName ?: "GetUnoccupiedAccountIndexUseCase.Error"

        data object DataNotFound : Error {
            override fun toString(): String = "$tag: Data not found"
        }

        /** Error indicating that the derivation index is invalid */
        data class InvalidDerivationIndex(val cause: DerivationIndex.Error) : Error {
            override fun toString(): String = "$tag: Invalid derivation index: $cause"
        }

        /** Error indicating that a data operation failed */
        data class DataOperationFailed(val cause: Throwable) : Error {
            override fun toString(): String = "$tag: Data operation failed: ${cause.message ?: "Unknown error"}"
        }
    }
}