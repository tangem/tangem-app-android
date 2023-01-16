@file:Suppress("MagicNumber")

package com.tangem.tap.features.send.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.DigitsKeyListener
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.core.view.postDelayed
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.textfield.TextInputEditText
import com.tangem.Message
import com.tangem.core.analytics.Analytics
import com.tangem.tangem_sdk_new.extensions.hideSoftKeyboard
import com.tangem.tap.common.KeyboardObserver
import com.tangem.tap.common.analytics.events.Token
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.common.extensions.getFromClipboard
import com.tangem.tap.common.extensions.setOnImeActionListener
import com.tangem.tap.common.qrCodeScan.ScanQrCodeActivity
import com.tangem.tap.common.recyclerView.SpaceItemDecoration
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.common.snackBar.MaxAmountSnackbar
import com.tangem.tap.common.text.truncateMiddleWith
import com.tangem.tap.common.toggleWidget.IndeterminateProgressButtonWidget
import com.tangem.tap.common.toggleWidget.ViewStateWidget
import com.tangem.tap.features.BaseStoreFragment
import com.tangem.tap.features.addBackPressHandler
import com.tangem.tap.features.send.redux.AddressPayIdActionUi
import com.tangem.tap.features.send.redux.AddressPayIdActionUi.CheckClipboard
import com.tangem.tap.features.send.redux.AddressPayIdActionUi.PasteAddressPayId
import com.tangem.tap.features.send.redux.AddressPayIdActionUi.SetTruncateHandler
import com.tangem.tap.features.send.redux.AddressPayIdActionUi.TruncateOrRestore
import com.tangem.tap.features.send.redux.AmountAction
import com.tangem.tap.features.send.redux.AmountActionUi
import com.tangem.tap.features.send.redux.AmountActionUi.CheckAmountToSend
import com.tangem.tap.features.send.redux.AmountActionUi.SetMainCurrency
import com.tangem.tap.features.send.redux.AmountActionUi.SetMaxAmount
import com.tangem.tap.features.send.redux.AmountActionUi.ToggleMainCurrency
import com.tangem.tap.features.send.redux.FeeActionUi.ChangeIncludeFee
import com.tangem.tap.features.send.redux.FeeActionUi.ChangeSelectedFee
import com.tangem.tap.features.send.redux.FeeActionUi.ToggleControlsVisibility
import com.tangem.tap.features.send.redux.ReceiptAction
import com.tangem.tap.features.send.redux.ReleaseSendState
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.features.send.redux.SendActionUi
import com.tangem.tap.features.send.redux.TransactionExtrasAction
import com.tangem.tap.features.send.redux.states.FeeType
import com.tangem.tap.features.send.redux.states.MainCurrencyType
import com.tangem.tap.features.send.ui.stateSubscribers.SendStateSubscriber
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.ui.adapters.WarningMessagesAdapter
import com.tangem.tap.mainScope
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentSendBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.text.DecimalFormatSymbols

/**
 * Created by Anton Zhilenkov on 31/08/2020.
 */
class SendFragment : BaseStoreFragment(R.layout.fragment_send) {

    lateinit var sendBtn: ViewStateWidget

    private lateinit var etAmountToSend: TextInputEditText
    private lateinit var warningsAdapter: WarningMessagesAdapter

    private val sendSubscriber = SendStateSubscriber(this)
    private lateinit var keyboardObserver: KeyboardObserver

    val binding: FragmentSendBinding by viewBinding(FragmentSendBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Analytics.send(Token.Send.ScreenOpened())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addBackPressHandler(this)

        etAmountToSend = view.findViewById(R.id.etAmountToSend)

        initSendButtonStates()
        setupAddressOrPayIdLayout()
        setupTransactionExtrasLayout()
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

    @Suppress("MagicNumber")
    private fun setupAddressOrPayIdLayout() = with(binding.lSendAddressPayid) {
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
            Analytics.send(Token.Send.ButtonPaste())
            store.dispatch(PasteAddressPayId(requireContext().getFromClipboard()?.toString() ?: ""))
            store.dispatch(TruncateOrRestore(!etAddressOrPayId.isFocused))
        }
        imvQrCode.setOnClickListener {
            Analytics.send(Token.Send.ButtonQRCode())
            startActivityForResult(
                Intent(requireContext(), ScanQrCodeActivity::class.java),
                ScanQrCodeActivity.SCAN_QR_REQUEST_CODE,
            )
        }
    }

    private fun setupTransactionExtrasLayout() = with(binding.lSendAddressPayid) {
        etXlmMemo.inputtedTextAsFlow()
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

        etBinanceMemo.inputtedTextAsFlow()
            .debounce(400)
            .filter {
                val info = store.state.sendState.transactionExtrasState
                info.binanceMemo?.viewFieldValue?.value != it
            }
            .onEach { store.dispatch(TransactionExtrasAction.BinanceMemo.HandleUserInput(it)) }
            .launchIn(mainScope)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != ScanQrCodeActivity.SCAN_QR_REQUEST_CODE) return

        val scannedCode = data?.getStringExtra(ScanQrCodeActivity.SCAN_RESULT) ?: ""
        if (scannedCode.isEmpty()) return

        // Delayed launch is needed in order for the UI to be drawn and to process the sent events.
        // If do not use the delay, then etAmount error field is not displayed when
        // inserting an incorrect amount by shareUri
        binding.lSendAddressPayid.imvQrCode.postDelayed(
            {
                store.dispatch(PasteAddressPayId(scannedCode))
                store.dispatch(TruncateOrRestore(!binding.lSendAddressPayid.etAddressOrPayId.isFocused))
            },
            200,
        )
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

    private fun setupFeeLayout() = with(binding.clNetworkFee) {
        flExpandCollapse.flExpandCollapse.setOnClickListener {
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
        val mainCurrency = sp.getString("mainCurrency", FiatCurrency.Default.code)
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
            store.dispatch(NavigationAction.PopBackTo())
        } else {
            store.dispatch(
                WalletAction.TradeCryptoAction.FinishSelling(externalTransactionData.transactionId),
            )
        }
    }

    override fun onDestroyView() {
        keyboardObserver.unregisterListener()
        super.onDestroyView()
    }

    override fun onDestroy() {
        store.dispatch(ReleaseSendState)
        super.onDestroy()
    }
}

@ExperimentalCoroutinesApi
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
