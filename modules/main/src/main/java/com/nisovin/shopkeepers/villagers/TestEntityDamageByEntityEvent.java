package com.nisovin.shopkeepers.villagers;

import java.util.EnumMap;
import java.util.Map;

import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.google.common.base.Functions;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;

/**
 * A test event that is called to check if a player can damage the given entity (e.g. when the
 * player tries to hire a villager).
 */
public class TestEntityDamageByEntityEvent extends EntityDamageByEntityEvent {

	public TestEntityDamageByEntityEvent(Entity damager, Entity damagee) {
		super(
				damager,
				damagee,
				DamageCause.CUSTOM,
				DamageSource.builder(Unsafe.assertNonNull(DamageType.GENERIC))
						.withCausingEntity(damager)
						.withDirectEntity(damager)
						.build(),
				new EnumMap<>(Map.of(DamageModifier.BASE, 1.0D)),
				new EnumMap<>(Map.of(DamageModifier.BASE, Functions.constant(-0.0D))),
				false
		);
	}
}
