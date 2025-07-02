package com.tangem.features.managetokens.utils

import arrow.core.getOrElse
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.managetokens.CheckIsCurrencyNotAddedUseCase
import com.tangem.domain.managetokens.CreateCryptoCurrencyUseCase
import com.tangem.domain.managetokens.FindTokenUseCase
import com.tangem.domain.managetokens.ValidateTokenFormUseCase
import com.tangem.domain.managetokens.model.AddCustomTokenForm
import com.tangem.domain.managetokens.model.exceptoin.CustomTokenFormValidationException
import com.tangem.domain.managetokens.model.exceptoin.FindTokenException
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveInAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@ModelScoped
internal class CustomCurrencyValidator @Inject constructor(
    private val validateTokenFormUseCase: ValidateTokenFormUseCase,
    private val createCryptoCurrencyUseCase: CreateCryptoCurrencyUseCase,
    private val findTokenUseCase: FindTokenUseCase,
    private val checkIsCurrencyNotAddedUseCase: CheckIsCurrencyNotAddedUseCase,
) {

    private val validateFormJobHolder = JobHolder()
    private val state: MutableStateFlow<State> = MutableStateFlow(
        value = State(
            prevValidatedForm = null,
            prevFoundOrCreatedCurrency = null,
            status = Status.NotStarted,
        ),
    )

    suspend fun consumeUpdates(block: suspend (Status) -> Unit) {
        state
            .map { it.status }
            .distinctUntilChanged()
            .collectLatest { block(it) }
    }

    suspend fun validateForm(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        formValues: AddCustomTokenForm.Raw,
    ) = coroutineScope {
        updateStatus(Status.Validating)

        val result = validateTokenFormUseCase(
            networkId = networkId,
            formValues = formValues,
        )

        val validatedForm = result.getOrElse { e ->
            updateStatus(Status.FormValidationException(e))
            return@coroutineScope
        }

        if (state.value.prevValidatedForm == validatedForm) {
            return@coroutineScope
        } else {
            state.update { state ->
                state.copy(prevValidatedForm = validatedForm)
            }
        }

        launch {
            when (validatedForm) {
                is AddCustomTokenForm.Validated.All -> {
                    findOrCreateCurrency(userWalletId, networkId, derivationPath, validatedForm)
                }
                is AddCustomTokenForm.Validated.ContractAddressOnly -> {
                    findToken(userWalletId, networkId, derivationPath, validatedForm)
                }
                is AddCustomTokenForm.Validated.Empty -> {
                    createCurrency(userWalletId, networkId, derivationPath, validatedForm = null)
                }
            }
        }.saveInAndJoin(validateFormJobHolder)
    }

    private suspend fun findOrCreateCurrency(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        validatedForm: AddCustomTokenForm.Validated.All,
    ) {
        val currentState = state.value
        if (currentState.prevFoundOrCreatedCurrency is CryptoCurrency.Token &&
            currentState.prevFoundOrCreatedCurrency.contractAddress == validatedForm.contractAddress
        ) {
            // No need to search for token again if contract address is not changed
            createCurrency(userWalletId, networkId, derivationPath, validatedForm)
            return
        }

        updateStatus(Status.SearchingToken)

        val foundToken = findTokenUseCase(
            userWalletId = userWalletId,
            contractAddress = validatedForm.contractAddress,
            networkId = networkId,
            derivationPath = derivationPath,
        ).getOrElse { e ->
            when (e) {
                is FindTokenException.DataError -> {
                    Timber.e(e.cause, "Unable to find custom currency")
                    updateStatus(Status.UnexpectedException(e.cause))
                    return
                }
                is FindTokenException.NotFound -> {
                    null
                }
            }
        }

        if (foundToken != null) {
            updateStateToValidated(userWalletId, foundToken, fillForm = true, isCustom = false)
        } else {
            createCurrency(userWalletId, networkId, derivationPath, validatedForm)
        }
    }

    private suspend fun findToken(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        validatedForm: AddCustomTokenForm.Validated.ContractAddressOnly,
    ) {
        updateStatus(Status.SearchingToken)

        val token = findTokenUseCase(
            userWalletId = userWalletId,
            contractAddress = validatedForm.contractAddress,
            networkId = networkId,
            derivationPath = derivationPath,
        ).getOrElse { e ->
            val newStatus = when (e) {
                is FindTokenException.DataError -> {
                    Timber.e(e.cause, "Unable to find custom currency")
                    Status.UnexpectedException(e.cause)
                }
                is FindTokenException.NotFound -> {
                    Status.TokenNotFound
                }
            }
            updateStatus(newStatus)

            return
        }

        updateStateToValidated(userWalletId, token, fillForm = true, isCustom = false)
    }

    private suspend fun createCurrency(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        validatedForm: AddCustomTokenForm.Validated.All?,
    ) {
        val currency = createCryptoCurrencyUseCase(
            userWalletId = userWalletId,
            networkId = networkId,
            derivationPath = derivationPath,
            formValues = validatedForm,
        ).getOrElse { e ->
            Timber.e(e, "Unable to create custom currency")
            updateStatus(Status.UnexpectedException(e))
            return
        }

        updateStateToValidated(userWalletId, currency, fillForm = false, isCustom = validatedForm != null)
    }

    private suspend fun updateStateToValidated(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        fillForm: Boolean,
        isCustom: Boolean,
    ) {
        val currentStatus = state.value.status
        if (currentStatus is Status.Validated && currentStatus.currency == currency) return

        val isNotAdded = checkIsCurrencyNotAddedUseCase(
            userWalletId = userWalletId,
            networkId = currency.network.id,
            derivationPath = currency.network.derivationPath,
            contractAddress = when (currency) {
                is CryptoCurrency.Coin -> null
                is CryptoCurrency.Token -> currency.contractAddress
            },
        ).getOrElse { e ->
            Timber.e(e, "Unable to check if currency is already added")
            updateStatus(Status.UnexpectedException(e))

            return
        }

        state.update { state ->
            state.copy(
                status = Status.Validated(
                    currency = currency,
                    fillForm = fillForm,
                    isAlreadyAdded = !isNotAdded,
                    isCustom = isCustom,
                ),
                prevFoundOrCreatedCurrency = currency,
            )
        }
    }

    private fun updateStatus(status: Status) {
        state.update { state ->
            state.copy(status = status)
        }
    }

    data class State(
        val prevValidatedForm: AddCustomTokenForm.Validated?,
        val prevFoundOrCreatedCurrency: CryptoCurrency?,
        val status: Status,
    )

    sealed class Status {

        data object NotStarted : Status()

        data object SearchingToken : Status()

        data object Validating : Status()

        data class Validated(
            val currency: CryptoCurrency,
            val fillForm: Boolean,
            val isAlreadyAdded: Boolean,
            val isCustom: Boolean,
        ) : Status()

        data class FormValidationException(
            val exceptions: List<CustomTokenFormValidationException>,
        ) : Status()

        data object TokenNotFound : Status()

        data class UnexpectedException(
            val cause: Throwable,
        ) : Status()
    }
}