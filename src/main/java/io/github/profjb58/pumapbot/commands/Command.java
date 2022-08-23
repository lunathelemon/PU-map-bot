package io.github.profjb58.pumapbot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import javax.annotation.Nonnull;

public abstract class Command {

    protected boolean isEphemeral = true;
    protected boolean expectPrivateResponse = false;
    protected boolean deferReply = true;

    protected abstract void onCommand(@Nonnull SlashCommandInteractionEvent interaction);

    public abstract CommandData getCommandData();
}
