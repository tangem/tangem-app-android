package com.tangem.tangemserver.android;

import androidx.annotation.NonNull;
import android.util.Log;

import com.tangem.tangemcommon.data.TangemCard;
import com.tangem.tangemserver.android.model.CardVerifyAndGetInfo;
import com.tangem.tangemcommon.util.Util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServerApiTangem {
    private static String TAG = ServerApiTangem.class.getSimpleName();

    /**
     * HTTP
     * Card verify
     */
    private CardVerifyAndGetInfoListener cardVerifyAndGetInfoListener;

    public interface CardVerifyAndGetInfoListener {
        void onSuccess(CardVerifyAndGetInfo.Response cardVerifyAndGetArtworkResponse);
        void onFail(String message);
    }

    public void setCardVerifyAndGetInfoListener(CardVerifyAndGetInfoListener listener) {
        cardVerifyAndGetInfoListener = listener;
    }

    public void cardVerifyAndGetInfo(TangemCard card) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://estimatefee.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        TangemApi tangemApi = retrofit.create(TangemApi.class);

        List<CardVerifyAndGetInfo.Request.Item> requests = new ArrayList<>();
        requests.add(new CardVerifyAndGetInfo.Request.Item(Util.bytesToHex(card.getCID()), Util.bytesToHex(card.getCardPublicKey())));

        CardVerifyAndGetInfo.Request requestBody = new CardVerifyAndGetInfo.Request(requests);

        Call<CardVerifyAndGetInfo.Response> call = tangemApi.getCardVerifyAndGetInfo(requestBody);
        call.enqueue(new Callback<CardVerifyAndGetInfo.Response>() {
            @Override
            public void onResponse(@NonNull Call<CardVerifyAndGetInfo.Response> call, @NonNull Response<CardVerifyAndGetInfo.Response> response) {
                if (response.code() == 200) {
                    CardVerifyAndGetInfo.Response cardVerifyAndGetArtworkResponse = response.body();
                    cardVerifyAndGetInfoListener.onSuccess(cardVerifyAndGetArtworkResponse);
                    Log.i(TAG, "cardVerifyAndGeInfo onResponse " + response.code());
                } else {
                    cardVerifyAndGetInfoListener.onFail(String.valueOf(response.code()));
                    Log.e(TAG, "cardVerifyAndGetInfo onResponse " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<CardVerifyAndGetInfo.Response> call, @NonNull Throwable t) {
                cardVerifyAndGetInfoListener.onFail(t.getMessage());
                Log.e(TAG, "cardVerifyAndGetInfo onFailure " + t.getMessage());
            }
        });
    }

    /**
     * HTTP
     * Last version request from GitHub
     */
    private ArtworkListener artworkListener;

    public interface ArtworkListener {
        void onSuccess(String artworkId, InputStream inputStream, Date updateDate);
        void onFail(String message);
    }

    public void setArtworkListener(ArtworkListener listener) {
        artworkListener = listener;
    }

    public void requestArtwork(final String artworkId, final Date updateDate, TangemCard card) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://verify.tangem.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        TangemApi tangemApi = retrofit.create(TangemApi.class);

        Call<ResponseBody> call = tangemApi.getArtwork(artworkId, Util.bytesToHex(card.getCID()), Util.bytesToHex(card.getCardPublicKey()));
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.i(TAG, "getArtwork onResponse " + response.code());
                if (response.code() == 200) {
                    try {
                        ResponseBody body = response.body();
                        if (body != null) {
                            artworkListener.onSuccess(artworkId, body.byteStream(), updateDate);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                artworkListener.onFail(t.getMessage());
                Log.e(TAG, "getArtwork onFailure " + t.getMessage());
            }
        });
    }


}