package grabl.tracing.client;

import java.util.UUID;

/**
 * A simple no-operation GrablTracing.
 *
 * Uses the Bill Pugh method of lazy singleton instantiation.
 */
public class GrablTracingNoOp implements GrablTracing {

    private GrablTracingNoOp() {
    }

    private static class LazyHolder {
        private static final GrablTracingNoOp TRACING = new GrablTracingNoOp();
        private static final TraceImpl TRACE = new TraceImpl();
        private static final AnalysisImpl ANALYSIS = new AnalysisImpl();
    }

    static GrablTracing getInstance() {
        return LazyHolder.TRACING;
    }

    @Override
    public Trace trace(UUID rootId, UUID parentId, String name) {
        return LazyHolder.TRACE;
    }

    @Override
    public Analysis analysis(String owner, String repo, String commit, String name) {
        return LazyHolder.ANALYSIS;
    }

    @Override
    public void close() {

    }

    private static class TraceImpl implements Trace {

        @Override
        public Trace trace(String name) {
            return LazyHolder.TRACE;
        }

        @Override
        public Trace data(String data) {
            return LazyHolder.TRACE;
        }

        @Override
        public Trace labels(String... labels) {
            return LazyHolder.TRACE;
        }

        @Override
        public Trace end() {
            return LazyHolder.TRACE;
        }

        @Override
        public UUID getRootId() {
            return null;
        }

        @Override
        public UUID getId() {
            return null;
        }
    }

    private static class AnalysisImpl implements Analysis {

        @Override
        public Trace trace(String name, String tracker, int iteration) {
            return LazyHolder.TRACE;
        }
    }
}
