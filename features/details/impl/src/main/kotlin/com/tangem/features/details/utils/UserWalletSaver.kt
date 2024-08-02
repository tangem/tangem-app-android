package com.tangem.features.details.utils

import androidx.compose.ui.res.stringResource
import arrow.core.raise.Raise
import arrow.core.raise.ensureNotNull
import arrow.core.raise.fold
import arrow.core.raise.recover
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.navigation.popTo
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.SimpleOkDialog
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.isNullOrEmpty
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.ContentMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.wallets.builder.UserWalletBuilder
import com.tangem.domain.wallets.models.SaveWalletError
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GenerateWalletNameUseCase
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.domain.wallets.usecase.ShouldSaveUserWalletsSyncUseCase
import com.tangem.features.details.impl.R
import javax.inject.Inject

@ComponentScoped
@Suppress("LongParameterList")
internal class UserWalletSaver @Inject constructor(
    private val scanCardProcessor: ScanCardProcessor,
    private val saveWalletUseCase: SaveWalletUseCase,
    private val generateWalletNameUseCase: GenerateWalletNameUseCase,
    private val shouldSaveUserWalletsSyncUseCase: ShouldSaveUserWalletsSyncUseCase,
    private val reduxStateHolder: ReduxStateHolder,
    private val messageSender: UiMessageSender,
    private val router: Router,
) {

    suspend fun scanAndSaveUserWallet() = recover(
        block = {
            val response = scanCard() ?: return@recover
            val userWallet = createUserWallet(response)

            saveWallet(userWallet)
        },
        recover = { error ->
            val message = error.message

            if (!message.isNullOrEmpty()) {
                messageSender.send(SnackbarMessage(message))
            }
        },
    )

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
            message = ContentMessage { onDismiss ->
                SimpleOkDialog(
                    message = stringResource(id = R.string.user_wallet_list_error_wallet_already_saved),
                    onDismissDialog = onDismiss,
                )
            },
        )
    }

    private suspend fun Raise<Error>.createUserWallet(response: ScanResponse): UserWallet {
        val userWallet = UserWalletBuilder(response, generateWalletNameUseCase).build()

        return ensureNotNull(userWallet) { Error.Unknown }
    }

    private suspend fun Raise<Error>.scanCard(): ScanResponse? {
        var response: ScanResponse? = null

        scanCardProcessor.scan(
            analyticsSource = AnalyticsParam.ScreensSources.Settings,
            onWalletNotCreated = {
                /* no-op */
            },
            disclaimerWillShow = {
                router.pop()
            },
            onSuccess = {
                response = it
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

                raise(error)
            },
        )

        return response
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