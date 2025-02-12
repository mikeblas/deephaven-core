package io.deephaven.grpc_api.session;

import io.deephaven.io.logger.Logger;
import com.google.protobuf.ByteString;
import com.google.rpc.Code;
import io.deephaven.util.auth.AuthContext;
import io.deephaven.grpc_api.auth.AuthContextProvider;
import io.deephaven.grpc_api.util.GrpcUtil;
import io.deephaven.internal.log.LoggerFactory;
import io.deephaven.proto.backplane.grpc.ExportNotification;
import io.deephaven.proto.backplane.grpc.ExportNotificationRequest;
import io.deephaven.proto.backplane.grpc.HandshakeRequest;
import io.deephaven.proto.backplane.grpc.HandshakeResponse;
import io.deephaven.proto.backplane.grpc.ReleaseResponse;
import io.deephaven.proto.backplane.grpc.SessionServiceGrpc;
import io.deephaven.proto.backplane.grpc.Ticket;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.stub.StreamObserver;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.UUID;

public class SessionServiceGrpcImpl extends SessionServiceGrpc.SessionServiceImplBase {
    public static final String DEEPHAVEN_SESSION_ID = "DEEPHAVEN_SESSION_ID";
    public static final Metadata.Key<String> SESSION_HEADER_KEY = Metadata.Key.of(DEEPHAVEN_SESSION_ID, Metadata.ASCII_STRING_MARSHALLER);
    public static final Context.Key<SessionState> SESSION_CONTEXT_KEY = Context.key(DEEPHAVEN_SESSION_ID);

    private static final Logger log = LoggerFactory.getLogger(SessionServiceGrpcImpl.class);

    private final SessionService service;
    private final AuthContextProvider authProvider;

    @Inject()
    public SessionServiceGrpcImpl(final SessionService service, final AuthContextProvider authProvider) {
        this.service = service;
        this.authProvider = authProvider;
    }

    @Override
    public void newSession(final HandshakeRequest request, final StreamObserver<HandshakeResponse> responseObserver) {
        GrpcUtil.rpcWrapper(log, responseObserver, () -> {
            if (!authProvider.supportsProtocol(request.getAuthProtocol())) {
                responseObserver.onError(GrpcUtil.statusRuntimeException(Code.INVALID_ARGUMENT, "Protocol version not allowed."));
                return;
            }

            final AuthContext authContext = authProvider.authenticate(request.getAuthProtocol(), request.getPayload());
            if (authContext == null) {
                responseObserver.onError(GrpcUtil.statusRuntimeException(Code.UNAUTHENTICATED, "Authentication failed."));
                return;
            }

            final SessionState session = service.newSession(authContext);
            responseObserver.onNext(HandshakeResponse.newBuilder()
                    .setMetadataHeader(ByteString.copyFromUtf8(DEEPHAVEN_SESSION_ID))
                    .setSessionToken(session.getExpiration().getTokenAsByteString())
                    .setTokenDeadlineTimeMillis(session.getExpiration().deadline.getMillis())
                    .setTokenExpirationDelayMillis(service.getExpirationDelayMs())
                    .build());

            responseObserver.onCompleted();
        });
    }

    @Override
    public void refreshSessionToken(final HandshakeRequest request, final StreamObserver<HandshakeResponse> responseObserver) {
        GrpcUtil.rpcWrapper(log, responseObserver, () -> {
            if (request.getAuthProtocol() != 0) {
                responseObserver.onError(GrpcUtil.statusRuntimeException(Code.INVALID_ARGUMENT, "Protocol version not allowed."));
                return;
            }

            final SessionState session = service.getCurrentSession();
            if (session != service.getSessionForToken(UUID.fromString(request.getPayload().toStringUtf8()))) {
                responseObserver.onError(GrpcUtil.statusRuntimeException(Code.INVALID_ARGUMENT, "Refresh request's session ID does not match metadata header provided ID."));
                return;
            }

            final SessionService.TokenExpiration expiration = service.refreshToken(session);

            responseObserver.onNext(HandshakeResponse.newBuilder()
                    .setMetadataHeader(ByteString.copyFromUtf8(DEEPHAVEN_SESSION_ID))
                    .setSessionToken(expiration.getTokenAsByteString())
                    .setTokenDeadlineTimeMillis(expiration.deadline.getMillis())
                    .setTokenExpirationDelayMillis(service.getExpirationDelayMs())
                    .build());

            responseObserver.onCompleted();
        });
    }

    @Override
    public void closeSession(final HandshakeRequest request, final StreamObserver<ReleaseResponse> responseObserver) {
        GrpcUtil.rpcWrapper(log, responseObserver, () -> {
            if (request.getAuthProtocol() != 0) {
                responseObserver.onError(GrpcUtil.statusRuntimeException(Code.INVALID_ARGUMENT, "Protocol version not allowed."));
                return;
            }

            final SessionState session = service.getCurrentSession();
            if (session != service.getSessionForToken(UUID.fromString(request.getPayload().toStringUtf8()))) {
                responseObserver.onError(GrpcUtil.statusRuntimeException(Code.INVALID_ARGUMENT, "Refresh request's session ID does not match metadata header provided ID."));
                return;
            }

            service.closeSession(session);
            responseObserver.onNext(ReleaseResponse.newBuilder().setSuccess(true).build());
            responseObserver.onCompleted();
        });
    }

    @Override
    public void release(final Ticket request, final StreamObserver<ReleaseResponse> responseObserver) {
        GrpcUtil.rpcWrapper(log, responseObserver, () -> {
            final SessionState.ExportObject<?> export = service.getCurrentSession().getExport(request);
            final SessionState.ExportState currState = export.getState();
            export.release();
            responseObserver.onNext(ReleaseResponse.newBuilder().setSuccess(currState != SessionState.ExportState.UNKNOWN).build());
            responseObserver.onCompleted();
        });
    }

    @Override
    public void exportNotifications(final ExportNotificationRequest request, final StreamObserver<ExportNotification> responseObserver) {
        GrpcUtil.rpcWrapper(log, responseObserver, () -> {
            service.getCurrentSession().addExportListener(responseObserver);
        });
    }

    @Singleton
    public static class AuthServerInterceptor implements ServerInterceptor {
        private final SessionService service;

        @Inject()
        public AuthServerInterceptor(final SessionService service) {
            this.service = service;
        }

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(final ServerCall<ReqT, RespT> serverCall,
                                                                     final Metadata metadata,
                                                                     final ServerCallHandler<ReqT, RespT> serverCallHandler) {
            final Optional<String> tokenBytes = Optional.ofNullable(metadata.get(SESSION_HEADER_KEY));
            final Optional<UUID> token = tokenBytes.map(UUID::fromString);
            final Context newContext = Context.current()
                    .withValue(SESSION_CONTEXT_KEY, token.map(service::getSessionForToken).orElse(null));
            return Contexts.interceptCall(newContext, serverCall, metadata, serverCallHandler);
        }
    }
}
