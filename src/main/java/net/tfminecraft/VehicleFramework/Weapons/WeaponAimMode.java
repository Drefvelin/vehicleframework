package net.tfminecraft.VehicleFramework.Weapons;

public enum WeaponAimMode {
	MANUAL,
	CURSOR;

	public static WeaponAimMode fromConfig(String value) {
		if (value != null && value.equalsIgnoreCase("cursor")) {
			return CURSOR;
		}
		return MANUAL;
	}
}
