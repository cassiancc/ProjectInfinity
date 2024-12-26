package net.lerariemann.infinity.util;

import net.lerariemann.infinity.entity.custom.AntEntity;
import net.lerariemann.infinity.entity.custom.BishopEntity;
import net.lerariemann.infinity.registry.core.ModEntities;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

public class AntBattle {
    public String teamName;
    public ServerWorld serverWorld;
    public Scoreboard scoreboard;
    public Team team;

    public AntBattle(ServerWorld world) {
        this(world, "ant_battle_" + world.random.nextInt());
    }

    public AntBattle(ServerWorld world, String teamName) {
        this.teamName = teamName;
        serverWorld = world;
        scoreboard = world.getScoreboard();
        team = getOrCreateTeam(teamName);
    }

    public Team getOrCreateTeam(String name) {
        Team t = scoreboard.getTeam(name);
        if (t == null) t = scoreboard.addTeam(name);
        return t;
    }

    public void addEntity(LivingEntity entity) {
        scoreboard.addScoreHolderToTeam(entity.getNameForScoreboard(), team);
    }

    public void start(BlockPos pos) {
        AntEntity ant = ModEntities.ANT.get().spawn(serverWorld, pos, SpawnReason.MOB_SUMMONED);
        BishopEntity bishop = ModEntities.BISHOP.get().spawn(serverWorld, pos, SpawnReason.MOB_SUMMONED);
        if (ant != null && bishop != null) {
            Objects.requireNonNull(ant.getAttributeInstance(EntityAttributes.GENERIC_SCALE)).setBaseValue(3.0);
            Objects.requireNonNull(ant.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)).setBaseValue(20);
            Objects.requireNonNull(ant.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.25f);
            ant.setHealth(20);
            bishop.startRiding(ant);
            ant.addToBattle(this);
            bishop.addToBattle(this);
        }
    }

    public void stop() {
        scoreboard.removeTeam(team);
    }
}
