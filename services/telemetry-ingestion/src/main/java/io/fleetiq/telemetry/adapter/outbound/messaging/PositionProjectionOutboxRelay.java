package io.fleetiq.telemetry.adapter.outbound.messaging;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;

import java.util.UUID;
import java.util.function.Function;

@ApplicationScoped
public class PositionProjectionOutboxRelay {

    private final PgPool pgPool;
    private final Function<byte[], Uni<Void>> publisher;

    private static final String CLAIM_BATCH = """
        SELECT id, payload
        FROM projection_outbox
        ORDER BY created_at
        LIMIT 100
        FOR UPDATE SKIP LOCKED
        """;

    @Inject
    public PositionProjectionOutboxRelay(PgPool pgPool,
        @Channel("position-projections-out") MutinyEmitter<byte[]> emitter) {
        this(pgPool, emitter::send);
    }

    PositionProjectionOutboxRelay(PgPool pgPool, Function<byte[], Uni<Void>> publisher) {
        this.pgPool = pgPool;
        this.publisher = publisher;
    }

    @Scheduled(every = "${fleetiq.outbox.relay-interval:1s}",
        concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    Uni<Void> relay() {
        return pgPool.withTransaction(connection -> connection.query(CLAIM_BATCH).execute()
            .onItem().transformToMulti(rows -> Multi.createFrom().iterable(rows))
            .onItem().transformToUniAndConcatenate(row -> {
                UUID id = row.getUUID("id");
                byte[] payload = row.getBuffer("payload").getBytes();
                return publisher.apply(payload)
                    .call(() -> connection.preparedQuery("DELETE FROM projection_outbox WHERE id = $1")
                        .execute(Tuple.of(id)));
            })
            .collect().asList().replaceWithVoid());
    }
}
