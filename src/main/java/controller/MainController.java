package controller;

import java.util.ArrayList;

import controller.Agents.AgentLRTAStar;
import controller.Agents.AgentRTAStar;
import controller.Agents.Node;
import controller.MiniZinc.MinizincInterface;
import controller.PDDL.PDDLInterface;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;

public class MainController extends AbstractPlayer{
    public static String gameConfigFile;

    public MinizincInterface minizincInterface;
	public PDDLInterface pddlPlanner;
	public AgentRTAStar agent;
	public Node node;

	public boolean hayPDDLPlan;
	public boolean hayAgentPlan;

	public MainController(StateObservation state, ElapsedCpuTimer timer) {
		minizincInterface = new MinizincInterface(gameConfigFile);
		pddlPlanner = new PDDLInterface(gameConfigFile);
		agent = new AgentRTAStar(state, timer);
		hayPDDLPlan = false;
		hayAgentPlan = false;
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
		else if(!hayAgentPlan){
			ArrayList<String> agentGoal = pddlPlanner.getNextAction(state);
			System.out.print(agentGoal);	
			agent.setObjetive(agentGoal, state);		
			
			hayAgentPlan = true;
		}
		else {
			Node node = agent.act(state, timer);
			action = node.accion;
			if(node.h == 0)
				hayAgentPlan = false;
			System.out.print(action);
		}
		
		/*
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		*/
		
		return action;
	}
	
}


