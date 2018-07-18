package com.tangem.data.network

import android.content.Context
import android.content.Intent
import android.support.v7.app.AlertDialog
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.tangem.AppController
import com.tangem.wallet.R

class VolleyHelper {

    fun doRequest(context: Context, url: String, params: Map<String, String>) {
        val stringRequest = object : StringRequest(Request.Method.POST, url,
                { response ->
                    val data = GsonBuilder().setPrettyPrinting().create().toJson(JsonParser().parse(response))
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle(url)
                            .setMessage(data)
                            .setPositiveButton(R.string.ok, null)
                            .setNeutralButton(R.string.send) { _, _ ->
                                val intent = Intent(Intent.ACTION_SEND)
                                intent.type = "text/html"
                                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("erogov@tangem.com"))
                                intent.putExtra(Intent.EXTRA_SUBJECT, url)
                                intent.putExtra(Intent.EXTRA_TEXT, data)
                                context.startActivity(Intent.createChooser(intent, "Send gson"))
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

}