package com.tangem.tangem_sdk_new

import androidx.fragment.app.FragmentActivity
import com.tangem.*
import com.tangem.tangem_sdk_new.nfc.NfcReader
import com.tangem.tangem_sdk_new.ui.NfcSessionDialog

/**
 * Default implementation of [SessionViewDelegate].
 * If no customisation is required, this is the preferred way to use Tangem SDK.
 */
class DefaultSessionViewDelegate(private val reader: NfcReader) : SessionViewDelegate {

    lateinit var activity: FragmentActivity
    private var readingDialog: NfcSessionDialog? = null

    init {
        setLogger()
    }

    override fun onSessionStarted(cardId: String?, message: Message?) {
        postUI { showReadingDialog(activity, cardId, message) }
    }

    private fun showReadingDialog(activity: FragmentActivity, cardId: String?, message: Message?) {
        val dialogView = activity.layoutInflater.inflate(R.layout.nfc_bottom_sheet, null)
        readingDialog = NfcSessionDialog(activity)
        readingDialog?.setContentView(dialogView)
        readingDialog?.dismissWithAnimation = true
        readingDialog?.create()
        readingDialog?.setOnShowListener {
            readingDialog?.show(SessionViewDelegateState.Ready(cardId, message))
        }
        readingDialog?.setOnCancelListener { reader.stopSession(true) }
        readingDialog?.show()
    }

    override fun onSecurityDelay(ms: Int, totalDurationSeconds: Int) {
        postUI {
            readingDialog?.show(SessionViewDelegateState.SecurityDelay(ms, totalDurationSeconds))
        }
    }

    override fun onDelay(total: Int, current: Int, step: Int) {
        postUI {
            readingDialog?.show(SessionViewDelegateState.Delay(total, current, step))
        }
    }

    override fun onTagLost() {
        postUI { readingDialog?.show(SessionViewDelegateState.TagLost) }

    }

    override fun onTagConnected() {
        postUI { readingDialog?.show(SessionViewDelegateState.TagConnected) }
    }

    override fun onWrongCard() {
        postUI { readingDialog?.show(SessionViewDelegateState.WrongCard) }
    }

    override fun onSessionStopped(message: Message?) {
        postUI { readingDialog?.show(SessionViewDelegateState.Success(message)) }
    }

    override fun onError(error: TangemSdkError) {
        postUI { readingDialog?.show(SessionViewDelegateState.Error(error)) }
    }

    override fun onPinRequested(callback: (pin: String?) -> Unit) {
        postUI { readingDialog?.show(SessionViewDelegateState.PinRequested(callback)) }
    }

    private fun setLogger() {
        Log.setLogger(
                object : LoggerInterface {
                    override fun i(logTag: String, message: String) {
                        android.util.Log.i(logTag, message)
                    }

                    override fun e(logTag: String, message: String) {
                        android.util.Log.e(logTag, message)
                    }

                    override fun v(logTag: String, message: String) {
                        android.util.Log.v(logTag, message)
                    }
                }
        )
    }
}