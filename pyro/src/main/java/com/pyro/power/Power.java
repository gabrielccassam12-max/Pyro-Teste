package com.pyro.power;

import net.minecraft.text.Text;

/** The five Pyrokinetic powers (key slots 1..5). */
public enum Power {
    FIREBALL(12, 20),
    PILLAR(22, 60),
    NOVA(30, 110),
    METEOR(60, 360),
    AURA(15, 50);

    public final int cost;
    public final int cooldown;

    Power(int cost, int cooldown) {
        this.cost = cost;
        this.cooldown = cooldown;
    }

    public Text displayName() {
        return Text.translatable("power.pyro." + (ordinal() + 1));
    }
}
