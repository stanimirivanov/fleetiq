package io.fleetiq.pekko.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PekkoConfig {

    private static final Logger log = LoggerFactory.getLogger(PekkoConfig.class);

    public static void logConfiguration(com.typesafe.config.Config config) {
        log.info("Pekko Cluster Configuration:");
        log.info("  Actor Provider: {}", config.getString("pekko.actor.provider"));
        log.info("  Seed Nodes: {}", config.getStringList("pekko.cluster.seed-nodes"));
    }
}
