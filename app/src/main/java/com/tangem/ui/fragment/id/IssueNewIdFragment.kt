package com.tangem.ui.fragment.id

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import com.tangem.tangem_card.util.Util
import com.tangem.ui.fragment.BaseFragment
import com.tangem.ui.navigation.NavigationResultListener
import com.tangem.wallet.CoinEngineFactory
import com.tangem.wallet.R
import com.tangem.wallet.TangemContext
import com.tangem.wallet.ethID.EthIdEngine
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_issue_new_id.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*


class IssueNewIdFragment : BaseFragment(), NavigationResultListener {
    lateinit var ctx: TangemContext
    private var photo: Bitmap? = null
    private var photoInBytes: ByteArray? = null
    private var gender: Gender? = null
    private var userIdData: UserIdData? = null

    override val layoutId = R.layout.fragment_issue_new_id

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ctx = TangemContext.loadFromBundle(context, arguments)

        ivPhoto?.setOnClickListener {
            navigateForResult(GET_PHOTO_REQUEST_CODE, R.id.action_issueNewIdFragment_to_cameraFragment)
        }

        if (photo != null) ivPhoto?.setImageBitmap(photo)

        btnConfirm?.setOnClickListener {
            if (!checkIfInfoProvided()) {
                Toast.makeText(context, "Fill in all data first", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            try {
                processData()
            } catch (exception: Exception) {
                Log.e(this.javaClass.simpleName,
                        "Exception while processing data: ${exception.localizedMessage}")
                Toast.makeText(context, "Check for correctness of data", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val bundle = bundleOf(USER_ID_DATA to userIdData)
            ctx.saveToBundle(bundle)
            navigateToDestination(R.id.action_issueNewIdFragment_to_validateIdFragment, bundle)
        }

        rbFemale?.setOnClickListener { onRadioButtonClicked(it) }
        rbMale?.setOnClickListener { onRadioButtonClicked(it) }

        DateInputMask(etBirthDate).listen()
//        etBirthDate?.addTextChangedListener(MaskWatcher("##/##/####"))

    }

    private fun onRadioButtonClicked(view: View) {
        if (view is RadioButton) {
            when (view.getId()) {
                R.id.rbFemale ->
                    if (view.isChecked) {
                        gender = Gender.F
                    }
                R.id.rbMale ->
                    if (view.isChecked) {
                        gender = Gender.M
                    }
            }
        }
    }

    override fun onNavigationResult(requestCode: String, resultCode: Int, data: Bundle?) {
        if (requestCode == GET_PHOTO_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                photo = data?.getParcelable<Bitmap>(CameraFragment.PHOTO_KEY)
                ivPhoto?.setImageBitmap(photo)
                val out = ByteArrayOutputStream()
                photo!!.compress(Bitmap.CompressFormat.JPEG, 100, out)
                photoInBytes = out.toByteArray()
            }
        }
    }


    private fun checkIfInfoProvided(): Boolean {
        return (etName.text.isNotBlank() && etSurname.text.isNotBlank() && etBirthDate.text.isNotBlank() &&
                (photo != null) && (gender != null))
    }

    private fun processData() {
        val name = etName.text.toString()
        val lastName = etSurname.text.toString()
        val birthDate = convertDate(etBirthDate.text)
        userIdData = UserIdData(name, lastName, birthDate, gender.toString(), photo!!)
        val issuerExpireDate = processIssueExpireDate()

        ctx.card.setTlvIDCardData(
                "$name $lastName",
                birthDate,
                gender.toString(),
                photoInBytes,
                issuerExpireDate.issueDate,
                issuerExpireDate.expireDate,
                (CoinEngineFactory.create(ctx) as? EthIdEngine)?.approvalAddress
                )

        val inputs = "$name $lastName;$birthDate${gender.toString()}"
        val info = inputs.toByteArray() + photoInBytes!!
        ctx.card.idHash = Util.calculateSHA256(info)

        Log.d(this.javaClass.simpleName,
                "ID info provided: $inputs,\n photo of size ${photoInBytes?.size}")
    }

    private fun convertDate(inputDate: CharSequence): String {
        val calendar = Calendar.getInstance()
        calendar.clear()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        try {
            calendar.time = sdf.parse(inputDate.toString())
        } catch (exception: Error) {
            etBirthDate.setTextColor(ContextCompat.getColor(requireContext(), R.color.bg_err))
        }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.timeZone = SimpleTimeZone(0, "UTC")
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.ENGLISH)
        dateFormat.timeZone = SimpleTimeZone(0, "UTC")
        return dateFormat.format(calendar.time)
    }

    private fun processIssueExpireDate(): IssuerExpireDate {
        val dt = Date()
        val issueDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(dt)
        dt.year = dt.year + 10
        val expireDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(dt)
        return IssuerExpireDate(issueDate, expireDate)
    }

    data class IssuerExpireDate(val issueDate: String, val expireDate: String)

    companion object {
        const val SEPARATOR = ";"
        const val COMPUTED_PUB_KEY = "ComputedPubKey"
        const val GET_PHOTO_REQUEST_CODE = "GetPhoto"
        const val USER_ID_DATA = "UserIdData"
    }
}


enum class Gender { M, F }

@Parcelize
data class UserIdData(
        val firstName: String,
        val lastName: String,
        val birthDate: String,
        val gender: String,
        val photo: Bitmap
) : Parcelable

class DateInputMask(val input: EditText) {
    fun listen() {
        input.addTextChangedListener(dateEntryWatcher)
    }

    private val dateEntryWatcher = object : TextWatcher {

        var edited = false
        val dividerCharacter = "/"

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            if (edited) {
                edited = false
                return
            }

            var working = getEditText()

            working = manageDateDivider(working, 2, start, before)
            working = manageDateDivider(working, 5, start, before)

            edited = true
            input.setText(working)
            input.setSelection(input.text.length)
        }

        private fun manageDateDivider(working: String, position: Int, start: Int, before: Int): String {
            if (working.length == position) {
                return if (before <= position && start < position)
                    working + dividerCharacter
                else
                    working.dropLast(1)
            }
            return working
        }

        private fun getEditText(): String {
            return if (input.text.length >= 10)
                input.text.toString().substring(0, 10)
            else
                input.text.toString()
        }

        override fun afterTextChanged(s: Editable) {}
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    }
}

class MaskWatcher(private val mask: String) : TextWatcher {
    private var isRunning = false
    private var isDeleting = false
    override fun beforeTextChanged(charSequence: CharSequence, start: Int, count: Int, after: Int) {
        isDeleting = count > after
    }

    override fun onTextChanged(charSequence: CharSequence, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(editable: Editable) {
        if (isRunning || isDeleting) {
            return
        }
        isRunning = true
        val editableLength = editable.length
        if (editableLength < mask.length) {
            if (mask[editableLength] != '#') {
                editable.append(mask[editableLength])
            } else if (mask[editableLength - 1] != '#') {
                editable.insert(editableLength - 1, mask, editableLength - 1, editableLength)
            }
        }
        if (editableLength > mask.length) editable.delete(editable.lastIndex, editableLength)
        isRunning = false
    }

    companion object {
        fun buildCpf(): MaskWatcher {
            return MaskWatcher("###.###.###-##")
        }
    }

}