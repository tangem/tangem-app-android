package com.tangem.features.jointaccount.creation.model

import androidx.compose.runtime.Stable
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.jointaccount.model.JointAccountCreationError
import com.tangem.domain.jointaccount.usecase.CreateJointAccountUseCase
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject
import com.tangem.core.ui.R as CoreUiR

@Stable
@ModelScoped
internal class JointAccountCreationModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val createJointAccount: CreateJointAccountUseCase,
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
) : Model() {

    private val logger = TangemLogger.withTag(tag = "JointAccountCreation")

    val draftHolder = JointAccountCreationDraftHolder()

    /**
     * Runs the creation: one card tap, then the account is registered and the flow lands on the members screen.
     *
     * Suspends until the whole operation completes — the display-name button stays in its loading state for
     * exactly that long.
     */
    suspend fun createAccount(displayName: String) {
        val draft = draftHolder.draft.value
        val config = draft.config
        val composition = draft.composition

        if (config == null || composition == null) {
            logger.e(messageString = "Creation draft is incomplete: config or composition is missing")
            showError()
            return
        }

        createJointAccount(
            userWalletId = config.walletId,
            config = JointAccountConfig(
                name = config.name,
                icon = config.icon.name,
                iconColor = config.color.name,
                membersCount = composition.totalMembers,
                threshold = composition.requiredToSign,
            ),
            creatorName = displayName,
        ).fold(
            ifLeft = ::onCreationError,
            ifRight = { openMembers(userWalletId = config.walletId) },
        )
    }

    // TODO: a separate task will handle the cases apart — no network, a signature the backend rejected (400)
    //  and "the account exists but is not in the list" all show the same generic dialog for now; the texts and
    //  the per-case behaviour (retry, contact support) come with it.
    private fun onCreationError(error: JointAccountCreationError) {
        when (error) {
            // Dismissing the card session is not a failure: the form stays as it was
            JointAccountCreationError.UserCancelled -> Unit
            // The cause is already logged by safeApiCall in the data layer
            JointAccountCreationError.ExistingAccountNotFound,
            is JointAccountCreationError.Failed,
            -> showError()
        }
    }

    private fun openMembers(userWalletId: UserWalletId) {
        router.replaceAll(AppRoute.JointAccountMembers(userWalletId = userWalletId))
    }

    private fun showError() {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(CoreUiR.string.common_error),
                message = resourceReference(CoreUiR.string.common_something_went_wrong),
            ), // TODO change error description when criteria will be ready
        )
    }
}