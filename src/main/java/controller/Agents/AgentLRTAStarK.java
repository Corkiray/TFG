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
	
	ArrayList <ACTIONS> plan;
	
	ArrayList<Node> explorados;
	
	private int k;
	
	int nExpandidos; //Número de nodos que han sido expandidos
	int maxMem; //Número máximo de nodos almacenados en memoria
	int numPlans; //Número de veces que se ha planificado
	int tamRuta; //Tamaño del plan calculado
	double runTime;
	double totalRunTime; //Tiempo, en milisegundos, total utilizado
	
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
		plan = new ArrayList<ACTIONS>();
		
		//Inicializo los resultados a 0
		nExpandidos = 0;
		maxMem = 0;
		numPlans = 0;
		tamRuta = 0;
		totalRunTime = 0;
		
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
	public ArrayList<ACTIONS> act(StateObservation state, ElapsedCpuTimer timer) {
		//Genero el nodo inicial y le inicio las variables de heurística
		Node root = new Node(state);
		root.inPath=true;

		//System.out.print("\nPosición del jugador: [" + root.x+ " " + root.y + "]\n"
		//		+ "Posición del objetivo: [" + Node.target_x+ " " +Node.target_y + "]\n");


		if(root.calcular_h() == 0) {
			ArrayList<ACTIONS> ret = new ArrayList<ACTIONS> ();
			ret.add(ACTIONS.ACTION_NIL);
			return ret;
			
		}

		//Llamo al algoritmo de búsqueda, que devolverá la acción a realizar
		long tInicio = System.nanoTime();
		ArrayList<ACTIONS> plan = LRTAStar(root);
		//Como se llama múltiples veces al algoritmo, y el Runtime es acumulado, voy sumándolos
		runTime = (System.nanoTime()-tInicio); 
		tamRuta = plan.size();

		numPlans++;
		totalRunTime += runTime/1000000.0;
		if(explorados.size() > maxMem) maxMem = explorados.size();

		//Imprimo los datos de la planificación
		System.out.print(" Runtime: " + runTime + 
				",\n Runtime total (ms): " + totalRunTime +
				",\n Tamaño de la ruta calculada: " + tamRuta +
				",\n Número de nodos expandidos: " + nExpandidos +
				",\n Máximo número de nodos en memoria: " + maxMem +
				",\n Número de veces que se ha planificado: " + numPlans +
				"\n");
		
		return plan;
	}
	
	//Algoritmo
	public ArrayList<ACTIONS> LRTAStar(Node actual) {
		actual.inPath = true;

		PriorityQueue<Node> sucesores = new PriorityQueue<Node>(Node.RTA_NodeComparator);

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

		ArrayList<ACTIONS> plan = new ArrayList<ACTIONS>();
		
		plan.add(best.accion);
		
		if(!best.orientation.equals(actual.orientation))
			plan.add(best.accion);
			
		return plan;		
	}
	
	
	ArrayList<Node> lookaheadUpdateK(Node root) {
		boolean inRoot = true;
		ArrayList<Node> ret = null;
		
		int cont = k-1;
		ArrayList<Node> candidates = new ArrayList<Node>();
		candidates.add(root);
		
		PriorityQueue<Node> sucesores = new PriorityQueue<Node>(Node.RTA_NodeComparator);
		
		while(!candidates.isEmpty()) {
			sucesores.clear();
			Node actual = candidates.remove(0);

			//LookaheadUpdate1
			nExpandidos++;

			for (Node child : actual.generate_rta_succ()) {
				Node node = child.getFrom(explorados);
				if(node!=null) {
					child.h = node.h;
					child.inPath = node.inPath;
					child.support = node.support;
				}
				else {
					child.calcular_h();
					child.support = null;
					child.inPath = false;
					explorados.add(child);	
					
				}
				sucesores.add(child);
			}
			
			if(inRoot) {
				inRoot=false;
				ret = new ArrayList<Node>(sucesores);
			}
			
			Node best = sucesores.peek();
			actual.support = best;
			
			if(actual.h < best.h+best.c) {
				actual.h = best.h+best.c;
				for (Node child : sucesores) {		
					if(cont > 0 && child.inPath && actual.equals(child.support)) {
						System.out.print("CONT " + Integer.toString(cont) +"\n");
						candidates.add(child);
						cont--;
					}
				}
			}
			explorados.remove(actual);
			explorados.add(actual);				

		}
		
		return ret;
	}
	
}


