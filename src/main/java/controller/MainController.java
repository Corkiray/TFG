package controller;

import controller.Agents.AgenteAStar.AgenteAStar;
import controller.Agents.AgenteAStar.Node;
import controller.MiniZinc.MinizincInterface;
import java.util.ArrayList;
import java.util.PriorityQueue;

import controller.PDDL.PDDLInterface;

import java.util.Comparator;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import core.player.AbstractPlayer;

public class MainController extends AbstractPlayer{
    public static String gameConfigFile;

    public MinizincInterface minizincInterface;
	public PDDLInterface pddlPlanner;
	public AgenteAStar agente;

	public boolean hayPDDLPlan;
	public boolean hayAgentPlan;

	public MainController(StateObservation state, ElapsedCpuTimer timer) {
		minizincInterface = new MinizincInterface(gameConfigFile);
		pddlPlanner = new PDDLInterface(gameConfigFile);
		agente = new AgenteAStar(state, timer);
		hayPDDLPlan = false;
		hayAgentPlan = false;
	}
	
    public static void setGameConfigFile(String path) {
        MainController.gameConfigFile = path;
    }

	@Override
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
			ArrayList<String> agentAction = pddlPlanner.getNextAction(state);
			System.out.print(agentAction);	
			agente.plan(state, timer, agentAction);		
			
			hayAgentPlan = true;
		}
		else {
			action = agente.act();
			if(action==ACTIONS.ACTION_NIL)
				hayAgentPlan = false;
			System.out.print(action);
		}
		
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return action;
	}
	
}


