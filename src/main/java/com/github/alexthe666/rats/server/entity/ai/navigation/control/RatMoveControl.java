package com.github.alexthe666.rats.server.entity.ai.navigation.control;

import com.github.alexthe666.rats.server.entity.rat.AbstractRat;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class RatMoveControl extends MoveControl {

	protected final AbstractRat rat;

	public RatMoveControl(AbstractRat rat) {
		super(rat);
		this.rat = rat;
	}

	@Override
	public void tick() {
		Path path = rat.getNavigation().getPath();
		if (operation != Operation.MOVE_TO || !rat.onGround() || path == null || path.isDone()) {
			super.tick();
			return;
		}

		double attribute = rat.getAttributeValue(Attributes.MOVEMENT_SPEED);
		double requested = speedModifier;
		if (requested * attribute > 0.55) {
			double dx = wantedX - rat.getX(), dz = wantedZ - rat.getZ();
			double distance = Math.sqrt(dx * dx + dz * dz);
			Vec3 motion = rat.getDeltaMovement();
			double carry = distance > 0.001 ? Math.max(0, (motion.x * dx + motion.z * dz) / distance) : 0;
			// Ground navigation advances at most one node per tick. Account for existing momentum
			// so a fast rat can cross that node without racing past the following one.
			double limit = Math.max(0.14, distance + 0.18 - carry);
			double bend = distanceToBend(path, distance);
			limit = Math.min(limit, 0.18 + 0.32 * bend);
			speedModifier = Math.min(requested, limit / attribute);
		}
		super.tick();
		speedModifier = requested;
	}

	private double distanceToBend(Path path, double distance) {
		int next = path.getNextNodeIndex();
		for (int i = next; i < path.getNodeCount() - 1 && distance < 4; i++) {
			Node current = path.getNode(i), after = path.getNode(i + 1);
			int dx = after.x - current.x, dz = after.z - current.z;
			if (current.y != after.y) break;
			if (i > 0) {
				Node before = path.getNode(i - 1);
				if (current.x - before.x != dx || current.z - before.z != dz) break;
			}
			distance += Math.sqrt(dx * dx + dz * dz);
		}
		return distance;
	}

	@Override
	public boolean hasWanted() {
		return super.hasWanted() && this.rat.canMove();
	}
}
