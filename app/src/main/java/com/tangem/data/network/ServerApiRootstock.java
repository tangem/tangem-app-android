package com.tangem.data.network;

import android.support.annotation.NonNull;
import android.util.Log;

import com.tangem.App;
import com.tangem.data.network.model.InfuraBody;
import com.tangem.data.network.model.InfuraResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ServerApiRootstock {
    private static String TAG = ServerApiRootstock.class.getSimpleName();

    /**
     * HTTP
     * Infura
     * <p>
     * eth_getBalance
     * eth_getTransactionCount
     * eth_call
     * eth_sendRawTransaction
     * eth_gasPrice
     */
    public static final String ROOTSTOCK_ETH_GET_BALANCE = "eth_getBalance";
    public static final String ROOTSTOCK_ETH_GET_TRANSACTION_COUNT = "eth_getTransactionCount";
    public static final String ROOTSTOCK_ETH_GET_PENDING_COUNT = "eth_getPendingCount";
    public static final String ROOTSTOCK_ETH_CALL = "eth_call";
    public static final String ROOTSTOCK_ETH_SEND_RAW_TRANSACTION = "eth_sendRawTransaction";
    public static final String ROOTSTOCK_ETH_GAS_PRICE = "eth_gasPrice";

    private int requestsCount=0;

    public boolean isRequestsSequenceCompleted() {
        Log.i(TAG, String.format("isRequestsSequenceCompleted: %s (%d requests left)", String.valueOf(requestsCount <= 0), requestsCount));
        return requestsCount <= 0;
    }

    private RootstockBodyListener rootstockBodyListener;

    public interface RootstockBodyListener {
        void onSuccess(String method, InfuraResponse infuraResponse);

        void onFail(String method, String message);
    }

    public void setRootstockResponse(RootstockBodyListener listener) {
        rootstockBodyListener = listener;
    }

    public void rootstock(String method, int id, String wallet, String contract, String tx) {
        requestsCount++;
        InfuraApi infuraApi = App.getNetworkComponent().getRetrofitInfura().create(InfuraApi.class);

        InfuraBody infuraBody;
        switch (method) {
            case ROOTSTOCK_ETH_GET_BALANCE:
            case ROOTSTOCK_ETH_GET_TRANSACTION_COUNT:
                infuraBody = new InfuraBody(method, new String[]{wallet, "latest"}, id);
                break;
            case ROOTSTOCK_ETH_GET_PENDING_COUNT:
                infuraBody = new InfuraBody(ROOTSTOCK_ETH_GET_TRANSACTION_COUNT, new String[]{wallet, "pending"}, id);
                break;
            case ROOTSTOCK_ETH_CALL:
                String address = wallet.substring(2);
                infuraBody = new InfuraBody(method, new Object[]{new InfuraBody.EthCallParams("0x70a08231000000000000000000000000" + address, contract), "latest"}, id);
                break;

            case ROOTSTOCK_ETH_SEND_RAW_TRANSACTION:
                infuraBody = new InfuraBody(method, new String[]{tx}, id);
                break;

            case ROOTSTOCK_ETH_GAS_PRICE:
                infuraBody = new InfuraBody(method, id);
                break;

            default:
                infuraBody = new InfuraBody();
        }

        Call<InfuraResponse> call = infuraApi.infura(infuraBody);
        call.enqueue(new Callback<InfuraResponse>() {
            @Override
            public void onResponse(@NonNull Call<InfuraResponse> call, @NonNull Response<InfuraResponse> response) {
                if (response.code() == 200) {
                    requestsCount--;
                    rootstockBodyListener.onSuccess(method, response.body());
                    Log.i(TAG, "rootstock " + method + " onResponse " + response.code());
                } else {
                    rootstockBodyListener.onFail(method, String.valueOf(response.code()));
                    Log.e(TAG, "rootstock " + method + " onResponse " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<InfuraResponse> call, @NonNull Throwable t) {
                rootstockBodyListener.onFail(method, String.valueOf(t.getMessage()));
                Log.e(TAG, "rootstock " + method + " onFailure " + t.getMessage());
            }
        });
    }

}