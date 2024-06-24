@file:Suppress("MagicNumber")

package com.tangem.tap.features.send.ui

import android.content.Context
import android.os.Bundle
import android.text.method.DigitsKeyListener
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.core.view.postDelayed
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.textfield.TextInputEditText
import com.tangem.Message
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.Analytics
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
import com.tangem.domain.qrscanning.usecases.ParseQrCodeUseCase
import com.tangem.domain.tokens.legacy.TradeCryptoAction
import com.tangem.sdk.extensions.hideSoftKeyboard
import com.tangem.tap.common.KeyboardObserver
import com.tangem.tap.common.analytics.events.Token
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.common.extensions.getFromClipboard
import com.tangem.tap.common.extensions.setOnImeActionListener
import com.tangem.tap.common.recyclerView.SpaceItemDecoration
import com.tangem.tap.common.snackBar.MaxAmountSnackbar
import com.tangem.tap.common.text.truncateMiddleWith
import com.tangem.tap.common.toggleWidget.IndeterminateProgressButtonWidget
import com.tangem.tap.common.toggleWidget.ViewStateWidget
import com.tangem.tap.features.BaseStoreFragment
import com.tangem.tap.features.addBackPressHandler
import com.tangem.tap.features.send.redux.*
import com.tangem.tap.features.send.redux.AddressActionUi.*
import com.tangem.tap.features.send.redux.AmountActionUi.*
import com.tangem.tap.features.send.redux.FeeActionUi.*
import com.tangem.tap.features.send.redux.states.FeeType
import com.tangem.tap.features.send.redux.states.MainCurrencyType
import com.tangem.tap.features.send.ui.adapters.WarningMessagesAdapter
import com.tangem.tap.features.send.ui.stateSubscribers.SendStateSubscriber
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentSendBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.DecimalFormatSymbols
import javax.inject.Inject

private const val EDIT_TEXT_INPUT_DEBOUNCE = 400L

/**
[REDACTED_AUTHOR]
 */
@Suppress("LargeClass")
@OptIn(FlowPreview::class)
@AndroidEntryPoint
class SendFragment : BaseStoreFragment(R.layout.fragment_send) {

    private val viewModel by viewModels<SendViewModel>()

    lateinit var sendBtn: ViewStateWidget

    private lateinit var etAmountToSend: TextInputEditText
    private lateinit var warningsAdapter: WarningMessagesAdapter

    private val sendSubscriber = SendStateSubscriber(this)
    private lateinit var keyboardObserver: KeyboardObserver

    val binding: FragmentSendBinding by viewBinding(FragmentSendBinding::bind)

    @Inject
    lateinit var listenToQrScanningUseCase: ListenToQrScanningUseCase

    @Inject
    lateinit var parseQrCodeUseCase: ParseQrCodeUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
        sendSubscriber.initViewModel(viewModel)
        Analytics.send(Token.Send.ScreenOpened())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                subscribeToTransactionExtrasFields()
                subscribeToAddressField()
                subscribeToAmountField()
            }
        }

        addBackPressHandler(this)

        etAmountToSend = view.findViewById(R.id.etAmountToSend)

        initSendButtonStates()
        setupAddressLayout()
        setupAmountLayout()
        setupFeeLayout()
        setupWarningMessages()
        store.dispatch(SendActionUi.CheckIfTransactionDataWasProvided)
    }

    private fun initSendButtonStates() = with(binding) {
        btnSend.setOnClickListener {
            store.dispatch(
                SendActionUi.SendAmountToRecipient(
                    Message(getString(R.string.initial_message_sign_header)),
                ),
            )
        }
        sendBtn = IndeterminateProgressButtonWidget(btnSend, progress)
    }

    private fun setupAddressLayout() = with(binding.lSendAddress) {
        store.dispatch(SetTruncateHandler { etAddress.truncateMiddleWith(it, "...") })
        store.dispatch(CheckClipboard(requireContext().getFromClipboard()?.toString()))

        etAddress.apply {
            setOnSystemPasteButtonClickListener {
                store.dispatch(
                    PasteAddress(
                        data = requireContext().getFromClipboard()?.toString() ?: "",
                        sourceType = Token.Send.AddressEntered.SourceType.PastePopup,
                    ),
                )
            }

            setOnFocusChangeListener { _, hasFocus ->
                store.dispatch(TruncateOrRestore(!hasFocus))
            }
        }

        imvPaste.setOnClickListener {
            Analytics.send(Token.Send.ButtonPaste())
            store.dispatch(
                PasteAddress(
                    data = requireContext().getFromClipboard()?.toString() ?: "",
                    sourceType = Token.Send.AddressEntered.SourceType.PasteButton,
                ),
            )
            store.dispatch(TruncateOrRestore(!etAddress.isFocused))
        }
        imvQrCode.setOnClickListener {
            Analytics.send(Token.Send.ButtonQRCode())

            store.dispatchNavigationAction {
                push(AppRoute.QrScanning(source = SourceType.SEND))
            }
        }
    }

    private fun CoroutineScope.subscribeToAddressField() = with(binding.lSendAddress) {
        etAddress.inputtedTextAsFlow()
            .debounce(EDIT_TEXT_INPUT_DEBOUNCE)
            .filter { store.state.sendState.addressState.viewFieldValue.value != it }
            .onEach {
                store.dispatch(AddressActionUi.HandleUserInput(it))
            }
            .launchIn(this@subscribeToAddressField)
    }

    private fun CoroutineScope.subscribeToAmountField() {
        etAmountToSend.inputtedTextAsFlow()
            .debounce(EDIT_TEXT_INPUT_DEBOUNCE)
            .filter { store.state.sendState.amountState.viewAmountValue.value != it && it.isNotEmpty() }
            .onEach { store.dispatch(AmountActionUi.HandleUserInput(it)) }
            .launchIn(this)
    }

    private fun CoroutineScope.subscribeToTransactionExtrasFields() = with(binding.lSendAddress) {
        // TODO: [REDACTED_TASK_KEY]
        etXlmMemo.inputtedTextAsFlow()
            .debounce(EDIT_TEXT_INPUT_DEBOUNCE)
            .filter {
                val info = store.state.sendState.transactionExtrasState
                info.xlmMemo?.viewFieldValue?.value != it
            }
            .onEach { store.dispatch(TransactionExtrasAction.XlmMemo.HandleUserInput(it)) }
            .launchIn(this@subscribeToTransactionExtrasFields)

        etDestinationTag.inputtedTextAsFlow()
            .debounce(EDIT_TEXT_INPUT_DEBOUNCE)
            .filter {
                val info = store.state.sendState.transactionExtrasState
                info.xrpDestinationTag?.viewFieldValue?.value != it
            }
            .onEach { store.dispatch(TransactionExtrasAction.XrpDestinationTag.HandleUserInput(it)) }
            .launchIn(this@subscribeToTransactionExtrasFields)

        etBinanceMemo.inputtedTextAsFlow()
            .debounce(EDIT_TEXT_INPUT_DEBOUNCE)
            .filter {
                val info = store.state.sendState.transactionExtrasState
                info.binanceMemo?.viewFieldValue?.value != it
            }
            .onEach { store.dispatch(TransactionExtrasAction.BinanceMemo.HandleUserInput(it)) }
            .launchIn(this@subscribeToTransactionExtrasFields)

        etTonMemo.inputtedTextAsFlow()
            .debounce(EDIT_TEXT_INPUT_DEBOUNCE)
            .filter {
                val info = store.state.sendState.transactionExtrasState
                info.tonMemoState?.viewFieldValue?.value != it
            }
            .onEach { store.dispatch(TransactionExtrasAction.TonMemo.HandleUserInput(it)) }
            .launchIn(this@subscribeToTransactionExtrasFields)

        etCosmosMemo.inputtedTextAsFlow()
            .debounce(EDIT_TEXT_INPUT_DEBOUNCE)
            .filter {
                val info = store.state.sendState.transactionExtrasState
                info.cosmosMemoState?.viewFieldValue?.value != it
            }
            .onEach { store.dispatch(TransactionExtrasAction.CosmosMemo.HandleUserInput(it)) }
            .launchIn(this@subscribeToTransactionExtrasFields)

        etHederaMemo.inputtedTextAsFlow()
            .debounce(EDIT_TEXT_INPUT_DEBOUNCE)
            .filter {
                val info = store.state.sendState.transactionExtrasState
                info.hederaMemoState?.viewFieldValue?.value != it
            }
            .onEach { store.dispatch(TransactionExtrasAction.HederaMemo.HandleUserInput(it)) }
            .launchIn(this@subscribeToTransactionExtrasFields)

        etAlgorandMemo.inputtedTextAsFlow()
            .debounce(EDIT_TEXT_INPUT_DEBOUNCE)
            .filter {
                val info = store.state.sendState.transactionExtrasState
                info.algorandMemoState?.viewFieldValue?.value != it
            }
            .onEach { store.dispatch(TransactionExtrasAction.AlgorandMemo.HandleUserInput(it)) }
            .launchIn(this@subscribeToTransactionExtrasFields)
    }

    private fun setupAmountLayout() {
        store.dispatch(SetMainCurrency(restoreMainCurrency()))
        store.dispatch(ReceiptAction.RefreshReceipt)
        store.dispatch(SendAction.ChangeSendButtonState(store.state.sendState.getButtonState()))

        val decimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator.toString()
        store.dispatch(AmountAction.SetDecimalSeparator(decimalSeparator))

        binding.lSendAmount.tvAmountCurrency.setOnClickListener {
            Analytics.send(Token.Send.ButtonSwapCurrency())
            store.dispatch(ToggleMainCurrency)
            store.dispatch(ReceiptAction.RefreshReceipt)
            store.dispatch(SendAction.ChangeSendButtonState(store.state.sendState.getButtonState()))
        }

        val maxAmountSnackbar = MaxAmountSnackbar.make(etAmountToSend) {
            etAmountToSend.clearFocus()
            etAmountToSend.postDelayed(200) { etAmountToSend.hideSoftKeyboard() }
            store.dispatch(SetMaxAmount)
        }
        var snackbarControlledByChangingFocus = false
        keyboardObserver = KeyboardObserver(requireActivity())
        keyboardObserver.registerListener { isShow ->
            if (snackbarControlledByChangingFocus) return@registerListener

            if (isShow) {
                if (etAmountToSend.isFocused && !maxAmountSnackbar.isShown) maxAmountSnackbar.show()
            } else {
                if (maxAmountSnackbar.isShown) maxAmountSnackbar.dismiss()
            }
        }

        etAmountToSend.keyListener = DigitsKeyListener.getInstance("0123456789,.")
        etAmountToSend.setOnFocusChangeListener { _, hasFocus ->
            snackbarControlledByChangingFocus = true
            if (hasFocus) {
                etAmountToSend.postDelayed(200) {
                    maxAmountSnackbar.show()
                    snackbarControlledByChangingFocus = false
                }
            } else {
                etAmountToSend.postDelayed(350) {
                    maxAmountSnackbar.dismiss()
                    snackbarControlledByChangingFocus = false
                }
            }
        }

        val prevFocusChangeListener = etAmountToSend.onFocusChangeListener
        etAmountToSend.setOnFocusChangeListener { v, hasFocus ->
            prevFocusChangeListener.onFocusChange(v, hasFocus)
            if (hasFocus && etAmountToSend.text?.toString() == "0") etAmountToSend.setText("")
            if (!hasFocus && etAmountToSend.text?.toString() == "") etAmountToSend.setText("0")
        }

        etAmountToSend.setOnImeActionListener(EditorInfo.IME_ACTION_DONE) {
            it.hideSoftKeyboard()
            it.clearFocus()
        }
    }

    private fun setupFeeLayout() = with(binding.clNetworkFee) {
        flExpandCollapse.flExpandCollapse.setOnClickListener {
            store.dispatch(ToggleControlsVisibility)
        }
        chipGroup.check(FeeUiHelper.toId(FeeType.NORMAL))
        chipGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == -1) return@setOnCheckedChangeListener

            store.dispatch(ChangeSelectedFee(FeeUiHelper.toType(checkedId)))
            store.dispatch(CheckAmountToSend)
        }
        swIncludeFee.setOnCheckedChangeListener { _, isChecked ->
            store.dispatch(ChangeIncludeFee(isChecked))
            store.dispatch(CheckAmountToSend)
        }
    }

    private fun setupWarningMessages() = with(binding) {
        warningsAdapter = WarningMessagesAdapter()
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        rvWarningMessages.layoutManager = layoutManager
        rvWarningMessages.addItemDecoration(SpaceItemDecoration.all(16f))
        rvWarningMessages.adapter = warningsAdapter

        store.dispatch(SendAction.Warnings.Update)
    }

    override fun subscribeToStore() {
        store.subscribe(sendSubscriber) { appState ->
            appState.skipRepeats { oldState, newState ->
                oldState.sendState == newState.sendState
            }.select { it.sendState }
        }
        storeSubscribersList.add(sendSubscriber)
    }

    private fun restoreMainCurrency(): MainCurrencyType {
        val sp = requireContext().getSharedPreferences("SendScreen", Context.MODE_PRIVATE)
        val mainCurrency = sp.getString("mainCurrency", AppCurrency.Default.code)
        return MainCurrencyType.values()
            .firstOrNull { it.name.equals(mainCurrency!!, ignoreCase = true) }
            ?: MainCurrencyType.CRYPTO
    }

    fun saveMainCurrency(type: MainCurrencyType) {
        val sp = requireContext().getSharedPreferences("SendScreen", Context.MODE_PRIVATE)
        sp.edit().putString("mainCurrency", type.name).apply()
    }

    override fun handleOnBackPressed() {
        val externalTransactionData = store.state.sendState.externalTransactionData
        if (externalTransactionData == null) {
            store.dispatchNavigationAction(AppRouter::pop)
        } else {
            store.dispatch(TradeCryptoAction.FinishSelling(externalTransactionData.transactionId))
        }
    }

    override fun onDestroyView() {
        keyboardObserver.unregisterListener()
        super.onDestroyView()
    }

    override fun onDestroy() {
        store.dispatch(ReleaseSendState)
        lifecycle.removeObserver(viewModel)
        super.onDestroy()
    }
}

fun EditText.inputtedTextAsFlow(): Flow<String> = callbackFlow {
    val watcher = addTextChangedListener { editable -> trySend(editable?.toString() ?: "") }
    awaitClose { removeTextChangedListener(watcher) }
}

object FeeUiHelper {
    fun toId(fee: FeeType): Int {
        return when (fee) {
            FeeType.SINGLE -> View.NO_ID
            FeeType.LOW -> R.id.chipLow
            FeeType.NORMAL -> R.id.chipNormal
            FeeType.PRIORITY -> R.id.chipPriority
        }
    }

    fun toType(id: Int): FeeType {
        return when (id) {
            R.id.chipLow -> FeeType.LOW
            R.id.chipNormal -> FeeType.NORMAL
            R.id.chipPriority -> FeeType.PRIORITY
            else -> FeeType.NORMAL
        }
    }
}