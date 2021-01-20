package org.tgt.async1710;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class MonitorRegistry {
    private static PrometheusMeterRegistry meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    public static PrometheusMeterRegistry getInstance() {
        return meterRegistry;
    }
    public static void startPrometheusServer() {
        meterRegistry.config().meterFilter(
                new MeterFilter() {
                    @Override
                    public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                        return DistributionStatisticConfig.builder()
                                .percentiles(0,0.25,0.5,0.75,1.0)
                                .build()
                                .merge(config);
                    }
                }
        );
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(7070), 0);
            server.createContext("/metrics", httpExchange -> {
                String response = getInstance().scrape();
                httpExchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });

            Thread thread = new Thread(server::start);
            thread.setDaemon(true);
            thread.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
