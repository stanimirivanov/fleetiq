package io.fleetiq.pekko.actor;

import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

public class DeadLetterMonitor extends AbstractBehavior<DeadLetterMonitor.DeadLetter> {

    public record DeadLetter(String message, String sender, String recipient) {}

    public static Behavior<DeadLetter> create() {
        return Behaviors.setup(DeadLetterMonitor::new);
    }

    private DeadLetterMonitor(ActorContext<DeadLetter> context) {
        super(context);
        context.getLog().info("Dead Letter Monitor started");
    }

    @Override
    public Receive<DeadLetter> createReceive() {
        return newReceiveBuilder()
            .onMessage(DeadLetter.class, this::onDeadLetter)
            .build();
    }

    private Behavior<DeadLetter> onDeadLetter(DeadLetter msg) {
        getContext().getLog().warn("Dead letter: {} -> {} : {}", msg.sender(), msg.recipient(), msg.message());
        return this;
    }
}
