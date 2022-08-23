package io.github.profjb58.pumapbot.maps;

import net.dv8tion.jda.api.entities.EmbedType;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MapPaginator {

    private static final Emoji NAV_FORWARDS_EMOJI = Emoji.fromUnicode("U+27A1");
    private static final Emoji NAV_BACKWARDS_EMOJI = Emoji.fromUnicode("U+2B05");

    private final List<PUMap> maps;
    private Button backwardsButton, forwardsButton;
    private Button downloadPlayableButton, downloadEditableButton;
    private int currentPageIndex;
    private AtomicLong prevFileMsgId = new AtomicLong();

    public MapPaginator(List<PUMap> maps, @Nonnull InteractionHook hook) {
        this.maps = maps;
        this.forwardsButton = Button.primary("nav_forwards", NAV_FORWARDS_EMOJI);
        this.backwardsButton = Button.primary("nav_backwards", NAV_BACKWARDS_EMOJI).withDisabled(true);
        this.downloadPlayableButton = Button.success("download_playable_map", "💾 Playable");
        this.downloadEditableButton = Button.secondary("download_editable_map", "💾 Editable");
        currentPageIndex = 0;

        if(maps.size() == 1) // Disable forwards button if only one entry exists
            this.forwardsButton = forwardsButton.asDisabled();
        showEmbedDeferred(hook);
    }

    public void switchPage(@Nonnull ButtonInteractionEvent event, long prevPageId, boolean isForwards) {
        if(maps.size() > 1) {
            // Return early if we cannot navigate forwards or backwards
            if((currentPageIndex == 0 && !isForwards) || (currentPageIndex == (maps.size() - 1) && isForwards))
                return;

            // Remove previous page entry
            event.getChannel().deleteMessageById(prevPageId).queue();
            if(prevFileMsgId.get() != 0)
                event.getChannel().deleteMessageById(prevFileMsgId.get()).queue();

            if(isForwards) {
                currentPageIndex++;
                if(currentPageIndex == maps.size() - 1) {
                    forwardsButton = forwardsButton.asDisabled();
                    backwardsButton = backwardsButton.asEnabled();
                } else {
                    forwardsButton = forwardsButton.asEnabled();
                    backwardsButton = backwardsButton.asEnabled();
                }
            }
            else {
                currentPageIndex--;
                if(currentPageIndex == 0) {
                    backwardsButton = backwardsButton.asDisabled();
                    forwardsButton = forwardsButton.asEnabled();
                } else {
                    forwardsButton = forwardsButton.asEnabled();
                    backwardsButton = backwardsButton.asEnabled();
                }
            }
            showEmbed(event);
        }
    }

    private MessageEmbed getMessageEmbed() {
        // Send page embed message
        var pageMap = maps.get(currentPageIndex);
        var author = pageMap.author();

        if(pageMap.puFile() != null && pageMap.puFile().exists())
            downloadPlayableButton = downloadPlayableButton.asEnabled();
        else
            downloadPlayableButton = downloadEditableButton.asDisabled();

        if(pageMap.pumapFile() != null && pageMap.pumapFile().exists())
            downloadEditableButton = downloadEditableButton.asEnabled();
        else
            downloadEditableButton = downloadEditableButton.asDisabled();

        return new MessageEmbed(null, pageMap.name(), pageMap.description(), EmbedType.RICH, pageMap.dateUpdated(),
                0x3fc7f, null, null, new MessageEmbed.AuthorInfo(
                author.getName(), null, author.getAvatarUrl(), null
        ),
                null, null, null, List.of(
                new MessageEmbed.Field("Version", String.valueOf(pageMap.version()), true),
                new MessageEmbed.Field("Tags", pageMap.tags(), true),
                new MessageEmbed.Field("Downloads", String.valueOf(pageMap.downloads()), true)
        ));
    }

    private void showEmbedDeferred(@Nonnull InteractionHook hook) {
        hook.sendMessageEmbeds(getMessageEmbed())
                .addActionRow(
                        backwardsButton,
                        forwardsButton,
                        downloadPlayableButton,
                        downloadEditableButton
                ).setEphemeral(false).queue();
    }

    private void showEmbed(@Nonnull ButtonInteractionEvent event) {
        event.replyEmbeds(getMessageEmbed())
                .addActionRow(
                        backwardsButton,
                        forwardsButton,
                        downloadPlayableButton,
                        downloadEditableButton
                ).queue();
    }

    @Nullable
    public File getPlayableMapFile() {
        var pageMap = maps.get(currentPageIndex);
        if(pageMap.puFile() != null && pageMap.puFile().exists())
            return maps.get(currentPageIndex).puFile();
        return null;
    }

    @Nullable
    public File getEditableMapFile() {
        var pageMap = maps.get(currentPageIndex);
        if(pageMap.pumapFile() != null && pageMap.pumapFile().exists())
            return maps.get(currentPageIndex).pumapFile();
        return null;
    }
}
