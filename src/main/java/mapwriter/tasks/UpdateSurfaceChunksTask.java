package mapwriter.tasks;

import mapwriter.Mw;
import mapwriter.map.MapTexture;
import mapwriter.region.MwChunk;
import mapwriter.region.RegionManager;
//import mapwriter.overlay.OverlayDebug;

public class UpdateSurfaceChunksTask extends Task {
	MwChunk[] chunkArray;
	RegionManager regionManager;
	MapTexture mapTexture;
	
	public UpdateSurfaceChunksTask(Mw mw, MwChunk[] chunkArray) {
		this.mapTexture = mw.mapTexture;
		this.regionManager = mw.regionManager;
		this.chunkArray = chunkArray;
	}
	
	@Override
	public void run() {
		for (MwChunk chunk : this.chunkArray) {
			if (chunk != null) {
				// update the chunk in the region pixels
				this.regionManager.updateChunk(chunk);
				// copy updated region pixels to maptexture
				this.mapTexture.updateArea(
					this.regionManager,
					chunk.x << 4, chunk.z << 4,
					MwChunk.SIZE, MwChunk.SIZE, chunk.dimension
				);
				//OverlayDebug.onChunkRedraw(chunk.x, chunk.z, chunk.dimension);
			}
		}
	}
	
	@Override
	public void onComplete() {
	}
}
