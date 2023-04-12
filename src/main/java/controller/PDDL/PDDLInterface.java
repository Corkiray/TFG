/*
 * PlanningAgent.java
 *
 * Copyright (C) 2020 Vladislav Nikolov Vasilev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see https://www.gnu.org/licenses/gpl-3.0.html.
 */

/**
 * Package that contains the planning agent along with its data structures.
 */
package controller.PDDL;

import controller.GameInformation;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import core.vgdl.VGDLRegistry;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;
import ontology.Types;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Planning agent class. It represents an agent which uses a planner to reach
 * a set of goals. See {@link Agenda} to find out how goals are structured.
 *
 * @author Vladislav Nikolov Vasilev
 */
public class PDDLInterface {
    // The following attributes can be modified
    protected static boolean debugMode;
    protected static boolean saveInformation;

    
    // PDDL predicates and objects
    protected List<String> PDDLGameStatePredicates;
    //protected Map<String, Set<String>> PDDLGameStateObjects;

    // Plan to the current goal and iterator to iterate over it
    ArrayList<String> plan;
    ArrayList<ArrayList<String>> translated_plan;
    protected Iterator<ArrayList<String>> iterPlan;

    // Game information data structure (loaded from a .yaml file) and file path
    protected GameInformation gameInformation;

    //Goal
    public static String goal;
    
    // Runtime information
    protected static long executionTime = 0;
    protected static int callsPlan = 0;

    // Logger
    private final static Logger LOGGER = Logger.getLogger(PDDLInterface.class.getName());

    /**
     * Class constructor. Creates a new planning agent.
     *
     * @param stateObservation State observation of the game.
     * @param elapsedCpuTimer  Elapsed CPU time.
     */
    public PDDLInterface(GameInformation gameInf) {
        // Load game information
        gameInformation = gameInf;

        // Initialize PDDL related variables
        this.PDDLGameStatePredicates = new ArrayList<>();
        //this.PDDLGameStateObjects = new HashMap<>();

        // Initialize plan and iterator
        plan = new ArrayList<String>();
        translated_plan = new ArrayList<ArrayList<String>>();
        this.iterPlan = translated_plan.iterator();

        // If the agent must save the information, create directories and initialize logger
        if (PDDLInterface.saveInformation) {

            // Ignore handlers used by parent loggers
            LOGGER.setUseParentHandlers(false);

            // Set locale language to english (logs should be in English :) )
            Locale.setDefault(Locale.ENGLISH);

            try {
                // Add a file handler to the logger
                FileHandler fh = new FileHandler("output/game_execution.log");
                PDDLInterface.LOGGER.addHandler(fh);

                // Set logger's formatter
                SimpleFormatter formatter = new SimpleFormatter();
                fh.setFormatter(formatter);

                PDDLInterface.LOGGER.info("Created agent successfully!");
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }
    }

    /**
     * Method called in each turn that returns the next action that the agent
     * will execute. It is responsible for controlling the agent's behaviour.
     *
     * @param stateObservation State observation of the game.
     * @param elapsedCpuTimer  Elapsed CPU time
     * @return Returns the action that will be executed by the agent in the
     * current turn.
     */
    
    public ArrayList<String> getNextAction(StateObservation stateObservation) {
		callsPlan+=1;

		ArrayList<String> action = this.iterPlan.next();

        // SHOW DEBUG INFORMATION
        if (PDDLInterface.debugMode) {
            this.printMessages("The agent will try to execute the following action:" + action,
                    "\nChecking preconditions...");

            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // SHOW DEBUG INFORMATION
        if (PDDLInterface.debugMode) {
            System.out.println("All preconditions satisfied!");

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        // If no actions are left, that means that the current goal has been reached
        
         if (!this.iterPlan.hasNext()) {
         
            // SHOW DEBUG INFORMATION
            if (PDDLInterface.debugMode) {
                this.showMessagesWait(String.format(
                        "The goal is going the be reached after executing the next action"));
            }


            // Save logging information
            if (PDDLInterface.saveInformation) {
                PDDLInterface.LOGGER.info(String.format(
                        "The goal is going to be reached in this turn"));
            }
        }
         
        // SHOW DEBUG INFORMATION
        if (PDDLInterface.debugMode) {
            System.out.println("The following action is going to be executed in this turn: "
            					+ action);
            try {
                Thread.sleep(2250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return action;
    }

    /**
     * Method that allows the agent to find a plan to the current given goal. It calls
     * the planner and translates its output, generating in the process a new PDDLPlan
     * instance.
     *
     * @return Returns a new PDDLPlan instance.
     * @throws PlannerException Thrown when the planner's response status is not OK.
     */
    public ArrayList<String> execute_metricff() throws PlannerException {
        // Read domain and problem files
        ProcessBuilder processBuilder = new ProcessBuilder();

        ArrayList<String> plan = new ArrayList<String>();
        
        processBuilder.command("cmd.exe", "/c", 
        		"wsl Metric-FF/ff"
        		+ " -o "
        		+ gameInformation.domainFile
        		+ " -f "
        		+ gameInformation.problemFile
        		);

        try {

            Process process = processBuilder.start();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            boolean print=false;
            String line;        
            while ((line = reader.readLine()) != null) {
            	if(line.contains("step")) {
            		line = line.replaceFirst("step", "");
            		print=true;
            	}
            	if(!line.contains(":")) print=false;
            	if(print){
            		line = line.trim();
            		line = line.replaceFirst("[0-9]+: ", "");
            		plan.add(line);
            	}
            }

            int exitCode = process.waitFor();
            System.out.println("\nExited with error code : " + exitCode);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        return plan;
    }

    /**
     * Method that translates a game state observation to PDDL predicates.
     *
     * @param stateObservation State observation of the game.
     */
    public void translateGameStateToPDDL(StateObservation stateObservation) {
        // Get the observations of the game state as elements of the VGDDLRegistry
        HashSet<String>[][] gameMap = this.getGameElementsMatrix(stateObservation);

        // Clear the list of predicates and objects
        this.PDDLGameStatePredicates.clear();
        //this.PDDLGameStateObjects.values().stream().forEach(val -> val.clear());

        final int X_MAX = gameMap.length, Y_MAX = gameMap[0].length;

        // Process game elements
        for (int y = 0; y < Y_MAX; y++) {
            for (int x = 0; x < X_MAX; x++) {
                for (String cellObservation : gameMap[x][y]) {
                    // If the observation is in the domain, instantiate its predicates
                    if (this.gameInformation.pddlCorrespondence.containsKey(cellObservation)) {
                        List<String> predicateList = this.gameInformation.pddlCorrespondence.get(cellObservation);

                        // Instantiate each predicate
                        for (String predicate : predicateList) {
                            String predicateInstance = predicate;
                            
                            // Save instantiated predicate
                            this.PDDLGameStatePredicates.add(predicateInstance);
                        }
                    }
                }
            }
        }
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

    /**
     * Method that creates a PDDL problem file. It writes the PDDL predicates, variables
     * and the current goal to the problem file.
     */
    public void createProblemFile() {
        String outGoal = goal;

        File outputProblemFile = new File(this.gameInformation.problemFile);

        if (outputProblemFile.getParentFile() != null) {
            outputProblemFile.getParentFile().mkdirs();
        }

        try (BufferedWriter bf = new BufferedWriter(new FileWriter(outputProblemFile))) {
            // Write problem name
            bf.write(String.format("(define (problem %sProblem)", this.gameInformation.domainName));
            bf.newLine();

            // Write domain that is used
            bf.write(String.format("    (:domain %s)", this.gameInformation.domainName));
            bf.newLine();

            // Write the objects
            // Each variable will be written
            bf.write("    (:objects");
            bf.newLine();

            // Write each object
            /*
            for (String key : this.PDDLGameStateObjects.keySet()) {
            
                if (!this.PDDLGameStateObjects.get(key).isEmpty()) {
                    String objectsStr = String.join(" ", this.PDDLGameStateObjects.get(key));
                    objectsStr += String.format(" - %s", this.gameInformation.variablesTypes.get(key));
                    bf.write(String.format("        %s", objectsStr));
                    bf.newLine();
                }
            }
			*/
            
            // Finish object writing
            bf.write("    )");
            bf.newLine();

            // Start init writing
            bf.write("    (:init");
            bf.newLine();

            // Write the predicates list into the file
            for (String predicate : this.PDDLGameStatePredicates) {
                bf.write(String.format("        %s", predicate));
                bf.newLine();
            }

            // Finish init writing
            bf.write("    )");
            bf.newLine();

            // Write goal
            // THIS HAS TO CHANGE
            bf.write("    (:goal");
            bf.newLine();

            bf.write("        (AND");
            bf.newLine();

            bf.write(String.format("            %s", outGoal));
            bf.newLine();

            bf.write("        )");
            bf.newLine();

            bf.write("    )");
            bf.newLine();

            // Finish problem writing
            bf.write(")");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void setDebugMode(boolean debugMode) {
        PDDLInterface.debugMode = debugMode;
    }

    public static void setSaveInformation(boolean saveInformation) {
        PDDLInterface.saveInformation = saveInformation;
    }


    /**
     * Method used to display game stats after the execution has finished. It displays
     * the execution time, the number of goals that were reached, the number of times
     * the planner was called and the number of discrepancies that were found.
     */
    public static void displayStats() {
        System.out.println("\n----STATS----\n");
        System.out.println("Execution time: " + PDDLInterface.executionTime + " ms");
        System.out.println("Goal:\n" + PDDLInterface.goal);
        System.out.println("Number of time the plan was called: " + PDDLInterface.callsPlan);
    }


    /**
     * Method that reads the content of a given file.
     *
     * @param filename Path of the file to be read.
     * @return Returns the content of the file.
     */
    private String readFile(String filename) {
        // Create builder that will contain the file's content
        StringBuilder contentBuilder = new StringBuilder();

        // Get content from file line per line
        try (Stream<String> stream = Files.lines(Paths.get(filename))) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return contentBuilder.toString();
    }

    /**
     * Method that prints an array of messages and asks the user to select some
     * of the available options. These options include displaying information about
     * the agenda, displaying information about the current plan or continuing the
     * program's execution. This method is used when the debug mode is enabled.
     *
     * @param messages Messages to be printed.
     */
    private void displayDebugInformation(String... messages) {
        this.printMessages(messages);

        // Request input
        Scanner scanner = new Scanner(System.in);
        int option = 0;
        final int EXIT_OPTION = 2;

        while (option != EXIT_OPTION) {
            this.printMessages("\nSelect what information you want to display:",
                    "[1] : Current plan",
                    "[2] : Continue execution");

            System.out.print("\n$ ");

            // Ignore option if it's not an integer
            while (!scanner.hasNextInt()) {
                scanner.next();
                System.out.print("\n$ ");
            }

            option = scanner.nextInt();

            switch (option) {
                case 1:
                    System.out.println(this.plan);
                    break;
                case EXIT_OPTION:
                    break;
                default:
                    System.out.println("Incorrect option!");
                    break;
            }
        }
    }

    /**
     * Method that prints an array of messages. This method is used when the debug
     * mode is enabled.
     *
     * @param messages Messages to be printed.
     */
    private void printMessages(String... messages) {
        for (String m : messages) {
            System.out.println(m);
        }
    }

    /**
     * Method that creates the output directories in which the generated files
     * will be stored. If the root output directory already exists, it is
     * deleted recursively.
     */
    private void createOutputDirectories() {
        // List of directories
        List<String> directories = Stream.of("output", "output/problems", "output/plans")
                .collect(Collectors.toList());

        // Delete top-level directory recursively if it exists
        if (Files.exists(Paths.get(directories.get(0)))) {
            try {
                Files.walk(Paths.get(directories.get(0)))
                        .map(Path::toFile)
                        .sorted(Comparator.reverseOrder())
                        .forEach(File::delete);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Create output directories
        for (String dir : directories) {
            new File(dir).mkdir();
        }
    }

    /**
     * Method that saves the plan generated by the planner into a file within
     * the output directories structure. It is saved in the directory
     * 'output/plans'.
     *
     * @param plannerResponse
     */
    private void savePlan(JSONObject plannerResponse) {
        String planFileName = String.format("output/plans/plan.txt");
        StringBuilder sb = new StringBuilder();

        // Get the plan from the JSON object
        JSONArray plan = plannerResponse.getJSONObject("result").getJSONArray("plan");

        // Add each action description to the builder
        for (int i = 0; i < plan.length(); i++) {
            String actionDescription = plan.getJSONObject(i).getString("action");
            sb.append(String.format("\n%s\n", actionDescription));
        }

        try (BufferedWriter bf = new BufferedWriter(
                new FileWriter(planFileName))) {
            bf.write(sb.toString());

            // Save logging information
            PDDLInterface.LOGGER.info(String.format("Plan saved to file %s", planFileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void set_goal(String input) {
    	goal = input;
    }
    
    /**
     * Method that prints an array of messages and waits for the user's input.
     * This method is used when the debug mode is enabled.
     *
     * @param messages Messages to be printed.
     */
    private void showMessagesWait(String... messages) {
        this.printMessages(messages);

        Scanner scanner = new Scanner(System.in);

        System.out.println("Press [ENTER] to continue");
        scanner.nextLine();
    }

	public ArrayList<ArrayList<String>> findplan(StateObservation state, ElapsedCpuTimer elapsedCpuTimer) {	
        // Translate game state to PDDL predicates
        this.translateGameStateToPDDL(state);

        // SHOW DEBUG INFORMATION
        if (PDDLInterface.debugMode) {
            this.displayDebugInformation("I don't have a plan to the current goal. I must plan!");
        }

        // Write PDDL predicates into the problem file
        try {
            this.createProblemFile();
        } catch (NullPointerException e) {
            if (PDDLInterface.debugMode) {
                this.printMessages("The agent has reached all goals but can't exit the level!", "Exiting...");
            }

            System.exit(1);
        }

        // Save logging information
        if (PDDLInterface.saveInformation) {
            PDDLInterface.LOGGER.info(String.format(
                    "The following goal has been set as the current goal: %s",
                    goal));
        }

        plan = execute_metricff();
        System.out.print(plan);
        translated_plan = translate_to_agent(plan);
        iterPlan = translated_plan.iterator();

        // SHOW DEBUG INFORMATION
        if (PDDLInterface.debugMode) {
            this.displayDebugInformation("Translated output plan");
        }
        
        executionTime = elapsedCpuTimer.elapsedMillis();

		return translated_plan;
	}
	
    public ArrayList<ArrayList<String>> translate_to_agent(ArrayList<String> input) {
    	ArrayList<ArrayList<String>> output = new ArrayList<ArrayList<String>>();
    	ArrayList<String> objetivesList = new ArrayList<String>();
    	for (String str : input) {
    		
        	objetivesList.clear();
    		for(String key : str.split(" ")) {
        		String objetive = gameInformation.pddl_to_agent_correspondence.get(key);
        		objetivesList.add(objetive);
    		}
    		if(!objetivesList.contains(null))
    			output.add((ArrayList<String>) objetivesList.clone());
    	}
    	
		return output;    	
    }
    
    
}
