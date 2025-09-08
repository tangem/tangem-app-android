package com.tangem.features.yieldlending.impl.subcomponents.startearning.model

import arrow.core.getOrElse
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.transaction.YieldLendingTransactionRepository
import com.tangem.domain.transaction.usecase.CreateApprovalTransactionUseCase
import com.tangem.domain.transaction.usecase.GetAllowanceUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.yieldlending.impl.subcomponents.startearning.entity.EnterStep
import com.tangem.features.yieldlending.impl.subcomponents.startearning.entity.EnterStepType
import com.tangem.features.yieldlending.impl.subcomponents.startearning.YieldLendingActionComponent
import com.tangem.features.yieldlending.impl.subcomponents.startearning.entity.YieldLendingStartEarningContentUM
import com.tangem.features.yieldlending.impl.subcomponents.startearning.entity.YieldLendingStartEarningUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.PeriodicTask
import com.tangem.utils.coroutines.SingleTaskScheduler
import com.tangem.utils.extensions.addOrReplace
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.properties.Delegates

@ModelScoped
internal class YieldLendingActionModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val yieldLendingTransactionRepository: YieldLendingTransactionRepository,
    private val getFeeUseCase: GetFeeUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val createApprovalTransactionUseCase: CreateApprovalTransactionUseCase,
    private val getAllowanceUseCase: GetAllowanceUseCase,
) : Model() {

    private val params: YieldLendingActionComponent.Params = paramsContainer.require()

    private val cryptoCurrency = params.cryptoCurrency
    private var userWallet: UserWallet by Delegates.notNull()

    private val cryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>
        field = MutableStateFlow(
            CryptoCurrencyStatus(
                cryptoCurrency,
                value = CryptoCurrencyStatus.Loading,
            ),
        )
    private val feeCryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>
        field = MutableStateFlow(
            CryptoCurrencyStatus(
                cryptoCurrency,
                value = CryptoCurrencyStatus.Loading,
            ),
        )

    val uiState: StateFlow<YieldLendingStartEarningUM>
        field: MutableStateFlow<YieldLendingStartEarningUM> = MutableStateFlow(
            YieldLendingStartEarningUM(
                TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = params.onDismiss,
                    content = YieldLendingStartEarningContentUM.Main(
                        currencyIconState = CryptoCurrencyToIconStateConverter().convert(cryptoCurrency),
                        fee = null,
                        currentStepType = EnterStepType.Deploy,
                        steps = persistentListOf(
                            EnterStep(
                                stepType = EnterStepType.Deploy,
                                isActive = false,
                                isComplete = false,
                            ),
                            EnterStep(
                                stepType = EnterStepType.Approve,
                                isActive = false,
                                isComplete = false,
                            ),
                            EnterStep(
                                stepType = EnterStepType.Enter,
                                isActive = false,
                                isComplete = false,
                            )
                        ),
                    ),
                ),
            ),
        )

    private var yieldContractAddress: String = ""

    private val quoteTaskScheduler = SingleTaskScheduler<Unit>()

    private val mainContent: YieldLendingStartEarningContentUM.Main?
        get() = uiState.value.bottomSheetConfig.content as? YieldLendingStartEarningContentUM.Main

    init {
        modelScope.launch {
            subscribeOnCurrencyStatusUpdates()

            yieldContractAddress = yieldLendingTransactionRepository.getYieldContractAddress(
                userWalletId = params.userWalletId,
                cryptoCurrency = cryptoCurrency,
            )

        }
    }

    fun onClick() {
        val step = mainContent?.steps?.firstOrNull { it.stepType == mainContent?.currentStepType } ?: return
        modelScope.launch {
            when (step.stepType) {
                EnterStepType.Deploy -> deployTransaction()
                EnterStepType.InitToken -> initTokenTransaction()
                EnterStepType.Approve -> approveTransaction()
                EnterStepType.Enter -> enterTransaction()
            }
        }
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        modelScope.launch {
            getUserWalletUseCase(params.userWalletId).fold(
                ifRight = { wallet ->
                    userWallet = wallet
                    getCurrenciesStatusUpdates()
                },
                ifLeft = {
                    Timber.w(it.toString())
                    // showAlertError() todo yield lending error alert
                    return@launch
                },
            )
        }
    }

    private fun getCurrenciesStatusUpdates() {
        getSingleCryptoCurrencyStatusUseCase.invokeMultiWallet(
            userWalletId = params.userWalletId,
            currencyId = cryptoCurrency.id,
            isSingleWalletWithTokens = false,
        ).onEach { maybeCryptoCurrency ->
            maybeCryptoCurrency.fold(
                ifRight = { cryptoCurrencyStatus ->
                    onDataLoaded(
                        currencyStatus = cryptoCurrencyStatus,
                        feeCurrencyStatus = getFeePaidCryptoCurrencyStatusSyncUseCase(
                            userWalletId = params.userWalletId,
                            cryptoCurrencyStatus = cryptoCurrencyStatus,
                        ).getOrNull() ?: cryptoCurrencyStatus,
                    )
                },
                ifLeft = {
                    // todo yield lending error
                    // sendConfirmAlertFactory.getGenericErrorState(
                    //     onFailedTxEmailClick = {
                    //         onFailedTxEmailClick(it.toString())
                    //     },
                    //     popBack = router::pop,
                    // )
                },
            )
        }.launchIn(modelScope)
    }

    private fun onDataLoaded(
        currencyStatus: CryptoCurrencyStatus,
        feeCurrencyStatus: CryptoCurrencyStatus,
    ) {
        cryptoCurrencyStatusFlow.update { currencyStatus }
        feeCryptoCurrencyStatusFlow.update { feeCurrencyStatus }

        modelScope.launch {
            if (yieldContractAddress == EthereumUtils.EMPTY_ADDRESS) {
                prepareDeployTransaction()
            } else {
                val yieldTokenStatus = currencyStatus.value.yieldLendingStatus
                when {
                    yieldTokenStatus == null -> Unit // todo error state
                    !yieldTokenStatus.isInitialized -> {
                        uiState.update {
                            it.copy(
                                bottomSheetConfig = it.bottomSheetConfig.copy(
                                    content = mainContent?.copy(
                                        steps = mainContent?.steps?.addOrReplace(
                                            item = EnterStep(
                                                stepType = EnterStepType.InitToken,
                                                isActive = false,
                                                isComplete = false,
                                            ),
                                            predicate = { it.stepType == EnterStepType.Deploy }
                                        ).orEmpty().toPersistentList(),
                                    ) ?: it.bottomSheetConfig.content
                                ),
                            )
                        }
                        prepareInitTokenTransaction()
                    }
                    !yieldTokenStatus.isActive -> Unit // todo reactivate
                    !yieldTokenStatus.isAllowedToSpend -> prepareApproveTransaction()
                    else -> prepareEnterTransaction()
                }
            }

        }
    }

    // region DEPLOY
    private suspend fun prepareDeployTransaction() {
        Timber.i("Yield Lending App: Deploy contract fee")
        val yieldLendingStatus = cryptoCurrencyStatusFlow.value.value.yieldLendingStatus
            ?: error("Invalid state")

        val deployFeeTransactionData = yieldLendingTransactionRepository.createDeployTransaction(
            userWalletId = params.userWalletId,
            cryptoCurrency = cryptoCurrency,
            yieldLendingStatus = yieldLendingStatus,
            fee = null
        )
        loadFee(
            currentStepType = EnterStepType.Deploy,
            transactionData = deployFeeTransactionData,
        )
    }

    private suspend fun deployTransaction() {
        Timber.i("Yield Lending App: Deploying contract")
        val yieldLendingStatus = cryptoCurrencyStatusFlow.value.value.yieldLendingStatus
            ?: error("Invalid state")

        val deployTransactionData = yieldLendingTransactionRepository.createDeployTransaction(
            userWalletId = params.userWalletId,
            cryptoCurrency = cryptoCurrency,
            yieldLendingStatus = yieldLendingStatus,
            fee = mainContent?.fee ?: error("Empty fee"),
        )
        sendTransaction(deployTransactionData)
        startCheckingCompletionTask(::waitForContractAddress)
    }

    private suspend fun waitForContractAddress() {
        yieldContractAddress = yieldLendingTransactionRepository.getYieldContractAddress(
            userWalletId = params.userWalletId,
            cryptoCurrency = cryptoCurrency
        )
        if (yieldContractAddress.isEmpty() || yieldContractAddress == EthereumUtils.EMPTY_ADDRESS) return

        Timber.i("Yield Lending App: Contract deployed")
        onFinishedStep(EnterStepType.Deploy)
        quoteTaskScheduler.cancelTask()
        prepareApproveTransaction()
    }
    // endregion

    // region INIT TOKEN
    private suspend fun prepareInitTokenTransaction() {
        Timber.i("Yield Lending App: Init token fee")
        val yieldLendingStatus = cryptoCurrencyStatusFlow.value.value.yieldLendingStatus
            ?: error("Invalid state")

        val deployFeeTransactionData = yieldLendingTransactionRepository.createInitTokenTransaction(
            userWalletId = params.userWalletId,
            cryptoCurrency = cryptoCurrency,
            yieldContractAddress = yieldContractAddress,
            yieldLendingStatus = yieldLendingStatus,
            fee = null
        )
        loadFee(
            currentStepType = EnterStepType.InitToken,
            transactionData = deployFeeTransactionData,
        )
    }

    private suspend fun initTokenTransaction() {
        Timber.i("Yield Lending App: Initializing token contract")
        val yieldLendingStatus = cryptoCurrencyStatusFlow.value.value.yieldLendingStatus
            ?: error("Invalid state")

        val initTokenTransactionData = yieldLendingTransactionRepository.createInitTokenTransaction(
            userWalletId = params.userWalletId,
            cryptoCurrency = cryptoCurrency,
            yieldContractAddress = yieldContractAddress,
            yieldLendingStatus = yieldLendingStatus,
            fee = mainContent?.fee ?: error("Empty fee"),
        )
        sendTransaction(initTokenTransactionData)
        prepareApproveTransaction()
    }
    // endregion

    // region APPROVE
    private suspend fun prepareApproveTransaction() {
        Timber.i("Yield Lending App: Approve contract fee")
        val approveFeeTransactionData = createApprovalTransactionUseCase(
            cryptoCurrency = cryptoCurrency as CryptoCurrency.Token,
            userWalletId = params.userWalletId,
            amount = null,
            contractAddress = cryptoCurrency.contractAddress,
            spenderAddress = yieldContractAddress
        ).getOrElse { Timber.i("Yield Lending App: $it"); return }
        loadFee(
            currentStepType = EnterStepType.Approve,
            transactionData = approveFeeTransactionData,
        )
    }

    private suspend fun approveTransaction() {
        Timber.i("Yield Lending App: Approving contract")
        val approveTransactionData = createApprovalTransactionUseCase(
            cryptoCurrency = cryptoCurrency as CryptoCurrency.Token,
            userWalletId = params.userWalletId,
            amount = null,
            fee = mainContent?.fee ?: error("Empty fee"),
            contractAddress = cryptoCurrency.contractAddress,
            spenderAddress = yieldContractAddress
        ).getOrElse { Timber.i("Yield Lending App: $it");return }

        sendTransactionUseCase(
            txData = approveTransactionData,
            userWallet = userWallet,
            network = cryptoCurrency.network,
        ).getOrElse {
            Timber.i("Yield Lending App: Failed to approve: $it")
        }
        startCheckingCompletionTask(::waitForApproval)
    }

    private suspend fun waitForApproval() {
        val allowance = getAllowanceUseCase(
            userWalletId = params.userWalletId,
            cryptoCurrency = cryptoCurrency,
            spenderAddress = yieldContractAddress,
        ).getOrElse { BigDecimal.ZERO }

        // check for balance or limit
        if (allowance <= BigDecimal.ZERO) return
        Timber.i("Yield Lending App: Contract approved")
        onFinishedStep(EnterStepType.Approve)
        quoteTaskScheduler.cancelTask()
        prepareEnterTransaction()
    }
    //endregion

    //region ENTER
    private suspend fun prepareEnterTransaction() {
        Timber.i("Yield Lending App: Enter protocol fee")
        val yieldLendingStatus = cryptoCurrencyStatusFlow.value.value.yieldLendingStatus
            ?: error("Invalid state")
        val enterFeeTransactionData = yieldLendingTransactionRepository.createEnterTransaction(
            userWalletId = params.userWalletId,
            cryptoCurrency = cryptoCurrency,
            yieldLendingStatus = yieldLendingStatus,
            fee = null,
        )
        loadFee(EnterStepType.Enter, enterFeeTransactionData)
    }

    private suspend fun enterTransaction() {
        Timber.i("Yield Lending App: Entering protocol")
        val yieldLendingStatus = cryptoCurrencyStatusFlow.value.value.yieldLendingStatus
            ?: error("Invalid state")
        val enterTransactionData = yieldLendingTransactionRepository.createEnterTransaction(
            userWalletId = params.userWalletId,
            cryptoCurrency = cryptoCurrency,
            yieldLendingStatus = yieldLendingStatus,
            fee = mainContent?.fee ?: error("Empty fee"),
        )

        sendTransactionUseCase(
            txData = enterTransactionData,
            userWallet = userWallet,
            network = cryptoCurrency.network,
        ).getOrElse {
            Timber.i("Yield Lending App: Failed to enter: $it")
            error(it)
        }
        onFinishedStep(EnterStepType.Enter)
    }
    //endregion

    private fun loadFee(
        currentStepType: EnterStepType,
        transactionData:
        TransactionData.Uncompiled,
    ) = runCatching {
        modelScope.launch {
            val transactionFee = getFeeUseCase(
                transactionData = transactionData,
                userWallet = userWallet,
                network = params.cryptoCurrency.network,
            ).getOrNull()
            onActiveStep(transactionFee = transactionFee, currentStepType = currentStepType)
        }
    }

    private suspend fun sendTransaction(transactionData: TransactionData.Uncompiled) {
        sendTransactionUseCase(
            txData = transactionData,
            userWallet = userWallet,
            network = params.cryptoCurrency.network,
        ).getOrElse {
            Timber.i("Yield Lending App: Failed to Deploy: $it")
        }
    }

    private fun startCheckingCompletionTask(task: suspend () -> Unit) {
        quoteTaskScheduler.cancelTask()
        quoteTaskScheduler.scheduleTask(
            scope = modelScope,
            task = PeriodicTask(
                delay = 5_000,
                isDelayFirst = true,
                task = {
                    runCatching { task() }
                },
                onSuccess = { /* no-op */ },
                onError = { /* no-op */ },
            ),
        )
    }

    private fun onFinishedStep(currentStepType: EnterStepType) {
        uiState.update {
            it.copy(
                bottomSheetConfig = it.bottomSheetConfig.copy(
                    content = YieldLendingStartEarningContentUM.Main(
                        currencyIconState = CryptoCurrencyToIconStateConverter().convert(params.cryptoCurrency),
                        currentStepType = currentStepType,
                        steps = mainContent?.steps?.map { step ->
                            if (step.stepType.ordinal < currentStepType.ordinal) {
                                step.copy(
                                    isActive = false,
                                    isComplete = true,
                                )
                            } else {
                                step
                            }
                        }.orEmpty().toPersistentList(),
                        fee = null,
                    )
                ),
            )
        }
    }

    private fun onActiveStep(
        currentStepType: EnterStepType,
        transactionFee: TransactionFee?,
    ) {
        uiState.update {
            it.copy(
                bottomSheetConfig = it.bottomSheetConfig.copy(
                    content = YieldLendingStartEarningContentUM.Main(
                        currencyIconState = CryptoCurrencyToIconStateConverter().convert(params.cryptoCurrency),
                        currentStepType = currentStepType,
                        steps = mainContent?.steps?.addOrReplace(
                            item = EnterStep(
                                stepType = currentStepType,
                                isActive = true,
                                isComplete = false,
                            ),
                            predicate = { it.stepType == currentStepType }
                        )?.map { step ->
                            if (step.stepType.ordinal < currentStepType.ordinal) {
                                step.copy(
                                    isActive = false,
                                    isComplete = true,
                                )
                            } else {
                                step
                            }
                        }.orEmpty().toPersistentList(),
                        fee = transactionFee?.normal,
                    )
                ),
            )
        }
    }
}