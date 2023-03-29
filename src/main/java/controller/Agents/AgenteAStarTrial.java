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

public class AgenteAStarTrial extends AbstractPlayer{
	
	Vector2d fescala;
	Nodo portal;
	boolean hayPlan;
	ArrayList <ACTIONS> plan;
	int [][] g; //Mapa que guardar� el menor valor de 'g' (coste desde el inicio hasta llegar a �l) encontrado.
	int nExpandidos; //N�mero de nodos que han sido expandidos(se ha comprobado si es objetivo)
	int maxMem; //N�mero m�ximo de nodos almacenados en memoria
	int tamRuta; //N�mero de nodos transitados por el agente
	double runTime; //Tiempo, en milisegundos, usado para calcular el plan
	
	/**
	 * initialize all variables for the agent
	 * @param stateObs Observation of the current state.
	 * @param elapsedTimer Timer when the action returned is due.
	 */
	public AgenteAStarTrial(StateObservation state, ElapsedCpuTimer timer) {
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
		
		//Creo el mapa de g's. Inicializado todo a 0. (Un coste de 0 significar� nodo no explorado)
		g = new int[state.getObservationGrid().length][state.getObservationGrid()[0].length];

	}
	
	
	
	/**
	 * Sigue un camino generado por A*
	 * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
	 * @return la pr�xima acci�n a realizar
	 */
	@Override
	public ACTIONS act(StateObservation state, ElapsedCpuTimer timer) {

		//Acci�n que devolver� el algoritmo
		ACTIONS accion = ACTIONS.ACTION_NIL;
		
		//Si no hay plan, busco uno nuevo
		if(!hayPlan) {
			
			//Genero el nodo inicial y le inicio las variables de heur�stica
			Vector2d pos_avatar = new Vector2d(
					state.getAvatarPosition().x / fescala.x,
					state.getAvatarPosition().y / fescala.y);
			Nodo inicio = new Nodo(state, pos_avatar);
			inicio.g = 1;
			inicio.calcular_h(portal);
			inicio.calcular_f();
	
			//Llamo al algoritmo de b�squeda, que devolver� el �ltimo nodo del camino encontrado
			long tInicio = System.nanoTime();
			Nodo ultimo = AStar(avatar, portal, state);
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
	
	//Algoritmo AStar
	public Nodo AStar(Nodo inicial, Nodo objetivo, StateObservation state) {
		//Cola donde se guardar�n, por orden de prioridad, los nodos a explorar
		PriorityQueue<Nodo> abiertos = new PriorityQueue<Nodo>(NodeComparator); 

		//Lista donde se guardar�n los nodos explorados
		ArrayList<Nodo> cerrados = new ArrayList<Nodo>();
		Nodo actual; //Nodo que se est� explorando
		Nodo hijo = new Nodo(); //Nodo en el que se ir� almacenando el hijo del actual
	
		//Cuando a�ado un nodo a la cola, ya est� visitado. Adem�s, aumento el n�mero de nodos en memoria.
		//Aunque el coste de ir al nodo inicial te�ricamente es 0, lo pongo como 1 y dejo el 0 reservado para indicar que ese nodo no est� explorado
		g[inicial.x][inicial.y] = 1;	
		abiertos.add(inicial);
		maxMem++;
		

		while (true){
			
			actual = abiertos.poll();
			
			nExpandidos++;
			if (actual.x == objetivo.x && actual.y == objetivo.y) return actual;
			
			//Como el coste es constante, se puede sencillamente a�adir uno de coste cada vez que se genere un hijo
			hijo.g = actual.g + 1;
			
			//Exploro UP
			hijo.x = actual.x;
			hijo.y = actual.y-1;

			//Si esa posici�n en el grid est� vac�a, es que es suelo. Se puede avanzar
			if( (state.getObservationGrid()[hijo.x][hijo.y].isEmpty())
			//Si las cordenadas son las mismas que las del objetivo, est� la meta. Se puede avanzar			
			|| ( (hijo.x == objetivo.x) && (hijo.y == objetivo.y) )	) {
				//Si no est� visitado, a�ado a�ado el nodo a la cola, con los atributos correspondientes.
				if (g[hijo.x][hijo.y] == 0) {
					g[hijo.x][hijo.y] = hijo.g; //Guardo su coste en la matriz de costes m�nimos
					
					hijo.padre = actual;
					hijo.accion = ACTIONS.ACTION_UP;
					abiertos.add(new Nodo(hijo, objetivo));
					maxMem++;
				}
				
				//Si su coste es mejor que el m�nimo, se quita el nodo de la cola en la que estaba y se a�ade a abiertos con el nuevo coste
				//Adem�s, se guarda el nuevo coste en la matriz de m�nimos
				else if(hijo.g < g[hijo.x][hijo.y]) {
					g[hijo.x][hijo.y] = hijo.g;
					if(abiertos.contains(hijo)) {
						abiertos.remove(hijo);
						abiertos.add(new Nodo(hijo, objetivo));
					}
					if(cerrados.contains(hijo)) {
						cerrados.remove(hijo);
						abiertos.add(new Nodo(hijo, objetivo));	
					}
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
				if (g[hijo.x][hijo.y] == 0){
					g[hijo.x][hijo.y] = hijo.g; //Guardo su coste en la matriz de costes m�nimos

					hijo.padre = actual;
					hijo.accion = ACTIONS.ACTION_DOWN;
					abiertos.add(new Nodo(hijo, objetivo));
					maxMem++;

				}
				
				//Si su coste es mejor que el m�nimo, se quita el nodo de la cola en la que estaba y se a�ade a abiertos con el nuevo coste
				//Adem�s, se guarda el nuevo coste en la matriz de m�nimos
				else if(hijo.g < g[hijo.x][hijo.y]) {
					g[hijo.x][hijo.y] = hijo.g;
					if(abiertos.contains(hijo)) {
						abiertos.remove(hijo);
						abiertos.add(new Nodo(hijo, objetivo));
					}
					if(cerrados.contains(hijo)) {
						cerrados.remove(hijo);
						abiertos.add(new Nodo(hijo, objetivo));	
					}
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
				if (g[hijo.x][hijo.y] == 0) {
					g[hijo.x][hijo.y] = hijo.g; //Guardo su coste en la matriz de costes m�nimos

					hijo.padre = actual;
					hijo.accion = ACTIONS.ACTION_LEFT;
					abiertos.add(new Nodo(hijo, objetivo));
					maxMem++;
				}
				
				//Si su coste es mejor que el m�nimo, se quita el nodo de la cola en la que estaba y se a�ade a abiertos con el nuevo coste
				//Adem�s, se guarda el nuevo coste en la matriz de m�nimos
				else if(hijo.g < g[hijo.x][hijo.y]) {
					g[hijo.x][hijo.y] = hijo.g;
					if(abiertos.contains(hijo)) {
						abiertos.remove(hijo);
						abiertos.add(new Nodo(hijo, objetivo));
					}
					if(cerrados.contains(hijo)) {
						cerrados.remove(hijo);
						abiertos.add(new Nodo(hijo, objetivo));	
					}
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
				if (g[hijo.x][hijo.y] == 0) {
					g[hijo.x][hijo.y] = hijo.g; //Guardo su coste en la matriz de costes m�nimos

					hijo.padre = actual;
					hijo.accion = ACTIONS.ACTION_RIGHT;
					abiertos.add(new Nodo(hijo, objetivo));
					maxMem++;
				}
				
				//Si su coste es mejor que el m�nimo, se quita el nodo de la cola en la que estaba y se a�ade a abiertos con el nuevo coste
				//Adem�s, se guarda el nuevo coste en la matriz de m�nimos
				else if(hijo.g < g[hijo.x][hijo.y]) {
					g[hijo.x][hijo.y] = hijo.g;
					if(abiertos.contains(hijo)) {
						abiertos.remove(hijo);
						abiertos.add(new Nodo(hijo, objetivo));
					}
					if(cerrados.contains(hijo)) {
						cerrados.remove(hijo);
						abiertos.add(new Nodo(hijo, objetivo));	
					}
				}
			}
			
			//Como el nodo ya ha sido explorado, se a�ade a cerrados
			cerrados.add(actual);
		}
	}
	
	//Comparador que se usa para el orden en la cola de prioridad
	//Primero se tiene en cuenta el valor de f, luego el de g y por �ltimo la acci�n a realizar
	Comparator<Nodo> NodeComparator = new Comparator<Nodo>() {
		public int compare(Nodo n1, Nodo n2) {
			int diff = n1.f - n2.f;
			if( diff > 0) return 1;
			else if (diff < 0) return -1;
			else {
				diff = n1.g - n2.g;
				if (diff > 0) return 1;
				else if (diff < 0) return -1;
				else {
					diff = n1.valorAccion() - n2.valorAccion();
					if(diff > 0) return 1;
					else if(diff < 0) return -1;
					else return 0;
				}
			}
		}
	};
}
