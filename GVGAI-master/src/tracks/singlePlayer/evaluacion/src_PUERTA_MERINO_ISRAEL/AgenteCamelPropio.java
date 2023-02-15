package tracks.singlePlayer.evaluacion.src_PUERTA_MERINO_ISRAEL;



import java.util.ArrayList;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;


public class AgenteCamelPropio extends AbstractPlayer{
	
	Vector2d fescala;
	Vector2d portal;
	
	public AgenteCamelPropio(StateObservation state, ElapsedCpuTimer timer) {
		//Calculo el factor de escala píxeles -> grid)
		fescala = new Vector2d(
				state.getWorldDimension().width / state.getObservationGrid().length,
				state.getWorldDimension().height / state.getObservationGrid()[0].length );
		
		//Creo la lista de portales
		ArrayList<Observation>[] posiciones = state.getPortalsPositions(state.getAvatarPosition());
		
		//Selecciono el más próximo
		portal = posiciones[0].get(0).position;
		portal.x = Math.floor(portal.x / fescala.x);
		portal.y = Math.floor(portal.y / fescala.y);
	}
	
	@Override
	public ACTIONS act(StateObservation state, ElapsedCpuTimer timer) {
		//Posiciones del avatar
		Vector2d avatar = 	new Vector2d(
				state.getAvatarPosition().x / fescala.x,
				state.getAvatarPosition().y / fescala.y);
		
		//Simulo las acciones y vamos actualizando la mejor. Para ello:
		
		//Guardo la distancia a la que se encuentra como la mejor
		int best_d = (int) (Math.abs(avatar.x - portal.x) + Math.abs(avatar.y - portal.y));
		ACTIONS best_act = Types.ACTIONS.ACTION_NIL;
		
		int aux_d;
		
		//Mido la distancia a la que se encontrará si hace una acción.
		//En caso de que mejore la solución, actualizo la mejor.
		
		if(avatar.y - 1 >= 0) {
			aux_d = (int) (Math.abs(avatar.x - portal.x) + Math.abs(avatar.y - 1 - portal.y));
			if (aux_d < best_d) {
				best_d = aux_d;
				best_act = Types.ACTIONS.ACTION_UP;						
			}
		}
		
		if(avatar.x - 1 >= 0) {
			aux_d = (int) (Math.abs(avatar.x - 1 - portal.x) + Math.abs(avatar.y - portal.y));
			if (aux_d < best_d) {
				best_d = aux_d;
				best_act = Types.ACTIONS.ACTION_LEFT;						
			}
		}
		
		if(avatar.x + 1 < state.getObservationGrid().length) {
			aux_d = (int) (Math.abs(avatar.x +1 - portal.x) + Math.abs(avatar.y - portal.y));
			if (aux_d < best_d) {
				best_d = aux_d;
				best_act = Types.ACTIONS.ACTION_RIGHT;						
			}
		}
		
		if(avatar.y + 1 < state.getObservationGrid()[0].length) {
			aux_d = (int) (Math.abs(avatar.x - portal.x) + Math.abs(avatar.y + 1 - portal.y));
			if (aux_d < best_d) {
				best_d = aux_d;
				best_act = Types.ACTIONS.ACTION_DOWN;						
			}
		}
		//System.out.print(state.getObservationGrid()[14][1].get(0).itype);
		

		return best_act;
	}
}
