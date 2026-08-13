package io.fleetiq.telemetry.adapter.outbound.messaging;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class PositionProjectionOutboxRelayIT {

    @Inject PgPool pgPool;

    @Test
    @RunOnVertxContext
    void competingRelaysPublishEachRecordOnce(UniAsserter asserter) {
        List<Integer> published = new CopyOnWriteArrayList<>();
        var first = new PositionProjectionOutboxRelay(pgPool,
            payload -> Uni.createFrom().item(() -> { published.add((int) payload[0]); return null; }));
        var second = new PositionProjectionOutboxRelay(pgPool,
            payload -> Uni.createFrom().item(() -> { published.add((int) payload[0]); return null; }));
        asserter.execute(this::reset);
        asserter.execute(() -> insert((byte) 1));
        asserter.execute(() -> insert((byte) 2));
        asserter.execute(() -> Uni.combine().all().unis(first.relay(), second.relay()).discardItems());
        asserter.assertThat(this::countRows, count -> {
            assertEquals(0L, count);
            assertEquals(List.of(1, 2), published.stream().sorted().toList());
        });
    }

    @Test
    @RunOnVertxContext
    void failedPublicationRollsBackDeletion(UniAsserter asserter) {
        var relay = new PositionProjectionOutboxRelay(pgPool,
            ignored -> Uni.createFrom().failure(new IllegalStateException("broker unavailable")));
        asserter.execute(this::reset);
        asserter.execute(() -> insert((byte) 3));
        asserter.assertFailedWith(relay::relay, IllegalStateException.class);
        asserter.assertEquals(this::countRows, 1L);
    }

    private Uni<?> insert(byte payload) {
        return pgPool.preparedQuery("""
            INSERT INTO projection_outbox (id, event_type, payload)
            VALUES (gen_random_uuid(), 'test', $1)
            """).execute(io.vertx.mutiny.sqlclient.Tuple.of(new byte[]{payload}));
    }

    private Uni<Long> countRows() {
        return pgPool.query("SELECT count(*) AS count FROM projection_outbox").execute()
            .map(rows -> rows.iterator().next().getLong("count"));
    }

    private Uni<?> reset() {
        return pgPool.query("TRUNCATE TABLE projection_outbox").execute();
    }
}
