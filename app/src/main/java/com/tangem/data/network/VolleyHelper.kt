package com.tangem.data.network

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.support.v7.app.AlertDialog
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.google.gson.Gson
import com.tangem.AppController
import com.tangem.data.network.model.CardVerifyModel
import com.tangem.domain.wallet.TangemCard
import com.tangem.util.Util
import com.tangem.wallet.R
import java.util.*

class VolleyHelper (private val requestCardVerify: IRequestCardVerify){

    interface IRequestCardVerify {

        fun success(cardVerifyModel: CardVerifyModel)

        fun error(error: String)
    }

    fun requestCardVerify(card: TangemCard) {
        val params = HashMap<String, String>()
        params["CID"] = Util.bytesToHex(card.cid)
        params["publicKey"] = Util.bytesToHex(card.cardPublicKey)
        doRequestString(Server.ApiTangem.Method.VERIFY, params)
    }

    fun requestCardVerifyShowResponse(context: Activity, card: TangemCard) {
        val params = HashMap<String, String>()
        params["CID"] = Util.bytesToHex(card.cid)
        params["publicKey"] = Util.bytesToHex(card.cardPublicKey)
        doRequestDebug(context, Server.ApiTangem.Method.VERIFY, params)
    }

    fun doRequestDebug(context: Context, url: String, params: Map<String, String>) {
        val stringRequest = object : StringRequest(Request.Method.GET, url,
                { response ->
                    //                    Log.i(LoadedWallet.TAG, response.toString())
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle(url)
                            .setMessage(response.toString())
                            .setPositiveButton(R.string.ok, null)
                            .setNeutralButton(R.string.send) { _, _ ->
                                val intent = Intent(Intent.ACTION_SEND)
                                intent.type = "text/html"
                                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("erogov@tangem.com"))
                                intent.putExtra(Intent.EXTRA_SUBJECT, url)
                                intent.putExtra(Intent.EXTRA_TEXT, response.toString())
                                context.startActivity(Intent.createChooser(intent, "Send json"))
                            }
                    val alert = builder.create()
                    alert.show()
                },
                { error -> val networkResponse = error.networkResponse }) {


            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                return Server.getHeader()
            }

            @Throws(AuthFailureError::class)
            override fun getParams(): Map<String, String> {
                return params
            }
        }

        stringRequest.retryPolicy = DefaultRetryPolicy(30000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        AppController.getInstance().addToRequestQueue(stringRequest)
    }

    private fun doRequestString(url: String, params: Map<String, String>) {
        val stringRequest = object : StringRequest(Request.Method.GET, url,
                Response.Listener<String> { response ->
                    val requestResponse = response.toString().substring(response.toString().lastIndexOf("Response:") + 10)

                    val responseVerify = Gson().fromJson(requestResponse, CardVerifyModel::class.java)

                    requestCardVerify.success(responseVerify)

//                    Log.i(LoadedWallet.TAG, responseVerify.results!![2].CID)
                },
                Response.ErrorListener { "That didn't work!" }) {

            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                return Server.getHeader()
            }

            @Throws(AuthFailureError::class)
            override fun getParams(): Map<String, String> {
                return params
            }
        }

        stringRequest.retryPolicy = DefaultRetryPolicy(30000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        AppController.getInstance().addToRequestQueue(stringRequest)
    }

}