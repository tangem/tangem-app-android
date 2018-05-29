package com.tangem.domain.cardReader;

import android.content.Context;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;

import java.io.IOException;

public class CustomCardReader implements Runnable {

    protected NfcManager mNfcManager;

    public interface UiCallbacks {
        // display console messages
        void onMessageSend(String raw, String name);
        void onMessageRcv(String raw, String name);
        void onOkay(String message);
        void onError(String message);
        void onStart(String message);
        void onAction(String message, String name);
        void onSeparator();

        // clear console messages
        void clearMessages();

        // ui listeners
        void setUserSelectListener(UiListener callback);
        
        // cleanup, if needed
        void onFinish(boolean err);
    }

    public interface UiListener {
        void onUserSelect(String aid);
    }

    public static final int SW_NO_ERROR = 0x9000;
    public static final int SW_GET_RESPONSE = 0x6700;

    protected IsoDep mIsoDep;
    protected UiCallbacks mUiCallbacks;

    protected String mAid;
    protected byte[] mAidBytes;

    protected Context mContext;

    public CustomCardReader(Context context, NfcManager manager, IsoDep isoDep, String aid, UiCallbacks uiCallbacks) {
        this.mIsoDep = isoDep;
        this.mAid = aid;
        this.mAidBytes = Util.hexToBytes(aid);
        this.mUiCallbacks = uiCallbacks;
        this.mNfcManager = manager;
        this.mContext = context;
    }

    // send command APDU, get response APDU, and display HEX data to user
    protected ResponseApdu sendAndRcv(CommandApdu cmdApdu)
            throws TagLostException, IOException {
        byte[] cmdBytes = cmdApdu.toBytes();
        String cmdStr = CommandApdu.toString(cmdBytes, cmdApdu.getLc());
        //mUiCallbacks.onMessageSend(cmdStr, cmdApdu.getCommandName());
        byte[] rsp = mIsoDep.transceive(cmdBytes);
        ResponseApdu rspApdu = new ResponseApdu(rsp);
        byte[] data = rspApdu.getData();

        //mUiCallbacks.onMessageRcv(Util.bytesToHex(rsp), cmdApdu.getCommandName());

        if (rspApdu.isParsedWithError()) {
            mUiCallbacks.onError(rspApdu.getParseErroMessage());
        }

        /*
        Log.d(TAG, "response APDU: " + Util.bytesToHex(rsp));
        if (data.length > 0) {
            Log.d(TAG, TLVUtil.prettyPrintAPDUResponse(data));
        }
        */
        return rspApdu;
    }


    @Override
    public void run() {
    }
}
