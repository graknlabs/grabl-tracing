package grabl.tracing.example;

import grabl.tracing.client.GrablTracing;
import grabl.tracing.client.GrablTracing.Analysis;
import grabl.tracing.client.GrablTracing.Trace;

import static grabl.tracing.client.GrablTracing.tracing;
import static grabl.tracing.client.GrablTracing.withLogging;

public class ExampleTracingClient {

    public static void main(String[] args) {
        int iterations = 10;
        if (args.length > 1) {
            iterations = Integer.parseInt(args[1]);
        }

        try (GrablTracing tracing = withLogging(tracing(args[0]))) {

            Analysis analysis = tracing.analysis("testowner", "testrepo", "testcommit", "testanalysis");

            for (int i = 0; i < iterations; ++i) {
                Trace outerTrace = analysis.trace("test.root", "my:tracker", i);
                outerTrace.data("my data");
                outerTrace.labels("my", "labels");

                tracedFunction(Integer.parseInt(args[2]), Integer.parseInt(args[3]), outerTrace);

                outerTrace.end();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void tracedFunction(int depth, int width, Trace trace) {
        if (depth > 0) {
            for (int i = 0; i < width; i++) {
                Trace inner = trace.trace("depth-" + depth + "-iter-" + i);

                tracedFunction(depth - 1, width, inner);

                inner.labels("label1", "label2");
                inner.data("data");
                inner.end();
            }
        }
    }
}
