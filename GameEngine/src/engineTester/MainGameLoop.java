package engineTester;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import models.RawModel;
import models.TexturedModel;
import normalMappingObjConverter.NormalMappedObjLoader;
import objConverter.ModelData;
import objConverter.OBJFileLoader;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import renderEngine.DisplayManager;
import renderEngine.Loader;
import renderEngine.MasterRenderer;
import renderEngine.OBJLoader;
import terrains.Terrain;
import textures.ModelTexture;
import textures.TerrainTexture;
import textures.TerrainTexturePack;
import toolbox.MousePicker;
import water.WaterFrameBuffers;
import water.WaterRenderer;
import water.WaterShader;
import water.WaterTile;
import entities.Camera;
import entities.Entity;
import entities.Light;
import entities.Player;


public class MainGameLoop {

	public static void main(String[] args) {

		DisplayManager.createDisplay();
		Loader loader = new Loader();

		//setup terrain and textures and other things
		
		TerrainTexture backgroundTexture = new TerrainTexture(loader.loadTexture("grassy2"));
		TerrainTexture rTexture = new TerrainTexture(loader.loadTexture("mud"));
		TerrainTexture gTexture = new TerrainTexture(loader.loadTexture("grassFlowers"));
		TerrainTexture bTexture = new TerrainTexture(loader.loadTexture("path"));

		TerrainTexturePack texturePack = new TerrainTexturePack(backgroundTexture, rTexture,
				gTexture, bTexture);
		TerrainTexture blendMap = new TerrainTexture(loader.loadTexture("blendMap"));

		TexturedModel rocks = new TexturedModel(OBJFileLoader.loadOBJ("rocks", loader),
				new ModelTexture(loader.loadTexture("rocks")));

		ModelTexture fernTextureAtlas = new ModelTexture(loader.loadTexture("fern"));
		fernTextureAtlas.setNumberOfRows(2);

		TexturedModel fern = new TexturedModel(OBJFileLoader.loadOBJ("fern", loader),
				fernTextureAtlas);

		TexturedModel bobble = new TexturedModel(OBJFileLoader.loadOBJ("pine", loader),
				new ModelTexture(loader.loadTexture("pine")));
		bobble.getTexture().setHasTransparency(true);

		fern.getTexture().setHasTransparency(true);

		Terrain terrain = new Terrain(0, -1, loader, texturePack, blendMap, "heightmap");
		List<Terrain> terrains = new ArrayList<Terrain>();
		terrains.add(terrain);

		TexturedModel lamp = new TexturedModel(OBJLoader.loadObjModel("lamp", loader),
				new ModelTexture(loader.loadTexture("lamp")));
		lamp.getTexture().setUseFakeLighting(true);

		List<Entity> entities = new ArrayList<Entity>();
		List<Entity> normalMapEntities = new ArrayList<Entity>();
		
		//normal map models
		
		TexturedModel boulderModel = new TexturedModel(NormalMappedObjLoader.loadOBJ("boulder", loader),
				new ModelTexture(loader.loadTexture("boulder")));
		boulderModel.getTexture().setNormalMap(loader.loadTexture("boulderNormal"));
		boulderModel.getTexture().setShineDamper(10);
		boulderModel.getTexture().setReflectivity(0.5f);
		
		
		//setting up entities in the map (the floating rock on the lake)
		

        
		Entity entity2 = new Entity(boulderModel, new Vector3f(85, 10, -75), 0, 0, 0, 1f);
		
		
		
		normalMapEntities.add(entity2);
		
		
		Random random = new Random(5666778);
		for (int i = 0; i < 60; i++) {
			if (i % 3 == 0) {
				float x = random.nextFloat() * 150;
				float z = random.nextFloat() * -150;
				if ((x > 50 && x < 100) || (z < -50 && z > -100)) {
				} else {
					float y = terrain.getHeightOfTerrain(x, z);

					entities.add(new Entity(fern, 3, new Vector3f(x, y, z), 0,
							random.nextFloat() * 360, 0, 0.9f));
				}
			}
			if (i % 2 == 0) {

				float x = random.nextFloat() * 150;
				float z = random.nextFloat() * -150;
				if ((x > 50 && x < 100) || (z < -50 && z > -100)) {

				} else {
					float y = terrain.getHeightOfTerrain(x, z);
					entities.add(new Entity(bobble, 1, new Vector3f(x, y, z), 0,
							random.nextFloat() * 360, 0, random.nextFloat() * 0.6f + 0.8f));
				}
			}
		}
		entities.add(new Entity(rocks, new Vector3f(75, 4.6f, -75), 0, 0, 0, 75));
		
		//setting up lights, textures, player, mouse

		List<Light> lights = new ArrayList<Light>();
		Light sun = new Light(new Vector3f(10000, 10000, -10000), new Vector3f(1.3f, 1.3f, 1.3f));
		lights.add(sun);

		MasterRenderer renderer = new MasterRenderer(loader);

		RawModel bunnyModel = OBJLoader.loadObjModel("person", loader);
		TexturedModel stanfordBunny = new TexturedModel(bunnyModel, new ModelTexture(
				loader.loadTexture("playerTexture")));

		Player player = new Player(stanfordBunny, new Vector3f(75, 5, -75), 0, 100, 0, 0.6f);
		entities.add(player);
		Camera camera = new Camera(player);
		
		MousePicker picker = new MousePicker(camera, renderer.getProjectionMatrix(), terrain);
	
		//setting up render engine for water
		
		WaterFrameBuffers buffers = new WaterFrameBuffers();
		WaterShader waterShader = new WaterShader();
		WaterRenderer waterRenderer = new WaterRenderer(loader, waterShader, renderer.getProjectionMatrix(), buffers);
		List<WaterTile> waters = new ArrayList<WaterTile>();
		WaterTile water = new WaterTile(75, -75, 0);
		waters.add(water);
		
		//code for the main game loop

		while (!Display.isCloseRequested()) {
			player.move(terrain);
			camera.move();
			picker.update();
			
			entity2.increaseRotation(0, 1, 0);
			
			GL11.glEnable(GL30.GL_CLIP_DISTANCE0);
			
			//our code to render reflection texture
			buffers.bindReflectionFrameBuffer();
			float distance = 2 * (camera.getPosition().y - water.getHeight());
			camera.getPosition().y -= distance;
			camera.invertPitch();
			renderer.renderScene(entities, normalMapEntities, terrains, lights, camera, new Vector4f(0, 1, 0, -water.getHeight()+1));
			camera.getPosition().y += distance;
			camera.invertPitch();
			
			//our code to render refraction texture
			buffers.bindRefractionFrameBuffer();
			renderer.renderScene(entities, normalMapEntities, terrains, lights, camera, new Vector4f(0, -1, 0, water.getHeight()));
			
			//our code to render everything to screen
			GL11.glDisable(GL30.GL_CLIP_DISTANCE0);
			buffers.unbindCurrentFrameBuffer();	
			renderer.renderScene(entities, normalMapEntities, terrains, lights, camera, new Vector4f(0, -1, 0, 100000));	
			waterRenderer.render(waters, camera, sun);
			
			DisplayManager.updateDisplay();
		}

		//clean up from memory after closing display 
		
		buffers.cleanUp();
		waterShader.cleanUp();
		renderer.cleanUp();
		loader.cleanUp();
		DisplayManager.closeDisplay();

	}


}