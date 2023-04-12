package controller.Agents.trash;


import java.util.ArrayList;
import java.util.Comparator;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import core.player.AbstractPlayer;

public class AgenteIDAStar extends AbstractPlayer{
	
	Vector2d fescala;
	Nodo portal;
	boolean hayPlan;
	ArrayList <ACTIONS> plan;
	int nExpandidos; //N�mero de nodos que han sido expandidos(se ha comprobado si es objetivo)
	int maxMem; //N�mero m�ximo de nodos almacenados en memoria
	int tamRuta; //N�mero de nodos transitados por el agente
	double runTime; //Tiempo, en milisegundos, usado para calcular el plan
	
	/**
	 * initialize all variables for the agent
	 * @param stateObs Observation of the current state.
	 * @param elapsedTimer Timer when the action returned is due.
	 */
	public AgenteIDAStar(StateObservation state, ElapsedCpuTimer timer) {
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
		
	}
	
	
	
	/**
	 * Sigue un camino generado por IDAStar
	 * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
	 * @return 	la pr�xima acci�n a realizar
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
			Nodo ultimo = IDAStar(avatar, portal, state);
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
	
	//Algoritmo IDAStar
	public Nodo IDAStar(Nodo inicial, Nodo objetivo, StateObservation state) {
		
		//Inicializo los atributos del nodo inicial
		inicial.calcular_h(objetivo);
		inicial.calcular_f();
		
		//La cota inicial ser� la heur�stica del nodo inicial
		int cota = inicial.h;

		//Lista de nodos que componen la ruta a partir de la cual se llama a la b�squeda. Se almacenan los nodos en forma de pila
		//Se inicializa conteniendo solo el nodo inicial
		ArrayList<Nodo> ruta = new ArrayList<Nodo>(); 
		ruta.add(inicial);
		
		int t; //Valor donde se almacena lo que devuelve la b�squeda
		while (true) {
			t = search(ruta, 0, cota, objetivo, state);
			if (t==0) return ruta.get(0); //Si t==0, significa que se ha encontrado el objetivo, 
										//por lo que se devuelve el primer elemento de la pila (que es tambi�n el nodo final)
			else cota = t; //Si no, la nueva cota ser� el devuelto por la b�squeda
		}
	}
	
	//B�squeda en profundidad acotada que hace el algotirmo IDA.
	int search(ArrayList<Nodo> ruta, int g, int cota, Nodo objetivo, StateObservation state) {
		Nodo actual = ruta.get(0);
		int f = g + actual.h;
		if (f > cota) return f; //Si f est� por encima de la cota, no se puede seguir profundizando y se devuelve f
		
		nExpandidos++;
		//Si se ha encontrado el objetivo, se devuelve 0
		if (actual.x == objetivo.x && actual.y == objetivo.y) {
			maxMem = g;
			return 0;
		}
		
		int min = 9999; //M�nima 't' encontrada hasta ahora. Se inicializa a un infinito ideal.
						//Este valor ser� el que devuelva la b�squeda a su nivel superior, indicando por qu� profundidad se ha quedado
	
		//Lista de sucesores del nodo actual
		ArrayList<Nodo> sucesores = new ArrayList<Nodo>();
		Nodo hijo = new Nodo(); //Nodo en el que se ir� almacenando el hijo del actual

		//Exploro UP
		hijo.x = actual.x;
		hijo.y = actual.y-1;
		//Si esa posici�n en el grid est� vac�a, es que es suelo. Se puede avanzar
		if( (state.getObservationGrid()[hijo.x][hijo.y].isEmpty())
		//Si las cordenadas son las mismas que las del objetivo, est� la meta. Se puede avanzar			
		|| ( (hijo.x == objetivo.x) && (hijo.y == objetivo.y) )	) {
			//Fijo los atributos y los a�ado a la lista de sucesores
			hijo.padre = actual;
			hijo.accion = ACTIONS.ACTION_UP;
			sucesores.add(new Nodo(hijo, objetivo));
		}
		
		//Exploro DOWN
		hijo.x = actual.x;
		hijo.y = actual.y+1;
		//Si esa posici�n en el grid est� vac�a, es que es suelo. Se puede avanzar
		if( (state.getObservationGrid()[hijo.x][hijo.y].isEmpty())
		//Si las cordenadas son las mismas que las del objetivo, est� la meta. Se puede avanzar			
		|| ( (hijo.x == objetivo.x) && (hijo.y == objetivo.y) )	) {
			//Fijo los atributos y los a�ado a la lista de sucesores
			hijo.padre = actual;
			hijo.accion = ACTIONS.ACTION_DOWN;
			sucesores.add(new Nodo(hijo, objetivo));
		}
		
		//Exploro LEFT
		hijo.x = actual.x-1;
		hijo.y = actual.y;
		//Si esa posici�n en el grid est� vac�a, es que es suelo. Se puede avanzar
		if( (state.getObservationGrid()[hijo.x][hijo.y].isEmpty())
		//Si las cordenadas son las mismas que las del objetivo, est� la meta. Se puede avanzar			
		|| ( (hijo.x == objetivo.x) && (hijo.y == objetivo.y) )	) {
			//Fijo los atributos y los a�ado a la lista de sucesores
			hijo.padre = actual;
			hijo.accion = ACTIONS.ACTION_LEFT;
			sucesores.add(new Nodo(hijo, objetivo));
		}
		
		//Exploro RIGHT
		hijo.x = actual.x+1;
		hijo.y = actual.y;
		//Si esa posici�n en el grid est� vac�a, es que es suelo. Se puede avanzar
		if( (state.getObservationGrid()[hijo.x][hijo.y].isEmpty())
		//Si las cordenadas son las mismas que las del objetivo, est� la meta. Se puede avanzar			
		|| ( (hijo.x == objetivo.x) && (hijo.y == objetivo.y) )	) {
			//Fijo los atributos y los a�ado a la lista de sucesores
			hijo.padre = actual;
			hijo.accion = ACTIONS.ACTION_RIGHT;
			sucesores.add(new Nodo(hijo, objetivo));
		}
		
		sucesores.sort(NodeComparator); //Ordeno los sucesores en funci�n de su heur�stica
		
		//Hago una b�squeda en profundidad acotada para cada sucesor (descarto los que ya estuvieran en la ruta por redundantes)
		while(!sucesores.isEmpty()){
			hijo = sucesores.remove(0);
			if(!ruta.contains(hijo)) {
				ruta.add(0,hijo);
				int t = search(ruta, g+1, cota, objetivo, state);
				if (t == 0) return 0;
				if (t < min) min = t; //Voy actualizando el valor del menor 't' encontrado
				ruta.remove(0);
			}
		}
		
		return min;
		
	}
	
	//Comparador que se usa para ordenar la lista de sucesores.
	//Primero se tiene en cuenta la heur�stica y luego el valor de la acci�n a realizar
	Comparator<Nodo> NodeComparator = new Comparator<Nodo>() {
		public int compare(Nodo n1, Nodo n2) {
			int diff = n1.h - n2.h;
			if( diff > 0) return 1;
			else if (diff < 0) return -1;
			else {
				diff = n1.valorAccion() - n2.valorAccion();
				if(diff > 0) return 1;
				else if (diff < 0) return -1;
				else return 0;
			}
		}
	};
}