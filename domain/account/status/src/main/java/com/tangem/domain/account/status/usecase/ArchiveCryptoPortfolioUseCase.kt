package com.tangem.domain.account.status.usecase

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.*
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.referral.ReferralRepository

/**
 * Use case for archiving a crypto portfolio.
 * This class provides functionality to archive a specific account within a user's crypto portfolio.
 * It ensures that the account exists and meets the necessary requirements before performing the operation.
 *
 * @property singleAccountStatusListSupplier supplier to get the list of account statuses
 * @property crudRepository repository for performing CRUD operations on accounts
 * @property referralRepository repository for handling referral status checks
 *
[REDACTED_AUTHOR]
 */
class ArchiveCryptoPortfolioUseCase(
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val crudRepository: AccountsCRUDRepository,
    private val referralRepository: ReferralRepository,
) {

    /** Archives the specified account by its [accountId] */
    suspend operator fun invoke(accountId: AccountId): Either<Error, Unit> = either {
        val accountStatusList = getAccountStatusList(userWalletId = accountId.userWalletId)
        val accountList = accountStatusList.toAccountList().getOrElse {
            raise(Error.CriticalTechError.AccountListRequirementsNotMet(cause = it))
        }

        ensure(accountList.accounts.any { it.accountId == accountId }) {
            Error.CriticalTechError.AccountNotFound(accountId = accountId)
        }

        checkReferralStatus(accountStatusList = accountStatusList, accountId = accountId)

        val archivingAccount = accountList.accounts
            .firstOrNull { it.accountId == accountId }
            ?: raise(Error.CriticalTechError.AccountNotFound(accountId = accountId))

        val updatedAccounts = (accountList - archivingAccount).getOrElse {
            raise(Error.CriticalTechError.AccountListRequirementsNotMet(cause = it))
        }

        saveAccounts(updatedAccounts)
    }

    private suspend fun Raise<Error>.checkReferralStatus(accountStatusList: AccountStatusList, accountId: AccountId) {
        val referralStatus = catch(
            block = { referralRepository.getReferralStatus(userWalletId = accountStatusList.userWalletId.stringValue) },
            catch = { raise(Error.DataOperationFailed(cause = it)) },
        )

        val referralToken = referralStatus.token
        val address = referralStatus.address

        if (!referralStatus.isActive || referralToken == null || address == null) return

        val account = accountStatusList.accountStatuses
            .asSequence()
            .filterIsInstance<AccountStatus.CryptoPortfolio>()
            .firstOrNull { it.accountId == accountId }

        ensureNotNull(account) {
            Error.CriticalTechError.AccountNotFound(accountId = accountId)
        }

        val statuses = account.flattenCurrencies()

        val hasNotReferralToken = statuses.none { status ->
            val currency = status.currency

            currency.network.backendId == referralToken.networkId &&
                (currency as? CryptoCurrency.Token)?.contractAddress == referralToken.contractAddress &&
                status.value.networkAddress?.availableAddresses?.any { it.value == address } == true
        }

        ensure(hasNotReferralToken) { Error.ActiveReferralStatus }
    }

    private suspend fun Raise<Error>.getAccountStatusList(userWalletId: UserWalletId): AccountStatusList {
        return singleAccountStatusListSupplier.getSyncOrNull(
            SingleAccountStatusListProducer.Params(userWalletId = userWalletId),
        )
            ?: raise(Error.CriticalTechError.AccountsNotCreated(userWalletId = userWalletId))
    }

    private suspend fun Raise<Error>.saveAccounts(accountList: AccountList) {
        arrow.core.raise.catch(
            block = { crudRepository.saveAccounts(accountList) },
            catch = { raise(Error.DataOperationFailed(cause = it)) },
        )
    }

    /**
     * Represents possible errors that can occur during the archiving process
     */
    sealed interface Error {

        val tag: String
            get() = this.javaClass.simpleName

        data object ActiveReferralStatus : Error

        /** Error indicating that a data operation failed */
        data class DataOperationFailed(val cause: Throwable) : Error {
            override fun toString(): String = "$tag: Data operation failed: ${cause.message ?: "Unknown error"}"
        }

        /**
         * Represents critical technical errors that can occur during the update operation.
         * These errors are a consequence of an inconsistent state.
         */
        sealed interface CriticalTechError : Error {

            /**

             *
             * @property userWalletId the unique identifier of the user wallet
             */
            data class AccountsNotCreated(val userWalletId: UserWalletId) : CriticalTechError {

                override fun toString(): String {
                    return "${this.javaClass.simpleName}: Accounts for $userWalletId are not created"
                }
            }

            /** Error indicating that the account with [accountId] was not found */
            data class AccountNotFound(val accountId: AccountId) : CriticalTechError {
                override fun toString(): String = "${this.javaClass.simpleName}: Account with ID $accountId not found"
            }

            /**
             * Error indicating that the account list requirements were not met.
             *
             * @property cause the underlying cause of the error
             */
            data class AccountListRequirementsNotMet(val cause: AccountList.Error) : CriticalTechError {

                override fun toString(): String {
                    return "${this.javaClass.simpleName}: Account list requirements not met: $cause"
                }
            }
        }
    }
}