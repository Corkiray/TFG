package controller.Agents.trash;

import java.util.ArrayList;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import core.player.AbstractPlayer;

public class AgenteBFS extends AbstractPlayer{
	
	Vector2d fescala;
	Nodo portal;
	boolean hayPlan;
	ArrayList <ACTIONS> plan;
	boolean [][] visitados; //Mapa de booleanos, que indicar� si la posici�n (x,y) ha sido visitada ya
	int nExpandidos; //N�mero de nodos que han sido expandidos(se ha comprobado si es objetivo)
	int maxMem; //N�mero m�ximo de nodos almacenados en memoria
	int tamRuta; //N�mero de nodos transitados por el agente
	double runTime; //Tiempo, en milisegundos, usado para calcular el plan
	
	/**
	 * initialize all variables for the agent
	 * @param stateObs Observation of the current state.
	 * @param elapsedTimer Timer when the action returned is due.
	 */
	public AgenteBFS(StateObservation state, ElapsedCpuTimer timer) {
		//Calculo el factor de escala p�xeles -> grid)
		fescala = new Vector2d(
				state.getWorldDimension().width / state.getObservationGrid().length,
				state.getWorldDimension().height / state.getObservationGrid()[0].length );
		
		//Creo la lista de portales
		ArrayList<Observation>[] posiciones = state.getPortalsPositions(state.getAvatarPosition());
		//Selecciono el m�s pr�ximo
		portal = new Nodo();
		portal.x = (int) (Math.floor(posiciones[0].get(0).position.x / fescala.x));
		portal.y = (int) (Math.floor(posiciones[0].get(0).position.y / fescala.y));
		
		//Inicializo el plan a vac�o
		hayPlan = false;
		plan = new ArrayList<ACTIONS>();
		
		//Inicializo los resultados a 0
		nExpandidos = 0;
		maxMem = 0;
		tamRuta = 0;
		
		//Creo el mapa de visitados, inicializado todo por defecto a false
		visitados = new boolean[state.getObservationGrid().length][state.getObservationGrid()[0].length];

	}
	
	
	
	/**
	 * Sigue un camino generado por BFS
	 * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
	 * @return 	La pr�xima acci�n a realizar
	 */
	@Override
	public ACTIONS act(StateObservation state, ElapsedCpuTimer timer) {

		//Acci�n que devolver� el algoritmo
		ACTIONS accion = ACTIONS.ACTION_NIL;
		
		//Si no hay plan, busco uno nuevo
		if(!hayPlan) {
			
			//Genero el nodo inicial
			Vector2d pos_avatar = new Vector2d(
					state.getAvatarPosition().x / fescala.x,
					state.getAvatarPosition().y / fescala.y);
			Nodo avatar = new Nodo(pos_avatar);
			
			//Llamo al algoritmo de b�squeda, que devolver� el �ltimo nodo del camino encontrado
			long tInicio = System.nanoTime();
			Nodo ultimo = BFS(avatar, portal, state);
			runTime = (System.nanoTime()-tInicio)/1000000.0;
			hayPlan = true;
			
			//Recorro los padres desde el nodo dado hasta el nodo inicial, y voy guard�ndolos al principio del plan
			while(ultimo != avatar) {
				plan.add(0, ultimo.accion);
				ultimo = ultimo.padre;			
			}
			tamRuta = plan.size();
			
			//Imprimo los datos de la planificaci�n
			System.out.print(" Runtime(ms): " + runTime + 
					",\n Tama�o de la ruta calculada: " + tamRuta +
					",\n N�mero de nodos expandidos: " + nExpandidos +
					",\n M�ximo n�mero de nodos en memoria: " + maxMem +
					"\n");
		}
		//Si hay plan, selecciono la siguiente acci�n y la saco del plan
		else if(!plan.isEmpty()) {
			accion = plan.get(0);
			plan.remove(0);			
		}
				
		return accion;
	}

	//Algoritmo BFS
	public Nodo BFS(Nodo inicial, Nodo objetivo, StateObservation state) {
		ArrayList<Nodo> cola = new ArrayList<Nodo>(); //Cola de nodos a explorar
		Nodo actual; //Nodo que se est� explorando
		Nodo hijo = new Nodo(); //Nodo en el que se ir� almacenando el hijo del actual
		
		//Cuando a�ado un nodo a la cola, ya est� visitado. Adem�s, aumento el n�mero de nodos en memoria.
		visitados[inicial.x][inicial.y] = true;
		maxMem++;
		cola.add(inicial);
		
		while(!cola.isEmpty()) {
			actual = cola.get(0);
			cola.remove(0);

			nExpandidos++;
			if (actual.x == objetivo.x && actual.y == objetivo.y) return actual;
			
			//Exploro UP
			hijo.x = actual.x;
			hijo.y = actual.y-1;
			
			//Si esa posici�n en el grid est� vac�a, es que es suelo. Se puede avanzar
			if( (state.getObservationGrid()[hijo.x][hijo.y].isEmpty())
			//Si las cordenadas son las mismas que las del objetivo, est� la meta. Se puede avanzar			
			|| ( (hijo.x == objetivo.x) && (hijo.y == objetivo.y) )	) {
				//Si no est� visitado, a�ado a�ado el nodo a la cola, con los atributos correspondientes.
				if (!visitados[hijo.x][hijo.y]) {
					visitados[hijo.x][hijo.y] = true;
					maxMem++;
					hijo.padre = actual;
					hijo.accion = ACTIONS.ACTION_UP;
					cola.add(new Nodo(hijo));
				}
			}
			
			//Exploro DOWN
			hijo.x = actual.x;
			hijo.y = actual.y+1;
			//Si esa posici�n en el grid est� vac�a, es que es suelo. Se puede avanzar
			if( (state.getObservationGrid()[hijo.x][hijo.y].isEmpty())
			//Si las cordenadas son las mismas que las del objetivo, est� la meta. Se puede avanzar			
			|| ( (hijo.x == objetivo.x) && (hijo.y == objetivo.y) )	) {
				//Si no est� visitado, a�ado a�ado el nodo a la cola, con los atributos correspondientes.
				if (!visitados[hijo.x][hijo.y]) {
					visitados[hijo.x][hijo.y] = true;
					maxMem++;
					hijo.padre = actual;
					hijo.accion = ACTIONS.ACTION_DOWN;
					cola.add(new Nodo(hijo));
				}
			}
			
			//Exploro LEFT
			hijo.x = actual.x-1;
			hijo.y = actual.y;
			//Si esa posici�n en el grid est� vac�a, es que es suelo. Se puede avanzar
			if( (state.getObservationGrid()[hijo.x][hijo.y].isEmpty())
			//Si las cordenadas son las mismas que las del objetivo, est� la meta. Se puede avanzar			
			|| ( (hijo.x == objetivo.x) && (hijo.y == objetivo.y) )	) {
				//Si no est� visitado, a�ado a�ado el nodo a la cola, con los atributos correspondientes.
				if (!visitados[hijo.x][hijo.y]) {
					visitados[hijo.x][hijo.y] = true;
					maxMem++;
					hijo.padre = actual;
					hijo.accion = ACTIONS.ACTION_LEFT;
					cola.add(new Nodo(hijo));
				}
			}
			
			//Exploro RIGHT
			hijo.x = actual.x+1;
			hijo.y = actual.y;
			
			//Si esa posici�n en el grid est� vac�a, es que es suelo. Se puede avanzar
			if( (state.getObservationGrid()[hijo.x][hijo.y].isEmpty())
			//Si las cordenadas son las mismas que las del objetivo, est� la meta. Se puede avanzar			
			|| ( (hijo.x == objetivo.x) && (hijo.y == objetivo.y) )	) {
				//Si no est� visitado, a�ado a�ado el nodo a la cola, con los atributos correspondientes.
				if (!visitados[hijo.x][hijo.y]) {
					visitados[hijo.x][hijo.y] = true;
					maxMem++;
					hijo.padre = actual;
					hijo.accion = ACTIONS.ACTION_RIGHT;
					cola.add(new Nodo(hijo));
				}
			}
		}
		return inicial;
		
	}
}
