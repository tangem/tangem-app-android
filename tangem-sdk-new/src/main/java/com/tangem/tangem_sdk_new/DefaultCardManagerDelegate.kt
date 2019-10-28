package com.tangem.tangem_sdk_new

import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tangem.CardManagerDelegate
import com.tangem.Log
import com.tangem.tasks.Task
import com.tangem.tasks.TaskError
import kotlinx.android.synthetic.main.layout_touch_card.*


class DefaultCardManagerDelegate(private val activity: FragmentActivity, private val reader: NfcReader) : CardManagerDelegate {

    private var dialog: BottomSheetDialog? = null

    private var task: Task<*>? = null

    override fun showSecurityDelay(ms: Int) {
//        activity.runOnUiThread{
//            activity.tv_security_delay?.text = "Security delay. $ms left."
//        }
    }

    override fun requestPin(success: () -> String, error: (cardError: TaskError.CardError) -> TaskError.CardError) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun openNfcPopup() {


        activity.runOnUiThread {
            val dialogView = activity.getLayoutInflater().inflate(R.layout.nfc_bottom_sheet, null)

            dialog = BottomSheetDialog(activity)
            dialog?.setContentView(dialogView)
            dialog?.dismissWithAnimation
            dialog?.create()
//                .setTitle("NFC reader mode")
            dialog?.setOnShowListener {
                    dialog!!.rippleBackgroundNfc?.startRippleAnimation()

                    // init NFC Antenna
                    val nfcDeviceAntenna = TouchCardAnimation(
                            activity, dialog!!.ivHandCardHorizontal,
                            dialog!!.ivHandCardVertical, dialog!!.llHand, dialog!!.llNfc)
                    nfcDeviceAntenna.init()
            }
            dialog?.setOnDismissListener {
                reader.readingCancelled = false
                task = null
                Log.i(this::class.simpleName!!, "readingCancelled is set to true")
            }
            dialog?.show()
        }
        Log.i(this::class.simpleName!!, "Nfc PopUp is opened")
    }

    override fun closeNfcPopup() {
        task = null
        activity.runOnUiThread {
            dialog?.dismiss()
        }
        Log.i(this::class.simpleName!!, "Nfc PopUp is closed")

    }
}