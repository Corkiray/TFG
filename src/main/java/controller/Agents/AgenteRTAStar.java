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
	int [][] h; //Mapa de booleanos, que indicar� la heur�stica que tiene en ese momento la posici�n (x,y)
	int nExpandidos; //N�mero de nodos que han sido expandidos(se ha comprobado si es objetivo)
	int maxMem; //N�mero m�ximo de nodos almacenados en memoria
	int tamRuta; //N�mero de nodos transitados por el agente
	double runTime; //Tiempo, en milisegundos, usado para calcular el plan
	
	/**
	 * initialize all variables for the agent
	 * @param stateObs Observation of the current state.
	 * @param elapsedTimer Timer when the action returned is due.
	 */
	public AgenteRTAStar(StateObservation state, ElapsedCpuTimer timer) {
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
		
		//Creo el mapa de h's. Inicializado todo a 0. (Un coste de 0 significar� nodo no explorado)
		h = new int[state.getObservationGrid().length][state.getObservationGrid()[0].length];

	}
	
	
	
	/**
	 * Sigue un camino generado por RTAStar
	 * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
	 * @return 	la acci�n a realizar en esta iteraci�n
	 */
	@Override
	public ACTIONS act(StateObservation state, ElapsedCpuTimer timer) {
		//Acci�n que devolver� el algoritmo
		ACTIONS accion = ACTIONS.ACTION_NIL;
	
		//Genero el nodo inicial y le inicio las variables de heur�stica
		Vector2d pos_avatar = new Vector2d(
			state.getAvatarPosition().x / fescala.x,
			state.getAvatarPosition().y / fescala.y);	
		Nodo avatar = new Nodo(pos_avatar);
		avatar.g = 1;
		avatar.calcular_h(portal);
		avatar.calcular_f();

		//Llamo al algoritmo de b�squeda, que devolver� la acci�n a realizar
		long tInicio = System.nanoTime();
		accion = RTAStar(avatar, portal, state);
		//Como se llama m�ltiples veces al algoritmo, y el Runtime es acumulado, voy sum�ndolos
		runTime += (System.nanoTime()-tInicio); 
		
		tamRuta++;
		
		//Compruebo si esta acci�n va a hacer llegar al portal. En ese caso, imprimo antes los datos de la planificaci�n
		pos_avatar = modificarPos(pos_avatar, accion);		
		if(pos_avatar.x == portal.x && pos_avatar.y == portal.y){
			System.out.print(" Runtime(ms): " + runTime/1000000.0 + 
					",\n Tama�o de la ruta calculada: " + tamRuta +
					",\n N�mero de nodos expandidos: " + nExpandidos +
					",\n M�ximo n�mero de nodos en memoria: " + maxMem +
					"\n");				
		}

		return accion;
	}
	
	//Algoritmo RTAstar
	public ACTIONS RTAStar(Nodo actual, Nodo objetivo, StateObservation state) {
		nExpandidos++; //Cada vez que llamo al algoritmo, expando el nodo en el que est� el avatar.
		
		//Si un nodo no est� explorado, inicializo la heur�stica actual por la generada matem�ticamente.
		if(h[actual.x][actual.y] == 0) {
			maxMem++;
			h[actual.x][actual.y] = actual.h;
		}
		
		//Cola de sucesores, que se ordenar�n por prioridad
		PriorityQueue<Nodo> sucesores = new PriorityQueue<Nodo>(NodeComparator);
		Nodo hijo = new Nodo(); //Nodo en el que se ir� almacenando el hijo del actual

		//Exploro UP
		hijo.x = actual.x;
		hijo.y = actual.y-1;
		//Si esa posici�n en el grid est� vac�a, es que es suelo. Se puede avanzar
		if( (state.getObservationGrid()[hijo.x][hijo.y].isEmpty())
		//Si las cordenadas son las mismas que las del objetivo, est� la meta. Se puede avanzar			
		|| ( (hijo.x == objetivo.x) && (hijo.y == objetivo.y) )	) {
			//Si no est� visitado le calculo la heur�stica
			//Adem�s, guardo dicha heur�sitcas en la matriz de heur�sticas del algoritmo
			if(h[hijo.x][hijo.y] == 0 ) {
				maxMem++;
				h[hijo.x][hijo.y] = hijo.calcular_h(objetivo);
			}
			//Si ya est� visitado,le establezco la heur�stica como la que tiene el algoritmo guardada para esa posici�n
			else hijo.h = h[hijo.x][hijo.y];
			
			//En cualquier caso, se a�ade a la cola de sucesores
			hijo.accion = ACTIONS.ACTION_UP;
			sucesores.add(new Nodo(hijo));
		}
		
		//Exploro DOWN
		hijo.x = actual.x;
		hijo.y = actual.y+1;
		//Si esa posici�n en el grid est� vac�a, es que es suelo. Se puede avanzar
		if( (state.getObservationGrid()[hijo.x][hijo.y].isEmpty())
		//Si las cordenadas son las mismas que las del objetivo, est� la meta. Se puede avanzar			
		|| ( (hijo.x == objetivo.x) && (hijo.y == objetivo.y) )	) {
			//Si no est� visitado le calculo la heur�stica
			//Adem�s, guardo dicha heur�sitcas en la matriz de heur�sticas del algoritmo
			if(h[hijo.x][hijo.y] == 0 ) {
				maxMem++;
				h[hijo.x][hijo.y] = hijo.calcular_h(objetivo);
			}
			//Si ya est� visitado,le establezco la heur�stica como la que tiene el algoritmo guardada para esa posici�n
			else hijo.h = h[hijo.x][hijo.y];
			
			//En cualquier caso, se a�ade a la cola de sucesores
			hijo.accion = ACTIONS.ACTION_DOWN;
			sucesores.add(new Nodo(hijo));
		}
		
		//Exploro LEFT
		hijo.x = actual.x-1;
		hijo.y = actual.y;
		//Si esa posici�n en el grid est� vac�a, es que es suelo. Se puede avanzar
		if( (state.getObservationGrid()[hijo.x][hijo.y].isEmpty())
		//Si las cordenadas son las mismas que las del objetivo, est� la meta. Se puede avanzar			
		|| ( (hijo.x == objetivo.x) && (hijo.y == objetivo.y) )	) {
			//Si no est� visitado le calculo la heur�stica
			//Adem�s, guardo dicha heur�sitcas en la matriz de heur�sticas del algoritmo
			if(h[hijo.x][hijo.y] == 0 ) {
				maxMem++;
				h[hijo.x][hijo.y] = hijo.calcular_h(objetivo);
			}
			//Si ya est� visitado,le establezco la heur�stica como la que tiene el algoritmo guardada para esa posici�n
			else hijo.h = h[hijo.x][hijo.y];
			
			//En cualquier caso, se a�ade a la cola de sucesores
			hijo.accion = ACTIONS.ACTION_LEFT;
			sucesores.add(new Nodo(hijo));
		}
		
		//Exploro RIGHT
		hijo.x = actual.x+1;
		hijo.y = actual.y;
		//Si esa posici�n en el grid est� vac�a, es que es suelo. Se puede avanzar
		if( (state.getObservationGrid()[hijo.x][hijo.y].isEmpty())
		//Si las cordenadas son las mismas que las del objetivo, est� la meta. Se puede avanzar			
		|| ( (hijo.x == objetivo.x) && (hijo.y == objetivo.y) )	) {
			//Si no est� visitado le calculo la heur�stica
			//Adem�s, guardo dicha heur�sitcas en la matriz de heur�sticas del algoritmo
			if(h[hijo.x][hijo.y] == 0 ) {
				maxMem++;
				h[hijo.x][hijo.y] = hijo.calcular_h(objetivo);
			}
			//Si ya est� visitado,le establezco la heur�stica como la que tiene el algoritmo guardada para esa posici�n
			else hijo.h = h[hijo.x][hijo.y];
			
			//En cualquier caso, se a�ade a la cola de sucesores
			hijo.accion = ACTIONS.ACTION_RIGHT;
			sucesores.add(new Nodo(hijo));
		}
		
		//Extraigo el hijo que tenga menor coste (seg�n la heur�stica) y guardo su acci�n y su valor h
		hijo = sucesores.poll();	
		ACTIONS accion = hijo.accion;
		int newh = hijo.h + 1; //La nueva heur�stica es la del hijo + el coste de desplazamiento que, como es constante, ser� 1
		
		//Como este algoritmo usa el segundo menor para actualizar la heur�stica, vuelvo a extraer un hijo. 
		hijo = sucesores.poll();
		
		//Solo si hay un segundo hijo en la cola de sucesores, el valor de la nueva h
		if(hijo != null) newh = hijo.h + 1; 
		
		//Si la nueva heur�stica es mejor que la que ten�a almacenado el algoritmo, se actualiza
		if(h[actual.x][actual.y] < newh) h[actual.x][actual.y] = newh;
		
		return accion;		
	}
	
	//Comparador que se usa para el orden en la cola de prioridad
	//Primero se tiene en cuenta el valor de h y luego el de la acci�n
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
	
	
	//Funci�n auxiliar que, dado un vector posici�n y una acci�n, devolver� la nueva posici�n alcanzada al moverse en esa direcci�n
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


