package com.tangem.presentation.dialog

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AppCompatDialogFragment
import android.widget.ProgressBar
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.dialog_wait_pin2.*
import java.util.*

class WaitSecurityDelayDialogNew : AppCompatDialogFragment() {

    companion object {
        val TAG: String = WaitSecurityDelayDialogNew::class.java.simpleName

        private const val MIN_REMAINING_DELAY_TO_SHOW_DIALOG = 1000
        private const val DELAY_BEFORE_SHOW_DIALOG = 5000
    }

    private lateinit var pb: ProgressBar

    private var msTimeout = 60000
    private var msProgress = 0
    private var timer: Timer? = null
    private var timerToShowDelayDialog: Timer? = null

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = activity!!.layoutInflater
        val v = inflater.inflate(R.layout.dialog_wait_pin2, null)

        isCancelable = false

        pb = v.findViewById(R.id.progressBar)


        pb.max = msTimeout
        pb.progress = msProgress

        timer = Timer()
        timer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                pb.post {
                    val progress = pb.progress
                    if (progress < pb.max)
                        pb.progress = progress + 1000
                }
            }
        }, 1000, 1000)

        return AlertDialog.Builder(activity)
                .setIcon(R.drawable.tangem_logo_small_new)
                .setTitle(R.string.security_delay)
                .setView(v)
                .setCancelable(false)
                .create()
    }

    fun onReadBeforeRequest(timeout: Int) {
        if (timerToShowDelayDialog != null || timeout < DELAY_BEFORE_SHOW_DIALOG + MIN_REMAINING_DELAY_TO_SHOW_DIALOG)
            return

        timerToShowDelayDialog = Timer()
        timerToShowDelayDialog!!.schedule(object : TimerTask() {
            override fun run() {
                setup(timeout, DELAY_BEFORE_SHOW_DIALOG)
                isCancelable = false
                show(activity?.supportFragmentManager, TAG)
            }
        }, DELAY_BEFORE_SHOW_DIALOG.toLong())

    }

    fun onReadAfterRequest() {
        if (timerToShowDelayDialog == null)
            return

        timerToShowDelayDialog!!.cancel()
        timerToShowDelayDialog = null
    }

    fun onReadWait(msec: Int) {
        if (timerToShowDelayDialog != null) {
            timerToShowDelayDialog!!.cancel()
            timerToShowDelayDialog = null
        }

        if (msec == 0) {
            dismiss()
            return
        }

        if (msec > MIN_REMAINING_DELAY_TO_SHOW_DIALOG) {
            // 1000ms - card delay notification interval
            setup(msec + 1000, 1000)
            isCancelable = false
            show(activity?.supportFragmentManager, TAG)

        } else
            setRemainingTimeout(msec)
    }

    private fun setup(msTimeout: Int, msProgress: Int) {
        this.msTimeout = msTimeout
        this.msProgress = msProgress
    }

    private fun setRemainingTimeout(msec: Int) {
        pb.post {
            val progress = pb.progress
            if (timer != null) {

                // we get delay latency from card for first time - don't change progress by timer, only by card answer
                pb.max = progress + msec
                timer!!.cancel()
                timer = null
            } else {
                val newProgress = pb.max - msec
                if (pb.max > progress)
                    progressBar.progress = newProgress
                else
                    pb.max = progress + msec
            }
        }
    }

}