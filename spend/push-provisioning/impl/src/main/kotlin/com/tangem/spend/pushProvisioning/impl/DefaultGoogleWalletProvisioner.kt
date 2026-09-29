package com.tangem.spend.pushProvisioning.impl

import android.app.Activity
import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.toOption
import com.google.android.gms.tapandpay.TapAndPayStatusCodes
import com.meawallet.mpp.GooglePayDataChangedListener
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.AddResult
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.Card
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.CardStatus
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.Environment
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.FailureReason
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
internal class DefaultGoogleWalletProvisioner @Inject constructor(
    private val sdk: MeaWalletGooglePay,
    private val activityBridge: DefaultPushProvisioningActivityBridge,
    private val dispatchers: CoroutineDispatcherProvider,
) : GoogleWalletProvisioner {

    private val logger = TangemLogger.withTag(PUSH_PROVISIONING_LOG_TAG)

    private val isSdkReady: Boolean by lazy(::initializeSdk)

    private val recentlyAddedCards = mutableSetOf<String>()

    override val environment: Environment by lazy {
        if (sdk.isProdSdk) Environment.PROD else Environment.TEST
    }

    override val isAdditionInProgress: Boolean
        get() = activityBridge.isAwaitingResult

    override val walletChanges: Flow<Unit> = callbackFlow {
        val listener = GooglePayDataChangedListener { trySend(Unit) }
        val isRegistered = isPushProvisioningSupported() &&
            runCatching { sdk.registerDataChangedListener(listener) }
                .onFailure { logger.e("Failed to subscribe to wallet changes: ${it.describe()}") }
                .isSuccess

        awaitClose {
            if (isRegistered) {
                runCatching { sdk.removeDataChangedListener(listener) }
                    .onFailure { logger.e("Failed to unsubscribe from wallet changes: ${it.describe()}") }
            }
        }
    }.conflate().flowOn(dispatchers.main)

    override suspend fun getCardStatus(lastFourDigits: String): CardStatus = withContext(dispatchers.main) {
        if (!isPushProvisioningSupported()) return@withContext CardStatus.Unsupported

        val walletStatus = findTokens(lastFourDigits).fold(
            ifLeft = { failure -> if (failure.isWalletUnsupported) CardStatus.Unsupported else CardStatus.Unknown },
            ifRight = { tokens -> tokens.map { it.state }.toCardStatus() },
        )
        val status = reconcileWithRecentlyAdded(lastFourDigits = lastFourDigits, status = walletStatus)

        if (status == CardStatus.NotAdded && !canPushCard(lastFourDigits)) {
            CardStatus.Unsupported
        } else {
            status
        }
    }

    override suspend fun addCard(card: Card): AddResult {
        if (card.environment != environment) {
            logger.e("Adding card refused: card environment ${card.environment} differs from SDK $environment")
            return AddResult.Failed(
                reason = FailureReason.ENVIRONMENT_MISMATCH,
                description = ENVIRONMENT_MISMATCH_REASON,
            )
        }

        return addFromActivity(card.lastFourDigits) { activity ->
            checkStableHardwareId() ?: awaitAddition { complete ->
                sdk.push(card = card, activity = activity, listener = pushListener(complete))
            }
        }
    }

    override suspend fun resumeAddingCard(lastFourDigits: String, displayName: String): AddResult =
        addFromActivity(lastFourDigits) { activity ->
            resumeAdding(lastFourDigits = lastFourDigits, displayName = displayName, activity = activity)
        }

    private suspend fun addFromActivity(lastFourDigits: String, add: suspend (Activity) -> AddResult): AddResult =
        withContext(dispatchers.main) {
            val activity = activityBridge.activity
            val result = when {
                activity == null -> AddResult.Failed(
                    reason = FailureReason.NO_ACTIVITY,
                    description = NO_ACTIVITY_REASON,
                )
                !isPushProvisioningSupported() -> AddResult.Failed(
                    reason = FailureReason.UNSUPPORTED,
                    description = UNSUPPORTED_REASON,
                )
                else -> add(activity)
            }

            onAddResult(lastFourDigits = lastFourDigits, result = result)
        }

    private fun isPushProvisioningSupported(): Boolean = sdk.isOsSupported && isWalletAvailable() && isSdkReady

    private fun isWalletAvailable(): Boolean = runCatching(sdk::isWalletAvailable)
        .onFailure { logger.e("Wallet availability check failed: ${it.describe()}") }
        .getOrDefault(false)

    private fun initializeSdk(): Boolean = runCatching(sdk::initialize)
        .onFailure { logger.e("SDK initialization failed: ${it.describe()}") }
        .isSuccess

    private suspend fun findTokens(lastFourDigits: String): TokenLookup {
        val lookup = awaitSdk<TokenLookup>(
            onTimeout = {
                TokenLookupFailure(isWalletUnsupported = false, description = TOKEN_LOOKUP_TIMEOUT_REASON).left()
            },
            onThrow = Throwable::toTokenLookup,
        ) { complete ->
            sdk.checkWalletForCardSuffix(lastFourDigits, registeredTokensListener(complete))
        }

        return lookup.onLeft { failure ->
            val walletEnvironment = if (failure.isWalletUnsupported) {
                ", TapAndPay environment: ${describeWalletEnvironment()}"
            } else {
                ""
            }
            logger.e("Token lookup failed: ${failure.description}$walletEnvironment")
        }
    }

    private suspend fun describeWalletEnvironment(): String = awaitSdk(
        onTimeout = { WALLET_ENVIRONMENT_TIMEOUT_REASON },
        onThrow = Throwable::describe,
    ) { complete ->
        sdk.getEnvironment { environment ->
            complete(environment.fold(ifLeft = { "unknown, code $it" }, ifRight = { it }))
        }
    }

    private suspend fun hasEligibleTokenizationTarget(lastFourDigits: String): Boolean {
        val eligibility = awaitSdk<Either<String, Boolean>>(
            onTimeout = { ELIGIBILITY_TIMEOUT_REASON.left() },
            onThrow = { it.describe().left() },
        ) { complete ->
            sdk.hasEligibleTokenizationTarget(lastFourDigits, eligibilityListener(complete))
        }

        return eligibility
            .onLeft { logger.e("Tokenization target check failed: $it") }
            .getOrElse { true }
    }

    private suspend fun canPushCard(lastFourDigits: String): Boolean {
        if (!hasEligibleTokenizationTarget(lastFourDigits)) return false

        val hardwareIdError = findStableHardwareIdError() ?: return true
        logger.e("$STABLE_HARDWARE_ID_UNAVAILABLE_REASON: $hardwareIdError, card treated as unsupported")
        return false
    }

    private suspend fun checkStableHardwareId(): AddResult.Failed? {
        val errorCode = findStableHardwareIdError() ?: return null

        return AddResult.Failed(
            reason = if (errorCode == TapAndPayStatusCodes.TAP_AND_PAY_UNAVAILABLE) {
                FailureReason.UNSUPPORTED
            } else {
                FailureReason.GOOGLE_WALLET_ERROR
            },
            description = "$STABLE_HARDWARE_ID_UNAVAILABLE_REASON: $errorCode",
            walletErrorCode = errorCode,
        )
    }

    private suspend fun findStableHardwareIdError(): Int? {
        val errorCode = awaitWalletCheck(sdk::checkStableHardwareId) ?: return null
        val isAppUnverified = errorCode == TapAndPayStatusCodes.TAP_AND_PAY_UNAVAILABLE
        return if (isAppUnverified || !isWalletMissing()) errorCode else null
    }

    private suspend fun isWalletMissing(): Boolean =
        awaitWalletCheck(sdk::checkActiveWallet) == TapAndPayStatusCodes.TAP_AND_PAY_NO_ACTIVE_WALLET

    private suspend fun awaitWalletCheck(check: (onComplete: (errorCode: Int?) -> Unit) -> Unit): Int? =
        awaitSdk<Option<Int>>(onTimeout = { None }, onThrow = { None }) { complete ->
            check { errorCode -> complete(errorCode.toOption()) }
        }.getOrNull()

    private suspend fun resumeAdding(lastFourDigits: String, displayName: String, activity: Activity): AddResult {
        val tokens = findTokens(lastFourDigits).getOrElse { failure -> return failure.toAddResult() }
        val pendingToken = tokens.firstOrNull { it.state.isAwaitingIdentityVerification }

        return when {
            pendingToken != null -> awaitAddition { complete ->
                sdk.tokenize(
                    token = pendingToken,
                    displayName = displayName,
                    activity = activity,
                    listener = tokenizeListener(complete),
                )
            }
            tokens.any { it.state.isInWallet } -> AddResult.AlreadyAdded
            else -> AddResult.Failed(
                reason = FailureReason.NO_TOKEN_AWAITING_VERIFICATION,
                description = NO_PENDING_TOKEN_REASON,
            )
        }
    }

    private suspend fun awaitAddition(call: (complete: (AddResult) -> Unit) -> Unit): AddResult {
        val addition = CompletableDeferred<AddResult>()
        if (!activityBridge.forwardResultsDuring(addition)) {
            return AddResult.Failed(
                reason = FailureReason.ADDITION_IN_PROGRESS,
                description = ADDITION_IN_PROGRESS_REASON,
            )
        }

        return addition.awaitSdkCall(onThrow = Throwable::toAddResult, call = call)
    }

    private suspend fun <T : Any> awaitSdk(
        onTimeout: () -> T,
        onThrow: (Throwable) -> T,
        call: (complete: (T) -> Unit) -> Unit,
    ): T = withTimeoutOrNull(SDK_CALLBACK_TIMEOUT) {
        CompletableDeferred<T>().awaitSdkCall(onThrow = onThrow, call = call)
    } ?: onTimeout()

    private suspend fun <T> CompletableDeferred<T>.awaitSdkCall(
        onThrow: (Throwable) -> T,
        call: (complete: (T) -> Unit) -> Unit,
    ): T {
        runCatching { call { value -> complete(value) } }.onFailure { complete(onThrow(it)) }
        return await()
    }

    private fun reconcileWithRecentlyAdded(lastFourDigits: String, status: CardStatus): CardStatus = when (status) {
        CardStatus.NotAdded -> if (lastFourDigits in recentlyAddedCards) CardStatus.Added else status
        CardStatus.Added, CardStatus.Incomplete -> {
            recentlyAddedCards -= lastFourDigits
            status
        }
        CardStatus.Unsupported, CardStatus.Unknown -> status
    }

    private fun onAddResult(lastFourDigits: String, result: AddResult): AddResult {
        when (result) {
            AddResult.Added -> recentlyAddedCards += lastFourDigits
            AddResult.AlreadyAdded -> {
                logger.i("Card is already in the wallet")
                recentlyAddedCards += lastFourDigits
            }
            AddResult.Cancelled -> Unit
            is AddResult.Failed -> logger.e("Adding card failed: ${result.description}")
        }
        return result
    }

    private companion object {
        const val NO_ACTIVITY_REASON = "No activity to open Google Wallet from"
        const val UNSUPPORTED_REASON = "Push provisioning is not supported on this device"
        const val ADDITION_IN_PROGRESS_REASON = "Another card is being added to Google Wallet"
        const val ENVIRONMENT_MISMATCH_REASON = "Card data belongs to another environment than the SDK"
        const val STABLE_HARDWARE_ID_UNAVAILABLE_REASON = "Google Wallet did not return the stable hardware id"
        const val NO_PENDING_TOKEN_REASON = "No token awaits identity verification"
        const val TOKEN_LOOKUP_TIMEOUT_REASON = "Token lookup timed out"
        const val ELIGIBILITY_TIMEOUT_REASON = "Tokenization target check timed out"
        const val WALLET_ENVIRONMENT_TIMEOUT_REASON = "check timed out"
        val SDK_CALLBACK_TIMEOUT = 10.seconds
    }
}