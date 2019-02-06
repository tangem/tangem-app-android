package com.tangem.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.*

object UtilHelper {

    @Throws(WriterException::class)
    fun generateQrCode(myCodeText: String): Bitmap {
        val hintMap = Hashtable<EncodeHintType, Any>()
        hintMap[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M // H = 30% damage
        hintMap[EncodeHintType.MARGIN] = 2

        val qrCodeWriter = QRCodeWriter()

        val size = 256

        val bitMatrix = qrCodeWriter.encode(myCodeText, BarcodeFormat.QR_CODE, size, size, hintMap)
        val width = bitMatrix.width
        val bmp = Bitmap.createBitmap(width, width, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until width) {
                bmp.setPixel(y, x, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }

    fun isOnline(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val netInfo: NetworkInfo?
            netInfo = cm.activeNetworkInfo
            netInfo != null && netInfo.isConnected
        } catch (e: NullPointerException) {
            e.printStackTrace()
            false
        }
    }

    private var singleToast: Toast? = null
    private var showTime: Date = Date()

    fun showSingleToast(context: Context?, text: String) {
        if (singleToast == null || !singleToast!!.view.isShown || showTime.time + 2000 < Date().time) {
            if (singleToast != null)
                singleToast!!.cancel()
            if (context != null) {
                singleToast = Toast.makeText(context, text, Toast.LENGTH_LONG)
                singleToast!!.show()
                showTime = Date()
            }
        }
    }

}