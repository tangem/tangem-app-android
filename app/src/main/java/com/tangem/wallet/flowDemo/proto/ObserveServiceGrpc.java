package com.tangem.wallet.flowDemo.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 * <pre>
 * ObserveService is the public-facing API provided by observation nodes.
 * </pre>
 */

public final class ObserveServiceGrpc {

  private ObserveServiceGrpc() {}

  public static final String SERVICE_NAME = "flow.services.observation.ObserveService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<Observation.PingRequest,
      Observation.PingResponse> getPingMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Ping",
      requestType = Observation.PingRequest.class,
      responseType = Observation.PingResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Observation.PingRequest,
      Observation.PingResponse> getPingMethod() {
    io.grpc.MethodDescriptor<Observation.PingRequest, Observation.PingResponse> getPingMethod;
    if ((getPingMethod = ObserveServiceGrpc.getPingMethod) == null) {
      synchronized (ObserveServiceGrpc.class) {
        if ((getPingMethod = ObserveServiceGrpc.getPingMethod) == null) {
          ObserveServiceGrpc.getPingMethod = getPingMethod =
              io.grpc.MethodDescriptor.<Observation.PingRequest, Observation.PingResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Ping"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.PingRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.PingResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ObserveServiceMethodDescriptorSupplier("Ping"))
              .build();
        }
      }
    }
    return getPingMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Observation.GetLatestBlockHeaderRequest,
      Observation.BlockHeaderResponse> getGetLatestBlockHeaderMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetLatestBlockHeader",
      requestType = Observation.GetLatestBlockHeaderRequest.class,
      responseType = Observation.BlockHeaderResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Observation.GetLatestBlockHeaderRequest,
      Observation.BlockHeaderResponse> getGetLatestBlockHeaderMethod() {
    io.grpc.MethodDescriptor<Observation.GetLatestBlockHeaderRequest, Observation.BlockHeaderResponse> getGetLatestBlockHeaderMethod;
    if ((getGetLatestBlockHeaderMethod = ObserveServiceGrpc.getGetLatestBlockHeaderMethod) == null) {
      synchronized (ObserveServiceGrpc.class) {
        if ((getGetLatestBlockHeaderMethod = ObserveServiceGrpc.getGetLatestBlockHeaderMethod) == null) {
          ObserveServiceGrpc.getGetLatestBlockHeaderMethod = getGetLatestBlockHeaderMethod =
              io.grpc.MethodDescriptor.<Observation.GetLatestBlockHeaderRequest, Observation.BlockHeaderResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetLatestBlockHeader"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.GetLatestBlockHeaderRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.BlockHeaderResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ObserveServiceMethodDescriptorSupplier("GetLatestBlockHeader"))
              .build();
        }
      }
    }
    return getGetLatestBlockHeaderMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Observation.GetBlockHeaderByIDRequest,
      Observation.BlockHeaderResponse> getGetBlockHeaderByIDMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetBlockHeaderByID",
      requestType = Observation.GetBlockHeaderByIDRequest.class,
      responseType = Observation.BlockHeaderResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Observation.GetBlockHeaderByIDRequest,
      Observation.BlockHeaderResponse> getGetBlockHeaderByIDMethod() {
    io.grpc.MethodDescriptor<Observation.GetBlockHeaderByIDRequest, Observation.BlockHeaderResponse> getGetBlockHeaderByIDMethod;
    if ((getGetBlockHeaderByIDMethod = ObserveServiceGrpc.getGetBlockHeaderByIDMethod) == null) {
      synchronized (ObserveServiceGrpc.class) {
        if ((getGetBlockHeaderByIDMethod = ObserveServiceGrpc.getGetBlockHeaderByIDMethod) == null) {
          ObserveServiceGrpc.getGetBlockHeaderByIDMethod = getGetBlockHeaderByIDMethod =
              io.grpc.MethodDescriptor.<Observation.GetBlockHeaderByIDRequest, Observation.BlockHeaderResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetBlockHeaderByID"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.GetBlockHeaderByIDRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.BlockHeaderResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ObserveServiceMethodDescriptorSupplier("GetBlockHeaderByID"))
              .build();
        }
      }
    }
    return getGetBlockHeaderByIDMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Observation.GetBlockHeaderByHeightRequest,
      Observation.BlockHeaderResponse> getGetBlockHeaderByHeightMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetBlockHeaderByHeight",
      requestType = Observation.GetBlockHeaderByHeightRequest.class,
      responseType = Observation.BlockHeaderResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Observation.GetBlockHeaderByHeightRequest,
      Observation.BlockHeaderResponse> getGetBlockHeaderByHeightMethod() {
    io.grpc.MethodDescriptor<Observation.GetBlockHeaderByHeightRequest, Observation.BlockHeaderResponse> getGetBlockHeaderByHeightMethod;
    if ((getGetBlockHeaderByHeightMethod = ObserveServiceGrpc.getGetBlockHeaderByHeightMethod) == null) {
      synchronized (ObserveServiceGrpc.class) {
        if ((getGetBlockHeaderByHeightMethod = ObserveServiceGrpc.getGetBlockHeaderByHeightMethod) == null) {
          ObserveServiceGrpc.getGetBlockHeaderByHeightMethod = getGetBlockHeaderByHeightMethod =
              io.grpc.MethodDescriptor.<Observation.GetBlockHeaderByHeightRequest, Observation.BlockHeaderResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetBlockHeaderByHeight"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.GetBlockHeaderByHeightRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.BlockHeaderResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ObserveServiceMethodDescriptorSupplier("GetBlockHeaderByHeight"))
              .build();
        }
      }
    }
    return getGetBlockHeaderByHeightMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Observation.GetLatestBlockRequest,
      Observation.BlockResponse> getGetLatestBlockMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetLatestBlock",
      requestType = Observation.GetLatestBlockRequest.class,
      responseType = Observation.BlockResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Observation.GetLatestBlockRequest,
      Observation.BlockResponse> getGetLatestBlockMethod() {
    io.grpc.MethodDescriptor<Observation.GetLatestBlockRequest, Observation.BlockResponse> getGetLatestBlockMethod;
    if ((getGetLatestBlockMethod = ObserveServiceGrpc.getGetLatestBlockMethod) == null) {
      synchronized (ObserveServiceGrpc.class) {
        if ((getGetLatestBlockMethod = ObserveServiceGrpc.getGetLatestBlockMethod) == null) {
          ObserveServiceGrpc.getGetLatestBlockMethod = getGetLatestBlockMethod =
              io.grpc.MethodDescriptor.<Observation.GetLatestBlockRequest, Observation.BlockResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetLatestBlock"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.GetLatestBlockRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.BlockResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ObserveServiceMethodDescriptorSupplier("GetLatestBlock"))
              .build();
        }
      }
    }
    return getGetLatestBlockMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Observation.GetBlockByIDRequest,
      Observation.BlockResponse> getGetBlockByIDMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetBlockByID",
      requestType = Observation.GetBlockByIDRequest.class,
      responseType = Observation.BlockResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Observation.GetBlockByIDRequest,
      Observation.BlockResponse> getGetBlockByIDMethod() {
    io.grpc.MethodDescriptor<Observation.GetBlockByIDRequest, Observation.BlockResponse> getGetBlockByIDMethod;
    if ((getGetBlockByIDMethod = ObserveServiceGrpc.getGetBlockByIDMethod) == null) {
      synchronized (ObserveServiceGrpc.class) {
        if ((getGetBlockByIDMethod = ObserveServiceGrpc.getGetBlockByIDMethod) == null) {
          ObserveServiceGrpc.getGetBlockByIDMethod = getGetBlockByIDMethod =
              io.grpc.MethodDescriptor.<Observation.GetBlockByIDRequest, Observation.BlockResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetBlockByID"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.GetBlockByIDRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.BlockResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ObserveServiceMethodDescriptorSupplier("GetBlockByID"))
              .build();
        }
      }
    }
    return getGetBlockByIDMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Observation.GetBlockByHeightRequest,
      Observation.BlockResponse> getGetBlockByHeightMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetBlockByHeight",
      requestType = Observation.GetBlockByHeightRequest.class,
      responseType = Observation.BlockResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Observation.GetBlockByHeightRequest,
      Observation.BlockResponse> getGetBlockByHeightMethod() {
    io.grpc.MethodDescriptor<Observation.GetBlockByHeightRequest, Observation.BlockResponse> getGetBlockByHeightMethod;
    if ((getGetBlockByHeightMethod = ObserveServiceGrpc.getGetBlockByHeightMethod) == null) {
      synchronized (ObserveServiceGrpc.class) {
        if ((getGetBlockByHeightMethod = ObserveServiceGrpc.getGetBlockByHeightMethod) == null) {
          ObserveServiceGrpc.getGetBlockByHeightMethod = getGetBlockByHeightMethod =
              io.grpc.MethodDescriptor.<Observation.GetBlockByHeightRequest, Observation.BlockResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetBlockByHeight"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.GetBlockByHeightRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.BlockResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ObserveServiceMethodDescriptorSupplier("GetBlockByHeight"))
              .build();
        }
      }
    }
    return getGetBlockByHeightMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Observation.GetCollectionByIDRequest,
      Observation.CollectionResponse> getGetCollectionByIDMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetCollectionByID",
      requestType = Observation.GetCollectionByIDRequest.class,
      responseType = Observation.CollectionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Observation.GetCollectionByIDRequest,
      Observation.CollectionResponse> getGetCollectionByIDMethod() {
    io.grpc.MethodDescriptor<Observation.GetCollectionByIDRequest, Observation.CollectionResponse> getGetCollectionByIDMethod;
    if ((getGetCollectionByIDMethod = ObserveServiceGrpc.getGetCollectionByIDMethod) == null) {
      synchronized (ObserveServiceGrpc.class) {
        if ((getGetCollectionByIDMethod = ObserveServiceGrpc.getGetCollectionByIDMethod) == null) {
          ObserveServiceGrpc.getGetCollectionByIDMethod = getGetCollectionByIDMethod =
              io.grpc.MethodDescriptor.<Observation.GetCollectionByIDRequest, Observation.CollectionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetCollectionByID"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.GetCollectionByIDRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.CollectionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ObserveServiceMethodDescriptorSupplier("GetCollectionByID"))
              .build();
        }
      }
    }
    return getGetCollectionByIDMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Observation.SendTransactionRequest,
      Observation.SendTransactionResponse> getSendTransactionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SendTransaction",
      requestType = Observation.SendTransactionRequest.class,
      responseType = Observation.SendTransactionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Observation.SendTransactionRequest,
      Observation.SendTransactionResponse> getSendTransactionMethod() {
    io.grpc.MethodDescriptor<Observation.SendTransactionRequest, Observation.SendTransactionResponse> getSendTransactionMethod;
    if ((getSendTransactionMethod = ObserveServiceGrpc.getSendTransactionMethod) == null) {
      synchronized (ObserveServiceGrpc.class) {
        if ((getSendTransactionMethod = ObserveServiceGrpc.getSendTransactionMethod) == null) {
          ObserveServiceGrpc.getSendTransactionMethod = getSendTransactionMethod =
              io.grpc.MethodDescriptor.<Observation.SendTransactionRequest, Observation.SendTransactionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SendTransaction"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.SendTransactionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.SendTransactionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ObserveServiceMethodDescriptorSupplier("SendTransaction"))
              .build();
        }
      }
    }
    return getSendTransactionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Observation.GetTransactionRequest,
      Observation.TransactionResponse> getGetTransactionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetTransaction",
      requestType = Observation.GetTransactionRequest.class,
      responseType = Observation.TransactionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Observation.GetTransactionRequest,
      Observation.TransactionResponse> getGetTransactionMethod() {
    io.grpc.MethodDescriptor<Observation.GetTransactionRequest, Observation.TransactionResponse> getGetTransactionMethod;
    if ((getGetTransactionMethod = ObserveServiceGrpc.getGetTransactionMethod) == null) {
      synchronized (ObserveServiceGrpc.class) {
        if ((getGetTransactionMethod = ObserveServiceGrpc.getGetTransactionMethod) == null) {
          ObserveServiceGrpc.getGetTransactionMethod = getGetTransactionMethod =
              io.grpc.MethodDescriptor.<Observation.GetTransactionRequest, Observation.TransactionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetTransaction"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.GetTransactionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.TransactionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ObserveServiceMethodDescriptorSupplier("GetTransaction"))
              .build();
        }
      }
    }
    return getGetTransactionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Observation.GetTransactionRequest,
      Observation.TransactionResultResponse> getGetTransactionResultMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetTransactionResult",
      requestType = Observation.GetTransactionRequest.class,
      responseType = Observation.TransactionResultResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Observation.GetTransactionRequest,
      Observation.TransactionResultResponse> getGetTransactionResultMethod() {
    io.grpc.MethodDescriptor<Observation.GetTransactionRequest, Observation.TransactionResultResponse> getGetTransactionResultMethod;
    if ((getGetTransactionResultMethod = ObserveServiceGrpc.getGetTransactionResultMethod) == null) {
      synchronized (ObserveServiceGrpc.class) {
        if ((getGetTransactionResultMethod = ObserveServiceGrpc.getGetTransactionResultMethod) == null) {
          ObserveServiceGrpc.getGetTransactionResultMethod = getGetTransactionResultMethod =
              io.grpc.MethodDescriptor.<Observation.GetTransactionRequest, Observation.TransactionResultResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetTransactionResult"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.GetTransactionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.TransactionResultResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ObserveServiceMethodDescriptorSupplier("GetTransactionResult"))
              .build();
        }
      }
    }
    return getGetTransactionResultMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Observation.GetAccountRequest,
      Observation.GetAccountResponse> getGetAccountMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetAccount",
      requestType = Observation.GetAccountRequest.class,
      responseType = Observation.GetAccountResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Observation.GetAccountRequest,
      Observation.GetAccountResponse> getGetAccountMethod() {
    io.grpc.MethodDescriptor<Observation.GetAccountRequest, Observation.GetAccountResponse> getGetAccountMethod;
    if ((getGetAccountMethod = ObserveServiceGrpc.getGetAccountMethod) == null) {
      synchronized (ObserveServiceGrpc.class) {
        if ((getGetAccountMethod = ObserveServiceGrpc.getGetAccountMethod) == null) {
          ObserveServiceGrpc.getGetAccountMethod = getGetAccountMethod =
              io.grpc.MethodDescriptor.<Observation.GetAccountRequest, Observation.GetAccountResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetAccount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.GetAccountRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.GetAccountResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ObserveServiceMethodDescriptorSupplier("GetAccount"))
              .build();
        }
      }
    }
    return getGetAccountMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Observation.ExecuteScriptRequest,
      Observation.ExecuteScriptResponse> getExecuteScriptMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExecuteScript",
      requestType = Observation.ExecuteScriptRequest.class,
      responseType = Observation.ExecuteScriptResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Observation.ExecuteScriptRequest,
      Observation.ExecuteScriptResponse> getExecuteScriptMethod() {
    io.grpc.MethodDescriptor<Observation.ExecuteScriptRequest, Observation.ExecuteScriptResponse> getExecuteScriptMethod;
    if ((getExecuteScriptMethod = ObserveServiceGrpc.getExecuteScriptMethod) == null) {
      synchronized (ObserveServiceGrpc.class) {
        if ((getExecuteScriptMethod = ObserveServiceGrpc.getExecuteScriptMethod) == null) {
          ObserveServiceGrpc.getExecuteScriptMethod = getExecuteScriptMethod =
              io.grpc.MethodDescriptor.<Observation.ExecuteScriptRequest, Observation.ExecuteScriptResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ExecuteScript"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.ExecuteScriptRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.ExecuteScriptResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ObserveServiceMethodDescriptorSupplier("ExecuteScript"))
              .build();
        }
      }
    }
    return getExecuteScriptMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Observation.GetEventsRequest,
      Observation.GetEventsResponse> getGetEventsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetEvents",
      requestType = Observation.GetEventsRequest.class,
      responseType = Observation.GetEventsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Observation.GetEventsRequest,
      Observation.GetEventsResponse> getGetEventsMethod() {
    io.grpc.MethodDescriptor<Observation.GetEventsRequest, Observation.GetEventsResponse> getGetEventsMethod;
    if ((getGetEventsMethod = ObserveServiceGrpc.getGetEventsMethod) == null) {
      synchronized (ObserveServiceGrpc.class) {
        if ((getGetEventsMethod = ObserveServiceGrpc.getGetEventsMethod) == null) {
          ObserveServiceGrpc.getGetEventsMethod = getGetEventsMethod =
              io.grpc.MethodDescriptor.<Observation.GetEventsRequest, Observation.GetEventsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetEvents"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.GetEventsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Observation.GetEventsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ObserveServiceMethodDescriptorSupplier("GetEvents"))
              .build();
        }
      }
    }
    return getGetEventsMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ObserveServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ObserveServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ObserveServiceStub>() {
        @Override
        public ObserveServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ObserveServiceStub(channel, callOptions);
        }
      };
    return ObserveServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ObserveServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ObserveServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ObserveServiceBlockingStub>() {
        @Override
        public ObserveServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ObserveServiceBlockingStub(channel, callOptions);
        }
      };
    return ObserveServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ObserveServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ObserveServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ObserveServiceFutureStub>() {
        @Override
        public ObserveServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ObserveServiceFutureStub(channel, callOptions);
        }
      };
    return ObserveServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * ObserveService is the public-facing API provided by observation nodes.
   * </pre>
   */
  public static abstract class ObserveServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Ping is used to check if the observation node is alive and healthy.
     * </pre>
     */
    public void ping(Observation.PingRequest request,
        io.grpc.stub.StreamObserver<Observation.PingResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getPingMethod(), responseObserver);
    }

    /**
     * <pre>
     * GetLatestBlockHeader gets the latest sealed or unsealed block header.
     * </pre>
     */
    public void getLatestBlockHeader(Observation.GetLatestBlockHeaderRequest request,
        io.grpc.stub.StreamObserver<Observation.BlockHeaderResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetLatestBlockHeaderMethod(), responseObserver);
    }

    /**
     * <pre>
     * GetBlockHeaderByID gets a block header by ID.
     * </pre>
     */
    public void getBlockHeaderByID(Observation.GetBlockHeaderByIDRequest request,
        io.grpc.stub.StreamObserver<Observation.BlockHeaderResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBlockHeaderByIDMethod(), responseObserver);
    }

    /**
     * <pre>
     * GetBlockHeaderByHeight gets a block header by height.
     * </pre>
     */
    public void getBlockHeaderByHeight(Observation.GetBlockHeaderByHeightRequest request,
        io.grpc.stub.StreamObserver<Observation.BlockHeaderResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBlockHeaderByHeightMethod(), responseObserver);
    }

    /**
     * <pre>
     * GetLatestBlock gets the full payload of the latest sealed or unsealed block.
     * </pre>
     */
    public void getLatestBlock(Observation.GetLatestBlockRequest request,
        io.grpc.stub.StreamObserver<Observation.BlockResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetLatestBlockMethod(), responseObserver);
    }

    /**
     * <pre>
     * GetBlockByID gets a full block by ID.
     * </pre>
     */
    public void getBlockByID(Observation.GetBlockByIDRequest request,
        io.grpc.stub.StreamObserver<Observation.BlockResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBlockByIDMethod(), responseObserver);
    }

    /**
     * <pre>
     * GetBlockByHeight gets a full block by height.
     * </pre>
     */
    public void getBlockByHeight(Observation.GetBlockByHeightRequest request,
        io.grpc.stub.StreamObserver<Observation.BlockResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBlockByHeightMethod(), responseObserver);
    }

    /**
     * <pre>
     * GetCollectionByID gets a collection by ID.
     * </pre>
     */
    public void getCollectionByID(Observation.GetCollectionByIDRequest request,
        io.grpc.stub.StreamObserver<Observation.CollectionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetCollectionByIDMethod(), responseObserver);
    }

    /**
     * <pre>
     * SendTransaction submits a transaction to the network.
     * </pre>
     */
    public void sendTransaction(Observation.SendTransactionRequest request,
        io.grpc.stub.StreamObserver<Observation.SendTransactionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSendTransactionMethod(), responseObserver);
    }

    /**
     * <pre>
     * GetTransaction gets a transaction by ID.
     * </pre>
     */
    public void getTransaction(Observation.GetTransactionRequest request,
        io.grpc.stub.StreamObserver<Observation.TransactionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTransactionMethod(), responseObserver);
    }

    /**
     * <pre>
     * GetTransactionResult gets the result of a transaction.
     * </pre>
     */
    public void getTransactionResult(Observation.GetTransactionRequest request,
        io.grpc.stub.StreamObserver<Observation.TransactionResultResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTransactionResultMethod(), responseObserver);
    }

    /**
     * <pre>
     * GetAccount gets an account by address.
     * </pre>
     */
    public void getAccount(Observation.GetAccountRequest request,
        io.grpc.stub.StreamObserver<Observation.GetAccountResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAccountMethod(), responseObserver);
    }

    /**
     * <pre>
     * ExecuteScript executes a script against the latest sealed world state.
     * </pre>
     */
    public void executeScript(Observation.ExecuteScriptRequest request,
        io.grpc.stub.StreamObserver<Observation.ExecuteScriptResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getExecuteScriptMethod(), responseObserver);
    }

    /**
     * <pre>
     * GetEvents retrieves events matching the given query.
     * </pre>
     */
    public void getEvents(Observation.GetEventsRequest request,
        io.grpc.stub.StreamObserver<Observation.GetEventsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetEventsMethod(), responseObserver);
    }

    @Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getPingMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                Observation.PingRequest,
                Observation.PingResponse>(
                  this, METHODID_PING)))
          .addMethod(
            getGetLatestBlockHeaderMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                Observation.GetLatestBlockHeaderRequest,
                Observation.BlockHeaderResponse>(
                  this, METHODID_GET_LATEST_BLOCK_HEADER)))
          .addMethod(
            getGetBlockHeaderByIDMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                Observation.GetBlockHeaderByIDRequest,
                Observation.BlockHeaderResponse>(
                  this, METHODID_GET_BLOCK_HEADER_BY_ID)))
          .addMethod(
            getGetBlockHeaderByHeightMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                Observation.GetBlockHeaderByHeightRequest,
                Observation.BlockHeaderResponse>(
                  this, METHODID_GET_BLOCK_HEADER_BY_HEIGHT)))
          .addMethod(
            getGetLatestBlockMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                Observation.GetLatestBlockRequest,
                Observation.BlockResponse>(
                  this, METHODID_GET_LATEST_BLOCK)))
          .addMethod(
            getGetBlockByIDMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                Observation.GetBlockByIDRequest,
                Observation.BlockResponse>(
                  this, METHODID_GET_BLOCK_BY_ID)))
          .addMethod(
            getGetBlockByHeightMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                Observation.GetBlockByHeightRequest,
                Observation.BlockResponse>(
                  this, METHODID_GET_BLOCK_BY_HEIGHT)))
          .addMethod(
            getGetCollectionByIDMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                Observation.GetCollectionByIDRequest,
                Observation.CollectionResponse>(
                  this, METHODID_GET_COLLECTION_BY_ID)))
          .addMethod(
            getSendTransactionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                Observation.SendTransactionRequest,
                Observation.SendTransactionResponse>(
                  this, METHODID_SEND_TRANSACTION)))
          .addMethod(
            getGetTransactionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                Observation.GetTransactionRequest,
                Observation.TransactionResponse>(
                  this, METHODID_GET_TRANSACTION)))
          .addMethod(
            getGetTransactionResultMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                Observation.GetTransactionRequest,
                Observation.TransactionResultResponse>(
                  this, METHODID_GET_TRANSACTION_RESULT)))
          .addMethod(
            getGetAccountMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                Observation.GetAccountRequest,
                Observation.GetAccountResponse>(
                  this, METHODID_GET_ACCOUNT)))
          .addMethod(
            getExecuteScriptMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                Observation.ExecuteScriptRequest,
                Observation.ExecuteScriptResponse>(
                  this, METHODID_EXECUTE_SCRIPT)))
          .addMethod(
            getGetEventsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                Observation.GetEventsRequest,
                Observation.GetEventsResponse>(
                  this, METHODID_GET_EVENTS)))
          .build();
    }
  }

  /**
   * <pre>
   * ObserveService is the public-facing API provided by observation nodes.
   * </pre>
   */
  public static final class ObserveServiceStub extends io.grpc.stub.AbstractAsyncStub<ObserveServiceStub> {
    private ObserveServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected ObserveServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ObserveServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Ping is used to check if the observation node is alive and healthy.
     * </pre>
     */
    public void ping(Observation.PingRequest request,
        io.grpc.stub.StreamObserver<Observation.PingResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPingMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * GetLatestBlockHeader gets the latest sealed or unsealed block header.
     * </pre>
     */
    public void getLatestBlockHeader(Observation.GetLatestBlockHeaderRequest request,
        io.grpc.stub.StreamObserver<Observation.BlockHeaderResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetLatestBlockHeaderMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * GetBlockHeaderByID gets a block header by ID.
     * </pre>
     */
    public void getBlockHeaderByID(Observation.GetBlockHeaderByIDRequest request,
        io.grpc.stub.StreamObserver<Observation.BlockHeaderResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBlockHeaderByIDMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * GetBlockHeaderByHeight gets a block header by height.
     * </pre>
     */
    public void getBlockHeaderByHeight(Observation.GetBlockHeaderByHeightRequest request,
        io.grpc.stub.StreamObserver<Observation.BlockHeaderResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBlockHeaderByHeightMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * GetLatestBlock gets the full payload of the latest sealed or unsealed block.
     * </pre>
     */
    public void getLatestBlock(Observation.GetLatestBlockRequest request,
        io.grpc.stub.StreamObserver<Observation.BlockResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetLatestBlockMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * GetBlockByID gets a full block by ID.
     * </pre>
     */
    public void getBlockByID(Observation.GetBlockByIDRequest request,
        io.grpc.stub.StreamObserver<Observation.BlockResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBlockByIDMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * GetBlockByHeight gets a full block by height.
     * </pre>
     */
    public void getBlockByHeight(Observation.GetBlockByHeightRequest request,
        io.grpc.stub.StreamObserver<Observation.BlockResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBlockByHeightMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * GetCollectionByID gets a collection by ID.
     * </pre>
     */
    public void getCollectionByID(Observation.GetCollectionByIDRequest request,
        io.grpc.stub.StreamObserver<Observation.CollectionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetCollectionByIDMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * SendTransaction submits a transaction to the network.
     * </pre>
     */
    public void sendTransaction(Observation.SendTransactionRequest request,
        io.grpc.stub.StreamObserver<Observation.SendTransactionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSendTransactionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * GetTransaction gets a transaction by ID.
     * </pre>
     */
    public void getTransaction(Observation.GetTransactionRequest request,
        io.grpc.stub.StreamObserver<Observation.TransactionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTransactionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * GetTransactionResult gets the result of a transaction.
     * </pre>
     */
    public void getTransactionResult(Observation.GetTransactionRequest request,
        io.grpc.stub.StreamObserver<Observation.TransactionResultResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTransactionResultMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * GetAccount gets an account by address.
     * </pre>
     */
    public void getAccount(Observation.GetAccountRequest request,
        io.grpc.stub.StreamObserver<Observation.GetAccountResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAccountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * ExecuteScript executes a script against the latest sealed world state.
     * </pre>
     */
    public void executeScript(Observation.ExecuteScriptRequest request,
        io.grpc.stub.StreamObserver<Observation.ExecuteScriptResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getExecuteScriptMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * GetEvents retrieves events matching the given query.
     * </pre>
     */
    public void getEvents(Observation.GetEventsRequest request,
        io.grpc.stub.StreamObserver<Observation.GetEventsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetEventsMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * ObserveService is the public-facing API provided by observation nodes.
   * </pre>
   */
  public static final class ObserveServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<ObserveServiceBlockingStub> {
    private ObserveServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected ObserveServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ObserveServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Ping is used to check if the observation node is alive and healthy.
     * </pre>
     */
    public Observation.PingResponse ping(Observation.PingRequest request) {
      return blockingUnaryCall(
          getChannel(), getPingMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetLatestBlockHeader gets the latest sealed or unsealed block header.
     * </pre>
     */
    public Observation.BlockHeaderResponse getLatestBlockHeader(Observation.GetLatestBlockHeaderRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetLatestBlockHeaderMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetBlockHeaderByID gets a block header by ID.
     * </pre>
     */
    public Observation.BlockHeaderResponse getBlockHeaderByID(Observation.GetBlockHeaderByIDRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetBlockHeaderByIDMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetBlockHeaderByHeight gets a block header by height.
     * </pre>
     */
    public Observation.BlockHeaderResponse getBlockHeaderByHeight(Observation.GetBlockHeaderByHeightRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetBlockHeaderByHeightMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetLatestBlock gets the full payload of the latest sealed or unsealed block.
     * </pre>
     */
    public Observation.BlockResponse getLatestBlock(Observation.GetLatestBlockRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetLatestBlockMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetBlockByID gets a full block by ID.
     * </pre>
     */
    public Observation.BlockResponse getBlockByID(Observation.GetBlockByIDRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetBlockByIDMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetBlockByHeight gets a full block by height.
     * </pre>
     */
    public Observation.BlockResponse getBlockByHeight(Observation.GetBlockByHeightRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetBlockByHeightMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetCollectionByID gets a collection by ID.
     * </pre>
     */
    public Observation.CollectionResponse getCollectionByID(Observation.GetCollectionByIDRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetCollectionByIDMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * SendTransaction submits a transaction to the network.
     * </pre>
     */
    public Observation.SendTransactionResponse sendTransaction(Observation.SendTransactionRequest request) {
      return blockingUnaryCall(
          getChannel(), getSendTransactionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetTransaction gets a transaction by ID.
     * </pre>
     */
    public Observation.TransactionResponse getTransaction(Observation.GetTransactionRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetTransactionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetTransactionResult gets the result of a transaction.
     * </pre>
     */
    public Observation.TransactionResultResponse getTransactionResult(Observation.GetTransactionRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetTransactionResultMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetAccount gets an account by address.
     * </pre>
     */
    public Observation.GetAccountResponse getAccount(Observation.GetAccountRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetAccountMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * ExecuteScript executes a script against the latest sealed world state.
     * </pre>
     */
    public Observation.ExecuteScriptResponse executeScript(Observation.ExecuteScriptRequest request) {
      return blockingUnaryCall(
          getChannel(), getExecuteScriptMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * GetEvents retrieves events matching the given query.
     * </pre>
     */
    public Observation.GetEventsResponse getEvents(Observation.GetEventsRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetEventsMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * ObserveService is the public-facing API provided by observation nodes.
   * </pre>
   */
  public static final class ObserveServiceFutureStub extends io.grpc.stub.AbstractFutureStub<ObserveServiceFutureStub> {
    private ObserveServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected ObserveServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ObserveServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Ping is used to check if the observation node is alive and healthy.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Observation.PingResponse> ping(
        Observation.PingRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getPingMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * GetLatestBlockHeader gets the latest sealed or unsealed block header.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Observation.BlockHeaderResponse> getLatestBlockHeader(
        Observation.GetLatestBlockHeaderRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetLatestBlockHeaderMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * GetBlockHeaderByID gets a block header by ID.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Observation.BlockHeaderResponse> getBlockHeaderByID(
        Observation.GetBlockHeaderByIDRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBlockHeaderByIDMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * GetBlockHeaderByHeight gets a block header by height.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Observation.BlockHeaderResponse> getBlockHeaderByHeight(
        Observation.GetBlockHeaderByHeightRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBlockHeaderByHeightMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * GetLatestBlock gets the full payload of the latest sealed or unsealed block.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Observation.BlockResponse> getLatestBlock(
        Observation.GetLatestBlockRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetLatestBlockMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * GetBlockByID gets a full block by ID.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Observation.BlockResponse> getBlockByID(
        Observation.GetBlockByIDRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBlockByIDMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * GetBlockByHeight gets a full block by height.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Observation.BlockResponse> getBlockByHeight(
        Observation.GetBlockByHeightRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBlockByHeightMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * GetCollectionByID gets a collection by ID.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Observation.CollectionResponse> getCollectionByID(
        Observation.GetCollectionByIDRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetCollectionByIDMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * SendTransaction submits a transaction to the network.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Observation.SendTransactionResponse> sendTransaction(
        Observation.SendTransactionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getSendTransactionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * GetTransaction gets a transaction by ID.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Observation.TransactionResponse> getTransaction(
        Observation.GetTransactionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTransactionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * GetTransactionResult gets the result of a transaction.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Observation.TransactionResultResponse> getTransactionResult(
        Observation.GetTransactionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTransactionResultMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * GetAccount gets an account by address.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Observation.GetAccountResponse> getAccount(
        Observation.GetAccountRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAccountMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * ExecuteScript executes a script against the latest sealed world state.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Observation.ExecuteScriptResponse> executeScript(
        Observation.ExecuteScriptRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getExecuteScriptMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * GetEvents retrieves events matching the given query.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Observation.GetEventsResponse> getEvents(
        Observation.GetEventsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetEventsMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_PING = 0;
  private static final int METHODID_GET_LATEST_BLOCK_HEADER = 1;
  private static final int METHODID_GET_BLOCK_HEADER_BY_ID = 2;
  private static final int METHODID_GET_BLOCK_HEADER_BY_HEIGHT = 3;
  private static final int METHODID_GET_LATEST_BLOCK = 4;
  private static final int METHODID_GET_BLOCK_BY_ID = 5;
  private static final int METHODID_GET_BLOCK_BY_HEIGHT = 6;
  private static final int METHODID_GET_COLLECTION_BY_ID = 7;
  private static final int METHODID_SEND_TRANSACTION = 8;
  private static final int METHODID_GET_TRANSACTION = 9;
  private static final int METHODID_GET_TRANSACTION_RESULT = 10;
  private static final int METHODID_GET_ACCOUNT = 11;
  private static final int METHODID_EXECUTE_SCRIPT = 12;
  private static final int METHODID_GET_EVENTS = 13;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ObserveServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(ObserveServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_PING:
          serviceImpl.ping((Observation.PingRequest) request,
              (io.grpc.stub.StreamObserver<Observation.PingResponse>) responseObserver);
          break;
        case METHODID_GET_LATEST_BLOCK_HEADER:
          serviceImpl.getLatestBlockHeader((Observation.GetLatestBlockHeaderRequest) request,
              (io.grpc.stub.StreamObserver<Observation.BlockHeaderResponse>) responseObserver);
          break;
        case METHODID_GET_BLOCK_HEADER_BY_ID:
          serviceImpl.getBlockHeaderByID((Observation.GetBlockHeaderByIDRequest) request,
              (io.grpc.stub.StreamObserver<Observation.BlockHeaderResponse>) responseObserver);
          break;
        case METHODID_GET_BLOCK_HEADER_BY_HEIGHT:
          serviceImpl.getBlockHeaderByHeight((Observation.GetBlockHeaderByHeightRequest) request,
              (io.grpc.stub.StreamObserver<Observation.BlockHeaderResponse>) responseObserver);
          break;
        case METHODID_GET_LATEST_BLOCK:
          serviceImpl.getLatestBlock((Observation.GetLatestBlockRequest) request,
              (io.grpc.stub.StreamObserver<Observation.BlockResponse>) responseObserver);
          break;
        case METHODID_GET_BLOCK_BY_ID:
          serviceImpl.getBlockByID((Observation.GetBlockByIDRequest) request,
              (io.grpc.stub.StreamObserver<Observation.BlockResponse>) responseObserver);
          break;
        case METHODID_GET_BLOCK_BY_HEIGHT:
          serviceImpl.getBlockByHeight((Observation.GetBlockByHeightRequest) request,
              (io.grpc.stub.StreamObserver<Observation.BlockResponse>) responseObserver);
          break;
        case METHODID_GET_COLLECTION_BY_ID:
          serviceImpl.getCollectionByID((Observation.GetCollectionByIDRequest) request,
              (io.grpc.stub.StreamObserver<Observation.CollectionResponse>) responseObserver);
          break;
        case METHODID_SEND_TRANSACTION:
          serviceImpl.sendTransaction((Observation.SendTransactionRequest) request,
              (io.grpc.stub.StreamObserver<Observation.SendTransactionResponse>) responseObserver);
          break;
        case METHODID_GET_TRANSACTION:
          serviceImpl.getTransaction((Observation.GetTransactionRequest) request,
              (io.grpc.stub.StreamObserver<Observation.TransactionResponse>) responseObserver);
          break;
        case METHODID_GET_TRANSACTION_RESULT:
          serviceImpl.getTransactionResult((Observation.GetTransactionRequest) request,
              (io.grpc.stub.StreamObserver<Observation.TransactionResultResponse>) responseObserver);
          break;
        case METHODID_GET_ACCOUNT:
          serviceImpl.getAccount((Observation.GetAccountRequest) request,
              (io.grpc.stub.StreamObserver<Observation.GetAccountResponse>) responseObserver);
          break;
        case METHODID_EXECUTE_SCRIPT:
          serviceImpl.executeScript((Observation.ExecuteScriptRequest) request,
              (io.grpc.stub.StreamObserver<Observation.ExecuteScriptResponse>) responseObserver);
          break;
        case METHODID_GET_EVENTS:
          serviceImpl.getEvents((Observation.GetEventsRequest) request,
              (io.grpc.stub.StreamObserver<Observation.GetEventsResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @Override
    @SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class ObserveServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ObserveServiceBaseDescriptorSupplier() {}

    @Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return Observation.getDescriptor();
    }

    @Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ObserveService");
    }
  }

  private static final class ObserveServiceFileDescriptorSupplier
      extends ObserveServiceBaseDescriptorSupplier {
    ObserveServiceFileDescriptorSupplier() {}
  }

  private static final class ObserveServiceMethodDescriptorSupplier
      extends ObserveServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    ObserveServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (ObserveServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ObserveServiceFileDescriptorSupplier())
              .addMethod(getPingMethod())
              .addMethod(getGetLatestBlockHeaderMethod())
              .addMethod(getGetBlockHeaderByIDMethod())
              .addMethod(getGetBlockHeaderByHeightMethod())
              .addMethod(getGetLatestBlockMethod())
              .addMethod(getGetBlockByIDMethod())
              .addMethod(getGetBlockByHeightMethod())
              .addMethod(getGetCollectionByIDMethod())
              .addMethod(getSendTransactionMethod())
              .addMethod(getGetTransactionMethod())
              .addMethod(getGetTransactionResultMethod())
              .addMethod(getGetAccountMethod())
              .addMethod(getExecuteScriptMethod())
              .addMethod(getGetEventsMethod())
              .build();
        }
      }
    }
    return result;
  }
}
