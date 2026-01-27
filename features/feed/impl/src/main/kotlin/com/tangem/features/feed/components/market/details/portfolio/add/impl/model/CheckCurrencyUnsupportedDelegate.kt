package com.tangem.features.feed.components.market.details.portfolio.add.impl.model

import arrow.core.getOrElse
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.managetokens.CheckCurrencyUnsupportedUseCase
import com.tangem.domain.managetokens.model.CurrencyUnsupportedState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.feed.impl.R
import timber.log.Timber
import javax.inject.Inject

class CheckCurrencyUnsupportedDelegate @Inject constructor(
    private val checkCurrencyUnsupportedUseCase: CheckCurrencyUnsupportedUseCase,
    private val messageSender: UiMessageSender,
) {

    suspend fun checkCurrencyUnsupportedState(
        userWalletId: UserWalletId,
        rawNetworkId: String,
        isMainNetwork: Boolean,
    ): CurrencyUnsupportedState? {
        val result = checkCurrencyUnsupportedUseCase(
            userWalletId = userWalletId,
            networkId = rawNetworkId,
            isMainNetwork = isMainNetwork,
        ).getOrElse { throwable ->
            Timber.e(
                throwable,
                """
                    Failed to check currency unsupported state
                    |- User wallet ID: $userWalletId
                    |- Network ID: $rawNetworkId
                    |- Is main network: $isMainNetwork
                """.trimIndent(),
            )

            val message = SnackbarMessage(
                message = throwable.localizedMessage?.let(::stringReference)
                    ?: resourceReference(R.string.common_error),
            )
            messageSender.send(message)

            null
        }

        if (result != null) {
            showUnsupportedWarning(result)
        }
        return result
    }

    private fun showUnsupportedWarning(unsupportedState: CurrencyUnsupportedState) {
        val message = DialogMessage(
            message = when (unsupportedState) {
                is CurrencyUnsupportedState.Token.NetworkTokensUnsupported -> resourceReference(
                    id = R.string.alert_manage_tokens_unsupported_message,
                    formatArgs = wrappedList(unsupportedState.networkName),
                )
                is CurrencyUnsupportedState.Token.UnsupportedCurve -> resourceReference(
                    id = R.string.alert_manage_tokens_unsupported_curve_message,
                    formatArgs = wrappedList(unsupportedState.networkName),
                )
                is CurrencyUnsupportedState.UnsupportedNetwork -> resourceReference(
                    id = R.string.alert_manage_tokens_unsupported_curve_message,
                    formatArgs = wrappedList(unsupportedState.networkName),
                )
            },
        )

        messageSender.send(message)
    }
}