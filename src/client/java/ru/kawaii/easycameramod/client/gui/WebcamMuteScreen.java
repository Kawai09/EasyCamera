package ru.kawaii.easycameramod.client.gui;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import ru.kawaii.easycameramod.client.MutedPlayersManager;
import java.util.*;

@Environment(EnvType.CLIENT)
public class WebcamMuteScreen extends Screen {

    private final Screen parent;
    private PlayerListWidget playerListWidget;
    private TextFieldWidget searchField;
    private List<PlayerListEntry> allPlayers = new ArrayList<>();


    public WebcamMuteScreen(@Nullable Screen parent) {
        super(Text.translatable("gui.easycameramod.mute.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        if (this.client == null || this.client.getNetworkHandler() == null) return;
        
        this.allPlayers = new ArrayList<>(this.client.getNetworkHandler().getPlayerList());
        
        this.searchField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 25, 200, 20, Text.translatable("gui.easycameramod.mute.search"));
        this.searchField.setChangedListener(this::onSearchChanged);
        this.addDrawableChild(this.searchField);
        
        this.playerListWidget = new PlayerListWidget(this.client, this.width, this.height, 50, this.height - 40);
        this.addDrawableChild(playerListWidget);

        this.playerListWidget.filter(this.allPlayers, "");
        
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), (btn) -> this.close())
                .position(this.width / 2 - 100, this.height - 30).size(200, 20).build());
    }

    @Override
    public void close() {
        if (this.parent != null) {
            Objects.requireNonNull(this.client).setScreen(this.parent);
        } else {
            super.close();
        }
    }

    private void onSearchChanged(String text) {
        this.playerListWidget.filter(this.allPlayers, text);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
        this.searchField.render(context, mouseX, mouseY, delta);
    }

    @Environment(EnvType.CLIENT)
    private static class PlayerListWidget extends ElementListWidget<PlayerListWidget.PlayerEntry> {

        public PlayerListWidget(MinecraftClient client, int width, int height, int top, int bottom) {
            super(client, width, height, top, 32);
        }
        
        public void filter(List<PlayerListEntry> allPlayers, String filter) {
            this.clearEntries();
            for (PlayerListEntry entry : allPlayers) {
                if (entry == null || entry.getProfile() == null) continue;
                
                String name = entry.getProfile().getName();
                UUID uuid = entry.getProfile().getId();

                if (uuid == null || (this.client != null && uuid.equals(this.client.player.getUuid()))) continue;

                if (filter == null || filter.isEmpty() || name.toLowerCase().contains(filter.toLowerCase())) {
                    boolean muted = MutedPlayersManager.isMuted(uuid);
                    this.addEntry(new PlayerEntry(uuid, name, muted, entry.getSkinTextures()));
                }
            }
        }
        
        @Override
        public int getRowWidth() {
            return super.getRowWidth() - 6;
        }

        @Environment(EnvType.CLIENT)
        public static class PlayerEntry extends ElementListWidget.Entry<PlayerEntry> {
            private final UUID uuid;
            private final String name;
            private final ButtonWidget muteButton;
            private final SkinTextures skinTextures;
            private final MinecraftClient client;

            public PlayerEntry(UUID uuid, String name, boolean muted, SkinTextures skinTextures) {
                this.client = MinecraftClient.getInstance();
                this.uuid = uuid;
                this.name = name;
                this.skinTextures = skinTextures;

                this.muteButton = ButtonWidget.builder(Text.translatable(muted ? "gui.easycameramod.mute.show" : "gui.easycameramod.mute.hide"), (btn) -> {
                    if (MutedPlayersManager.isMuted(uuid)) {
                        MutedPlayersManager.unmute(uuid);
                        btn.setMessage(Text.translatable("gui.easycameramod.mute.hide"));
                    } else {
                        MutedPlayersManager.mute(uuid);
                        btn.setMessage(Text.translatable("gui.easycameramod.mute.show"));
                    }
                }).size(60, 20).build();
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int headSize = 20;
                int headX = x + 5;
                int headY = y + (entryHeight - headSize) / 2;

                PlayerSkinDrawer.draw(context, this.skinTextures, headX, headY, headSize);

                if (MutedPlayersManager.isMuted(this.uuid)) {
                    context.fill(headX, headY, headX + headSize, headY + headSize, 0x80000000);
                }

                Text displayName = MutedPlayersManager.isMuted(this.uuid)
                        ? Text.literal(this.name).styled(style -> style.withStrikethrough(true))
                        : Text.literal(this.name);

                context.drawTextWithShadow(client.textRenderer, displayName, headX + headSize + 5, y + (entryHeight - 9) / 2, 0xFFFFFF);
                
                this.muteButton.setPosition(x + entryWidth - 70, y + (entryHeight - 20) / 2);
                this.muteButton.render(context, mouseX, mouseY, tickDelta);
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return ImmutableList.of(this.muteButton);
            }

            @Override
            public List<? extends Element> children() {
                return ImmutableList.of(this.muteButton);
            }
        }
    }
} 