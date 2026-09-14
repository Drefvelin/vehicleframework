package net.tfminecraft.VehicleFramework.Database;

public final class VehicleSnapshot {
	private final String uuid;
	private final String typeId;
	private final String name;
	private final String owner;
	private final String world;
	private final double x;
	private final double y;
	private final double z;
	private final float yaw;
	private final int chunkX;
	private final int chunkZ;
	private final String payloadJson;
	private final int schemaVersion;
	private final int revision;
	private final boolean deleted;
	private final long updatedAt;

	public VehicleSnapshot(
			String uuid,
			String typeId,
			String world,
			double x,
			double y,
			double z,
			float yaw,
			int chunkX,
			int chunkZ,
			String payloadJson,
			int schemaVersion,
			int revision,
			boolean deleted,
			long updatedAt) {
		this(uuid, typeId, typeId, "none", world, x, y, z, yaw, chunkX, chunkZ, payloadJson, schemaVersion, revision, deleted, updatedAt);
	}

	public VehicleSnapshot(
			String uuid,
			String typeId,
			String name,
			String owner,
			String world,
			double x,
			double y,
			double z,
			float yaw,
			int chunkX,
			int chunkZ,
			String payloadJson,
			int schemaVersion,
			int revision,
			boolean deleted,
			long updatedAt) {
		this.uuid = uuid;
		this.typeId = typeId;
		this.name = name == null ? "" : name;
		this.owner = owner == null || owner.isBlank() ? "none" : owner;
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
		this.payloadJson = payloadJson;
		this.schemaVersion = schemaVersion;
		this.revision = revision;
		this.deleted = deleted;
		this.updatedAt = updatedAt;
	}

	public String getUuid() {
		return uuid;
	}

	public String getTypeId() {
		return typeId;
	}

	public String getName() {
		return name;
	}

	public String getOwner() {
		return owner;
	}

	public String getWorld() {
		return world;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getZ() {
		return z;
	}

	public float getYaw() {
		return yaw;
	}

	public int getChunkX() {
		return chunkX;
	}

	public int getChunkZ() {
		return chunkZ;
	}

	public String getPayloadJson() {
		return payloadJson;
	}

	public int getSchemaVersion() {
		return schemaVersion;
	}

	public int getRevision() {
		return revision;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public long getUpdatedAt() {
		return updatedAt;
	}

	public VehicleSnapshot withRevision(int nextRevision, long nextUpdatedAt) {
		return new VehicleSnapshot(
				uuid,
				typeId,
				name,
				owner,
				world,
				x,
				y,
				z,
				yaw,
				chunkX,
				chunkZ,
				payloadJson,
				schemaVersion,
				nextRevision,
				deleted,
				nextUpdatedAt);
	}

	public VehicleSnapshot withPayload(String nextPayload, String nextName, String nextOwner, long nextUpdatedAt) {
		return new VehicleSnapshot(
				uuid,
				typeId,
				nextName,
				nextOwner,
				world,
				x,
				y,
				z,
				yaw,
				chunkX,
				chunkZ,
				nextPayload,
				schemaVersion,
				revision,
				deleted,
				nextUpdatedAt);
	}
}
