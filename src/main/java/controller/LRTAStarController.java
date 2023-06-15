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

public class LRTAStarController extends AbstractPlayer{
    public static String gameConfigFile;

    public MinizincInterface minizincInterface;
	public PDDLInterface pddlInterface;
	public AgentLRTAStar agent;
	public Node actualNode;

	ArrayList<String> agentGoal;
	
	public boolean hayPDDLPlan;
	public boolean hayAgentObjetive;
	public boolean agentNeedsReplan;

	private GameInformation gameInformation;
	
	ArrayList<ACTIONS> plan;

	public LRTAStarController(StateObservation state, ElapsedCpuTimer timer) {
		// Load game information
		Yaml yaml = new Yaml(new Constructor(GameInformation.class));
		
		try {
			InputStream inputStream = new FileInputStream(new File(MainController.gameConfigFile));
			this.gameInformation = yaml.load(inputStream);
		} catch (FileNotFoundException e) {
			System.out.println(e.getStackTrace());
		}
		
		minizincInterface = new MinizincInterface(gameInformation);
		pddlInterface = new PDDLInterface(gameInformation);
		Node.initialize(gameInformation, state, true);
		agent = new AgentLRTAStar();
		
		plan = new ArrayList<ACTIONS>();

		hayPDDLPlan = false;
		hayAgentObjetive = false;
		agentNeedsReplan = false;
		
	}
	
    public static void setGameConfigFile(String path) {
        LRTAStarController.gameConfigFile = path;
    }

	public ACTIONS act(StateObservation state, ElapsedCpuTimer timer) {
		ACTIONS action = ACTIONS.ACTION_NIL;
		
		if (!hayPDDLPlan) {
			String goals = minizincInterface.plan(state, timer);
			System.out.print("\n"+goals+"\n");
			
			
			pddlInterface.set_goal(goals);
			ArrayList<ArrayList<String>> plan = pddlInterface.findplan(state, timer);
			System.out.print("\n"+plan+"\n");
					
			
			hayPDDLPlan = true;
		}
		else if(!hayAgentObjetive){
			agentGoal = pddlInterface.getNextAction();
			System.out.print(agentGoal);
			
			Node.setObjetive(agentGoal, state);
			
			if(agentGoal.get(0).contentEquals("drop")) {
				plan = Dropper.plan(state);
			}
			else {
				agent.clear();
				plan = agent.act(state, timer);
			}
			
			hayAgentObjetive = true;
			agentNeedsReplan = false;
		}
		else if(agentNeedsReplan){
			plan = agent.act(state, timer);
			
			agentNeedsReplan = false;
						
		}
		
		if(!agentNeedsReplan) {
			if(!plan.isEmpty())
				action = plan.remove(0);
			
			System.out.print(action);
			
			if(plan.isEmpty())
				agentNeedsReplan = true;
			
			if(action == ACTIONS.ACTION_NIL)
				hayAgentObjetive = false;	
		}
		
		/*
		try {
			Thread.sleep(0);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		*/
		
		return action;
	}

}

