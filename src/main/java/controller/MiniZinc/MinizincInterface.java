package controller.MiniZinc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import controller.GameInformation;
import controller.PDDL.PDDLInterface;
import core.game.Observation;
import core.game.StateObservation;
import core.vgdl.VGDLRegistry;
import tools.ElapsedCpuTimer;

public class MinizincInterface {
    protected GameInformation gameInformation;

    // Game information data structure (loaded from a .yaml file) and file path
    //protected GameInformation gameInformation;

    protected Map<String, ArrayList<String>> gameState;
    protected ArrayList<String> salida;
    protected static long executionTime = 0;

    public MinizincInterface(String gameConfigFile) {
    	//Inicializar variables
    	salida = new ArrayList<String>();
        gameState = new HashMap<String,ArrayList<String>>();

	    // Load game information
	    Yaml yaml = new Yaml(new Constructor(GameInformation.class));
	    
        try {
            InputStream inputStream = new FileInputStream(new File(gameConfigFile));
            this.gameInformation = yaml.load(inputStream);
        } catch (FileNotFoundException e) {
            System.out.println(e.getStackTrace());
        }
        
    }
    
    public String plan(StateObservation state, ElapsedCpuTimer timer) {
    	
    	generate_dzn(state);
    	execute_solver();
    	String goals = translate_to_PDDL(salida);
    	
    	executionTime+=timer.elapsedMillis();
    	
    	return goals;
    }
    
    public ArrayList<String> execute_solver() {   	
        ProcessBuilder processBuilder = new ProcessBuilder();

        processBuilder.command("cmd.exe", "/c", 
        		"wsl minizinc "
        		+ gameInformation.modelFile
        		+ " "
        		+ gameInformation.dzn_dataFile);
        try {
            Process process = processBuilder.start();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;         
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                salida.add(line);
            }
            salida.remove(salida.size()-1);
            salida.remove(salida.size()-1);

            int exitCode = process.waitFor();
            System.out.println("\nExited with error code : " + exitCode);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        return salida;
    }
    
    /**
     * Method that translates a game state observation to a matrix of strings which
     * represent the elements of the game in each position according to the VGDDL
     * registry. There can be more than one game element in each position.
     *
     * @param stateObservation State observation of the game.
     * @return Returns a matrix containing the elements of the game in each position.
     */
    public HashSet<String>[][] getGameElementsMatrix(StateObservation stateObservation) {
        // Get the current game state
        ArrayList<Observation>[][] gameState = stateObservation.getObservationGrid();

        // Get the number of X tiles and Y tiles
        final int X_MAX = gameState.length, Y_MAX = gameState[0].length;

        // Create a new matrix, representing the game's map
        HashSet<String>[][] gameStringMap = new HashSet[X_MAX][Y_MAX];

        // Iterate over the map and transform the observations in a [x, y] cell
        // to a HashSet of Strings. In case there's no observation, add a
        // "background" string. The VGDLRegistry contains the needed information
        // to transform the StateObservation to a matrix of sets of Strings.
        for (int y = 0; y < Y_MAX; y++) {
            for (int x = 0; x < X_MAX; x++) {
                gameStringMap[x][y] = new HashSet<>();

                if (gameState[x][y].size() > 0) {
                    for (int i = 0; i < gameState[x][y].size(); i++) {
                        int itype = gameState[x][y].get(i).itype;
                        gameStringMap[x][y].add(VGDLRegistry.GetInstance().getRegisteredSpriteKey(itype));
                    }
                } else {
                    gameStringMap[x][y].add("background");
                }
            }
        }

        return gameStringMap;
    }
 
    //AAAATERMINAR
    public void setGameState(StateObservation stateObservation) {   	
        // Get the observations of the game state as elements of the VGDDLRegistry
        HashSet<String>[][] gameMap = this.getGameElementsMatrix(stateObservation);
        
        final int X_MAX = gameMap.length, Y_MAX = gameMap[0].length;

        // Process game elements
        for (int y = 0; y < Y_MAX; y++) {
            for (int x = 0; x < X_MAX; x++) {
                for (String cellObservation : gameMap[x][y]) {
                    if (gameInformation.minizincCorrespondence.containsKey(cellObservation)) {
                    	Map<String, ArrayList<String>> map = (Map<String, ArrayList<String>>) gameInformation.minizincCorrespondence.get(cellObservation);
                    	//System.out.print(map.get("existe")+"\n");
                    	gameState.put(cellObservation, map.get("existe"));
                    }
                }
            }
        }
        
        for (String key : gameInformation.minizincCorrespondence.keySet()){
        	if (gameState.get(key) == null){
            	Map<String, ArrayList<String>> map = (Map<String, ArrayList<String>>) gameInformation.minizincCorrespondence.get(key);
            	gameState.put(key, map.get("no_existe"));
            	//System.out.print(map.get("no_existe")+"\n");
        	}
        }
        	
    }
    
    public void generate_dzn(StateObservation state) {
    	//Extraemos la información usando el estado del juego y la información dada
    	setGameState(state);
    	
    	//Guardamos en gameState en el fichero de salida
        File outputDataFile = new File(gameInformation.dzn_dataFile);

        if (outputDataFile.getParentFile() != null) {
        	outputDataFile.getParentFile().mkdirs();
        }
        
        try (BufferedWriter bf = new BufferedWriter(new FileWriter(outputDataFile))) {
        	for (String key: gameState.keySet()) {
        		for (String str : gameState.get(key)){
        			//System.out.print(str+"\n");
            		bf.write(str+"\n");
        		}
        	}
        	
        } catch (IOException e) {
            e.printStackTrace();
        }       
    	
    }

    
    public String translate_to_PDDL(ArrayList<String> input) {
    	String goals = gameInformation.minizinc_to_PDDL_correspondence.get("default");
    	
    	String goal;
    	for (String str : input) {
    		goal = gameInformation.minizinc_to_PDDL_correspondence.get(str);
    		if( goal != null) goals+="\n"+goal;
    	}
    	
		return goals;
    }
}
