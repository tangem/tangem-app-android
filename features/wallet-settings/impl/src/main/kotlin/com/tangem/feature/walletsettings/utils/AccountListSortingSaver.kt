package com.tangem.feature.walletsettings.utils

import com.tangem.domain.account.usecase.ApplyAccountListSortingUseCase
import com.tangem.domain.models.account.AccountId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * Saves the sorting order of accounts with a debounce to prevent frequent updates.
 *
 * @property applyAccountListSortingUseCase Use case to apply the account list sorting.
 * @param dispatchers Coroutine dispatcher provider for managing threading.
 *
[REDACTED_AUTHOR]
 */
@Singleton
@OptIn(FlowPreview::class)
internal class AccountListSortingSaver @Inject constructor(
    private val applyAccountListSortingUseCase: ApplyAccountListSortingUseCase,
    dispatchers: CoroutineDispatcherProvider,
) {

    /** Flow to hold the account IDs for sorting, with an initial null value. */
    val accountsOrderFlow: StateFlow<List<AccountId>?>
        private field = MutableStateFlow<List<AccountId>?>(value = null)

    private val coroutineScope = CoroutineScope(SupervisorJob() + dispatchers.io)

    init {
        accountsOrderFlow
            .filterNotNull()
            .debounce { 3.seconds }
            .onEach { accountIds ->
                applyAccountListSortingUseCase.invoke(accountIds).onLeft {
                    Timber.e("Error while saving account list sorting: $it")
                }
            }
            .launchIn(coroutineScope)
    }

    /**
     * Saves the provided list of account IDs to apply the sorting order.
     *
     * @param accountIds List of AccountId representing the desired order.
     */
    fun save(accountIds: List<AccountId>) {
        accountsOrderFlow.value = accountIds
    }
}