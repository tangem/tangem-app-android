package com.tangem.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.*

object UtilHelper {

    @Throws(WriterException::class)
    fun generateQrCode(myCodeText: String): Bitmap {
        val hintMap = Hashtable<EncodeHintType, ErrorCorrectionLevel>()
        hintMap[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H // H = 30% damage

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

    //    private static Bitmap generateQrCode(String myCodeText) throws WriterException {
//        Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<>();
//        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // H = 30% damage
//
//        QRCodeWriter qrCodeWriter = new QRCodeWriter();
//
//        int size = 256;
//
//        BitMatrix bitMatrix = qrCodeWriter.encode(myCodeText, BarcodeFormat.QR_CODE, size, size, hintMap);
//        int width = bitMatrix.getWidth();
//        Bitmap bmp = Bitmap.createBitmap(width, width, Bitmap.Config.RGB_565);
//        for (int x = 0; x < width; x++) {
//            for (int y = 0; y < width; y++) {
//                bmp.setPixel(y, x, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
//            }
//        }
//        return bmp;
//    }



}