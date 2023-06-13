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

public class AStarController extends AbstractPlayer{
    public static String gameConfigFile;

    public MinizincInterface minizincInterface;
	public PDDLInterface pddlPlanner;
	public AgentAStar agent;
	public Node currenNode;

	ArrayList<String> agentGoal;
	
	public boolean hayPDDLPlan;
	public boolean hayAgentObjetive;
	public boolean agentNeedsReplan;
	
	public ArrayList<ACTIONS> plan;

	private GameInformation gameInformation;

	public AStarController(StateObservation state, ElapsedCpuTimer timer) {
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
		agent = new AgentAStar();

		hayPDDLPlan = false;
		hayAgentObjetive = false;
		agentNeedsReplan = false;
				
		plan = new ArrayList<ACTIONS> ();
	}
	
    public static void setGameConfigFile(String path) {
        MainController.gameConfigFile = path;
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
			agentGoal = pddlPlanner.getNextAction();
			System.out.print(agentGoal);

			Node.setObjetive(agentGoal, state);

			if(agentGoal.get(0).contentEquals("drop")) {
				plan = Dropper.plan(state);
			}
			else {				
				agent.plan(state, timer);
			}
			
			hayAgentObjetive = true;
		}
		else if(agentNeedsReplan) {
			agent.plan(state, timer);
			agentNeedsReplan=false;
		}
		else {
			
			if(!plan.isEmpty()) {
				action =  plan.remove(0);
			}
			else {				
				currenNode = agent.act(state);
				if(currenNode == null) {
					agentNeedsReplan=true;
				}
				else {
					action = currenNode.get_action();
					if(action==ACTIONS.ACTION_NIL)
						hayAgentObjetive = false;
				}
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

