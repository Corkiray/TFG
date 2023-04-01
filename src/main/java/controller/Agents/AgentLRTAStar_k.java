package controller.Agents;


import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Comparator;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class AgentLRTAStar_k{
	
	public static ACTIONS[] PosibleActions = new ACTIONS[] {ACTIONS.ACTION_UP, ACTIONS.ACTION_LEFT, 
			ACTIONS.ACTION_DOWN, ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_USE};

	boolean hayPlan;
	ArrayList <ACTIONS> plan;
	
	ArrayList<Node> explorados;
	
	int k;
	
	int nExpandidos; //N�mero de nodos que han sido expandidos(se ha comprobado si es objetivo)
	int maxMem; //N�mero m�ximo de nodos almacenados en memoria
	int tamRuta; //N�mero de nodos transitados por el agente
	double runTime; //Tiempo, en milisegundos, usado para calcular el plan
	
	/**
	 * initialize all variables for the agent
	 * @param stateObs Observation of the current state.
	 * @param elapsedTimer Timer when the action returned is due.
	 */
	public AgentLRTAStar_k(int k) {		
		this.k = k;
		
		//Inicializo lista de nodos
		explorados = new ArrayList<Node>();
		
		//Inicializo el plan a vac�o
		hayPlan = false;
		plan = new ArrayList<ACTIONS>();
		
		//Inicializo los resultados a 0
		nExpandidos = 0;
		maxMem = 0;
		tamRuta = 0;
		
	}
	
	public void setObjetive(ArrayList<String> objetive, StateObservation state) {
		//Calculo el factor de escala p�xeles -> grid)
		Node.fescala = new Vector2d(
				state.getWorldDimension().width / state.getObservationGrid().length,
				state.getWorldDimension().height / state.getObservationGrid()[0].length );

		Node.setObjetive(objetive, state);
		explorados.clear();
	}
	
	/**
	 * Sigue un camino generado por RTAStar
	 * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
	 * @return 	la acci�n a realizar en esta iteraci�n
	 */
	public Node act(StateObservation state, ElapsedCpuTimer timer) {
		//Genero el nodo inicial y le inicio las variables de heur�stica
		Node root = new Node(state);
		root.g = 0;
		root.calcular_h();
		root.calcular_f();
		root.inPath=true;

		//System.out.print("\nPosici�n del jugador: [" + root.x+ " " + root.y + "]\n"
		//		+ "Posici�n del objetivo: [" + Node.target_x+ " " +Node.target_y + "]\n");


		//Llamo al algoritmo de b�squeda, que devolver� la acci�n a realizar
		long tInicio = System.nanoTime();
		Node child = LRTAStar(root, state);
		//Como se llama m�ltiples veces al algoritmo, y el Runtime es acumulado, voy sum�ndolos
		runTime += (System.nanoTime()-tInicio); 
		
		tamRuta++;
		
		//Compruebo si esta acci�n va a hacer llegar al portal. En ese caso, imprimo antes los datos de la planificaci�n
		if(child.h == 0){
			System.out.print(" Runtime(ms): " + runTime/1000000.0 + 
					",\n Tama�o de la ruta calculada: " + tamRuta +
					",\n N�mero de nodos expandidos: " + nExpandidos +
					",\n M�ximo n�mero de nodos en memoria: " + maxMem +
					"\n");				
		}

		return child;
	}
	
	//Algoritmo
	public Node LRTAStar(Node actual, StateObservation state) {
		PriorityQueue<Node> sucesores = new PriorityQueue<Node>(Node.NodeComparator);

		nExpandidos++; //Cada vez que llamo al algoritmo, expando el nodo en el que est� el avatar.


		System.out.print("NODO PADRE: " + actual);

		boolean found = false;
		for(Node node : explorados) {
			if(node.equals(actual)) {
				explorados.remove(node);
				node.inPath = true;
				explorados.add(node);
				found=true;
				break;
			}
		}
		if(!found)
			explorados.add(actual);
		
		lookaheadUpdateK(actual);

		for (ACTIONS action : PosibleActions) {
			Node child = new Node(actual);
			child = child.advance(action);
			child.accion = action;
			
			if(!child.equals(actual)) {
				for(Node node : explorados) {
					if(node.equals(child)) {
						child.h = node.h;
						sucesores.add(child);
						System.out.print(child);
					}
				}			
			}
		}
		
		Node best = sucesores.poll();

		return best;
	}

	
	void lookaheadUpdateK(Node root) {
		int cont = k-1;
		ArrayList<Node> candidates = new ArrayList<Node>();
		candidates.add(root);
		PriorityQueue<Node> sucesores = new PriorityQueue<Node>(Node.NodeComparator);
		
		while(!candidates.isEmpty()) {
			sucesores.clear();
			Node actual = candidates.get(0);
			candidates.remove(0);

			//LookaheadUpdate1
			boolean updated = false;
			for (ACTIONS action : PosibleActions){
				Node child = new Node(actual);
				child = child.advance(action);
				child.padre = actual;
				child.accion = action;

				boolean found=false;
				for(Node node :  explorados) {
					if(node.equals(child)) {
						child = node;
						
						found=true;
						break;
					}
				}
				
				if(!found) {
					child.calcular_h();
					explorados.add(child);
				}
				
				if(!child.equals(actual))
					sucesores.add(child);
			}
			
			Node best = sucesores.peek();
			
			actual.h = best.h + 1;
			actual.support = best;
			
			for(Node node : explorados) {
				if(node.equals(actual)) {
					if(node.h > actual.h) {
						actual.h=node.h;
					}
					explorados.remove(node);
					explorados.add(actual);

					updated = true;
					break;
				}
			}
			
			if(updated && cont > 0) {
				if(cont<49) System.out.print("CONT " + Integer.toString(cont) +"\n");
				for (Node child : sucesores) {		
					if(cont > 0 && child.inPath && actual.equals(child.support)) {
						candidates.add(child);
						cont--;
					}
				}
			}
		}
	}

	
	
}


