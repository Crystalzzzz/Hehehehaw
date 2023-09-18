package net.horizonsend.ion.server.features.starship.active.ai.engine.movement

import co.aikar.commands.ConditionFailedException
import net.horizonsend.ion.server.command.admin.debug
import net.horizonsend.ion.server.features.starship.active.ActiveControlledStarship
import net.horizonsend.ion.server.features.starship.active.ai.engine.pathfinding.PathfindingEngine
import net.horizonsend.ion.server.features.starship.control.controllers.ai.AIController
import net.horizonsend.ion.server.features.starship.control.controllers.ai.interfaces.ActiveAIController
import net.horizonsend.ion.server.features.starship.movement.StarshipMovement
import net.horizonsend.ion.server.miscellaneous.utils.Tasks
import net.horizonsend.ion.server.miscellaneous.utils.Vec3i
import net.horizonsend.ion.server.miscellaneous.utils.debugAudience
import net.horizonsend.ion.server.miscellaneous.utils.distanceSquared
import org.bukkit.Location
import org.bukkit.block.BlockFace
import kotlin.math.abs

/**
 * Basic cruise flight engine.
 * Variable destination, it automatically flies towards it.
 *
 * @param shiftFlightType Condition on when to use shift flight
 * @param maximumCruiseDistanceSquared Does not cruise if the target is within this distance.
 **/
class CruiseEngine(
	controller: ActiveAIController,
	pathfindingEngine: PathfindingEngine,
	var cruiseDestination: Vec3i,
	var shiftFlightType: ShiftFlightType,
	var maximumCruiseDistanceSquared: Double = 90000.0,
) : MovementEngine(controller, pathfindingEngine) {
	// The pathfinding controller will change the destination, so store the eventual destination in a seperate variable.
	var speedLimit = -1

	override fun tick() {
		starship as ActiveControlledStarship
		starship.speedLimit = speedLimit

		val origin = starshipLocation.toLocation(world)

		Tasks.sync {
			if (assessDistance()) handleCruise()

			shiftFlightType.handleShiftFlight(this, origin)
			shiftFlightType.refresh(controller)
		}
	}

	fun handleCruise() {
		if (controller.blocked) {
			debugAudience.debug("Blocked, stopping cruising")
			stopCruising(true)
			return
		}

		debugAudience.debug("More than 500 blocks away, cruising")
		cruiseToVec3i(starshipLocation, cruiseDestination ?: return)
	}

	/** Returns true if the destination is sufficiently far that it should cruise */
	private fun assessDistance(): Boolean {
		val destination = getDestination()

		val distance = distanceSquared(destination.toVector(), getCenter().toVector())

		// 300 Blocks or more and it should cruise
		return distance >= maximumCruiseDistanceSquared // Don't bother with the sqrt
	}

	override fun shutDown() {
		stopCruising()
	}

	override fun onBlocked(movement: StarshipMovement, reason: ConditionFailedException) {
		debugAudience.debug("$controller is blocked $reason by movement $movement trying to move to ${getDestination()} eventual destination $cruiseDestination")
	}

	enum class ShiftFlightType {
		NONE,
		MATCH_Y {
			override fun handleShiftFlight(engine: CruiseEngine, origin: Location) {
				val destination = engine.getDestination()

				val difference = origin.y - destination.y
				if (abs(difference) < 5) {
					engine.controller.isShiftFlying = false
					return
				}

				val blockFace = if (difference > 0) BlockFace.UP else BlockFace.DOWN

				engine.shiftFlyTowardsBlockFace(blockFace, false)
			}
		},
		IF_BLOCKED {
			override fun handleShiftFlight(engine: CruiseEngine, origin: Location) {
				val blocked = engine.controller.pathfindingEngine.blocked

				if (!blocked) {
					engine.controller.isShiftFlying = false
					return
				}

				engine.controller.pathfindingEngine.triggerTask()
				engine.shiftFlyToVec3i(origin, engine.controller.pathfindingEngine.getFirstNavPoint(), true)
			}
		},
		IF_BLOCKED_AND_MATCH_Y {
			override fun handleShiftFlight(engine: CruiseEngine, origin: Location) {
				val blocked = engine.controller.pathfindingEngine.blocked
				val yObjective = engine.cruiseDestination.y

				val yDifference = yObjective - origin.y

				// Only try to match y if not blocked
				if (abs(yDifference) > 10.0 && !blocked) {
					val blockFace = if (yDifference > 0) BlockFace.UP else BlockFace.DOWN
					engine.shiftFlyTowardsBlockFace(blockFace, false)

					return
				}

				if (!blocked) {
					engine.controller.isShiftFlying = false
					return
				}

				engine.controller.pathfindingEngine.triggerTask()
				engine.shiftFlyToVec3i(origin, engine.controller.pathfindingEngine.getFirstNavPoint(), true)
			}
		},
		ALL {
			override fun handleShiftFlight(engine: CruiseEngine, origin: Location) {
				engine.shiftFly(origin, false)
			}

			override fun refresh(controller: AIController) { }
		}

		;

		/** Must be executed sync */
		open fun handleShiftFlight(engine: CruiseEngine, origin: Location) {}
		open fun refresh(controller: AIController) {
			if (!controller.blocked) controller.isShiftFlying = false
		}
	}
}