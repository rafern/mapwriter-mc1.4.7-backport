package mapwriter.overlay;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import mapwriter.api.IMwChunkOverlay;
import mapwriter.api.IMwDataProvider;
import mapwriter.map.MapView;
import mapwriter.map.mapmode.MapMode;
import mapwriter.MwUtil;
import net.minecraft.util.MathHelper;

public class OverlayDebug implements IMwDataProvider {

	public class ChunkOverlay implements IMwChunkOverlay{

		int refs;
		long age;
		long redrawAge;
		Point coord;
		
		public ChunkOverlay(int x, int z, long age, long redrawAge, int refs){
			this.age = age;
			this.redrawAge = redrawAge;
			this.refs = refs;
			this.coord = new Point(x, z);
		}

		public int getBaseColor() {
			int baseColor = 0x000000ff;
			if(this.refs == 0)
				baseColor = 0x00ff0000;
			else if(this.refs == 1)
				baseColor = 0x0000ff00;
			else if(this.refs > 1)
				baseColor |= 0x0000ff00;
			else
				baseColor |= 0x00ff0000;
			return baseColor;
		}
		
		@Override
		public Point getCoordinates() {	return this.coord; }

		@Override
		public int getColor() {
			long redrawAlpha = 0x80 - (this.redrawAge / 12);
			if(redrawAlpha > 0)
				return 0x00ffffff | ((int)redrawAlpha << 24);

			long alpha = 0x80 - (this.age / 25);
			if(alpha < 0)
				return 0x00000000;
			else
				return this.getBaseColor() | ((int)alpha << 24);
		}

		@Override
		public float getFilling() {	return 1.0f; }

		@Override
		public boolean hasBorder() { return true; }

		@Override
		public float getBorderWidth() { return 0.5f; }

		@Override
		public int getBorderColor() { return this.getBaseColor() | 0xff000000; }
		
	}

	public static class ChunkRef {
		int refs = 0;
		long lastAccess = 0;
		long lastRedraw = 0;

		public void access() {
			this.lastAccess = (new Date()).getTime();
		}

		public void load() {
			this.refs++;
			this.access();
		}

		public void unload() {
			this.refs--;
			this.access();
		}

		public void redraw() {
			this.lastRedraw = (new Date()).getTime();
		}
	}

	private static HashMap<Integer, HashMap<Point, ChunkRef>> dimList = new HashMap<Integer, HashMap<Point, ChunkRef>>();

	private static HashMap<Point, ChunkRef> getChunkRefs(int dim) {
		Integer key = Integer.valueOf(dim);
		HashMap<Point, ChunkRef> chunkRefs = dimList.get(key);
		if(chunkRefs == null) {
			chunkRefs = new HashMap<Point, ChunkRef>();
			dimList.put(key, chunkRefs);
		}
		return chunkRefs;
	}

	private static ChunkRef getChunkRef(int x, int z, int dim) {
		return getChunkRefs(dim).get(new Point(x, z));
	}

	private static ChunkRef getChunkRefForce(int x, int z, int dim) {
		HashMap<Point, ChunkRef> chunkRefs = getChunkRefs(dim);
		Point key = new Point(x, z);
		ChunkRef ref = chunkRefs.get(key);
		if(ref == null) {
			ref = new ChunkRef();
			chunkRefs.put(key, ref);
		}
		return ref;
	}

	public static void onChunkLoad(int x, int z, int dim) {
		ChunkRef ref = getChunkRefForce(x, z, dim);
		ref.load();
		if(ref.refs != 1)
			MwUtil.log("chunk loaded but reference count not 1 (%d, %d [dim %d]): %d refs", x, z, dim, ref.refs);
	}

	public static void onChunkUnload(int x, int z, int dim) {
		ChunkRef ref = getChunkRefForce(x, z, dim);
		ref.unload();
		if(ref.refs != 0)
			MwUtil.log("chunk unloaded but reference count not 0 (%d, %d [dim %d]): %d refs", x, z, dim, ref.refs);
	}

	public static void onChunkRedraw(int x, int z, int dim) {
		ChunkRef ref = getChunkRef(x, z, dim);
		if(ref == null) {
			MwUtil.log("redraw occurred on chunk that doesn't exist (%d, %d [dim %d])", x, z, dim);
			return;
		}
		ref.redraw();
	}

	public static void clear() {
		dimList.clear();
	}
	
	@Override
	public ArrayList<IMwChunkOverlay> getChunksOverlay(int dim, double centerX, double centerZ, double minX, double minZ, double maxX, double maxZ) {
		int minChunkX = (MathHelper.ceiling_double_int(minX) >> 4) - 1;
		int minChunkZ = (MathHelper.ceiling_double_int(minZ) >> 4) - 1;
		int maxChunkX = (MathHelper.ceiling_double_int(maxX) >> 4) + 1;
		int maxChunkZ = (MathHelper.ceiling_double_int(maxZ) >> 4) + 1;
		int cX = (MathHelper.ceiling_double_int(centerX) >> 4) + 1;
		int cZ = (MathHelper.ceiling_double_int(centerZ) >> 4) + 1;
		
		int limitMinX = Math.max(minChunkX, cX - 100);
		int limitMaxX = Math.min(maxChunkX, cX + 100);
		int limitMinZ = Math.max(minChunkZ, cZ - 100);
		int limitMaxZ = Math.min(maxChunkZ, cZ + 100);
		
		ArrayList<IMwChunkOverlay> chunks = new ArrayList<IMwChunkOverlay>();
		long t = (new Date()).getTime();
		for (int x = limitMinX; x <= limitMaxX; x++) {
			for (int z = limitMinZ; z <= limitMaxZ; z++) {
				ChunkRef ref = this.getChunkRef(x, z, dim);
				if(ref != null)
					chunks.add(new ChunkOverlay(x, z, t - ref.lastAccess, t - ref.lastRedraw, ref.refs));
			}
		}
				
		return chunks;
	}

	@Override
	public String getStatusString(int dim, int bX, int bY, int bZ) { return "";	}

	@Override
	public void onMiddleClick(int dim, int bX, int bZ, MapView mapview){	}

	@Override
	public void onDimensionChanged(int dimension, MapView mapview) {	}

	@Override
	public void onMapCenterChanged(double vX, double vZ, MapView mapview) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onZoomChanged(int level, MapView mapview) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOverlayActivated(MapView mapview) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOverlayDeactivated(MapView mapview) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDraw(MapView mapview, MapMode mapmode) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onMouseInput(MapView mapview, MapMode mapmode) {
		// TODO Auto-generated method stub
		return false;
	}

}
