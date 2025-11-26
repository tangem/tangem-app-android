package com.tangem.data.account.tokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.toNonEmptyListOrNull
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.data.account.store.AccountsResponseStoreFactory
import com.tangem.data.account.utils.assignTokens
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.datasource.api.tangemTech.models.account.toUserTokensResponse
import com.tangem.datasource.local.accounts.AccountTokenMigrationStore
import com.tangem.datasource.utils.getSyncOrNull
import com.tangem.domain.account.tokens.MainAccountTokensMigration
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.lib.crypto.derivation.AccountNodeRecognizer
import timber.log.Timber

/**
 * Implementation of [MainAccountTokensMigration] for migrating tokens associated with a main account.
 * The migration process involves transferring unassigned tokens from the main account to a selected account.
 *
 * @property accountsResponseStoreFactory Factory for creating stores to access cached account responses.
 * @property userTokensSaver Saver for updating user tokens in persistent storage.
 *
[REDACTED_AUTHOR]
 */
internal class DefaultMainAccountTokensMigration(
    private val accountsResponseStoreFactory: AccountsResponseStoreFactory,
    private val accountTokenMigrationStore: AccountTokenMigrationStore,
    private val userTokensSaver: UserTokensSaver,
) : MainAccountTokensMigration {

    override suspend fun migrate(
        userWalletId: UserWalletId,
        derivationIndex: DerivationIndex,
    ): Either<Throwable, Unit> = either {
        if (derivationIndex == DerivationIndex.Main) {
            Timber.i("Migration skipped: derivation index is Main")
            return@either
        }

        val store = accountsResponseStoreFactory.create(userWalletId)

        val response = store.getSyncOrNull()

        ensureNotNull(response) {
            val exception = IllegalStateException("No cached accounts response found")
            Timber.e(exception)
            exception
        }

        val mainAccount = findAccount(response = response, derivationIndex = DerivationIndex.Main)
        val selectedAccount = findAccount(response = response, derivationIndex = derivationIndex)

        val unassignedTokens = mainAccount.findUnassignedTokens(derivationIndex)

        if (unassignedTokens == null) {
            Timber.i("No unassigned tokens found for migration")
            return@either
        }

        val updatedResponse = response.copy(
            accounts = response.accounts.map { account ->
                when (account.id) {
                    mainAccount.id -> {
                        account.copy(tokens = account.tokens.orEmpty() - unassignedTokens)
                    }
                    selectedAccount.id -> {
                        selectedAccount.assignTokens(userWalletId, unassignedTokens)
                    }
                    else -> account
                }
            },
        )

        store.updateData { updatedResponse }

        val mainAccountName = mainAccount.name
        val selectedAccountName = selectedAccount.name
        if (mainAccountName != null && selectedAccountName != null) {
            accountTokenMigrationStore.store(userWalletId, mainAccountName to selectedAccountName)
        }

        val userTokensResponse = updatedResponse.toUserTokensResponse()
        userTokensSaver.pushWithRetryer(
            userWalletId = userWalletId,
            response = userTokensResponse,
            onFailSend = {
                val exception = IllegalStateException("Failed to push updated tokens after migration")
                Timber.e(exception)
                raise(exception)
            },
        )
    }

    private fun Raise<Throwable>.findAccount(
        response: GetWalletAccountsResponse,
        derivationIndex: DerivationIndex,
    ): WalletAccountDTO {
        val account = response.accounts.firstOrNull { it.derivationIndex == derivationIndex.value }

        return ensureNotNull(account) {
            val exception = IllegalStateException("No account found with derivation index: $derivationIndex")
            Timber.e(exception)
            exception
        }
    }

    private fun WalletAccountDTO.findUnassignedTokens(
        derivationIndex: DerivationIndex,
    ): List<UserTokensResponse.Token>? {
        val tokens = this.tokens

        if (tokens.isNullOrEmpty()) return tokens

        return tokens
            .filterByDerivationIndex(derivationIndex)
            .map { it.copy(accountId = null) }
            .toNonEmptyListOrNull()
    }

    private fun List<UserTokensResponse.Token>.filterByDerivationIndex(
        derivationIndex: DerivationIndex,
    ): List<UserTokensResponse.Token> {
        return filter { savedToken ->
            val blockchain = Blockchain.fromNetworkId(networkId = savedToken.networkId)
            if (blockchain == null) {
                Timber.e("Token has unknown networkId: $savedToken")
                return@filter false
            }

            val derivationPathValue = savedToken.derivationPath
            if (derivationPathValue == null) {
                Timber.e("Token has no derivation path: $savedToken")
                return@filter false
            }

            val accountNodeRecognizer = AccountNodeRecognizer(blockchain)
            val accountNodeValue = accountNodeRecognizer.recognize(derivationPathValue)

            if (accountNodeValue == null) {
                Timber.e("Token has unrecognized derivation path: $savedToken")
                return@filter false
            }

            accountNodeValue == derivationIndex.value.toLong()
        }
    }
}