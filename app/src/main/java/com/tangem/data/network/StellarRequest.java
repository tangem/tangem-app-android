package com.tangem.data.network;

import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Server;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.requests.ErrorResponse;
import org.stellar.sdk.requests.RequestBuilder;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.LedgerResponse;
import org.stellar.sdk.responses.Page;
import org.stellar.sdk.responses.SubmitTransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by dvol on 7.01.2019.
 */

public class StellarRequest {

    public static abstract class Base {
        public ErrorResponse errorResponse;
        private String error = null;

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public abstract void process(Server server) throws IOException;
    }

    public static class Balance extends Base {
        KeyPair accountKeyPair;
        public AccountResponse accountResponse;

        public Balance(String walletAddress) {
            accountKeyPair = KeyPair.fromAccountId(walletAddress);
        }

        @Override
        public void process(Server server) throws IOException {
            accountResponse = server.accounts().account(accountKeyPair);
        }
    }

    public static class SubmitTransaction extends Base {
        public Transaction transaction;
        public SubmitTransactionResponse response;

        public SubmitTransaction(Transaction transaction) {
            this.transaction = transaction;
        }

        @Override
        public void process(Server server) throws IOException {
//            // First, check to make sure that the destination account exists.
//            // You could skip this, but if the account does not exist, you will be charged
//            // the transaction fee when the transaction fails.
//            // It will throw HttpResponseException if account does not exist or there was another error.
//            server.accounts().account(targetAccount);
//
//            // If there was no error, load up-to-date information on your account.
//            AccountResponse sourceAccount = server.accounts().account(sourceAccount);

            // And finally, send it off to Stellar!
            response = server.submitTransaction(transaction);
        }
    }

    public static class Ledgers extends Base {
        public LedgerResponse ledgerResponse;

        public Ledgers() {
        }

        @Override
        public void process(Server server) throws IOException {
            int latestLedger = server.root().getHistoryLatestLedger();
            ledgerResponse = server.ledgers().ledger(latestLedger);
        }
    }

    public static class Operations extends Base {
        KeyPair accountKeyPair;
        public List<OperationResponse> operationsList;
        int limit = 200;

        public Operations(String walletAddress) {
            accountKeyPair = KeyPair.fromAccountId(walletAddress);
        }

        @Override
        public void process(Server server) throws IOException {
            Page<OperationResponse> operationsResponse = server.operations().forAccount(accountKeyPair).limit(limit).order(RequestBuilder.Order.DESC).execute();
            operationsList = operationsResponse.getRecords();
            while (operationsResponse.getRecords().size() == limit) {
                try {
                    operationsResponse = operationsResponse.getNextPage(server.getHttpClient());
                    operationsList.addAll(operationsResponse.getRecords());
                } catch (URISyntaxException e) {
                    break;
                }
            }
        }
    }
}