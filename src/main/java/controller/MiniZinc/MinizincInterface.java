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

    protected static ArrayList<String> salida;
    protected static long executionTime = 0;
	static double runTime; //Tiempo, en milisegundos, total utilizado

    public MinizincInterface(GameInformation gameInf) {
    	//Inicializar variables
    	salida = new ArrayList<String>();

        //load game information
	    gameInformation = gameInf;        
    }
    
    public String plan(StateObservation state, ElapsedCpuTimer timer) {
		long tInicio = System.nanoTime();

    	generate_dzn(state);
    	ArrayList<String> salida = execute_solver();
    	String goals = translate_to_PDDL(salida);
    	
    	executionTime+=timer.elapsedMillis();
    	
		runTime = (System.nanoTime()-tInicio)/1000000.0;

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
    
    
    public void generate_dzn(StateObservation state) {
        Map<String, ArrayList<String>> gameState = new HashMap<String,ArrayList<String>>(); ;

    	//Extraemos la información usando el estado del juego y la información dada
        ArrayList<Observation>[][] gameMap = state.getObservationGrid();
        
        // Process game elements
        for (ArrayList<Observation>[] observationListList : gameMap) {
        	for(ArrayList<Observation> observationList : observationListList) {
        		for (Observation observation : observationList) {
        			String cellObservation = VGDLRegistry.GetInstance().getRegisteredSpriteKey(observation.itype);
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
        
    	//Guardamos el gameState en el fichero de salida
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
    
    public static void displayStats() {
        System.out.println("\n----MiniZinc STATS----\n");
        System.out.println("Execution time: " + executionTime + " ms");
        System.out.println("Run time: " + runTime + " ms");
        System.out.println("Salida: " + MinizincInterface.salida );
    }
}
