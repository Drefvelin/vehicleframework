package net.tfminecraft.VehicleFramework.Database;

public record VehiclePersistResult(Kind kind, String reason) {

	public enum Kind {
		SAVED,
		ALREADY_STORED,
		FAILED
	}

	public static VehiclePersistResult saved() {
		return new VehiclePersistResult(Kind.SAVED, null);
	}

	public static VehiclePersistResult alreadyStored(String reason) {
		return new VehiclePersistResult(Kind.ALREADY_STORED, reason);
	}

	public static VehiclePersistResult failed(String reason) {
		return new VehiclePersistResult(Kind.FAILED, reason == null || reason.isBlank() ? "unknown" : reason);
	}

	public boolean isSaved() {
		return kind == Kind.SAVED;
	}

	public boolean isAlreadyStored() {
		return kind == Kind.ALREADY_STORED;
	}

	public boolean isFailed() {
		return kind == Kind.FAILED;
	}
}
