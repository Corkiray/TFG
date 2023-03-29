package controller.Agents;


import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Comparator;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import core.player.AbstractPlayer;

public class AgenteRTAStar extends AbstractPlayer{
	
	Vector2d fescala;
	Nodo portal;
	boolean hayPlan;
	ArrayList <ACTIONS> plan;
	int [][] h; //Mapa de booleanos, que indicará la heurística que tiene en ese momento la posición (x,y)
	int nExpandidos; //Número de nodos que han sido expandidos(se ha comprobado si es objetivo)
	int maxMem; //Número máximo de nodos almacenados en memoria
	int tamRuta; //Número de nodos transitados por el agente
	double runTime; //Tiempo, en milisegundos, usado para calcular el plan
	
	/**
	 * initialize all variables for the agent
	 * @param stateObs Observation of the current state.
	 * @param elapsedTimer Timer when the action returned is due.
	 */
	public AgenteRTAStar(StateObservation state, ElapsedCpuTimer timer) {
		//Calculo el factor de escala píxeles -> grid)
		fescala = new Vector2d(
				state.getWorldDimension().width / state.getObservationGrid().length,
				state.getWorldDimension().height / state.getObservationGrid()[0].length );
		
		//Creo la lista de portales
		ArrayList<Observation>[] posiciones = state.getPortalsPositions(state.getAvatarPosition());
		//Selecciono el más próximo
		portal = new Nodo();
		portal.x = (int) (Math.floor(posiciones[0].get(0).position.x / fescala.x));
		portal.y = (int) (Math.floor(posiciones[0].get(0).position.y / fescala.y));
		
		//Inicializo el plan a vacío
		hayPlan = false;
		plan = new ArrayList<ACTIONS>();
		
		//Inicializo los resultados a 0
		nExpandidos = 0;
		maxMem = 0;
		tamRuta = 0;
		
		//Creo el mapa de h's. Inicializado todo a 0. (Un coste de 0 significará nodo no explorado)
		h = new int[state.getObservationGrid().length][state.getObservationGrid()[0].length];

	}
	
	
	
	/**
	 * Sigue un camino generado por RTAStar
	 * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
	 * @return 	la acción a realizar en esta iteración
	 */
	@Override
	public ACTIONS act(StateObservation state, ElapsedCpuTimer timer) {
		//Acción que devolverá el algoritmo
		ACTIONS accion = ACTIONS.ACTION_NIL;
	
		//Genero el nodo inicial y le inicio las variables de heurística
		Vector2d pos_avatar = new Vector2d(
			state.getAvatarPosition().x / fescala.x,
			state.getAvatarPosition().y / fescala.y);	
		Nodo avatar = new Nodo(pos_avatar);
		avatar.g = 1;
		avatar.calcular_h(portal);
		avatar.calcular_f();

		//Llamo al algoritmo de búsqueda, que devolverá la acción a realizar
		long tInicio = System.nanoTime();
		accion = RTAStar(avatar, portal, state);
		//Como se llama múltiples veces al algoritmo, y el Runtime es acumulado, voy sumándolos
		runTime += (System.nanoTime()-tInicio); 
		
		tamRuta++;
		
		//Compruebo si esta acción va a hacer llegar al portal. En ese caso, imprimo antes los datos de la planificación
		pos_avatar = modificarPos(pos_avatar, accion);		
		if(pos_avatar.x == portal.x && pos_avatar.y == portal.y){
			System.out.print(" Runtime(ms): " + runTime/1000000.0 + 
					",\n Tamaño de la ruta calculada: " + tamRuta +
					",\n Número de nodos expandidos: " + nExpandidos +
					",\n Máximo número de nodos en memoria: " + maxMem +
					"\n");				
		}

		return accion;
	}
	
	//Algoritmo RTAstar
	public ACTIONS RTAStar(Nodo actual, Nodo objetivo, StateObservation state) {
		nExpandidos++; //Cada vez que llamo al algoritmo, expando el nodo en el que está el avatar.
		
		//Si un nodo no está explorado, inicializo la heurística actual por la generada matemáticamente.
		if(h[actual.x][actual.y] == 0) {
			maxMem++;
			h[actual.x][actual.y] = actual.h;
		}
		
		//Cola de sucesores, que se ordenarán por prioridad
		PriorityQueue<Nodo> sucesores = new PriorityQueue<Nodo>(NodeComparator);
		Nodo hijo = new Nodo(); //Nodo en el que se irá almacenando el hijo del actual

		//Exploro UP
		hijo.x = actual.x;
		hijo.y = actual.y-1;
		//Si esa posición en el grid está vacía, es que es suelo. Se puede avanzar
		if( (state.getObservationGrid()[hijo.x][hijo.y].isEmpty())
		//Si las cordenadas son las mismas que las del objetivo, está la meta. Se puede avanzar			
		|| ( (hijo.x == objetivo.x) && (hijo.y == objetivo.y) )	) {
			//Si no está visitado le calculo la heurística
			//Además, guardo dicha heurísitcas en la matriz de heurísticas del algoritmo
			if(h[hijo.x][hijo.y] == 0 ) {
				maxMem++;
				h[hijo.x][hijo.y] = hijo.calcular_h(objetivo);
			}
			//Si ya está visitado,le establezco la heurística como la que tiene el algoritmo guardada para esa posición
			else hijo.h = h[hijo.x][hijo.y];
			
			//En cualquier caso, se añade a la cola de sucesores
			hijo.accion = ACTIONS.ACTION_UP;
			sucesores.add(new Nodo(hijo));
		}
		
		//Exploro DOWN
		hijo.x = actual.x;
		hijo.y = actual.y+1;
		//Si esa posición en el grid está vacía, es que es suelo. Se puede avanzar
		if( (state.getObservationGrid()[hijo.x][hijo.y].isEmpty())
		//Si las cordenadas son las mismas que las del objetivo, está la meta. Se puede avanzar			
		|| ( (hijo.x == objetivo.x) && (hijo.y == objetivo.y) )	) {
			//Si no está visitado le calculo la heurística
			//Además, guardo dicha heurísitcas en la matriz de heurísticas del algoritmo
			if(h[hijo.x][hijo.y] == 0 ) {
				maxMem++;
				h[hijo.x][hijo.y] = hijo.calcular_h(objetivo);
			}
			//Si ya está visitado,le establezco la heurística como la que tiene el algoritmo guardada para esa posición
			else hijo.h = h[hijo.x][hijo.y];
			
			//En cualquier caso, se añade a la cola de sucesores
			hijo.accion = ACTIONS.ACTION_DOWN;
			sucesores.add(new Nodo(hijo));
		}
		
		//Exploro LEFT
		hijo.x = actual.x-1;
		hijo.y = actual.y;
		//Si esa posición en el grid está vacía, es que es suelo. Se puede avanzar
		if( (state.getObservationGrid()[hijo.x][hijo.y].isEmpty())
		//Si las cordenadas son las mismas que las del objetivo, está la meta. Se puede avanzar			
		|| ( (hijo.x == objetivo.x) && (hijo.y == objetivo.y) )	) {
			//Si no está visitado le calculo la heurística
			//Además, guardo dicha heurísitcas en la matriz de heurísticas del algoritmo
			if(h[hijo.x][hijo.y] == 0 ) {
				maxMem++;
				h[hijo.x][hijo.y] = hijo.calcular_h(objetivo);
			}
			//Si ya está visitado,le establezco la heurística como la que tiene el algoritmo guardada para esa posición
			else hijo.h = h[hijo.x][hijo.y];
			
			//En cualquier caso, se añade a la cola de sucesores
			hijo.accion = ACTIONS.ACTION_LEFT;
			sucesores.add(new Nodo(hijo));
		}
		
		//Exploro RIGHT
		hijo.x = actual.x+1;
		hijo.y = actual.y;
		//Si esa posición en el grid está vacía, es que es suelo. Se puede avanzar
		if( (state.getObservationGrid()[hijo.x][hijo.y].isEmpty())
		//Si las cordenadas son las mismas que las del objetivo, está la meta. Se puede avanzar			
		|| ( (hijo.x == objetivo.x) && (hijo.y == objetivo.y) )	) {
			//Si no está visitado le calculo la heurística
			//Además, guardo dicha heurísitcas en la matriz de heurísticas del algoritmo
			if(h[hijo.x][hijo.y] == 0 ) {
				maxMem++;
				h[hijo.x][hijo.y] = hijo.calcular_h(objetivo);
			}
			//Si ya está visitado,le establezco la heurística como la que tiene el algoritmo guardada para esa posición
			else hijo.h = h[hijo.x][hijo.y];
			
			//En cualquier caso, se añade a la cola de sucesores
			hijo.accion = ACTIONS.ACTION_RIGHT;
			sucesores.add(new Nodo(hijo));
		}
		
		//Extraigo el hijo que tenga menor coste (según la heurística) y guardo su acción y su valor h
		hijo = sucesores.poll();	
		ACTIONS accion = hijo.accion;
		int newh = hijo.h + 1; //La nueva heurística es la del hijo + el coste de desplazamiento que, como es constante, será 1
		
		//Como este algoritmo usa el segundo menor para actualizar la heurística, vuelvo a extraer un hijo. 
		hijo = sucesores.poll();
		
		//Solo si hay un segundo hijo en la cola de sucesores, el valor de la nueva h
		if(hijo != null) newh = hijo.h + 1; 
		
		//Si la nueva heurística es mejor que la que tenía almacenado el algoritmo, se actualiza
		if(h[actual.x][actual.y] < newh) h[actual.x][actual.y] = newh;
		
		return accion;		
	}
	
	//Comparador que se usa para el orden en la cola de prioridad
	//Primero se tiene en cuenta el valor de h y luego el de la acción
	Comparator<Nodo> NodeComparator = new Comparator<Nodo>() {
		public int compare(Nodo n1, Nodo n2) {
			int diff = n1.h - n2.h;
			if (diff > 0) return 1;
			else if (diff < 0) return -1;
			else {
				diff = n1.valorAccion() - n2.valorAccion();
				if(diff > 0) return 1;
				else if (diff < 0) return -1;
				else return 0;
			}
		}
	};
	
	
	//Función auxiliar que, dado un vector posición y una acción, devolverá la nueva posición alcanzada al moverse en esa dirección
	public Vector2d modificarPos(Vector2d avatar, ACTIONS accion) {

		switch(accion) {
		case ACTION_UP:
			avatar.y--; break;
		case ACTION_DOWN:
			avatar.y++; break;
		case ACTION_RIGHT:
			avatar.x++; break;
		case ACTION_LEFT:
			avatar.x--; break;
		default: break;
		}
		

		return avatar;
	}

}


