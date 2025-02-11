/*
 *  Copyright (C) <2022> <XiaoMoMi>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.customnameplates.paper.mechanic.nameplate.tag.unlimited;

import net.momirealms.customnameplates.api.CustomNameplatesPlugin;
import net.momirealms.customnameplates.api.mechanic.nameplate.TagMode;
import net.momirealms.customnameplates.api.mechanic.tag.unlimited.*;
import net.momirealms.customnameplates.common.team.TeamColor;
import net.momirealms.customnameplates.common.team.TeamTagVisibility;
import net.momirealms.customnameplates.paper.setting.CNConfig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

public class UnlimitedPlayer extends UnlimitedEntity implements EntityTagPlayer {

    private final Player owner;
    private final Vector<DynamicTextEntity> dynamicTags;
    private DynamicTextEntity[] dynamicTagArray;
    private double hatOffset;
    private boolean isPreviewing;

    public UnlimitedPlayer(UnlimitedTagManagerImpl manager, Player player) {
        super(manager, player);
        this.owner = player;
        this.dynamicTags = new Vector<>();
        this.dynamicTagVectorToArray();
    }

    @Override
    public void addTag(DynamicTextEntity tag) {
        if (dynamicTags.contains(tag)) {
            return;
        }
        dynamicTags.add(tag);
        dynamicTagVectorToArray();
        for (Player all : getNearbyPlayers()) {
            if (tag.canShow() && tag.canSee(all)) {
                tag.addPlayerToViewers(all);
            }
        }
    }

    @Override
    public void addTag(StaticTextEntity tag) {
        if (staticTags.contains(tag)) {
            return;
        }
        staticTags.add(tag);
        staticTagVectorToArray();
        for (Player all : getNearbyPlayers()) {
            if (tag.getComeRule().isPassed(all, owner)) {
                tag.addPlayerToViewers(all);
            }
        }
    }

    @Override
    public DynamicTextEntity addTag(DynamicTextTagSetting setting) {
        var tag = manager.createNamedEntity(this, setting);
        addTag(tag);
        return tag;
    }

    @Override
    public StaticTextEntity addTag(StaticTextTagSetting setting) {
        var tag = manager.createNamedEntity(this, setting);
        addTag(tag);
        return tag;
    }

    @Override
    public void removeTag(DynamicTextEntity tag) {
        if (dynamicTags.remove(tag)) {
            tag.destroy();
            dynamicTagVectorToArray();
        }
    }

    @Override
    public Collection<DynamicTextEntity> getDynamicTags() {
        return new ArrayList<>(dynamicTags);
    }

    @Override
    public void setHatOffset(double hatOffset) {
        this.hatOffset = hatOffset;
    }

    @Override
    public void setPreview(boolean preview) {
        if (isPreviewing == preview) {
            return;
        }
        isPreviewing = preview;
        if (isPreviewing) {
            addNearbyPlayerNaturally(owner);
        } else {
            removeNearbyPlayerNaturally(owner);
        }
    }

    @Override
    public boolean isPreviewing() {
        return isPreviewing;
    }

    @Override
    public Player getPlayer() {
        return owner;
    }

    @Override
    public void updateText() {
        for (DynamicTextEntity tag : dynamicTagArray) {
            tag.updateText();
        }
    }

    @Override
    public double getHatOffset() {
        return hatOffset;
    }

    @Override
    public void addNearbyPlayerNaturally(Player player) {
        if (nearbyPlayers.contains(player)) {
            return;
        }
        nearbyPlayers.add(player);
        playerVectorToArray();
        for (StaticTextEntity tag : staticTagArray) {
            if (tag.getComeRule().isPassed(player, entity)) {
                tag.addPlayerToViewers(player);
            }
        }
        for (DynamicTextEntity tag : dynamicTagArray) {
            if (tag.canShow() && tag.canSee(player)) {
                tag.addPlayerToViewers(player);
            }
        }
        setNameInvisibleFor(player);
    }

    @Override
    public void removeNearbyPlayerNaturally(Player player) {
        if (!nearbyPlayers.contains(player)) {
            return;
        }
        nearbyPlayers.remove(player);
        playerVectorToArray();
        for (StaticTextEntity tag : staticTagArray) {
            if (tag.getLeaveRule().isPassed(player, entity)) {
                tag.removePlayerFromViewers(player);
            }
        }
        for (DynamicTextEntity tag : dynamicTagArray) {
            tag.removePlayerFromViewers(player);
        }
        setNameVisibleFor(player);
    }

    @Override
    public void destroy() {
        manager.removeUnlimitedEntityFromMap(entity.getUniqueId());
        for (Player viewer : getNearbyPlayers()) {
            if (getPlayer().isOnline()) {
                setNameVisibleFor(viewer);
            }
        }
        for (DynamicTextEntity tag : dynamicTagArray) {
            tag.destroy();
        }
        for (StaticTextEntity tag : staticTagArray) {
            tag.destroy();
        }
        nearbyPlayers.clear();
        dynamicTags.clear();
        staticTags.clear();
        staticTagArray = null;
    }

    public void sneak(boolean sneaking, boolean flying) {
        for (StaticTextEntity tag : staticTagArray) {
            tag.setSneak(sneaking, !flying);
        }
        for (DynamicTextEntity tag : dynamicTagArray) {
            tag.setSneak(sneaking, !flying);
        }
    }

    @Override
    public void respawn() {
        super.respawn();
        for (DynamicTextEntity tag : dynamicTagArray) {
            tag.respawn(owner.getPose());
        }
    }

    @Override
    public void move(Player receiver, short x, short y, short z, boolean onGround) {
        super.move(receiver, x, y, z, onGround);
        for (DynamicTextEntity tag : dynamicTagArray) {
            tag.move(receiver, x, y, z, onGround);
        }
    }

    @Override
    public void teleport(Player receiver, double x, double y, double z, boolean onGround) {
        super.teleport(receiver, x, y, z, onGround);
        for (DynamicTextEntity tag : dynamicTagArray) {
            tag.teleport(receiver, x, y, z, onGround);
        }
    }

    @Override
    public void teleport() {
        super.teleport();
        for (DynamicTextEntity tag : dynamicTagArray) {
            tag.teleport();
        }
    }

    @Override
    public void handlePose(Pose previous, Pose pose) {
        super.handlePose(previous, pose);
        for (DynamicTextEntity tag : dynamicTagArray) {
            tag.handlePose(previous, pose);
        }
    }

    private void setNameInvisibleFor(Player viewer) {
        if (CNConfig.nameplateModule && CustomNameplatesPlugin.get().getNameplateManager().getTagMode() == TagMode.UNLIMITED) {
            CustomNameplatesPlugin.get().getTeamManager().updateTeam(
                    owner,
                    viewer,
                    "",
                    "",
                    TeamColor.WHITE,
                    TeamTagVisibility.NEVER
            );
        }
    }

    private void setNameVisibleFor(Player viewer) {
        if (CNConfig.nameplateModule && CustomNameplatesPlugin.get().getNameplateManager().getTagMode() == TagMode.UNLIMITED) {
            CustomNameplatesPlugin.get().getTeamManager().updateTeam(
                    owner,
                    viewer,
                    "",
                   "",
                    TeamColor.WHITE,
                    TeamTagVisibility.ALWAYS
            );
        }
    }

    public void timer() {
        for (DynamicTextEntity tag : dynamicTagArray) {
            tag.timer();
        }
    }

    public void updateVisibility() {
        for (DynamicTextEntity tag : dynamicTagArray) {
            tag.updateVisibility();
        }
    }

    private void dynamicTagVectorToArray() {
        dynamicTagArray = dynamicTags.toArray(new DynamicTextEntity[0]);
    }
}
