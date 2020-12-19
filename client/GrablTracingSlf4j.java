package grabl.tracing.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

public class GrablTracingSlf4j implements GrablTracing {
    private static final Logger LOG = LoggerFactory.getLogger(GrablTracingSlf4j.class);

    private final GrablTracing innerTracing;

    private GrablTracingSlf4j(GrablTracing inner) {
        innerTracing = inner;
    }

    static GrablTracing wrapIfLoggingEnabled(GrablTracing inner) {
        if (LOG.isTraceEnabled()) {
            return new GrablTracingSlf4j(inner);
        } else {
            return inner;
        }
    }

    @Override
    public Trace trace(UUID rootId, UUID parentId, String name) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("trace: {} {} {} {}", rootId, parentId, name, Instant.now());
        }
        return new TraceImpl(innerTracing.trace(rootId, parentId, name), name);
    }

    @Override
    public Analysis analysis(String owner, String repo, String commit, String name) {
        LOG.trace("analysis: {} {} {} {}", owner, repo, commit, name);
        return new AnalysisImpl(innerTracing.analysis(owner, repo, commit, name));
    }

    @Override
    public void close() throws Exception {
        LOG.trace("close");
        innerTracing.close();
    }

    private static class TraceImpl implements Trace {

        private final Trace innerTrace;
        private final String name;

        private TraceImpl(Trace innerTrace, String name) {
            this.innerTrace = innerTrace;
            this.name = name;
        }

        @Override
        public Trace trace(String name) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("trace.trace: {} {}", name, Instant.now());
            }
            return new TraceImpl(innerTrace.trace(name), name);
        }

        @Override
        public Trace data(String data) {
            LOG.trace("trace.data: {} {}", name, data);
            return wrapIfNecessary(innerTrace.data(data));
        }

        @Override
        public Trace labels(String... labels) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("trace.labels: {} {}", name, String.join(", ", Arrays.asList(labels)));
            }
            return wrapIfNecessary(innerTrace.labels(labels));
        }

        @Override
        public Trace end() {
            if (LOG.isTraceEnabled()) {
                LOG.trace("trace.end: {} {}", name, Instant.now());
            }
            return wrapIfNecessary(innerTrace.end());
        }

        @Override
        public UUID getRootId() {
            return innerTrace.getRootId();
        }

        @Override
        public UUID getId() {
            return innerTrace.getId();
        }

        private TraceImpl wrapIfNecessary(Trace returnedTrace) {
            return returnedTrace == innerTrace ? this : new TraceImpl(returnedTrace, name);
        }
    }

    private static class AnalysisImpl implements Analysis {

        private final Analysis innerAnalysis;

        private AnalysisImpl(Analysis innerAnalysis) {
            this.innerAnalysis = innerAnalysis;
        }

        @Override
        public Trace trace(String name, String tracker, int iteration) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("analysis.trace: {} {} {} {}", name, tracker, iteration, Instant.now());
            }
            return new TraceImpl(innerAnalysis.trace(name, tracker, iteration), name);
        }
    }
}
