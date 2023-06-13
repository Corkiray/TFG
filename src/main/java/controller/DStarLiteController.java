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
	public PDDLInterface pddlInterface;
	public AgentDStarLite agent;
	public Node currentNode;

	ArrayList<String> agentGoal;
	
	public boolean hayPDDLPlan;
	public boolean hayAgentObjetive;
	public boolean agentNeedsReplan;

	public ArrayList<ACTIONS> plan;

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
		pddlInterface = new PDDLInterface(gameInformation);
		Node.initialize(gameInformation, state);
		agent = new AgentDStarLite();

		hayPDDLPlan = false;
		hayAgentObjetive = false;
		agentNeedsReplan = false;
		
		plan = new ArrayList<ACTIONS> ();
	}
	
    public static void setGameConfigFile(String path) {
        DStarLiteController.gameConfigFile = path;
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
				agent.initialize(state);
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
				agent.updateCosts(state, timer);
				currentNode = agent.act(state);
				if(currentNode == null) {
					agentNeedsReplan=true;
					action = ACTIONS.ACTION_NIL;
				}
				else {
					if(currentNode.get_action()==ACTIONS.ACTION_NIL 
							&& agent.is_goal(currentNode))
						hayAgentObjetive = false;
					action = currentNode.get_action();
				}
				System.out.print(action);
			}
		}
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return action;
	}

}

