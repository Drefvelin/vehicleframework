package net.tfminecraft.VehicleFramework.Vehicles.Handlers.State;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

public final class TerrainFollowConfig {

	public static final double DEFAULT_STEP_HEIGHT = 1.0;
	public static final double DEFAULT_SNAP_SPEED = 0.25;
	public static final double DEFAULT_CLIMB_LEAD_TICKS = 3.0;
	public static final double DEFAULT_CLIMB_LEAD_FACTOR = 1.0;
	public static final double DEFAULT_AIR_GRAVITY = 0.08;
	public static final double DEFAULT_AIR_DRAG = 0.98;

	private final boolean enabled;
	private final double stepHeight;
	private final double snapSpeed;
	private final double climbLeadTicks;
	private final double climbLeadFactor;
	private final double airGravity;
	private final double airDrag;
	private final List<String> groundProbes;

	public TerrainFollowConfig(
			boolean enabled,
			double stepHeight,
			double snapSpeed,
			double climbLeadTicks,
			double climbLeadFactor,
			double airGravity,
			double airDrag,
			List<String> groundProbes) {
		this.enabled = enabled;
		this.stepHeight = stepHeight;
		this.snapSpeed = snapSpeed;
		this.climbLeadTicks = climbLeadTicks;
		this.climbLeadFactor = climbLeadFactor;
		this.airGravity = airGravity;
		this.airDrag = airDrag;
		this.groundProbes = groundProbes == null
				? List.of()
				: Collections.unmodifiableList(new ArrayList<>(groundProbes));
	}

	public static TerrainFollowConfig disabled() {
		return new TerrainFollowConfig(
				false,
				DEFAULT_STEP_HEIGHT,
				DEFAULT_SNAP_SPEED,
				DEFAULT_CLIMB_LEAD_TICKS,
				DEFAULT_CLIMB_LEAD_FACTOR,
				DEFAULT_AIR_GRAVITY,
				DEFAULT_AIR_DRAG,
				List.of());
	}

	public static TerrainFollowConfig from(ConfigurationSection config) {
		if (config == null) {
			return disabled();
		}
		boolean enabled = config.getBoolean("terrain-follow", false);
		double stepHeight = config.getDouble("step-height", DEFAULT_STEP_HEIGHT);
		double snapSpeed = config.getDouble("snap-speed", DEFAULT_SNAP_SPEED);
		double climbLeadTicks = config.getDouble("climb-lead-ticks", DEFAULT_CLIMB_LEAD_TICKS);
		double climbLeadFactor = config.getDouble("climb-lead-factor", DEFAULT_CLIMB_LEAD_FACTOR);
		double airGravity = config.getDouble("air-gravity", DEFAULT_AIR_GRAVITY);
		double airDrag = config.getDouble("air-drag", DEFAULT_AIR_DRAG);
		List<String> probes = config.getStringList("ground-probes");
		return new TerrainFollowConfig(
				enabled,
				stepHeight,
				snapSpeed,
				climbLeadTicks,
				climbLeadFactor,
				airGravity,
				airDrag,
				probes);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public double getStepHeight() {
		return stepHeight;
	}

	public double getSnapSpeed() {
		return snapSpeed;
	}

	public double getClimbLeadTicks() {
		return climbLeadTicks;
	}

	public double getClimbLeadFactor() {
		return climbLeadFactor;
	}

	public double getAirGravity() {
		return airGravity;
	}

	public double getAirDrag() {
		return airDrag;
	}

	public List<String> getGroundProbes() {
		return groundProbes;
	}

	public boolean hasGroundProbes() {
		return !groundProbes.isEmpty();
	}
}
