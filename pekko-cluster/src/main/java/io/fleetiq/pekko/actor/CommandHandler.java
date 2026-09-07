package io.fleetiq.pekko.actor;

import io.fleetiq.pekko.serialization.CborSerializable;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Receive;

/**
 * Minimal command-processing actor retained as an extension point for device command delivery.
 * The baseline implementation records receipt only; it does not claim that a command reached a
 * physical vehicle. Production acknowledgement semantics belong in a future outbound adapter.
 */
public class CommandHandler extends AbstractBehavior<CommandHandler.ProcessCommand> {

    public record ProcessCommand(String vin, String command, String payload) implements CborSerializable {}

    public static Behavior<ProcessCommand> create() {
        return org.apache.pekko.actor.typed.javadsl.Behaviors.setup(CommandHandler::new);
    }

    private CommandHandler(ActorContext<ProcessCommand> context) {
        super(context);
        context.getLog().info("Command Handler started");
    }

    @Override
    public Receive<ProcessCommand> createReceive() {
        return newReceiveBuilder()
            .onMessage(ProcessCommand.class, this::onProcessCommand)
            .build();
    }

    private Behavior<ProcessCommand> onProcessCommand(ProcessCommand msg) {
        getContext().getLog().info("Processing command for {}: {}", msg.vin(), msg.command());
        return this;
    }
}
