package com.tangem.data.network.task.confirm_payment;

import android.app.Activity;
import android.view.View;

import com.tangem.data.network.request.ElectrumRequest;
import com.tangem.data.network.task.ElectrumTask;
import com.tangem.domain.wallet.SharedData;
import com.tangem.presentation.activity.ConfirmPaymentActivity;

import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

public class ConnectTask extends ElectrumTask {
    private WeakReference<ConfirmPaymentActivity> reference;

    public ConnectTask(ConfirmPaymentActivity context, String host, int port) {
        super(host, port);
        reference = new WeakReference<>(context);
    }

    public ConnectTask(ConfirmPaymentActivity context, String host, int port, SharedData sharedData) {
        super(host, port, sharedData);
        reference = new WeakReference<>(context);
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(List<ElectrumRequest> requests) {
        super.onPostExecute(requests);
        ConfirmPaymentActivity confirmPaymentActivity = reference.get();

        for (ElectrumRequest request : requests) {
            try {
                if (request.error == null) {
                    if (request.isMethod(ElectrumRequest.METHOD_GetBalance)) {
                        try {
                            confirmPaymentActivity.getEtFee().setText("--");

                            //String mWalletAddress = request.getParams().getString(0);
                            if ((request.getResult().getInt("confirmed") + request.getResult().getInt("unconfirmed")) / confirmPaymentActivity.getCard().getBlockchain().getMultiplier() * 1000000.0 < Float.parseFloat(confirmPaymentActivity.getEtAmount().getText().toString())) {
                                confirmPaymentActivity.getEtFee().setError("Not enough funds");
                                if (sharedCounter == null) {
                                    confirmPaymentActivity.setBalanceRequestSuccess(false);
                                    confirmPaymentActivity.getBtnSend().setVisibility(View.INVISIBLE);
                                    confirmPaymentActivity.setDtVerified(null);
                                    confirmPaymentActivity.setNodeCheck(false);
                                } else {
                                    int errCounter = sharedCounter.errorRequest.incrementAndGet();
                                    if (errCounter >= sharedCounter.allRequest) {
                                        confirmPaymentActivity.setBalanceRequestSuccess(false);
                                        confirmPaymentActivity.getBtnSend().setVisibility(View.INVISIBLE);
                                        confirmPaymentActivity.setDtVerified(null);
                                        confirmPaymentActivity.setNodeCheck(false);
                                    }
                                }

                            } else {
                                confirmPaymentActivity.getEtFee().setError(null);
                                confirmPaymentActivity.setBalanceRequestSuccess(true);
                                if (confirmPaymentActivity.getFeeRequestSuccess() && confirmPaymentActivity.getBalanceRequestSuccess()) {
                                    confirmPaymentActivity.getBtnSend().setVisibility(View.VISIBLE);
                                }
                                confirmPaymentActivity.setDtVerified(new Date());
                                confirmPaymentActivity.setNodeCheck(true);
                            }
                        } catch (JSONException e) {
                            if (sharedCounter != null) {
                                int errCounter = sharedCounter.errorRequest.incrementAndGet();
                                if (errCounter >= sharedCounter.allRequest) {
                                    e.printStackTrace();
                                    confirmPaymentActivity.finishActivityWithError(Activity.RESULT_CANCELED, "Cannot check balance! No connection with blockchain nodes");
                                }
                            } else {
                                e.printStackTrace();
                                confirmPaymentActivity.finishActivityWithError(Activity.RESULT_CANCELED, "Cannot check balance! No connection with blockchain nodes");
                            }
                        }
                    } else if (request.isMethod(ElectrumRequest.METHOD_GetFee)) {
                        if (request.getResultString() == "-1") {
                            confirmPaymentActivity.getEtFee().setText("3");
                        }
                    }
                } else {
//                        etFee.setError(request.error);
//                        btnSend.setVisibility(View.INVISIBLE);
                    if (sharedCounter != null) {
                        int errCounter = sharedCounter.errorRequest.incrementAndGet();
                        if (errCounter >= sharedCounter.allRequest) {
                            confirmPaymentActivity.finishActivityWithError(Activity.RESULT_CANCELED, "Cannot calculate fee! No connection with blockchain nodes");
                        }
                    } else {
                        confirmPaymentActivity.finishActivityWithError(Activity.RESULT_CANCELED, "Cannot calculate fee! No connection with blockchain nodes");
                    }
                    return;
                }
            } catch (JSONException e) {
                if (sharedCounter != null) {
                    int errCounter = sharedCounter.errorRequest.incrementAndGet();
                    if (errCounter >= sharedCounter.allRequest) {
                        e.printStackTrace();
                        confirmPaymentActivity.finishActivityWithError(Activity.RESULT_CANCELED, "Cannot calculate fee! No connection with blockchain nodes");

                    }
                } else {
                    e.printStackTrace();
                    confirmPaymentActivity.finishActivityWithError(Activity.RESULT_CANCELED, "Cannot calculate fee! No connection with blockchain nodes");
                }
            }
        }
    }

}