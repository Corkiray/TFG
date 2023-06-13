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
        if (saveInformation) {

            // Ignore handlers used by parent loggers
            LOGGER.setUseParentHandlers(false);

            // Set locale language to english
            Locale.setDefault(Locale.ENGLISH);

            try {
                // Add a file handler to the logger
                FileHandler fh = new FileHandler("output/game_execution.log");
                LOGGER.addHandler(fh);

                // Set logger's formatter
                SimpleFormatter formatter = new SimpleFormatter();
                fh.setFormatter(formatter);

                LOGGER.info("Created agent successfully!");
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
    
    public ArrayList<String> getNextAction() {
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
    public ArrayList<String> execute_metricff() {
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
    public void translateGameStateToPDDL(StateObservation state) {
        // Clear the list of predicates and objects
        this.PDDLGameStatePredicates.clear();
        //this.PDDLGameStateObjects.values().stream().forEach(val -> val.clear());

    	//Extraemos la información usando el estado del juego y la información dada
        ArrayList<Observation>[][] gameMap = state.getObservationGrid();
        
        // Process game elements
        for (ArrayList<Observation>[] observationListList : gameMap) {
        	for(ArrayList<Observation> observationList : observationListList) {
        		for (Observation observation : observationList) {
        			String cellObservation = VGDLRegistry.GetInstance().getRegisteredSpriteKey(observation.itype);
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

	public ArrayList<ArrayList<String>> findplan(StateObservation state, ElapsedCpuTimer timer) {	
        // Translate game state to PDDL predicates
        translateGameStateToPDDL(state);

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
        
        executionTime = timer.elapsedMillis();

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
