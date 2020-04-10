package com.tangem.tangemtest

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tangem.CardEnvironment
import com.tangem.SessionError
import com.tangem.TangemSdk
import com.tangem.common.CompletionResult
import com.tangem.tangem_sdk_new.extensions.init
import kotlinx.android.synthetic.main.activity_test_user_data.*
import java.nio.charset.StandardCharsets

/**
[REDACTED_AUTHOR]
 */
class TestUserDataActivity : AppCompatActivity() {

    private lateinit var tangemSdk: TangemSdk
    private lateinit var writeOptions: WriteOptions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_user_data)

        init()
        initWriteOptions()
    }

    private fun init() {
        tangemSdk = TangemSdk.init(this)

        btn_scan?.setOnClickListener { _ ->
            tangemSdk.scanCard { taskEvent ->
                when (taskEvent) {
                    is CompletionResult.Success -> {
                        // Handle returned card data
                        writeOptions.cardId = taskEvent.data.cardId
                        runOnUiThread { showReadWriteSection(true) }
                    }
                }
            }
        }

        btn_write.setOnClickListener {
            if (writeOptions.cardId == null) return@setOnClickListener

            tangemSdk.writeUserData(
                    writeOptions.cardId!!,
                    writeOptions.userData,
                    writeOptions.userProtectedData,
                    writeOptions.userCounter,
                    writeOptions.userProtectedCounter
            ) {
                when (it) {
                    is CompletionResult.Failure -> handleError(tv_write_result, it.error)
                    is CompletionResult.Success -> {
                        runOnUiThread { tv_write_result?.text = "Success" }
                    }
                }
            }
        }

        btn_read.setOnClickListener {
            if (writeOptions.cardId == null) return@setOnClickListener

            tangemSdk.readUserData(writeOptions.cardId!!) {
                when (it) {
                    is CompletionResult.Failure -> handleError(tv_write_result, it.error)
                    is CompletionResult.Success -> {
                        runOnUiThread {

                            tv_read_result?.text = "Success"

                            writeOptions.userData = it.data.userData
                            writeOptions.userProtectedData = it.data.userProtectedData
                            writeOptions.userCounter = it.data.userCounter
                            writeOptions.userProtectedCounter = it.data.userProtectedCounter

                            tv_card_cid.text = it.data.cardId
                            tv_data.text = String(it.data.userData, StandardCharsets.US_ASCII)
                            tv_protected_data.text = String(it.data.userProtectedData, StandardCharsets.US_ASCII)
                            tv_counter.text = it.data.userCounter.toString()
                            tv_protected_counter.text = it.data.userProtectedCounter.toString()
                        }

                    }
                }
            }
        }
    }

    private fun handleError(tv: TextView, error: SessionError) {
        if (error is SessionError.UserCancelled) return

        runOnUiThread { tv.text = error::class.simpleName }
    }

    private fun initWriteOptions() {
        writeOptions = WriteOptions()

        chb_with_ud.setOnCheckedChangeListener { buttonView, isChecked -> writeOptions.updateData(buttonView) }
        chb_with_ud_protected.setOnCheckedChangeListener { buttonView, isChecked -> writeOptions.updateProtectedData(buttonView) }
        chb_with_counter.setOnCheckedChangeListener { buttonView, isChecked -> writeOptions.updateCounter(buttonView) }
        chb_with_protected_counter.setOnCheckedChangeListener { buttonView, isChecked -> writeOptions.updateProtectedCounter(buttonView) }
        chb_with_pin2.setOnCheckedChangeListener { buttonView, isChecked -> writeOptions.updatePin2(buttonView) }
    }

    private fun showReadWriteSection(show: Boolean) {
        val state = if (show) View.VISIBLE else View.GONE
        cl_read_write.visibility = state
    }
}

class WriteOptions {
    var cardId: String? = null
    var userData: ByteArray? = null
    var userProtectedData: ByteArray? = null
    var userCounter: Int? = null
    var userProtectedCounter: Int? = null
    var pin2: String? = null

    fun updateData(chbx: CompoundButton) {
        val value = "simple user data".toByteArray()
        userData = if (chbx.isChecked) value else null
    }

    fun updateProtectedData(chbx: CompoundButton) {
        val value = "protected user data".toByteArray()
        userProtectedData = if (chbx.isChecked) value else null
    }

    fun updateCounter(chbx: CompoundButton) {
        val value = if (userCounter == null) 0 else userCounter!! + 1
        userCounter = if (chbx.isChecked) value else null
    }

    fun updateProtectedCounter(chbx: CompoundButton) {
        val value = if (userProtectedCounter == null) 0 else userProtectedCounter!! + 1
        userProtectedCounter = if (chbx.isChecked) value else null
    }

    fun updatePin2(chbx: CompoundButton) {
        val value = CardEnvironment.DEFAULT_PIN2
        pin2 = if (chbx.isChecked) value else null
    }
}