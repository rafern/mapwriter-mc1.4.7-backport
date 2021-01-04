package mapwriter;

import java.util.Map;

import mapwriter.region.MwChunk;
import mapwriter.tasks.SaveChunkTask;
import mapwriter.tasks.UpdateSurfaceChunksTask;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class ChunkManager {
	public class FlaggedChunk {
		public Chunk chunk;
		public int flags = 0;

		public FlaggedChunk(Chunk chunk) {
			this.chunk = chunk;
		}
	}

	public Mw mw;
	private boolean closed = false;
	private CircularHashMap<ChunkCoordIntPair, FlaggedChunk> chunkMap = new CircularHashMap<ChunkCoordIntPair, FlaggedChunk>();
	
	private static final int VISIBLE_FLAG = 0x01;
	private static final int VIEWED_FLAG = 0x02;
	
	public ChunkManager(Mw mw) {
		this.mw = mw;
	}
	
	public synchronized void close() {
		this.closed = true;
		this.clear();
	}

	public synchronized void clear() {
		this.saveChunks();
		this.chunkMap.clear();
	}
	
	// create MwChunk from Minecraft chunk.
	// only MwChunk's should be used in the background thread.
	// TODO: make this a full copy of chunk data to prevent possible race conditions
	public static MwChunk copyToMwChunk(Chunk chunk) {
		
		byte[][] msbArray = new byte[16][];
		byte[][] lsbArray = new byte[16][];
		byte[][] metaArray = new byte[16][];
		byte[][] lightingArray = new byte[16][];
		
		ExtendedBlockStorage[] storageArrays = chunk.getBlockStorageArray();
		if (storageArrays != null) {
			for (ExtendedBlockStorage storage : storageArrays) {
				if (storage != null) {
					int y = (storage.getYLocation() >> 4) & 0xf;
					lsbArray[y] = storage.getBlockLSBArray();
					msbArray[y] = (storage.getBlockMSBArray() != null) ? storage.getBlockMSBArray().data : null;
					metaArray[y] = (storage.getMetadataArray() != null) ? storage.getMetadataArray().data : null;
					lightingArray[y] = (storage.getBlocklightArray() != null) ? storage.getBlocklightArray().data : null;
				}
			}
		}
		
		return new MwChunk(chunk.xPosition, chunk.zPosition, chunk.worldObj.provider.dimensionId,
				msbArray, lsbArray, metaArray, lightingArray, chunk.getBiomeArray());
	}
	
	public synchronized boolean addChunk(Chunk chunk) {
		// Returns true if the new chunk was added, or false if the chunk was
		// ignored or replaced an old one
		if (!this.closed && (chunk != null)) {
			// Doing this resets the flags for this chunk if there was a chunk
			// at these coordinates, but this is harmless as it just causes it
			// to save, as it should since its a new chunk
			return this.chunkMap.put(chunk.getChunkCoordIntPair(), new FlaggedChunk(chunk)) == null;
		}
		else
			return false;
	}
	
	public synchronized void removeChunk(Chunk chunk) {
		if (!this.closed && (chunk != null)) {
			ChunkCoordIntPair key = chunk.getChunkCoordIntPair();
            if(!this.chunkMap.containsKey(key)) return;
            FlaggedChunk flaggedChunk = this.chunkMap.get(key);
			if ((flaggedChunk.flags & VIEWED_FLAG) != 0) {
				this.addSaveChunkTask(flaggedChunk.chunk);
			}
			this.chunkMap.remove(key);
		}
	}
	
	public synchronized void saveChunks() {
		for (FlaggedChunk flaggedChunk : this.chunkMap.values()) {
			if ((flaggedChunk.flags & VIEWED_FLAG) != 0) {
				this.addSaveChunkTask(flaggedChunk.chunk);
			}
		}
	}
	
	public void updateUndergroundChunks() {
		// FIXME this does nothing, what did they want to do here?
		/*this.mw.mc.mcProfiler.startSection("updateUndergroundChunks");
		int chunkArrayX = (this.mw.playerXInt >> 4) - 1;
		int chunkArrayZ = (this.mw.playerZInt >> 4) - 1;
		MwChunk[] chunkArray = new MwChunk[9];
		for (int z = 0; z < 3; z++) {
			for (int x = 0; x < 3; x++) {
				this.mw.mc.mcProfiler.startSection("getChunkFromChunkCoords");
				Chunk chunk = this.mw.mc.theWorld.getChunkFromChunkCoords(
					chunkArrayX + x,
					chunkArrayZ + z
				);
				this.mw.mc.mcProfiler.endStartSection("copyToMwChunk");
				if (!chunk.isEmpty()) {
					chunkArray[(z * 3) + x] = copyToMwChunk(chunk);
				}
				this.mw.mc.mcProfiler.endSection();
			}
		}
		this.mw.mc.mcProfiler.endSection();*/
	}
	
	public void updateSurfaceChunks() {
		this.mw.mc.mcProfiler.startSection("updateSurfaceChunks");
		int chunksToUpdate = Math.min(this.chunkMap.size(), this.mw.chunksPerTick);
		MwChunk[] chunkArray = new MwChunk[chunksToUpdate];
		for (int i = 0; i < chunksToUpdate; i++) {
			this.mw.mc.mcProfiler.startSection("getNextEntry");
			Map.Entry<ChunkCoordIntPair, FlaggedChunk> entry = this.chunkMap.getNextEntry();
			this.mw.mc.mcProfiler.endSection();
			if (entry != null) {
				// if this chunk is within a certain distance to the player then
				// add it to the viewed set
				FlaggedChunk flaggedChunk = entry.getValue();
				this.mw.mc.mcProfiler.startSection("distToChunkSq");
				if (MwUtil.distToChunkSq(this.mw.playerXInt, this.mw.playerZInt, flaggedChunk.chunk) <= this.mw.maxChunkSaveDistSq) {
					flaggedChunk.flags |= (VISIBLE_FLAG | VIEWED_FLAG);
				} else {
					flaggedChunk.flags &= ~VISIBLE_FLAG;
				}
				
				this.mw.mc.mcProfiler.endStartSection("copyToMwChunk");
				if ((flaggedChunk.flags & VISIBLE_FLAG) != 0) {
					chunkArray[i] = copyToMwChunk(flaggedChunk.chunk);
				} else {
					chunkArray[i] = null;
				}
				this.mw.mc.mcProfiler.endSection();
			}
		}
		
		this.mw.mc.mcProfiler.startSection("addTaskUpdateSurfaceChunksTask");
		this.mw.executor.addTask(new UpdateSurfaceChunksTask(this.mw, chunkArray));
		this.mw.mc.mcProfiler.endSection();
		this.mw.mc.mcProfiler.endSection();
	}
	
	public void onTick() {
		if (!this.closed) {
			if ((this.mw.tickCounter & 0xf) == 0) {
				this.updateUndergroundChunks();
			} else {
				this.updateSurfaceChunks();
			}
		}
	}
	
	public void forceChunks(MwChunk[] chunkArray){
		this.mw.executor.addTask(new UpdateSurfaceChunksTask(this.mw, chunkArray));
	}
	
	private void addSaveChunkTask(Chunk chunk) {
		if ((this.mw.multiplayer && this.mw.regionFileOutputEnabledMP) || 
			(!this.mw.multiplayer && this.mw.regionFileOutputEnabledSP)) {
			if (!chunk.isEmpty()) {
				this.mw.executor.addTask(new SaveChunkTask(copyToMwChunk(chunk), this.mw.regionManager));
			}
		}
	}
}
