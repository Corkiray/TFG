package controller.Agents;

import java.util.ArrayList;

import core.game.StateObservation;
import ontology.Types.ACTIONS;
import tools.Vector2d;

public class Dropper {
	public static final ArrayList<Vector2d> orientations = new ArrayList<Vector2d>(
		    new ArrayList<Vector2d>() {{
		        add(new Vector2d(-1, 0));
		        add(new Vector2d(1, 0));
		        add(new Vector2d(0, -1));
		        add(new Vector2d(0, 1));
		    }});

	
	public static ArrayList<ACTIONS> plan(StateObservation state){
		Node actual = new Node(state);
		actual.accion = ACTIONS.ACTION_USE;
		
		ArrayList<ACTIONS> plan = new ArrayList<ACTIONS> ();
		plan.add(ACTIONS.ACTION_USE);
		if(actual.is_able(state)) {
			return plan;
		}
		
		for(Vector2d ori : orientations) {
			actual.orientation = ori;
			if(actual.is_able(state)) {
				ACTIONS action = actual.get_action(ori);
				plan.add(0, action);
			}
		}
		
		return plan;
	}	

}
