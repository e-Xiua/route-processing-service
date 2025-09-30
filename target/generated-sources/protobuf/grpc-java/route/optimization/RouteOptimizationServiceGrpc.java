package route.optimization;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * Servicio principal para optimización de rutas
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.58.0)",
    comments = "Source: route_optimization.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class RouteOptimizationServiceGrpc {

  private RouteOptimizationServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "route.optimization.RouteOptimizationService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<route.optimization.RouteOptimization.RouteOptimizationRequest,
      route.optimization.RouteOptimization.RouteOptimizationResponse> getOptimizeRouteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "OptimizeRoute",
      requestType = route.optimization.RouteOptimization.RouteOptimizationRequest.class,
      responseType = route.optimization.RouteOptimization.RouteOptimizationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<route.optimization.RouteOptimization.RouteOptimizationRequest,
      route.optimization.RouteOptimization.RouteOptimizationResponse> getOptimizeRouteMethod() {
    io.grpc.MethodDescriptor<route.optimization.RouteOptimization.RouteOptimizationRequest, route.optimization.RouteOptimization.RouteOptimizationResponse> getOptimizeRouteMethod;
    if ((getOptimizeRouteMethod = RouteOptimizationServiceGrpc.getOptimizeRouteMethod) == null) {
      synchronized (RouteOptimizationServiceGrpc.class) {
        if ((getOptimizeRouteMethod = RouteOptimizationServiceGrpc.getOptimizeRouteMethod) == null) {
          RouteOptimizationServiceGrpc.getOptimizeRouteMethod = getOptimizeRouteMethod =
              io.grpc.MethodDescriptor.<route.optimization.RouteOptimization.RouteOptimizationRequest, route.optimization.RouteOptimization.RouteOptimizationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "OptimizeRoute"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  route.optimization.RouteOptimization.RouteOptimizationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  route.optimization.RouteOptimization.RouteOptimizationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RouteOptimizationServiceMethodDescriptorSupplier("OptimizeRoute"))
              .build();
        }
      }
    }
    return getOptimizeRouteMethod;
  }

  private static volatile io.grpc.MethodDescriptor<route.optimization.RouteOptimization.HealthRequest,
      route.optimization.RouteOptimization.HealthResponse> getHealthCheckMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "HealthCheck",
      requestType = route.optimization.RouteOptimization.HealthRequest.class,
      responseType = route.optimization.RouteOptimization.HealthResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<route.optimization.RouteOptimization.HealthRequest,
      route.optimization.RouteOptimization.HealthResponse> getHealthCheckMethod() {
    io.grpc.MethodDescriptor<route.optimization.RouteOptimization.HealthRequest, route.optimization.RouteOptimization.HealthResponse> getHealthCheckMethod;
    if ((getHealthCheckMethod = RouteOptimizationServiceGrpc.getHealthCheckMethod) == null) {
      synchronized (RouteOptimizationServiceGrpc.class) {
        if ((getHealthCheckMethod = RouteOptimizationServiceGrpc.getHealthCheckMethod) == null) {
          RouteOptimizationServiceGrpc.getHealthCheckMethod = getHealthCheckMethod =
              io.grpc.MethodDescriptor.<route.optimization.RouteOptimization.HealthRequest, route.optimization.RouteOptimization.HealthResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "HealthCheck"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  route.optimization.RouteOptimization.HealthRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  route.optimization.RouteOptimization.HealthResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RouteOptimizationServiceMethodDescriptorSupplier("HealthCheck"))
              .build();
        }
      }
    }
    return getHealthCheckMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static RouteOptimizationServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RouteOptimizationServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RouteOptimizationServiceStub>() {
        @java.lang.Override
        public RouteOptimizationServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RouteOptimizationServiceStub(channel, callOptions);
        }
      };
    return RouteOptimizationServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static RouteOptimizationServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RouteOptimizationServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RouteOptimizationServiceBlockingStub>() {
        @java.lang.Override
        public RouteOptimizationServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RouteOptimizationServiceBlockingStub(channel, callOptions);
        }
      };
    return RouteOptimizationServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static RouteOptimizationServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RouteOptimizationServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RouteOptimizationServiceFutureStub>() {
        @java.lang.Override
        public RouteOptimizationServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RouteOptimizationServiceFutureStub(channel, callOptions);
        }
      };
    return RouteOptimizationServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * Servicio principal para optimización de rutas
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * Método principal para ejecutar el modelo MRL-AMIS
     * </pre>
     */
    default void optimizeRoute(route.optimization.RouteOptimization.RouteOptimizationRequest request,
        io.grpc.stub.StreamObserver<route.optimization.RouteOptimization.RouteOptimizationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getOptimizeRouteMethod(), responseObserver);
    }

    /**
     * <pre>
     * Método para verificar el estado del servicio
     * </pre>
     */
    default void healthCheck(route.optimization.RouteOptimization.HealthRequest request,
        io.grpc.stub.StreamObserver<route.optimization.RouteOptimization.HealthResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getHealthCheckMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service RouteOptimizationService.
   * <pre>
   * Servicio principal para optimización de rutas
   * </pre>
   */
  public static abstract class RouteOptimizationServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return RouteOptimizationServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service RouteOptimizationService.
   * <pre>
   * Servicio principal para optimización de rutas
   * </pre>
   */
  public static final class RouteOptimizationServiceStub
      extends io.grpc.stub.AbstractAsyncStub<RouteOptimizationServiceStub> {
    private RouteOptimizationServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RouteOptimizationServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RouteOptimizationServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Método principal para ejecutar el modelo MRL-AMIS
     * </pre>
     */
    public void optimizeRoute(route.optimization.RouteOptimization.RouteOptimizationRequest request,
        io.grpc.stub.StreamObserver<route.optimization.RouteOptimization.RouteOptimizationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getOptimizeRouteMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Método para verificar el estado del servicio
     * </pre>
     */
    public void healthCheck(route.optimization.RouteOptimization.HealthRequest request,
        io.grpc.stub.StreamObserver<route.optimization.RouteOptimization.HealthResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getHealthCheckMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service RouteOptimizationService.
   * <pre>
   * Servicio principal para optimización de rutas
   * </pre>
   */
  public static final class RouteOptimizationServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<RouteOptimizationServiceBlockingStub> {
    private RouteOptimizationServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RouteOptimizationServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RouteOptimizationServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Método principal para ejecutar el modelo MRL-AMIS
     * </pre>
     */
    public route.optimization.RouteOptimization.RouteOptimizationResponse optimizeRoute(route.optimization.RouteOptimization.RouteOptimizationRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getOptimizeRouteMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Método para verificar el estado del servicio
     * </pre>
     */
    public route.optimization.RouteOptimization.HealthResponse healthCheck(route.optimization.RouteOptimization.HealthRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getHealthCheckMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service RouteOptimizationService.
   * <pre>
   * Servicio principal para optimización de rutas
   * </pre>
   */
  public static final class RouteOptimizationServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<RouteOptimizationServiceFutureStub> {
    private RouteOptimizationServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RouteOptimizationServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RouteOptimizationServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Método principal para ejecutar el modelo MRL-AMIS
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<route.optimization.RouteOptimization.RouteOptimizationResponse> optimizeRoute(
        route.optimization.RouteOptimization.RouteOptimizationRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getOptimizeRouteMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Método para verificar el estado del servicio
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<route.optimization.RouteOptimization.HealthResponse> healthCheck(
        route.optimization.RouteOptimization.HealthRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getHealthCheckMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_OPTIMIZE_ROUTE = 0;
  private static final int METHODID_HEALTH_CHECK = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_OPTIMIZE_ROUTE:
          serviceImpl.optimizeRoute((route.optimization.RouteOptimization.RouteOptimizationRequest) request,
              (io.grpc.stub.StreamObserver<route.optimization.RouteOptimization.RouteOptimizationResponse>) responseObserver);
          break;
        case METHODID_HEALTH_CHECK:
          serviceImpl.healthCheck((route.optimization.RouteOptimization.HealthRequest) request,
              (io.grpc.stub.StreamObserver<route.optimization.RouteOptimization.HealthResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getOptimizeRouteMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              route.optimization.RouteOptimization.RouteOptimizationRequest,
              route.optimization.RouteOptimization.RouteOptimizationResponse>(
                service, METHODID_OPTIMIZE_ROUTE)))
        .addMethod(
          getHealthCheckMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              route.optimization.RouteOptimization.HealthRequest,
              route.optimization.RouteOptimization.HealthResponse>(
                service, METHODID_HEALTH_CHECK)))
        .build();
  }

  private static abstract class RouteOptimizationServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    RouteOptimizationServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return route.optimization.RouteOptimization.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("RouteOptimizationService");
    }
  }

  private static final class RouteOptimizationServiceFileDescriptorSupplier
      extends RouteOptimizationServiceBaseDescriptorSupplier {
    RouteOptimizationServiceFileDescriptorSupplier() {}
  }

  private static final class RouteOptimizationServiceMethodDescriptorSupplier
      extends RouteOptimizationServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    RouteOptimizationServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (RouteOptimizationServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new RouteOptimizationServiceFileDescriptorSupplier())
              .addMethod(getOptimizeRouteMethod())
              .addMethod(getHealthCheckMethod())
              .build();
        }
      }
    }
    return result;
  }
}
