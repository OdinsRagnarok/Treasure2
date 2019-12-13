/**
 * 
 */
package com.someguyssoftware.treasure2.world.gen.structure;

import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import com.someguyssoftware.gottschcore.positional.Coords;
import com.someguyssoftware.gottschcore.positional.ICoords;
import com.someguyssoftware.gottschcore.world.gen.structure.GottschTemplate;
import com.someguyssoftware.gottschcore.world.gen.structure.StructureMarkers;
import com.someguyssoftware.treasure2.Treasure;
import com.someguyssoftware.treasure2.generator.GenUtil;
import com.someguyssoftware.treasure2.generator.GeneratorResult;
import com.someguyssoftware.treasure2.generator.TemplateGeneratorData;
import com.someguyssoftware.treasure2.meta.StructureMeta;
import com.sun.media.jfxmedia.logging.Logger;

import lombok.Setter;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.PlacementSettings;

/**
 * 
		// TODO getMarkerBlock should be in TemplateGenerator as well (passed in)
		// TODO move TemplateGenerator to world.gen.structure (in gottschcore)
		// TODO structure gen should probably pass in the replacement map
 * @author Mark Gottschling on Jan 24, 2019
 *
 */
@Setter
public class TemplateGenerator implements ITemplateGenerator<GeneratorResult<TemplateGeneratorData>> {
	// facing property of a vanilla chest
	private static final PropertyDirection VANILLA_CHEST_FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
	
	private Block nullBlock;
	
	// TODO constructor should probably take in null block or list of null blocks, markersblocks, and replacement blocks
	public TemplateGenerator() {
		// use the default null block
		setNullBlock(GenUtil.getMarkerBlock(StructureMarkers.NULL));
	}
	
	/**
	 * 
	 */
	@Override
	public GeneratorResult<TemplateGeneratorData> generate(World world, Random random, 
			TemplateHolder templateHolder, PlacementSettings placement, ICoords coords) {
		
		GeneratorResult<TemplateGeneratorData> result = new GeneratorResult<>(TemplateGeneratorData.class);
		
		GottschTemplate template = (GottschTemplate) templateHolder.getTemplate();
		
		// get the meta
		StructureMeta meta = (StructureMeta) Treasure.META_MANAGER.getMetaMap().get(templateHolder.getMetaLocation().toString());
		if (meta == null) {
			Treasure.logger.debug("Unable to locate meta data for template -> {}", templateHolder.getLocation());
			return result.fail();
		}
		
		// find the offset block
		int offset = 0;
		ICoords offsetCoords = null;
		if (meta.getOffset() != null) {
			// NOTE going to need to negate meta offset since a negative value will be provided for downward movement, whereas
			// an offset derived from a template will always be positive and thus is negated later to correct the positioning.
			offsetCoords = new Coords(0, -meta.getOffset().getY(), 0);
			Treasure.logger.debug("Using meta offset coords -> {}", offsetCoords);
		}
		else {
			offsetCoords = template.findCoords(random, GenUtil.getMarkerBlock(StructureMarkers.OFFSET));
		}
		
		if (offsetCoords != null) {
			offset = -offsetCoords.getY();
		}
		
		// update the spawn coords with the offset
		ICoords spawnCoords = coords.add(0, offset, 0);
		
		// TODO provide decayProcessor
		// generate the structure
		template.addBlocksToWorld(world, spawnCoords.toPos(), placement, getNullBlock(), Treasure.TEMPLATE_MANAGER.getReplacementMap(), 3);
		
		Treasure.logger.debug("added blocks to the world.");
		
		// TODO do this BEFORE removing specials
		// process all markers and adding them to the result data (relative positioned)
		for (Entry<Block, ICoords> entry : template.getMap().entries()) {
			ICoords c = new Coords(GottschTemplate.transformedCoords(placement, entry.getValue()));
			result.getData().getMap().put(entry.getKey(), c);
			Treasure.logger.debug("adding to structure info transformed coords -> {} : {}", entry.getKey().getLocalizedName(), c.toShortString());
		}
		
		// find the chest and update chest coords (absolute positioned)
		List<ICoords> chestCoordsList = (List<ICoords>) result.getData().getMap().get(GenUtil.getMarkerBlock(StructureMarkers.CHEST));
		if (!chestCoordsList.isEmpty()) {
			ICoords chestCoords = spawnCoords.add(chestCoordsList.get(0));
			result.getData().setChestCoords(chestCoords);		
			// get the block state of the chest
			IBlockState chestState = world.getBlockState(chestCoords.toPos());
			 if (chestState.getProperties().containsKey(VANILLA_CHEST_FACING)) {
				 result.getData().setChestState(chestState);
				 Treasure.logger.debug("saving chest state -> {}", chestState.toString());
			 }
		}
				 
		// TODO if this is handled on template read, this block can go away - remove this when using GottschCore v1.9.0
		// remove any extra special blocks
		for (ICoords mapCoords : template.getMapCoords()) {
			ICoords c = GottschTemplate.transformedCoords(placement, mapCoords);
			// TODO shouldn't be setting to air, but to null block
			world.setBlockToAir(spawnCoords.toPos().add(c.toPos()));
		}
		
		// get the transformed size
		BlockPos transformedSize = template.transformedSize(placement.getRotation());
		Treasure.logger.debug("transformed size -> {}", transformedSize.toString());
		
		// calculate the new spawn coords - that includes the rotation, and negates the Y offset
		spawnCoords = getTransformedSpawnCoords(spawnCoords, new Coords(transformedSize), placement).add(0, -offset, 0);
		
		Treasure.logger.debug("spawn coords after rotation -> " + spawnCoords);
		// update result data
		result.getData().setSpawnCoords(spawnCoords);
		result.getData().setSize(new Coords(transformedSize));
		
		return result.success();
	}

	/**
	 * 
	 * @param coords
	 * @param size
	 * @param placement
	 * @return
	 */
	public ICoords getTransformedSpawnCoords(final ICoords coords, final ICoords size, final PlacementSettings placement) {
			
		ICoords spawnCoords = null;
		int x = 0;
		int z = 0;
		switch(placement.getRotation()) {
		case NONE:
			x = coords.getX();
			z = coords.getZ();
			break;
		case CLOCKWISE_90:
			x = coords.getX() - (size.getZ()-1);
			z = coords.getZ();
			break;
		case CLOCKWISE_180:
			x = coords.getX() - (size.getX()-1);
			z = coords.getZ() - (size.getZ()-1);
			break;
		case COUNTERCLOCKWISE_90:
			x = coords.getX();
			z = coords.getZ() - (size.getX()-1);
			break;
		default:
			break;
		}
		spawnCoords = new Coords(x, coords.getY(), z);
		return spawnCoords;
	}
	
	
	@Override
	public Block getNullBlock() {
		if (nullBlock == null) {
			nullBlock = GenUtil.getMarkerBlock(StructureMarkers.NULL);
		}
		return nullBlock;
	}

}