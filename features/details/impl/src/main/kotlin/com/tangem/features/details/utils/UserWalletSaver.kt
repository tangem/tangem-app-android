package com.tangem.features.details.utils

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.ensureNotNull
import arrow.core.raise.fold
import arrow.core.raise.recover
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.navigation.popTo
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.isNullOrEmpty
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.wallets.builder.ColdUserWalletBuilder
import com.tangem.domain.wallets.models.SaveWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.domain.wallets.usecase.ShouldSaveUserWalletsSyncUseCase
import com.tangem.features.details.impl.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

@ModelScoped
@Suppress("LongParameterList")
internal class UserWalletSaver @Inject constructor(
    private val scanCardProcessor: ScanCardProcessor,
    private val saveWalletUseCase: SaveWalletUseCase,
    private val coldUserWalletBuilderFactory: ColdUserWalletBuilder.Factory,
    private val shouldSaveUserWalletsSyncUseCase: ShouldSaveUserWalletsSyncUseCase,
    private val reduxStateHolder: ReduxStateHolder,
    private val messageSender: UiMessageSender,
    private val router: Router,
) {

    suspend fun scanAndSaveUserWallet(scope: CoroutineScope) {
        val response = scanCard(scope)
        response.fold(
            ifLeft = {
                val message = it.message

                if (!message.isNullOrEmpty()) {
                    messageSender.send(SnackbarMessage(message))
                }
            },
            ifRight = { scanResponse ->
                recover(
                    block = {
                        scanResponse ?: return
                        val userWallet = createUserWallet(scanResponse)
                        saveWallet(userWallet)
                    },
                    recover = {
                        val message = it.message

                        if (!message.isNullOrEmpty()) {
                            messageSender.send(SnackbarMessage(message))
                        }
                    },
                )
            },
        )
    }

    private suspend fun Raise<Error>.saveWallet(userWallet: UserWallet) {
        fold(
            block = { saveWalletUseCase(userWallet).bind() },
            recover = { error ->
                when (error) {
                    is SaveWalletError.WalletAlreadySaved -> {
                        if (shouldSaveUserWalletsSyncUseCase()) {
                            selectUserWallet()
                        } else {
                            router.popTo<AppRoute.Wallet>()
                        }
                    }
                    is SaveWalletError.DataError -> {
                        val messageRef = ensureNotNull(error.messageId?.let(::resourceReference)) {
                            Error.Unknown
                        }

                        raise(Error.Message(messageRef))
                    }
                }
            },
            transform = {
                // call only if wallet is successfully saved
                reduxStateHolder.onUserWalletSelected(userWallet)

                router.popTo<AppRoute.Wallet>()
            },
        )
    }

    private fun selectUserWallet() {
        messageSender.send(
            message = DialogMessage(
                message = resourceReference(R.string.user_wallet_list_error_wallet_already_saved),
            ),
        )
    }

    private suspend fun Raise<Error>.createUserWallet(response: ScanResponse): UserWallet {
        val userWallet = coldUserWalletBuilderFactory.create(scanResponse = response).build()

        return ensureNotNull(userWallet) { Error.Unknown }
    }

    private suspend fun scanCard(scope: CoroutineScope) = suspendCancellableCoroutine { continuation ->
        scope.launch {
            scanCardProcessor.scan(
                analyticsSource = AnalyticsParam.ScreensSources.Settings,
                onWalletNotCreated = {
                    continuation.resume(Either.Right(null))
                },
                disclaimerWillShow = {
                    continuation.resume(Either.Right(null))
                    router.pop()
                },
                onSuccess = {
                    continuation.resume(Either.Right(it))
                },
                onCancel = {
                    continuation.resume(Either.Right(null))
                },
                onFailure = { tangemError ->
                    val error = if (!tangemError.silent) {
                        val message = tangemError.messageResId
                            ?.let(::resourceReference)
                            ?: stringReference(tangemError.customMessage)

                        Error.Message(message)
                    } else {
                        Error.Silent
                    }
                    continuation.resume(Either.Left(error))
                },
            )
        }
    }

    sealed class Error {

        open val message: TextReference? = null

        data object Silent : Error()

        data class Message(override val message: TextReference) : Error()

        data object Unknown : Error() {

            override val message: TextReference = resourceReference(R.string.common_unknown_error)
        }
    }
}