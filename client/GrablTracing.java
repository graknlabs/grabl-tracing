package grabl.tracing.client;

import io.grpc.ManagedChannelBuilder;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Grabl Tracing client.
 */
public interface GrablTracing extends AutoCloseable {

    /**
     * Create a new performance analysis in Grabl to add new traces to.
     *
     * @param owner The repository organisation/owner.
     * @param repo The repository.
     * @param commit The commit in this repository.
     * @return An instance of the Analysis to send traces on.
     */
    Analysis analysis(String owner, String repo, String commit, String name);

    /**
    * Begin a continuation trace from a given root and parent ID. This trace links to a trace tree from an analysis
    * that may have been created from another application. This starts the trace immediately with a timestamp generated
    * from {@link System#currentTimeMillis()}.
    *
    * @param rootId The trace tree rootId.
    * @param parentId The trace parentId.
    * @param name The trace name.
    * @return An instance of the Trace to send further data on.
    */
    Trace trace(UUID rootId, UUID parentId, String name);

    /**
     * Represents a performance analysis that can receive further tracing data.
     */
    interface Analysis {
        /**
         * Begin a new trace tree on this performance analysis. This starts the trace immediately with a timestamp
         * generated from {@link System#currentTimeMillis()}.
         *
         * @param name The trace name.
         * @param tracker The tracker to link this trace tree to.
         * @param iteration The iteration of this trace.
         * @return An instance for the root trace to send further data and traces on.
         */
        Trace trace(String name, String tracker, int iteration);
    }

    /**
     * Represents a started trace in some trace tree. This starts the trace immediately with a timestamp generated from
     * {@link System#currentTimeMillis()}.
     */
    interface Trace {
        /**
         * Begin a new child trace of this trace.
         *
         * @param name The child trace name (will be appended to the trace path for the child).
         * @return An instance of the child trace.
         */
        Trace trace(String name);

        /**
         * Submit text data for the trace (such as JSON). Behaviour when called more than once on an instance is
         * undefined and should be avoided.
         *
         * @param data Trace data.
         * @return This trace.
         */
        Trace data(String data);

        /**
         * Submit String labels to add to this trace. This may be called many times as each call is additive.
         *
         * @param labels Labels to append to this instance.
         * @return This trace.
         */
        Trace labels(String... labels);

        /**
         * Ends the trace immediately, using the current time from {@link System#currentTimeMillis()} as the end
         * timestamp.
         *
         * Once end is called, further method calls should not be made. Messages are serialized but method access is
         * not. As such, it is up to the user to ensure they do not call this method out of order with other methods
         * when accessed by other threads.
         *
         * @return This trace.
         */
        Trace end();

        /**
         * Get the root ID for this trace. (If this trace is a root trace, this ID will return the same value as
         * {@link #getId()}.)
         *
         * @return The root trace ID for this Trace's trace tree.
         */
        UUID getRootId();

        /**
         * Get the ID for this trace.
         *
         * @return The ID for this trace.
         */
        UUID getId();
    }

    /**
     * Connect to the Grabl tracing server with TLS and providing
     *
     * @param grablUri The URI of your Grabl server.
     * @param username Your username on the Grabl server.
     * @param apiToken Your API token for the username.
     * @return An instance that has securely connected to your Grabl server.
     */
    static GrablTracing tracing(String grablUri, String username, String apiToken) {
        return new GrablTracingStandard(
                ManagedChannelBuilder.forTarget(grablUri)
                        .keepAliveTime(1, TimeUnit.MINUTES)
                        .keepAliveWithoutCalls(true)
                        .useTransportSecurity()
                        .intercept(new GrablTokenAuthClientInterceptor(username, apiToken))
                        .build()
        );
    }

    /**
     * A plaintext variation of Grabl tracing, useful for testing the tracing protocol but should not be used in real
     * applications.
     *
     * @param grablUri The URI of your test tracing server.
     * @return An instance that has connected to your server without any authentication.
     */
    static GrablTracing tracing(String grablUri) {
        return new GrablTracingStandard(
                ManagedChannelBuilder.forTarget(grablUri)
                        .keepAliveTime(1, TimeUnit.MINUTES)
                        .keepAliveWithoutCalls(true)
                        .usePlaintext()
                        .build()
        );
    }

    /**
     * Get a GrablTracing that can be used to safely run tracing-enabled applications with no connection and minimal
     * associated overhead.
     *
     * @return an instance that does nothing but adheres to the {@link GrablTracing} contract sufficiently to work with
     *      any code that does tracing.
     */
    static GrablTracing tracingNoOp() {
        return GrablTracingNoOp.getInstance();
    }

    /**
     * Decorate a GrablTracing with Slf4j logging (if the logging is enabled to the TRACE level).
     *
     * @param inner The actual GrablTracing that underlies this implementation.
     * @return If logging is enabled, a wrapped instance that logs to Slf4j, otherwise the instance {@param inner}.
     */
    static GrablTracing withLogging(GrablTracing inner) {
        return GrablTracingSlf4j.wrapIfLoggingEnabled(inner);
    }
}
