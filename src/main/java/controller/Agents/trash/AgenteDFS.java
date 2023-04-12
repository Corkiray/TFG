package controller.Agents.trash;


import java.util.ArrayList;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import core.player.AbstractPlayer;

public class AgenteDFS extends AbstractPlayer{
	
	Vector2d fescala;
	Nodo portal;
	boolean hayPlan;
	ArrayList<ACTIONS> plan;
	boolean [][] visitados; //Mapa de booleanos, que indicará si la posición (x,y) ha sido visitada ya
	int nExpandidos; //Número de nodos que han sido expandidos(se ha comprobado si es objetivo)
	int maxMem; //Número máximo de nodos almacenados en memoria
	int tamRuta; //Número de nodos transitados por el agente
	double runTime; //Tiempo, en milisegundos, usado para calcular el plan
	
	/**
	 * initialize all variables for the agent
	 * @param stateObs Observation of the current state.
	 * @param elapsedTimer Timer when the action returned is due.
	 */
	public AgenteDFS(StateObservation state, ElapsedCpuTimer timer) {
		//Calculo el factor de escala píxeles -> grid)
		fescala = new Vector2d(
				state.getWorldDimension().width / state.getObservationGrid().length,
				state.getWorldDimension().height / state.getObservationGrid()[0].length );
		
		//Creo la lista de portales AAAA BORRAR
		//ArrayList<Observation>[] posiciones = state.getPortalsPositions(state.getAvatarPosition());
		//Selecciono el más próximo
		//portal = new Nodo();
		//portal.x = (int) (Math.floor(posiciones[0].get(0).position.x / fescala.x));
		//portal.y = (int) (Math.floor(posiciones[0].get(0).position.y / fescala.y));
		
		//Inicializo el plan a vacío
		hayPlan = false;
		plan = new ArrayList<ACTIONS>();
		
		//Inicializo los resultados a 0
		nExpandidos = 0;
		maxMem = 0;
		tamRuta = 0;
		
		//Creo el mapa de visitados
		visitados = new boolean[state.getObservationGrid().length][state.getObservationGrid()[0].length];

	}
	
	
	public ArrayList<ACTIONS> plan(StateObservation state, ElapsedCpuTimer timer, String objetivo) {		
		//Genero el nodo inicial
		Vector2d pos_avatar = new Vector2d(
				state.getAvatarPosition().x / fescala.x,
				state.getAvatarPosition().y / fescala.y);
		Nodo avatar = new Nodo(pos_avatar);
		
		//Llamo al algoritmo de búsqueda, que devolverá el último nodo del camino encontrado
		long tInicio = System.nanoTime();
		Nodo ultimo = DFS(avatar, portal, state);
		runTime = (System.nanoTime()-tInicio)/1000000.0;
		hayPlan = true;

		//Recorro los padres desde el nodo dado hasta el nodo inicial, y voy guardándolos al principio del plan			
		while(ultimo != avatar) {
			plan.add(0, ultimo.accion);
			ultimo = ultimo.padre;			
		}
		tamRuta = plan.size();

		//Imprimo los datos de la planificación
		System.out.print(" Runtime(ms): " + runTime + 
				",\n Tamaño de la ruta calculada: " + tamRuta +
				",\n Número de nodos expandidos: " + nExpandidos +
				",\n Máximo número de nodos en memoria: " + maxMem +
				"\n");
	}
	
	/**
	 * Sigue un camino generado por un DFS
	 * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
	 * @return 	La próxima acción a realizar
	 */
	@Override
	public ACTIONS act(StateObservation state, ElapsedCpuTimer timer) {

		//Acción que devolverá el algoritmo
		ACTIONS accion = ACTIONS.ACTION_NIL;

		if(!plan.isEmpty()) {
			accion = plan.get(0);
			plan.remove(0);			
		}
				
		return accion;
	}
	
	//Algoritmo DFS
	public Nodo DFS(Nodo inicial, Nodo objetivo, StateObservation state){
		//Cuando llamo a la búsqueda con un nodo, lo marco como visitado. Además, aumento el número de nodos en memoria.
		visitados[inicial.x][inicial.y] = true;
		maxMem++;
		return DFS_search(inicial, objetivo, state);
	}
	
	//Búsqueda en profundidad a partir de un nodo concreto.
	public Nodo DFS_search(Nodo inicial, Nodo objetivo, StateObservation state){
		Nodo hijo = new Nodo(); //Nodo en el que se irá almacenando el hijo del actual
		Nodo ultimo; //Nodo que se devuelve. Es en el que se almacena el resultado de esta búsqueda en profundidad.
					//Si no se ha llegado al objetivo, se devolverá null.
		
		nExpandidos++;
		if (inicial.x == objetivo.x && inicial.y == objetivo.y) return inicial;
		
		//Exploro UP
		hijo.x = inicial.x;
		hijo.y = inicial.y-1;

		//Si esa posición en el grid está vacía, es que es suelo. Se puede avanzar
		if( (state.getObservationGrid()[hijo.x][hijo.y].isEmpty())
		//Si las cordenadas son las mismas que las del objetivo, también se puede avanzar			
		|| ( (hijo.x == objetivo.x) && (hijo.y == objetivo.y) )	) {
			//Si no está visitado, le añado los atributos correspondientes e inicio una búsqueda en profundidad desde este nodo
			if (!visitados[hijo.x][hijo.y]) {
				visitados[hijo.x][hijo.y] = true;
				maxMem++;
				hijo.padre = inicial;
				hijo.accion = ACTIONS.ACTION_UP;
				ultimo = DFS_search(new Nodo(hijo), objetivo, state);
				//Si no es null, es que se ha encontrado el objetivo. Se devuelve
				if(ultimo != null) return ultimo;
			}
		}
		
		//Exploro DOWN
		hijo.x = inicial.x;
		hijo.y = inicial.y+1;
		//Si esa posición en el grid está vacía, es que es suelo. Se puede avanzar
		if( (state.getObservationGrid()[hijo.x][hijo.y].isEmpty())
		//Si las cordenadas son las mismas que las del objetivo, está la meta. Se puede avanzar			
		|| ( (hijo.x == objetivo.x) && (hijo.y == objetivo.y) )	) {
			//Si no está visitado, le añado los atributos correspondientes e inicio una búsqueda en profundidad desde este nodo
			if (!visitados[hijo.x][hijo.y]) {
				visitados[hijo.x][hijo.y] = true;
				maxMem++;
				hijo.padre = inicial;
				hijo.accion = ACTIONS.ACTION_DOWN;
				ultimo = DFS_search(new Nodo(hijo), objetivo, state);
				//Si no es null, es que se ha encontrado el objetivo. Se devuelve
				if(ultimo != null) return ultimo;
			}
		}
		
		//Exploro LEFT
		hijo.x = inicial.x-1;
		hijo.y = inicial.y;
		//Si esa posición en el grid está vacía, es que es suelo. Se puede avanzar
		if( (state.getObservationGrid()[hijo.x][hijo.y].isEmpty())
		//Si las cordenadas son las mismas que las del objetivo, está la meta. Se puede avanzar			
		|| ( (hijo.x == objetivo.x) && (hijo.y == objetivo.y) )	) {
			//Si no está visitado, le añado los atributos correspondientes e inicio una búsqueda en profundidad desde este nodo
			if (!visitados[hijo.x][hijo.y]) {
				visitados[hijo.x][hijo.y] = true;
				maxMem++;
				hijo.padre = inicial;
				hijo.accion = ACTIONS.ACTION_LEFT;
				ultimo = DFS_search(new Nodo(hijo), objetivo, state);
				//Si no es null, es que se ha encontrado el objetivo. Se devuelve
				if(ultimo != null) return ultimo;
			}
		} 
		
		//Exploro RIGHT
		hijo.x = inicial.x+1;
		hijo.y = inicial.y;
		//Si esa posición en el grid está vacía, es que es suelo. Se puede avanzar
		if( (state.getObservationGrid()[hijo.x][hijo.y].isEmpty())
		//Si las cordenadas son las mismas que las del objetivo, está la meta. Se puede avanzar			
		|| ( (hijo.x == objetivo.x) && (hijo.y == objetivo.y) )	) {
			//Si no está visitado, le añado los atributos correspondientes e inicio una búsqueda en profundidad desde este nodo
			if (!visitados[hijo.x][hijo.y]) {
				visitados[hijo.x][hijo.y] = true;
				maxMem++;
				hijo.padre = inicial;
				hijo.accion = ACTIONS.ACTION_RIGHT;
				ultimo = DFS_search(new Nodo(hijo), objetivo, state);
				//Si no es null, es que se ha encontrado el objetivo. Se devuelve
				if(ultimo != null) return ultimo;
			}
		}		
		//Si llega hasta aquí, es que ninguna de las búsquedas en profundidad ha encontrado el objetivo.
		//En ese caso, se devuelve null.
		return null;
	}

}