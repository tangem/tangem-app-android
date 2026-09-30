package com.tangem.feature.tester.presentation.sociallogin.viewmodels

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.SocialLoginFeatureToggles
import com.tangem.domain.sociallogin.models.SocialLoginProvider
import com.tangem.domain.sociallogin.repository.SocialBackupRepository
import com.tangem.domain.sociallogin.usecase.GetAvailableSocialLoginProvidersUseCase
import com.tangem.domain.sociallogin.usecase.GetSocialBackupInfoUseCase
import com.tangem.domain.sociallogin.usecase.RevokeSocialRecoveryUseCase
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import com.tangem.feature.tester.presentation.sociallogin.state.SocialLoginTesterUM
import com.tangem.feature.tester.presentation.sociallogin.state.SocialLoginTesterUM.Action
import com.tangem.feature.tester.presentation.sociallogin.state.SocialLoginTesterUM.StatusRow
import com.tangem.features.hotwallet.SocialLoginOutcome
import com.tangem.features.hotwallet.SocialLoginStage
import com.tangem.features.hotwallet.SocialLoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@Suppress("LongParameterList")
@HiltViewModel
internal class SocialLoginTesterViewModel @Inject constructor(
    private val socialLoginFeatureToggles: SocialLoginFeatureToggles,
    private val getAvailableSocialLoginProvidersUseCase: GetAvailableSocialLoginProvidersUseCase,
    private val getSocialBackupInfoUseCase: GetSocialBackupInfoUseCase,
    private val revokeSocialRecoveryUseCase: RevokeSocialRecoveryUseCase,
    // the stored flag and a bare file delete, which no use case exposes on purpose
    private val socialBackupRepository: SocialBackupRepository,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val socialLoginUseCase: SocialLoginUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val uiState: StateFlow<SocialLoginTesterUM>
        field = MutableStateFlow(SocialLoginTesterUM())

    private var lastResult: String? = null

    init {
        viewModelScope.launch { rebuild() }
    }

    fun setupNavigation(router: InnerTesterRouter) {
        uiState.update { it.copy(onBackClick = router::back) }
    }

    private suspend fun rebuild() {
        val providers = getAvailableSocialLoginProvidersUseCase()
        val hotWallets = userWalletsListRepository.userWalletsSync().filterIsInstance<UserWallet.Hot>()
        uiState.update { state ->
            state.copy(rows = buildRows(providers, hotWallets), actions = buildActions(providers, hotWallets))
        }
    }

    private suspend fun buildRows(
        providers: List<SocialLoginProvider>,
        hotWallets: List<UserWallet.Hot>,
    ): ImmutableList<StatusRow> = buildList {
        add(StatusRow("Feature toggle", if (socialLoginFeatureToggles.isSocialLoginEnabled) "ON" else "OFF"))
        add(StatusRow("Available providers", providers.joinToString().ifEmpty { "none" }))

        getSocialBackupInfoUseCase().fold(
            ifLeft = { add(StatusRow("Backup in the hidden Drive folder", "absent ($it)")) },
            ifRight = { info ->
                add(StatusRow("Backup wallet id", info.walletId.stringValue))
                add(StatusRow("Backup created at", info.createdAtMillis.formatted()))
                add(StatusRow("Backup format version", info.version.toString()))
            },
        )

        if (hotWallets.isEmpty()) add(StatusRow("Mobile wallets", "none"))
        hotWallets.forEach { wallet ->
            val flag = if (socialBackupRepository.isSocialBackupCompleted(wallet.walletId)) "✅" else "❌"
            add(StatusRow("Wallet ${wallet.name}", "${wallet.walletId.stringValue}\nstored social flag: $flag"))
        }

        lastResult?.let { add(StatusRow("Last action", it)) }
    }.toImmutableList()

    private suspend fun buildActions(
        providers: List<SocialLoginProvider>,
        hotWallets: List<UserWallet.Hot>,
    ): ImmutableList<Action> = buildList {
        providers.forEach { provider ->
            add(action("Sign in with $provider") { signIn(provider) })
            add(
                action("Sign out of $provider") {
                    socialLoginUseCase.signOut(provider)
                    "done"
                },
            )
        }

        hotWallets.forEach { wallet ->
            val isFlagged = socialBackupRepository.isSocialBackupCompleted(wallet.walletId)
            add(action("${wallet.name}: revoke social recovery") { revoke(wallet.walletId) })
            add(
                action("${wallet.name}: ${if (isFlagged) "clear" else "set"} the social flag") {
                    setFlag(wallet.walletId, isCompleted = !isFlagged)
                },
            )
            add(action("${wallet.name}: delete the cloud file only") { deleteFile(wallet.walletId) })
        }

        add(action("Refresh") { "done" })
    }.toImmutableList()

    private suspend fun signIn(provider: SocialLoginProvider): String {
        val stages = mutableListOf<SocialLoginStage>()
        val result = socialLoginUseCase.signIn(provider = provider, onStageChanged = { stages += it })
        val outcome = result.fold(
            ifLeft = { "failed: $it" },
            ifRight = { outcome ->
                when (outcome) {
                    is SocialLoginOutcome.WalletCreated -> "created ${outcome.userWalletId.stringValue}"
                    is SocialLoginOutcome.WalletRestored -> "restored ${outcome.userWalletId.stringValue}"
                }
            },
        )
        return "$outcome\nstages: ${stages.joinToString(separator = " → ")}"
    }

    private suspend fun revoke(walletId: UserWalletId): String =
        revokeSocialRecoveryUseCase(walletId).fold(ifLeft = { "failed: $it" }, ifRight = { "done" })

    private suspend fun setFlag(walletId: UserWalletId, isCompleted: Boolean): String = socialBackupRepository
        .setSocialBackupCompleted(walletId = walletId, isCompleted = isCompleted)
        .fold(ifLeft = { "failed: $it" }, ifRight = { "done" })

    private suspend fun deleteFile(walletId: UserWalletId): String = socialBackupRepository
        .deleteBackup(walletId)
        .fold(ifLeft = { "failed: $it" }, ifRight = { "done" })

    private fun action(label: String, block: suspend () -> String): Action = Action(label) {
        if (uiState.value.runningAction != null) return@Action

        viewModelScope.launch {
            uiState.update { it.copy(runningAction = label) }
            val result = try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "error: ${e.message}"
            }
            lastResult = "$label: $result"
            Toast.makeText(context, lastResult, Toast.LENGTH_LONG).show()
            rebuild()
            uiState.update { it.copy(runningAction = null) }
        }
    }

    private fun Long.formatted(): String = if (this == 0L) "unknown" else DATE_FORMAT.format(Date(this))

    private companion object {
        val DATE_FORMAT = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US)
    }
}