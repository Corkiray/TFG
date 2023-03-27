package controller;

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

public class MainAgent extends AbstractPlayer{
    public static String gameConfigFile;

	public PDDLInterface pddlPlanner;
	public MinizincInterface minizincInterface;
	public boolean hayPlan;

	public MainAgent(StateObservation state, ElapsedCpuTimer timer) {
		pddlPlanner = new PDDLInterface(gameConfigFile);
		minizincInterface = new MinizincInterface(gameConfigFile);
		hayPlan = false;
	}
	
    public static void setGameConfigFile(String path) {
        MainAgent.gameConfigFile = path;
    }

	@Override
	public ACTIONS act(StateObservation state, ElapsedCpuTimer timer) {
		ACTIONS accion = ACTIONS.ACTION_NIL;
	
		if (!hayPlan) {
			String goals = minizincInterface.plan(state, timer);
			System.out.print("\n"+goals+"\n");
			
			
			pddlPlanner.set_goal(goals);
			ArrayList<ArrayList<String>> plan = pddlPlanner.findplan(state, timer);
			System.out.print("\n"+plan+"\n");
		
			
			System.out.print(pddlPlanner.getNextAction(state));
			System.out.print(pddlPlanner.getNextAction(state));
			System.out.print("\n");
					
			hayPlan = true;
		}
	
		return accion;
	}

}


