package net.momirealms.customnameplates.api.feature.nametag;

import net.momirealms.customnameplates.api.CNPlayer;
import net.momirealms.customnameplates.api.CustomNameplates;
import net.momirealms.customnameplates.api.feature.CarouselText;
import net.momirealms.customnameplates.api.feature.DynamicText;
import net.momirealms.customnameplates.api.feature.RelationalFeature;
import net.momirealms.customnameplates.api.helper.AdventureHelper;
import net.momirealms.customnameplates.api.network.PassengerProperties;
import net.momirealms.customnameplates.api.placeholder.Placeholder;
import net.momirealms.customnameplates.api.util.SelfIncreaseEntityID;

import java.util.*;

public class TagDisplay implements RelationalFeature {

    private final CNPlayer owner;
    private final TagConfig config;
    private final UUID uuid = UUID.randomUUID();
    private final int entityID = SelfIncreaseEntityID.getAndIncrease();
    private final TagDisplayController controller;

    private boolean isShown = false;

    private int order;
    private int timeLeft;
    private DynamicText currentText;

    private final Vector<CNPlayer> viewers = new Vector<>();
    private CNPlayer[] viewerArray = new CNPlayer[0];

    public TagDisplay(CNPlayer owner, TagConfig config, TagDisplayController controller) {
        this.owner = owner;
        this.config = config;
        this.controller = controller;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TagDisplay that = (TagDisplay) o;
        return owner == that.owner && uuid == that.uuid && config == that.config;
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    public void init() {
        order = config.carouselTexts().length - 1;
        timeLeft = 0;
    }

    public void tick() {
        if (timeLeft > 0)
            timeLeft--;

        if (timeLeft == 0) {
            int triedTimes = 0;

            do {
                if (triedTimes == config.carouselTexts().length) {
                    timeLeft = 20;
                    currentText = null;
                    CustomNameplates.getInstance().getPluginLogger().warn("No text in order is available for player " + owner.name() + ". Please check your tag's conditions.");
                    return;
                }
                order++;
                if (order >= config.carouselTexts().length) {
                    order = 0;
                }
                triedTimes++;
            } while (
                    !owner.isMet(config.carouselTexts()[order].requirements())
            );

            CarouselText carouselText = config.carouselTexts()[order];
            timeLeft = carouselText.duration();
            currentText = carouselText.preParsedDynamicText().fastCreate(owner);

            if (carouselText.updateOnDisplay()) {
                owner.forceUpdate(currentText.placeholders());
            }

            refresh();
        }
    }

    public int entityID() {
        return entityID;
    }

    @Override
    public void notifyPlaceholderUpdates(CNPlayer p1, CNPlayer p2, boolean force) {
        refresh(p2);
    }

    @Override
    public void notifyPlaceholderUpdates(CNPlayer p1, boolean force) {
        refresh();
    }

    public boolean checkOwnerConditions() {
        return owner.isMet(config.ownerRequirements());
    }

    public boolean checkViewerConditions(CNPlayer viewer) {
        return viewer.isMet(owner, config.viewerRequirements());
    }

    public void hide(CNPlayer viewer) {
        if (!isShown()) throw new IllegalStateException("This tag is currently hidden");
        viewers.remove(viewer);
        resetViewerArray();
        viewer.untrackPassengers(owner, entityID);
        Object packet = CustomNameplates.getInstance().getPlatform().removeEntityPacket(entityID);
        CustomNameplates.getInstance().getPacketSender().sendPacket(viewer, packet);
    }

    public void show(CNPlayer viewer) {
        if (!isShown()) throw new IllegalStateException("This tag is currently hidden");
        viewers.add(viewer);
        resetViewerArray();
        viewer.trackPassengers(owner, entityID);
        String newName = currentText.render(viewer);
        Object component = AdventureHelper.miniMessageToMinecraftComponent(newName);
        PassengerProperties properties = viewer.getTrackedProperties(owner);
        List<Object> packets = CustomNameplates.getInstance().getPlatform().createTextDisplayPacket(
                entityID, uuid,
                owner.position().add(0,(1.8 + (config.affectedByCrouching() && properties.isCrouching() && !owner.isFlying() ? -0.3 : 0) + controller.hatOffset()) * (config.affectedByScale() ? properties.getScale() : 1),0),
                0f, 0f, 0d, component,
                config.backgroundColor(), config.opacity(), config.hasShadow(), config.isSeeThrough(), config.useDefaultBackgroundColor(),
                config.alignment(), config.viewRange(), config.shadowRadius(), config.shadowStrength(),
                (config.affectedByScale() ? config.scale().multiply(properties.getScale()) : config.scale()),
                (config.affectedByScale() ? config.translation().multiply(properties.getScale()) : config.translation()),
                config.lineWidth(),
                (config.affectedByCrouching() && properties.isCrouching())
       );
        CustomNameplates.getInstance().getPacketSender().sendPacket(viewer, packets);
    }

    private void resetViewerArray() {
        this.viewerArray = viewers.toArray(new CNPlayer[0]);
    }

    public void hide() {
        if (!isShown) return;
        for (CNPlayer viewer : viewerArray) {
            hide(viewer);
        }
        this.isShown = false;
    }

    public void show() {
        if (isShown) return;
        this.isShown = true;
        for (CNPlayer viewer : viewerArray) {
            show(viewer);
        }
    }

    public void refresh() {
        for (CNPlayer viewer : viewerArray) {
            refresh(viewer);
        }
    }

    public void refresh(CNPlayer viewer) {
        String newName = currentText.render(viewer);
        Object component = AdventureHelper.miniMessageToMinecraftComponent(newName);
        Object packet = CustomNameplates.getInstance().getPlatform().updateTextDisplayPacket(
                viewer, entityID, List.of(
                        CustomNameplates.getInstance().getPlatform().createTextComponentModifier(component)
                )
        );
        CustomNameplates.getInstance().getPacketSender().sendPacket(viewer, packet);
    }

    public List<Object> respawn(CNPlayer viewer, boolean sendPackets) {
        ArrayList<Object> packets = new ArrayList<>();
        String newName = currentText.render(viewer);
        Object component = AdventureHelper.miniMessageToMinecraftComponent(newName);
        PassengerProperties properties = viewer.getTrackedProperties(owner);
        packets.add(CustomNameplates.getInstance().getPlatform().removeEntityPacket(entityID));
        packets.addAll(CustomNameplates.getInstance().getPlatform().createTextDisplayPacket(
                entityID, uuid,
                owner.position().add(0,(1.8 + (config.affectedByCrouching() && properties.isCrouching() && !owner.isFlying() ? -0.3 : 0) + controller.hatOffset()) * (config.affectedByScale() ? properties.getScale() : 1),0),
                0f, 0f, 0d, component,
                config.backgroundColor(), config.opacity(), config.hasShadow(), config.isSeeThrough(), config.useDefaultBackgroundColor(),
                config.alignment(), config.viewRange(), config.shadowRadius(), config.shadowStrength(),
                (config.affectedByScale() ? config.scale().multiply(properties.getScale()) : config.scale()),
                (config.affectedByScale() ? config.translation().multiply(properties.getScale()) : config.translation()),
                config.lineWidth(),
                (config.affectedByCrouching() && properties.isCrouching())
        ));
        if (sendPackets) {
            CustomNameplates.getInstance().getPacketSender().sendPacket(viewer, packets);
        }
        return packets;
    }

    public boolean isShown() {
        return isShown;
    }

    public boolean isShown(CNPlayer another) {
        if (!isShown) return false;
        return viewers.contains(another);
    }

    @Override
    public String name() {
        return "UnlimitedTag";
    }

    @Override
    public Set<Placeholder> activePlaceholders() {
        if (currentText == null || !isShown()) return Collections.emptySet();
        return currentText.placeholders();
    }

    @Override
    public Set<Placeholder> allPlaceholders() {
        HashSet<Placeholder> placeholders = new HashSet<>();
        for (CarouselText text : config.carouselTexts()) {
            placeholders.addAll(text.preParsedDynamicText().placeholders());
        }
        return placeholders;
    }
}
