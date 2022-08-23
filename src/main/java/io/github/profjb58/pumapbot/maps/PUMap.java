package io.github.profjb58.pumapbot.maps;

import net.dv8tion.jda.api.entities.User;

import javax.annotation.Nullable;
import java.io.File;
import java.time.OffsetDateTime;

public record PUMap(long discordMessageId, String name, double version, String description, User author, OffsetDateTime dateUpdated, @Nullable String tags, int downloads, File puFile, @Nullable File pumapFile) {}
