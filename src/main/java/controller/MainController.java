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

public class MainController extends AbstractPlayer{
    public static String gameConfigFile;

    public MinizincInterface minizincInterface;
	public PDDLInterface pddlInterface;
	public AgentAStar agent;
	public Node actualNode;

	ArrayList<String> agentGoal;
	
	public boolean hayPDDLPlan;
	public boolean hayAgentObjetive;
	public boolean agentNeedsReplan;

	private GameInformation gameInformation;

	public MainController(StateObservation state, ElapsedCpuTimer timer) {
		// Load game information
		Yaml yaml = new Yaml(new Constructor(GameInformation.class));
		
		try {
			InputStream inputStream = new FileInputStream(new File(gameConfigFile));
			this.gameInformation = yaml.load(inputStream);
		} catch (FileNotFoundException e) {
			System.out.println(e.getStackTrace());
		}
		
		minizincInterface = new MinizincInterface(gameInformation);
		pddlInterface = new PDDLInterface(gameInformation);
		Node.initialize(gameInformation, state);
		agent = new AgentAStar();

		hayPDDLPlan = false;
		hayAgentObjetive = false;
		agentNeedsReplan = false;
		
	}
	
    public static void setGameConfigFile(String path) {
        MainController.gameConfigFile = path;
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

			//ArrayList<String> exit = new ArrayList<String>();
			//exit.add("exit");
			//Node.setObjetive(exit , state);
			
			Node.setObjetive(agentGoal , state);
			
			hayAgentObjetive = true;
		}
		else {
				actualNode = agent.act(state);
				
				if(actualNode.h==0)
					hayAgentObjetive = false;
				action = actualNode.accion;
				
				System.out.print(action);
			}
		
		try {
			Thread.sleep(0);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return action;
	}

}

