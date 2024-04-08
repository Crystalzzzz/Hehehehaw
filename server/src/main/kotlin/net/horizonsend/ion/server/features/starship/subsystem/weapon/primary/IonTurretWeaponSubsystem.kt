package net.horizonsend.ion.server.features.starship.subsystem.weapon.primary

import net.horizonsend.ion.server.configuration.StarshipWeapons
import net.horizonsend.ion.server.features.multiblock.starshipweapon.turret.IonTurretMultiblock
import net.horizonsend.ion.server.features.starship.active.ActiveStarship
import net.horizonsend.ion.server.features.starship.subsystem.weapon.TurretWeaponSubsystem
import net.horizonsend.ion.server.miscellaneous.utils.Vec3i
import org.bukkit.block.BlockFace
import java.util.concurrent.TimeUnit


class IonTurretWeaponSubsystem(
	ship: ActiveStarship,
	pos: Vec3i,
	face: BlockFace,
	override val multiblock: IonTurretMultiblock
) : TurretWeaponSubsystem(ship, pos, face) {
	override val balancing: StarshipWeapons.StarshipWeapon = starship.balancing.weapons.ionTurret

	override val inaccuracyRadians: Double get() = Math.toRadians(balancing.inaccuracyRadians)
	override val powerUsage: Int get() = balancing.powerUsage
	override var fireCooldownNanos: Long = TimeUnit.MILLISECONDS.toNanos(balancing.fireCooldownMillis)


}
