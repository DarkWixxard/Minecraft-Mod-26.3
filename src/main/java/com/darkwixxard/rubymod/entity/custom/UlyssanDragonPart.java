package com.darkwixxard.rubymod.entity.custom;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraftforge.entity.PartEntity;

public class UlyssanDragonPart extends PartEntity<UlyssanDragonEntity> {
    private final EntityDimensions size;
    private final float damageMultiplier;

    public UlyssanDragonPart(UlyssanDragonEntity parent, float width, float height, float damageMultiplier) {
        super(parent);
        this.size = EntityDimensions.scalable(width, height);
        this.damageMultiplier = damageMultiplier;
        this.refreshDimensions();
    }


    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return this.getParent().hurtServer(level, source, amount * this.damageMultiplier);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean is (Entity entity) {
        return this == entity || this.getParent() == entity;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.size;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {

    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {

    }
}
