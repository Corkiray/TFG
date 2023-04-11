package controller.Agents;


import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Comparator;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class AgentLRTAStarK{
	
	public static ACTIONS[] PosibleActions = new ACTIONS[] {ACTIONS.ACTION_UP, ACTIONS.ACTION_LEFT, 
			ACTIONS.ACTION_DOWN, ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_USE};

	boolean hayPlan;
	ArrayList <ACTIONS> plan;
	
	ArrayList<Node> explorados;
	
	int k;
	
	int nExpandidos; //Número de nodos que han sido expandidos(se ha comprobado si es objetivo)
	int maxMem; //Número máximo de nodos almacenados en memoria
	int tamRuta; //Número de nodos transitados por el agente
	double runTime; //Tiempo, en milisegundos, usado para calcular el plan
	
	/**
	 * initialize all variables for the agent
	 * @param stateObs Observation of the current state.
	 * @param elapsedTimer Timer when the action returned is due.
	 */
	public AgentLRTAStarK(int k) {		
		this.k = k;
		
		//Inicializo lista de nodos
		explorados = new ArrayList<Node>();
		
		//Inicializo el plan a vacío
		hayPlan = false;
		plan = new ArrayList<ACTIONS>();
		
		//Inicializo los resultados a 0
		nExpandidos = 0;
		maxMem = 0;
		tamRuta = 0;
		
	}
	
	public void clear() {
		explorados.clear();
	}
	
	/**
	 * Sigue un camino generado por RTAStar
	 * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
	 * @return 	la acción a realizar en esta iteración
	 */
	public Node act(StateObservation state, ElapsedCpuTimer timer) {
		//Genero el nodo inicial y le inicio las variables de heurística
		Node root = new Node(state);
		root.inPath=true;

		//System.out.print("\nPosición del jugador: [" + root.x+ " " + root.y + "]\n"
		//		+ "Posición del objetivo: [" + Node.target_x+ " " +Node.target_y + "]\n");


		//Llamo al algoritmo de búsqueda, que devolverá la acción a realizar
		long tInicio = System.nanoTime();
		Node child = LRTAStar(root);
		//Como se llama múltiples veces al algoritmo, y el Runtime es acumulado, voy sumándolos
		runTime += (System.nanoTime()-tInicio); 
		
		tamRuta++;
		
		//Compruebo si esta acción va a hacer llegar al portal. En ese caso, imprimo antes los datos de la planificación
		if(child.h == 0){
			System.out.print(" Runtime(ms): " + runTime/1000000.0 + 
					",\n Tamaño de la ruta calculada: " + tamRuta +
					",\n Número de nodos expandidos: " + nExpandidos +
					",\n Máximo número de nodos en memoria: " + maxMem +
					"\n");				
		}

		return child;
	}
	
	//Algoritmo
	public Node LRTAStar(Node actual) {
		actual.inPath = true;

		nExpandidos++; //Cada vez que llamo al algoritmo, expando el nodo en el que está el avatar.

		PriorityQueue<Node> sucesores = new PriorityQueue<Node>(Node.H_NodeComparator);

		Node node = actual.getFrom(explorados);
		if(node!=null) {
			actual.h = node.h;
			explorados.remove(node);
		}
		else {
			actual.calcular_h();
		}
		explorados.add(actual);
		
		System.out.print("NODO PADRE: " + actual);

		ArrayList<Node> succ = lookaheadUpdateK(actual);

		for (Node child : succ) {	
			child.h = child.getFrom(explorados).h;
			System.out.print(child);				
			sucesores.add(child);
		}
		
		Node best = sucesores.poll();

		return best;
	}

	
	ArrayList<Node> lookaheadUpdateK(Node root) {
		boolean inRoot = true;
		ArrayList<Node> ret = null;
		
		int cont = k-1;
		ArrayList<Node> candidates = new ArrayList<Node>();
		candidates.add(root);
		
		PriorityQueue<Node> sucesores = new PriorityQueue<Node>(Node.H_NodeComparator);
		
		while(!candidates.isEmpty()) {
			sucesores.clear();
			Node actual = candidates.remove(0);

			boolean updated=false;
			//LookaheadUpdate1
			for (Node child : actual.generate_succ()) {
				Node node = child.getFrom(explorados);
				if(node!=null) {
					child.h = node.h;
					child.inPath = node.inPath;
					child.support = node.support;
				}
				else {
					child.calcular_h();
					explorados.add(child);					
				}
				sucesores.add(child);
			}
			
			if(inRoot) {
				ret = new ArrayList<Node>(sucesores);
				inRoot=false;
			}
			
			Node best = sucesores.peek();
			actual.support = best;
			
			if(actual.h < best.h+1) {
				actual.h = best.h+1;
				updated=true;
			}
			explorados.remove(actual);
			explorados.add(actual);				
			//end LookaheadUpdate1
			
			if(updated) {
				for (Node child : sucesores) {		
					if(cont > 0 && child.inPath && actual.equals(child.support)) {
						System.out.print("CONT " + Integer.toString(cont) +"\n");
						candidates.add(child);
						cont--;
					}
				}
			}
		}
		
		return ret;
	}

	
	
}


