package com.flipkart.foxtrot.client.cluster;

import com.flipkart.foxtrot.client.FoxtrotClientConfig;
import feign.Feign;
import feign.FeignException;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

public class ClusterStatusUpdater implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClusterStatusUpdater.class.getSimpleName());
    private final FoxtrotClusterHttpClient httpClient;

    private AtomicReference<FoxtrotClusterStatus> status;

    final static JacksonDecoder decoder = new JacksonDecoder();
    final static JacksonEncoder encoder = new JacksonEncoder();
    final static Slf4jLogger slf4jLogger = new Slf4jLogger();

    private ClusterStatusUpdater(AtomicReference<FoxtrotClusterStatus> status, FoxtrotClusterHttpClient httpClient) {
        this.status = status;
        this.httpClient = httpClient;
    }

    public static ClusterStatusUpdater create(FoxtrotClientConfig config,
                                              AtomicReference<FoxtrotClusterStatus> status) throws Exception {

        FoxtrotClusterHttpClient httpClient = Feign.builder()
                .decoder(decoder)
                .encoder(encoder)
                .client(new OkHttpClient())
                .logger(slf4jLogger)
                .logLevel(feign.Logger.Level.BASIC)
                .target(FoxtrotClusterHttpClient.class, String.format("http://%s:%s", config.getHost(), config.getPort()));
        return new ClusterStatusUpdater(status, httpClient);
    }

    @Override
    public void run() {
        loadClusterData();
    }

    public void loadClusterData() {
        try {
            logger.trace("Initiating data get");
            status.set(httpClient.load());
        } catch (Exception e) {
            if(ExceptionUtils.getRootCause(e) instanceof FeignException) {
                FeignException feignException = (FeignException)e;
                logger.error("Error getting status:{} - {}", feignException.status(), feignException.getMessage());
            }
            logger.error("Error getting cluster data: ", e);
        }
    }
}
