package com.tangem.features.managetokens.utils

import arrow.core.getOrElse
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.domain.managetokens.CheckIsCurrencyNotAddedUseCase
import com.tangem.domain.managetokens.CreateCurrencyUseCase
import com.tangem.domain.managetokens.FindTokenUseCase
import com.tangem.domain.managetokens.ValidateTokenFormUseCase
import com.tangem.domain.managetokens.model.AddCustomTokenForm
import com.tangem.domain.managetokens.model.exceptoin.CustomTokenFormValidationException
import com.tangem.domain.managetokens.model.exceptoin.FindTokenException
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import javax.inject.Inject

@ComponentScoped
internal class CustomCurrencyValidator @Inject constructor(
    private val validateTokenFormUseCase: ValidateTokenFormUseCase,
    private val createCustomCurrencyUseCase: CreateCurrencyUseCase,
    private val findTokenUseCase: FindTokenUseCase,
    private val checkIsCurrencyNotAddedUseCase: CheckIsCurrencyNotAddedUseCase,
) {

    private val state: MutableStateFlow<State> = MutableStateFlow(State.NotStarted)

    suspend fun consumeUpdates(block: suspend (State) -> Unit) {
        state.collectLatest { s ->
            block(s)
        }
    }

    suspend fun validateForm(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        formValues: AddCustomTokenForm.Raw,
    ) {
        val result = validateTokenFormUseCase(
            networkId = networkId,
            formValues = formValues,
        )

        val validatedForm = result.getOrElse { e ->
            state.value = State.FormValidationException(e)
            return
        }

        when (validatedForm) {
            is AddCustomTokenForm.Validated.All -> {
                findOrCreateCurrency(userWalletId, networkId, derivationPath, validatedForm)
            }
            is AddCustomTokenForm.Validated.ContractAddress -> {
                findToken(userWalletId, networkId, derivationPath, validatedForm)
            }
        }
    }

    suspend fun createCoin(userWalletId: UserWalletId, networkId: Network.ID, derivationPath: Network.DerivationPath) {
        createCurrency(userWalletId, networkId, derivationPath, validatedForm = null)
    }

    private suspend fun findOrCreateCurrency(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        validatedForm: AddCustomTokenForm.Validated.All,
    ) {
        state.value = State.SearchingToken

        val foundToken = findTokenUseCase(
            userWalletId = userWalletId,
            contractAddress = validatedForm.contractAddress,
            networkId = networkId,
            derivationPath = derivationPath,
        ).getOrElse { e ->
            when (e) {
                is FindTokenException.DataError -> {
                    Timber.e(e.cause, "Unable to find custom currency")
                    state.value = State.UnexpectedException(e.cause)
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
        validatedForm: AddCustomTokenForm.Validated.ContractAddress,
    ) {
        state.value = State.SearchingToken

        val token = findTokenUseCase(
            userWalletId = userWalletId,
            contractAddress = validatedForm.contractAddress,
            networkId = networkId,
            derivationPath = derivationPath,
        ).getOrElse { e ->
            state.value = when (e) {
                is FindTokenException.DataError -> {
                    Timber.e(e.cause, "Unable to find custom currency")
                    State.UnexpectedException(e.cause)
                }
                is FindTokenException.NotFound -> {
                    State.TokenNotFound
                }
            }

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
        val currency = createCustomCurrencyUseCase(
            networkId = networkId,
            derivationPath = derivationPath,
            formValues = validatedForm,
        ).getOrElse { e ->
            Timber.e(e, "Unable to create custom currency")
            state.value = State.UnexpectedException(e)
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
            state.value = State.UnexpectedException(e)

            return
        }

        state.value = State.Validated(
            currency = currency,
            fillForm = fillForm,
            isAlreadyAdded = !isNotAdded,
            isCustom = isCustom,
        )
    }

    sealed class State {

        abstract val isFinished: Boolean

        data object NotStarted : State() {
            override val isFinished: Boolean = false
        }

        data object SearchingToken : State() {
            override val isFinished: Boolean = false
        }

        data class Validated(
            val currency: CryptoCurrency,
            val fillForm: Boolean,
            val isAlreadyAdded: Boolean,
            val isCustom: Boolean,
        ) : State() {
            override val isFinished: Boolean = true
        }

        data class FormValidationException(
            val exceptions: List<CustomTokenFormValidationException>,
        ) : State() {
            override val isFinished: Boolean = true
        }

        data object TokenNotFound : State() {
            override val isFinished: Boolean = true
        }

        data class UnexpectedException(
            val cause: Throwable,
        ) : State() {
            override val isFinished: Boolean = true
        }
    }
}
