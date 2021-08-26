package com.tangem.tap.features.send.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.DigitsKeyListener
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.core.view.postDelayed
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.tangem.Message
import com.tangem.tangem_sdk_new.extensions.dpToPx
import com.tangem.tangem_sdk_new.extensions.hideSoftKeyboard
import com.tangem.tap.common.KeyboardObserver
import com.tangem.tap.common.entities.TapCurrency
import com.tangem.tap.common.extensions.getFromClipboard
import com.tangem.tap.common.extensions.setOnImeActionListener
import com.tangem.tap.common.qrCodeScan.ScanQrCodeActivity
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.common.snackBar.MaxAmountSnackbar
import com.tangem.tap.common.text.truncateMiddleWith
import com.tangem.tap.common.toggleWidget.*
import com.tangem.tap.features.send.BaseStoreFragment
import com.tangem.tap.features.send.redux.*
import com.tangem.tap.features.send.redux.AddressPayIdActionUi.*
import com.tangem.tap.features.send.redux.AmountActionUi.*
import com.tangem.tap.features.send.redux.FeeActionUi.*
import com.tangem.tap.features.send.redux.states.FeeType
import com.tangem.tap.features.send.redux.states.MainCurrencyType
import com.tangem.tap.features.send.ui.stateSubscribers.SendStateSubscriber
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.ui.adapters.SpacesItemDecoration
import com.tangem.tap.features.wallet.ui.adapters.WarningMessagesAdapter
import com.tangem.tap.mainScope
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_send.*
import kotlinx.android.synthetic.main.fragment_send.rv_warning_messages
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.layout_send_address_payid.*
import kotlinx.android.synthetic.main.layout_send_amount.*
import kotlinx.android.synthetic.main.layout_send_fee.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.text.DecimalFormatSymbols

/**
[REDACTED_AUTHOR]
 */
class SendFragment : BaseStoreFragment(R.layout.fragment_send) {

    lateinit var sendBtn: ViewStateWidget

    private lateinit var etAmountToSend: TextInputEditText
    private lateinit var warningsAdapter: WarningMessagesAdapter

    private val sendSubscriber = SendStateSubscriber(this)
    private lateinit var keyboardObserver: KeyboardObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val externalTransactionData = store.state.sendState.externalTransactionData
                if (externalTransactionData == null) {
                    store.dispatch(NavigationAction.PopBackTo())
                } else {
                    store.dispatch(
                        WalletAction.TradeCryptoAction.FinishSelling(externalTransactionData.transactionId)
                    )
                }
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etAmountToSend = view.findViewById(R.id.etAmountToSend)

        initSendButtonStates()
        setupAddressOrPayIdLayout()
        setupTransactionExtrasLayout()
        setupAmountLayout()
        setupFeeLayout()
        setupWarningMessages()
        store.dispatch(SendActionUi.CheckIfTransactionDataWasProvided)
    }

    private fun initSendButtonStates() {
        btnSend.setOnClickListener {
            store.dispatch(SendActionUi.SendAmountToRecipient(
                    Message(getString(R.string.initial_message_sign_header))
            ))
        }
        sendBtn = IndeterminateProgressButtonWidget(btnSend, progress)
    }

    private fun setupAddressOrPayIdLayout() {
        store.dispatch(SetTruncateHandler { etAddressOrPayId.truncateMiddleWith(it, "...") })
        store.dispatch(CheckClipboard(requireContext().getFromClipboard()?.toString()))

        etAddressOrPayId.setOnFocusChangeListener { v, hasFocus ->
            store.dispatch(TruncateOrRestore(!hasFocus))
        }
        etAddressOrPayId.inputtedTextAsFlow()
                .debounce(400)
                .filter { store.state.sendState.addressPayIdState.viewFieldValue.value != it }
                .onEach {
                    store.dispatch(AddressPayIdActionUi.HandleUserInput(it))
                }
                .launchIn(mainScope)

        imvPaste.setOnClickListener {
            store.dispatch(PasteAddressPayId(requireContext().getFromClipboard()?.toString() ?: ""))
            store.dispatch(TruncateOrRestore(!etAddressOrPayId.isFocused))
        }
        imvQrCode.setOnClickListener {
            startActivityForResult(
                    Intent(requireContext(), ScanQrCodeActivity::class.java),
                    ScanQrCodeActivity.SCAN_QR_REQUEST_CODE
            )
        }
    }

    private fun setupTransactionExtrasLayout() {
        etMemo.inputtedTextAsFlow()
                .debounce(400)
                .filter {
                    val info = store.state.sendState.transactionExtrasState
                    info.xlmMemo?.viewFieldValue?.value != it
                }
                .onEach { store.dispatch(TransactionExtrasAction.XlmMemo.HandleUserInput(it)) }
                .launchIn(mainScope)

        etDestinationTag.inputtedTextAsFlow()
                .debounce(400)
                .filter {
                    val info = store.state.sendState.transactionExtrasState
                    info.xrpDestinationTag?.viewFieldValue?.value != it
                }
                .onEach { store.dispatch(TransactionExtrasAction.XrpDestinationTag.HandleUserInput(it)) }
                .launchIn(mainScope)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != ScanQrCodeActivity.SCAN_QR_REQUEST_CODE) return

        val scannedCode = data?.getStringExtra(ScanQrCodeActivity.SCAN_RESULT) ?: ""
        if (scannedCode.isEmpty()) return

        // Delayed launch is needed in order for the UI to be drawn and to process the sent events.
        // If do not use the delay, then etAmount error field is not displayed when
        // inserting an incorrect amount by shareUri
        imvQrCode.postDelayed({
            store.dispatch(PasteAddressPayId(scannedCode))
            store.dispatch(TruncateOrRestore(!etAddressOrPayId.isFocused))
        }, 200)
    }

    private fun setupAmountLayout() {
        store.dispatch(SetMainCurrency(restoreMainCurrency()))
        store.dispatch(ReceiptAction.RefreshReceipt)
        store.dispatch(SendAction.ChangeSendButtonState(store.state.sendState.getButtonState()))

        val decimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator.toString()
        store.dispatch(AmountAction.SetDecimalSeparator(decimalSeparator))

        tvAmountCurrency.setOnClickListener {
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
        etAmountToSend.setOnFocusChangeListener { v, hasFocus ->
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

        etAmountToSend.inputtedTextAsFlow()
                .debounce(400)
                .filter { store.state.sendState.amountState.viewAmountValue.value != it && it.isNotEmpty() }
                .onEach { store.dispatch(AmountActionUi.HandleUserInput(it)) }
                .launchIn(mainScope)

        etAmountToSend.setOnImeActionListener(EditorInfo.IME_ACTION_DONE) {
            it.hideSoftKeyboard()
            it.clearFocus()
        }
    }

    private fun setupFeeLayout() {
        flExpandCollapse.setOnClickListener {
            store.dispatch(ToggleControlsVisibility)
        }
        chipGroup.check(FeeUiHelper.toId(FeeType.NORMAL))
        chipGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == -1) return@setOnCheckedChangeListener

            store.dispatch(ChangeSelectedFee(FeeUiHelper.toType(checkedId)))
            store.dispatch(CheckAmountToSend)
        }
        swIncludeFee.setOnCheckedChangeListener { btn, isChecked ->
            store.dispatch(ChangeIncludeFee(isChecked))
            store.dispatch(CheckAmountToSend)
        }
    }

    private fun setupWarningMessages() {
        warningsAdapter = WarningMessagesAdapter()
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        rv_warning_messages.layoutManager = layoutManager
        rv_warning_messages.addItemDecoration(SpacesItemDecoration(rv_warning_messages.dpToPx(16f).toInt()))
        rv_warning_messages.adapter = warningsAdapter

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
        val mainCurrency = sp.getString("mainCurrency", TapCurrency.DEFAULT_FIAT_CURRENCY)
        val foundType = MainCurrencyType.values()
                .firstOrNull { it.name.equals(mainCurrency!!, ignoreCase = true) }
                ?: MainCurrencyType.CRYPTO
        return foundType
    }

    fun saveMainCurrency(type: MainCurrencyType) {
        val sp = requireContext().getSharedPreferences("SendScreen", Context.MODE_PRIVATE)
        sp.edit().putString("mainCurrency", type.name).apply()
    }

    override fun onDestroy() {
        keyboardObserver.unregisterListener()
        store.dispatch(ReleaseSendState)
        super.onDestroy()
    }
}

@ExperimentalCoroutinesApi
fun EditText.inputtedTextAsFlow(): Flow<String> = callbackFlow {
    val watcher = addTextChangedListener { editable -> offer(editable?.toString() ?: "") }
    awaitClose { removeTextChangedListener(watcher) }
}

class FeeUiHelper {
    companion object {
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
}