package controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import controller.Agents.*;

import controller.MiniZinc.MinizincInterface;
import controller.PDDL.PDDLInterface;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class DStarLiteController extends AbstractPlayer{
    public static String gameConfigFile;

    public MinizincInterface minizincInterface;
	public PDDLInterface pddlPlanner;
	public AgentDStarLite agent;
	public Node actualNode;

	ArrayList<String> agentGoal;
	
	public boolean hayPDDLPlan;
	public boolean hayAgentObjetive;
	public boolean agentNeedsReplan;

	private GameInformation gameInformation;

	public DStarLiteController(StateObservation state, ElapsedCpuTimer timer) {
		// Load game information
		Yaml yaml = new Yaml(new Constructor(GameInformation.class));
		
		try {
			InputStream inputStream = new FileInputStream(new File(MainController.gameConfigFile));
			this.gameInformation = yaml.load(inputStream);
		} catch (FileNotFoundException e) {
			System.out.println(e.getStackTrace());
		}
		
		minizincInterface = new MinizincInterface(gameInformation);
		pddlPlanner = new PDDLInterface(gameInformation);
		Node.initialize(gameInformation, state);
		agent = new AgentDStarLite();

		hayPDDLPlan = false;
		hayAgentObjetive = false;
		agentNeedsReplan = false;
		
	}
	
    public static void setGameConfigFile(String path) {
        DStarLiteController.gameConfigFile = path;
    }

	public ACTIONS act(StateObservation state, ElapsedCpuTimer timer) {
		ACTIONS action = ACTIONS.ACTION_NIL;
		
		if (!hayPDDLPlan) {
			String goals = minizincInterface.plan(state, timer);
			System.out.print("\n"+goals+"\n");
			
			
			pddlPlanner.set_goal(goals);
			ArrayList<ArrayList<String>> plan = pddlPlanner.findplan(state, timer);
			System.out.print("\n"+plan+"\n");
					
			
			hayPDDLPlan = true;
		}
		else if(!hayAgentObjetive){
			agentGoal = pddlPlanner.getNextAction(state);
			System.out.print(agentGoal);

			Node.setObjetive(agentGoal, state);
			agent.initialize(state);
			agent.plan(state, timer);
			
			//agent.clear();
			hayAgentObjetive = true;
		}
		else if(agentNeedsReplan) {
			agent.plan(state, timer);
			agentNeedsReplan=false;
		}
		else {
			agent.updateCosts(state, timer);
			actualNode = agent.act(state);
			if(actualNode == null) {
				agentNeedsReplan=true;
				action = ACTIONS.ACTION_NIL;
			}
			else {
				if(actualNode.accion==ACTIONS.ACTION_NIL 
				&& agent.is_goal(actualNode))
					hayAgentObjetive = false;
				action = actualNode.accion;
			}
			System.out.print(action);
		}
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return action;
	}

}

