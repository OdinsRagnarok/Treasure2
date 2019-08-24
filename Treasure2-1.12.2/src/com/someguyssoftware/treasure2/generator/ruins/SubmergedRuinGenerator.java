/**
 * 
 */
package com.someguyssoftware.treasure2.generator.ruins;

import java.util.List;
import java.util.Random;

import com.someguyssoftware.gottschcore.measurement.Quantity;
import com.someguyssoftware.gottschcore.positional.ICoords;
import com.someguyssoftware.gottschcore.world.WorldInfo;
import com.someguyssoftware.gottschcore.world.gen.structure.GottschTemplate;
import com.someguyssoftware.gottschcore.world.gen.structure.StructureMarkers;
import com.someguyssoftware.treasure2.Treasure;
import com.someguyssoftware.treasure2.block.TreasureBlocks;
import com.someguyssoftware.treasure2.generator.GenUtil;
import com.someguyssoftware.treasure2.generator.TemplateGeneratorData;
import com.someguyssoftware.treasure2.generator.TreasureGeneratorData;
import com.someguyssoftware.treasure2.generator.TreasureGeneratorResult;
import com.someguyssoftware.treasure2.meta.StructureArchetype;
import com.someguyssoftware.treasure2.meta.StructureType;
import com.someguyssoftware.treasure2.tileentity.ProximitySpawnerTileEntity;
import com.someguyssoftware.treasure2.world.gen.structure.IStructureInfo;
import com.someguyssoftware.treasure2.world.gen.structure.ITemplateGenerator;
import com.someguyssoftware.treasure2.world.gen.structure.TemplateGenerator;
import com.someguyssoftware.treasure2.world.gen.structure.TemplateHolder;

import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.common.DungeonHooks;

/**
 * @author Mark Gottschling on Aug 13, 2019
 *
 */
public class SubmergedRuinGenerator implements IRuinGenerator<TreasureGeneratorResult<TemplateGeneratorData>> {
	
	/**
	 * 
	 */
	public SubmergedRuinGenerator() {}
	
	
	@Override
	public TreasureGeneratorResult<TemplateGeneratorData> generate2(World world, Random random,
			ICoords spawnCoords) {
		TreasureGeneratorResult<TemplateGeneratorData> result = new TreasureGeneratorResult<>(TemplateGeneratorData.class);
	
		// TODO can abstract to AbstractRuinGenerator which Submerged and Ruin implement.
		// TODO create a method selectTemplate() in abstract that will be overridden by concrete classes, provided the archetype and type
		
		// get the biome ID
		Biome biome = world.getBiome(spawnCoords.toPos());
		
		// get the template holder from the given archetype, type and biome
		TemplateHolder holder = Treasure.TEMPLATE_MANAGER.getTemplate(world, random, StructureArchetype.SUBMERGED, StructureType.RUIN, biome);
		if (holder == null) return result.fail();		
		
		// select a random rotation
		Rotation rotation = Rotation.values()[random.nextInt(Rotation.values().length)];
		Treasure.logger.debug("rotation used -> {}", rotation);
		
		// setup placement
		PlacementSettings placement = new PlacementSettings();
		placement.setRotation(rotation).setRandom(random);
		
		BlockPos transformedSize = holder.getTemplate().transformedSize(rotation);

		for (int i = 0; i < 3; i++) {
			if (!WorldInfo.isSolidBase(world, spawnCoords, transformedSize.getX(), transformedSize.getZ(), 50)) {
				if (i == 3) {
					Treasure.logger.debug("Coords -> [{}] does not meet {}% solid base requirements for size -> {} x {}", 50, spawnCoords.toShortString(), transformedSize.getX(), transformedSize.getY());
					return result.fail();
				}
				else {
					spawnCoords = spawnCoords.add(0, -1, 0);
				}
			}
			else {
				continue;
			}
		}

		// generate the structure
		TemplateGenerator generator = new TemplateGenerator();
		generator.setNullBlock(Blocks.AIR);
		
		TreasureGeneratorResult<TemplateGeneratorData> genResult = generator.generate2(world, random, holder, placement, spawnCoords);
		 if (!genResult.isSuccess()) return result.fail();

		// interrogate info for spawners and any other special block processing (except chests that are handler by caller
		List<ICoords> spawnerCoords = (List<ICoords>) genResult.getData().getMap().get(GenUtil.getMarkerBlock(StructureMarkers.SPAWNER));
		List<ICoords> proximityCoords = (List<ICoords>) genResult.getData().getMap().get(GenUtil.getMarkerBlock(StructureMarkers.PROXIMITY_SPAWNER));
		
		// populate vanilla spawners
		buildVanillaSpawners(world, random, spawnCoords, spawnerCoords);
		
		// populate proximity spawners
		buildOneTimeSpawners(world, random, spawnCoords, proximityCoords, new Quantity(1,2), 5D);
					
		result.setData(genResult.getData());

		return result;
	}
	
	@Override
	public void buildOneTimeSpawners(World world, Random random, ICoords spawnCoords, List<ICoords> proximityCoords, Quantity quantity, double d) {
		for (ICoords c : proximityCoords) {
			ICoords c2 = spawnCoords.add(c);
	    	world.setBlockState(c2.toPos(), TreasureBlocks.PROXIMITY_SPAWNER.getDefaultState());
	    	ProximitySpawnerTileEntity te = (ProximitySpawnerTileEntity) world.getTileEntity(c2.toPos());
	    	ResourceLocation r = DungeonHooks.getRandomDungeonMob(random);
	    	te.setMobName(r);
	    	te.setMobNum(new Quantity(1, 2));
	    	te.setProximity(5D);
		}
	}

	@Override
	public void buildVanillaSpawners(World world, Random random, ICoords spawnCoords, List<ICoords> spawnerCoords) {
		for (ICoords c : spawnerCoords) {
			ICoords c2 = spawnCoords.add(c);
			world.setBlockState(c2.toPos(), Blocks.MOB_SPAWNER.getDefaultState());
			TileEntityMobSpawner te = (TileEntityMobSpawner) world.getTileEntity(c2.toPos());
			ResourceLocation r = DungeonHooks.getRandomDungeonMob(random);
			te.getSpawnerBaseLogic().setEntityId(r);
		}
	}


	/**
	 * 
	 * @param world
	 * @param random
	 * @param coords
	 * @return
	 */
	public boolean generate(World world, Random random, ICoords spawnCoords) {
		// get the biome ID
		Biome biome = world.getBiome(spawnCoords.toPos());
		
		// get the template holder from the given archetype, type and biome
		TemplateHolder holder = Treasure.TEMPLATE_MANAGER.getTemplate(world, random, StructureArchetype.SUBMERGED, StructureType.RUIN, biome);
		if (holder == null) return false;		
		
		// select a random rotation
		Rotation rotation = Rotation.values()[random.nextInt(Rotation.values().length)];
		Treasure.logger.debug("rotation used -> {}", rotation);
		
		// setup placement
		PlacementSettings placement = new PlacementSettings();
		placement.setRotation(rotation).setRandom(random);
		
		BlockPos transformedSize = holder.getTemplate().transformedSize(rotation);
		
		for (int i = 0; i < 3; i++) {
			if (!WorldInfo.isSolidBase(world, spawnCoords, transformedSize.getX(), transformedSize.getZ(), 50)) {
				if (i == 3) {
					Treasure.logger.debug("Coords -> [{}] does not meet {}% solid base requirements for size -> {} x {}", 50, spawnCoords.toShortString(), transformedSize.getX(), transformedSize.getY());
					return false;
				}
				else {
					spawnCoords = spawnCoords.add(0, -1, 0);
				}
			}
			else {
				continue;
			}
		}
		
		// generate the structure
		ITemplateGenerator generator = new TemplateGenerator();
		generator.setNullBlock(Blocks.AIR);
		
		 IStructureInfo info = generator.generate(world, random, holder, placement, spawnCoords);
		if (info == null) return false;
		
		Treasure.logger.debug("returned info -> {}", info);
//		setInfo(info);
		
		// interrogate info for spawners and any other special block processing (except chests that are handler by caller
		List<ICoords> spawnerCoords = (List<ICoords>) info.getMap().get(GenUtil.getMarkerBlock(StructureMarkers.SPAWNER));
		List<ICoords> proximityCoords = (List<ICoords>) info.getMap().get(GenUtil.getMarkerBlock(StructureMarkers.PROXIMITY_SPAWNER));
		
		// TODO 1.6.0: move to own method
		// populate vanilla spawners
		for (ICoords c : spawnerCoords) {
			ICoords c2 = spawnCoords.add(c);
			world.setBlockState(c2.toPos(), Blocks.MOB_SPAWNER.getDefaultState());
			TileEntityMobSpawner te = (TileEntityMobSpawner) world.getTileEntity(c2.toPos());
			ResourceLocation r = DungeonHooks.getRandomDungeonMob(random);
			te.getSpawnerBaseLogic().setEntityId(r);
		}
		
		// TODO 1.6.0: move to own method
		// populate proximity spawners
		for (ICoords c : proximityCoords) {
			ICoords c2 = spawnCoords.add(c);
	    	world.setBlockState(c2.toPos(), TreasureBlocks.PROXIMITY_SPAWNER.getDefaultState());
	    	ProximitySpawnerTileEntity te = (ProximitySpawnerTileEntity) world.getTileEntity(c2.toPos());
	    	ResourceLocation r = DungeonHooks.getRandomDungeonMob(random);
	    	te.setMobName(r);
	    	te.setMobNum(new Quantity(1, 2));
	    	te.setProximity(5D);
		}
		
		return true;
	}
}
