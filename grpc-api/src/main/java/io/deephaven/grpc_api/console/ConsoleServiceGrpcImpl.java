package io.deephaven.grpc_api.console;

import com.google.rpc.Code;
import io.deephaven.configuration.Configuration;
import io.deephaven.db.tables.Table;
import io.deephaven.db.tables.live.LiveTableMonitor;
import io.deephaven.db.tables.remote.preview.ColumnPreviewManager;
import io.deephaven.db.util.ExportedObjectType;
import io.deephaven.db.util.ScriptSession;
import io.deephaven.db.util.liveness.LivenessArtifact;
import io.deephaven.db.util.liveness.LivenessReferent;
import io.deephaven.grpc_api.session.SessionService;
import io.deephaven.grpc_api.session.SessionState;
import io.deephaven.grpc_api.table.TableServiceGrpcImpl;
import io.deephaven.grpc_api.util.GrpcUtil;
import io.deephaven.internal.log.LoggerFactory;
import io.deephaven.io.logger.LogBuffer;
import io.deephaven.io.logger.LogBufferRecord;
import io.deephaven.io.logger.LogBufferRecordListener;
import io.deephaven.io.logger.Logger;
import io.deephaven.proto.backplane.grpc.ExportedTableCreationResponse;
import io.deephaven.proto.backplane.grpc.TableReference;
import io.deephaven.proto.backplane.script.grpc.*;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;
import java.util.function.Consumer;

import static io.deephaven.grpc_api.util.GrpcUtil.safelyExecute;
import static io.deephaven.grpc_api.util.GrpcUtil.safelyExecuteLocked;

public class ConsoleServiceGrpcImpl extends ConsoleServiceGrpc.ConsoleServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(ConsoleServiceGrpcImpl.class);

    public static final boolean ALLOW_ADMIN_CONSOLE_ATTACHMENT = Configuration.getInstance().getBooleanWithDefault("ConsoleAttachment.allowAdmins", false);

    private final Map<String, Provider<ScriptSession>> scriptTypes;
    private final SessionService sessionService;
    private final LogBuffer logBuffer;
    private final LiveTableMonitor liveTableMonitor;

    @Inject
    public ConsoleServiceGrpcImpl(final Map<String, Provider<ScriptSession>> scriptTypes, final SessionService sessionService, final LogBuffer logBuffer, final LiveTableMonitor liveTableMonitor) {
        this.scriptTypes = scriptTypes;
        this.sessionService = sessionService;
        this.logBuffer = logBuffer;
        this.liveTableMonitor = liveTableMonitor;
    }

    @Override
    public void startConsole(StartConsoleRequest request, StreamObserver<StartConsoleResponse> responseObserver) {
        GrpcUtil.rpcWrapper(log, responseObserver, () -> {
            SessionState session = sessionService.getCurrentSession();
            // TODO auth hook, ensure the user can do this (owner of worker or admin)
//            session.getAuthContext().requirePrivilege(CreateConsole);

            session.newExport(request.getResultId())
                    .onError(responseObserver::onError)
                    .submit(() -> {
                        //ConsoleAttachmentQuery
                        String sessionType = request.getSessionType();

                        final ScriptSession scriptSession = scriptTypes.get(sessionType).get();

                        safelyExecute(() -> {
                            responseObserver.onNext(StartConsoleResponse.newBuilder()
                                    .setResultId(request.getResultId())
                                    .build());
                            responseObserver.onCompleted();
                        });
                        return scriptSession;
                    });
        });
    }

    @Override
    public void subscribeToLogs(LogSubscriptionRequest request, StreamObserver<LogSubscriptionData> responseObserver) {
        GrpcUtil.rpcWrapper(log, responseObserver, () -> {
            SessionState session = sessionService.getCurrentSession();
            // if that didn't fail, we at least are authenticated, but possibly not authorized
            // TODO auth hook, ensure the user can do this (owner of worker or admin). same rights as creating a console
//            session.getAuthContext().requirePrivilege(LogBuffer);

            LogBufferStreamAdapter listener = new LogBufferStreamAdapter(request, responseObserver, session::unmanageNonExport);
            ((ServerCallStreamObserver<LogSubscriptionData>) responseObserver).setOnCancelHandler(listener::destroy);
            session.manage(listener);
            logBuffer.subscribe(listener);
        });
    }

    @Override
    public void executeCommand(ExecuteCommandRequest request, StreamObserver<ExecuteCommandResponse> responseObserver) {
        GrpcUtil.rpcWrapper(log, responseObserver, () -> {
            final SessionState session = sessionService.getCurrentSession();

            SessionState.ExportObject<ScriptSession> exportedConsole = session.getExport(request.getConsoleId());
            session.nonExport()
                    .requireExclusiveLock()
                    .require(exportedConsole)
                    .onError(responseObserver::onError)
                    .submit(() -> {
                        ScriptSession scriptSession = exportedConsole.get();

                        //produce a diff
                        ExecuteCommandResponse.Builder diff = ExecuteCommandResponse.newBuilder();

                        ScriptSession.Changes changes = scriptSession.evaluateScript(request.getCode());

                        changes.created.entrySet().forEach(entry -> diff.addCreated(makeVariableDefinition(entry)));
                        changes.updated.entrySet().forEach(entry -> diff.addUpdated(makeVariableDefinition(entry)));
                        changes.removed.entrySet().forEach(entry -> diff.addRemoved(makeVariableDefinition(entry)));

                        responseObserver.onNext(diff.build());
                        responseObserver.onCompleted();
                    });
        });
    }

    private static VariableDefinition makeVariableDefinition(Map.Entry<String, ExportedObjectType> entry) {
        return VariableDefinition.newBuilder().setName(entry.getKey()).setType(entry.getValue().name()).build();
    }

    @Override
    public void cancelCommand(CancelCommandRequest request, StreamObserver<CancelCommandResponse> responseObserver) {
        // TODO not yet implemented, need a way to handle stopping a command in a consistent way
        super.cancelCommand(request, responseObserver);
    }

    @Override
    public void bindTableToVariable(BindTableToVariableRequest request, StreamObserver<BindTableToVariableResponse> responseObserver) {
        GrpcUtil.rpcWrapper(log, responseObserver, () -> {
            final SessionState session = sessionService.getCurrentSession();

            SessionState.ExportObject<ScriptSession> exportedConsole = session.getExport(request.getConsoleId());
            SessionState.ExportObject<Table> exportedTable = session.getExport(request.getTableId());
            session.nonExport()
                    .require(exportedConsole, exportedTable)
                    .submit(() -> {
                        exportedConsole.get().setVariable(request.getVariableName(), exportedTable.get());
                    });
        });
    }

    // TODO will be moved to a more general place, serve as a general "Fetch from scope" and this will be deprecated
    @Override
    public void fetchTable(FetchTableRequest request, StreamObserver<ExportedTableCreationResponse> responseObserver) {
        GrpcUtil.rpcWrapper(log, responseObserver, () -> {
            final SessionState session = sessionService.getCurrentSession();

            SessionState.ExportObject<ScriptSession> exportedConsole = session.getExport(request.getConsoleId());

            session.newExport(request.getTableId())
                    .require(exportedConsole)
                    .onError(responseObserver::onError)
                    .submit(() -> liveTableMonitor.exclusiveLock().computeLocked(() -> {
                        ScriptSession scriptSession = exportedConsole.get();
                        String tableName = request.getTableName();
                        if (!scriptSession.hasVariableName(tableName)) {
                            throw GrpcUtil.statusRuntimeException(Code.INVALID_ARGUMENT, "No value exists with name " + tableName);
                        }

                        // Explicit typecheck to catch any wrong-type-ness right away
                        Object result = scriptSession.unwrapObject(scriptSession.getVariable(tableName));
                        if (!(result instanceof Table)) {
                            throw GrpcUtil.statusRuntimeException(Code.INVALID_ARGUMENT, "Value bound to name " + tableName + " is not a Table");
                        }

                        // Apply preview columns TODO core#107 move to table service
                        Table table = ColumnPreviewManager.applyPreview((Table) result);

                        safelyExecute(() -> {
                            final TableReference resultRef = TableReference.newBuilder().setTicket(request.getTableId()).build();
                            responseObserver.onNext(TableServiceGrpcImpl.buildTableCreationResponse(resultRef, table));
                            responseObserver.onCompleted();
                        });
                        return table;
                    }));
        });
    }

    // TODO fetch other object types
    @Override
    public void fetchPandasTable(FetchPandasTableRequest request, StreamObserver<ExportedTableCreationResponse> responseObserver) {
        super.fetchPandasTable(request, responseObserver);
    }
    @Override
    public void fetchFigure(FetchFigureRequest request, StreamObserver<FetchFigureResponse> responseObserver) {
        super.fetchFigure(request, responseObserver);
    }
    @Override
    public void fetchTableMap(FetchTableMapRequest request, StreamObserver<FetchTableMapResponse> responseObserver) {
        super.fetchTableMap(request, responseObserver);
    }

    // TODO(core#101) autocomplete support
    @Override
    public void openDocument(OpenDocumentRequest request, StreamObserver<OpenDocumentResponse> responseObserver) {
        super.openDocument(request, responseObserver);
    }
    @Override
    public void changeDocument(ChangeDocumentRequest request, StreamObserver<ChangeDocumentResponse> responseObserver) {
        super.changeDocument(request, responseObserver);
    }
    @Override
    public void getCompletionItems(GetCompletionItemsRequest request, StreamObserver<GetCompletionItemsResponse> responseObserver) {
        super.getCompletionItems(request, responseObserver);
    }
    @Override
    public void closeDocument(CloseDocumentRequest request, StreamObserver<CloseDocumentResponse> responseObserver) {
        super.closeDocument(request, responseObserver);
    }

    private class LogBufferStreamAdapter extends LivenessArtifact implements LogBufferRecordListener {
        private final LogSubscriptionRequest request;
        private final StreamObserver<LogSubscriptionData> responseObserver;
        private final Consumer<LivenessReferent> unmanageLambda;
        private boolean isClosed = false;

        public LogBufferStreamAdapter(LogSubscriptionRequest request, StreamObserver<LogSubscriptionData> responseObserver, Consumer<LivenessReferent> unmanageLambda) {
            this.request = request;
            this.responseObserver = responseObserver;
            this.unmanageLambda = unmanageLambda;
        }

        @Override
        protected void destroy() {
            synchronized (this) {
                if (isClosed) {
                    return;
                }
                isClosed = true;
            }

            safelyExecute(() -> unmanageLambda.accept(this));
            safelyExecute(() -> logBuffer.unsubscribe(this));
            safelyExecuteLocked(responseObserver, responseObserver::onCompleted);
        }

        @Override
        public void record(LogBufferRecord record) {
            // only pass levels the client wants
            if (request.getLevelCount() != 0 && !request.getLevelList().contains(record.getLevel().getName())) {
                return;
            }

            // since the subscribe() method auto-replays all existing logs, filter to just once this
            // consumer probably hasn't seen
            if (record.getTimestampMicros() < request.getLastSeenLogTimestamp()) {
                return;
            }

            // TODO this is not a good implementation, just a quick one, but it does appear to be safe,
            //      since LogBuffer is synchronized on access to the listeners. We're on the same thread
            //      as all other log receivers and
            try {
                LogSubscriptionData payload = LogSubscriptionData.newBuilder()
                        .setMicros(record.getTimestampMicros())
                        .setLogLevel(record.getLevel().getName())
                        //this could be done on either side, doing it here because its a weird charset and we should own that
                        .setMessage(record.getDataString())
                        .build();
                synchronized (responseObserver) {
                    responseObserver.onNext(payload);
                }
            } catch (Throwable t) {
                // we are ignoring exceptions here deliberately, and just shutting down
                destroy();
            }
        }
    }
}
