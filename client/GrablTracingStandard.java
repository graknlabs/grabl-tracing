package grabl.tracing.client;

import grabl.tracing.protocol.TracingProto;
import grabl.tracing.protocol.TracingServiceGrpc;
import grabl.tracing.protocol.TracingServiceGrpc.TracingServiceBlockingStub;
import grabl.tracing.protocol.TracingServiceGrpc.TracingServiceStub;
import io.grpc.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static grabl.tracing.util.ProtobufUUIDUtil.fromBuf;
import static java.util.Objects.requireNonNull;

public class GrablTracingStandard implements GrablTracing {
    private final ManagedChannel channel;
    private final TracingServiceBlockingStub tracingServiceBlockingStub;
    private final TracingServiceStub tracingServiceStub;

    private final TraceStream stream;

    public GrablTracingStandard(ManagedChannel channel) {
        this.channel = channel;
        tracingServiceBlockingStub = TracingServiceGrpc.newBlockingStub(channel);
        tracingServiceStub = TracingServiceGrpc.newStub(channel);
        stream = new TraceStream(tracingServiceStub);
    }

    public Trace trace(UUID rootId, UUID parentId, String name) {
        requireNonNull(rootId, "Cannot use null rootId");
        requireNonNull(parentId, "Cannot use null traceId");
        requireNonNull(name, "Cannot use null name");
        return new TraceImpl(rootId, parentId, name);
    }

    public Analysis analysis(String owner, String repo, String commit) {
        requireNonNull(owner, "Cannot use null owner");
        requireNonNull(repo, "Cannot use null repo");
        requireNonNull(commit, "Cannot use null commit");
        return new AnalysisImpl(owner, repo, commit);
    }

    @Override
    public void close() throws Exception {
        stream.close();
        channel.shutdown();
        channel.awaitTermination(10, TimeUnit.SECONDS);
        if (!channel.isTerminated()) {
            channel.shutdownNow();
        }
    }

    private class AnalysisImpl implements Analysis {

        private final UUID analysisId;

        private AnalysisImpl(String owner, String repo, String commit) {
            TracingProto.Analysis.Req req = TracingProto.Analysis.Req.newBuilder()
                    .setOwner(owner)
                    .setRepo(repo)
                    .setCommit(commit)
                    .build();
            TracingProto.Analysis.Res res = tracingServiceBlockingStub.create(req);
            analysisId = fromBuf(res.getAnalysisId());
        }

        public Trace trace(String name, String tracker, int iteration) {
            requireNonNull(name, "Cannot use null name");
            requireNonNull(tracker, "Cannot use null tracker");
            return new TraceImpl(analysisId, name, tracker, iteration);
        }
    }

    private class TraceImpl implements Trace {
        private final UUID id;
        private final UUID rootId;

        private TraceImpl(UUID analysisId, String name, String tracker, int iteration) {
            id = UUID.randomUUID();
            rootId = id;
            stream.traceRootStart(id, analysisId, name, tracker, iteration, System.currentTimeMillis());
        }

        private TraceImpl(UUID rootId, UUID parentId, String name) {
            this.rootId = rootId;
            id = UUID.randomUUID();
            stream.traceChildStart(rootId, id, parentId, name, System.currentTimeMillis());
        }

        public Trace trace(String name) {
            requireNonNull(name, "Cannot use null name");
            return new TraceImpl(rootId, id, name);
        }

        public Trace data(String data) {
            requireNonNull(data, "Cannot use null data");
            stream.traceData(rootId, id, data);
            return this;
        }

        public Trace labels(String... labels) {
            stream.traceLabels(rootId, id, labels);
            return this;
        }

        public Trace end() {
            stream.traceEnd(rootId, id, System.currentTimeMillis());
            return this;
        }

        @Override
        public UUID getRootId() {
            return rootId;
        }

        @Override
        public UUID getId() {
            return id;
        }
    }
}
