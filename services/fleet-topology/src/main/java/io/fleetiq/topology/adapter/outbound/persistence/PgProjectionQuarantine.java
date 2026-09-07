package io.fleetiq.topology.adapter.outbound.persistence;

import io.fleetiq.topology.domain.port.outbound.ProjectionQuarantine;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/** PostgreSQL quarantine used for inspection and deliberate replay of poison events. */
@ApplicationScoped
@RequiredArgsConstructor
public class PgProjectionQuarantine implements ProjectionQuarantine {

    private static final String INSERT = """
        INSERT INTO projection_quarantine
            (id, event_type, payload, failure_class, failure_message, attempts)
        VALUES ($1, $2, $3, $4, $5, $6)
        """;

    private final PgPool pgPool;

    @Override
    public Uni<Void> quarantine(String eventType, byte[] payload, Throwable failure, int attempts) {
        String message = failure.getMessage();
        if (message != null && message.length() > 2_000) {
            message = message.substring(0, 2_000);
        }
        return pgPool.preparedQuery(INSERT).execute(Tuple.of(
            UUID.randomUUID(), eventType, payload, failure.getClass().getName(), message, attempts))
            .replaceWithVoid();
    }
}
