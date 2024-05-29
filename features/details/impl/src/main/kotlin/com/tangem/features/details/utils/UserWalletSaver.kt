package com.tangem.features.details.utils

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.raise.withError
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.AppScreen
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.wallets.builder.UserWalletBuilder
import com.tangem.domain.wallets.models.SaveWalletError
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GenerateWalletNameUseCase
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.domain.wallets.usecase.SelectWalletUseCase
import com.tangem.features.details.impl.R
import com.tangem.features.details.routing.DetailsRoute
import javax.inject.Inject

@ComponentScoped
internal class UserWalletSaver @Inject constructor(
    private val scanCardProcessor: ScanCardProcessor,
    private val saveWalletUseCase: SaveWalletUseCase,
    private val generateWalletNameUseCase: GenerateWalletNameUseCase,
    private val selectWalletUseCase: SelectWalletUseCase,
    private val reduxStateHolder: ReduxStateHolder,
    private val router: Router,
) {

    suspend fun saveUserWallet(): Either<TextReference?, Unit> = either {
        val response = scanCard()
        val userWallet = createUserWallet(response)

        saveWallet(userWallet)
    }

    private suspend fun Raise<TextReference?>.saveWallet(userWallet: UserWallet) {
        val error = saveWalletUseCase(userWallet).leftOrNull() ?: return

        when (error) {
            is SaveWalletError.DataError -> raise(error.messageId?.let(::resourceReference))
            is SaveWalletError.WalletAlreadySaved -> {
                withError({ resourceReference(R.string.common_unknown_error) }) {
                    selectWalletUseCase(userWallet.walletId).bind()
                }

                reduxStateHolder.onUserWalletSelected(userWallet)
                router.popTo(DetailsRoute.Screen(AppScreen.Wallet))

                raise(null)
            }
        }
    }

    private suspend fun Raise<TextReference?>.createUserWallet(response: ScanResponse): UserWallet {
        val userWallet = UserWalletBuilder(response, generateWalletNameUseCase).build()

        return ensureNotNull(userWallet) { null }
    }

    private suspend fun Raise<TextReference?>.scanCard(): ScanResponse {
        var response: ScanResponse? = null

        scanCardProcessor.scan(
            analyticsSource = AnalyticsParam.ScreensSources.Settings,
            onWalletNotCreated = {
                raise(null)
            },
            disclaimerWillShow = {
                router.pop()
                raise(null)
            },
            onSuccess = {
                response = it
            },
            onFailure = { error ->
                val message = if (!error.silent) {
                    error.messageResId
                        ?.let(::resourceReference)
                        ?: stringReference(error.customMessage)
                } else {
                    null
                }

                raise(message)
            },
        )

        return response!!
    }
}
