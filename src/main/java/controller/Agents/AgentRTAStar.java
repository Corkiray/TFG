package controller.Agents;


import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Comparator;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class AgentRTAStar{
	
	public static ACTIONS[] PosibleActions = new ACTIONS[] {ACTIONS.ACTION_UP, ACTIONS.ACTION_LEFT, 
			ACTIONS.ACTION_DOWN, ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_USE, ACTIONS.ACTION_NIL};

	boolean hayPlan;
	ArrayList <ACTIONS> plan;
	
	ArrayList<Node> explorados;
	
	int nExpandidos; //Número de nodos que han sido expandidos(se ha comprobado si es objetivo)
	int maxMem; //Número máximo de nodos almacenados en memoria
	int tamRuta; //Número de nodos transitados por el agente
	double runTime; //Tiempo, en milisegundos, usado para calcular el plan
	
	/**
	 * initialize all variables for the agent
	 * @param stateObs Observation of the current state.
	 * @param elapsedTimer Timer when the action returned is due.
	 */
	public AgentRTAStar() {			
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
	public ArrayList<ACTIONS> act(StateObservation state, ElapsedCpuTimer timer) {
		//Genero el nodo inicial y le inicio las variables de heurística
		Node root = new Node(state);

		System.out.print("\nPosición del jugador: [" + root.x+ " " + root.y + "]\n"
				+ "Posición del objetivo: [" + Node.target_x+ " " +Node.target_y + "]\n");
		
		if(root.calcular_h() == 0) {
			ArrayList<ACTIONS> ret = new ArrayList<ACTIONS> ();
			ret.add(ACTIONS.ACTION_NIL);
			return ret;
		}

		//Llamo al algoritmo de búsqueda, que devolverá la acción a realizar
		long tInicio = System.nanoTime();
		ArrayList<ACTIONS> plan = RTAStar(root);
		//Como se llama múltiples veces al algoritmo, y el Runtime es acumulado, voy sumándolos
		runTime += (System.nanoTime()-tInicio); 
		
		tamRuta++;
		
		System.out.print(" Runtime(ms): " + runTime + 
				",\n Tamaño de la ruta calculada: " + tamRuta +
				",\n Número de nodos expandidos: " + nExpandidos +
				",\n Máximo número de nodos en memoria: " + maxMem +
				",\n Plan:\n" + plan.toString() +
				"\n");
		
		return plan;
	}
	
	//Algoritmo RTAstar
	public ArrayList<ACTIONS> RTAStar(Node actual) {
		nExpandidos++; //Cada vez que llamo al algoritmo, expando el nodo en el que está el avatar.
		

		//Si un nodo no está explorado, inicializo la heurística actual por la generada matemáticamente.
		Node aux = actual.getFrom(explorados);
		if(aux!=null) {
			actual.h = aux.h;
		}
		else {
			actual.calcular_h();
			explorados.add(actual);
		}
		
		System.out.print("NODO PADRE: " + actual);

		//Cola de sucesores, que se ordenarán por prioridad
		PriorityQueue<Node> sucesores = new PriorityQueue<Node>(Node.RTA_NodeComparator);
		
		for (Node child : actual.generate_rta_succ()) {

			//Si ya está visitado,le establezco la heurística como la que tiene el algoritmo guardada para esa posición
			aux = child.getFrom(explorados);
			if(aux!=null) {
				child.h = aux.h;
			}
			else {
				child.calcular_h();
				explorados.add(child);
			}

			System.out.print("HIJO: " + child);

			//En cualquier caso, si se ha completado la acción, se añade a la cola de sucesores
			sucesores.add(child);		
		}
		
		//Extraigo el hijo que tenga menor coste (según la heurística)
		Node best = sucesores.poll();	

		Node second_best = best;
		if(!sucesores.isEmpty())
			second_best = sucesores.poll();
				
		//Si la nueva heurística es mejor que la que tenía almacenado el algoritmo, se actualiza
		if(actual.h < second_best.h + second_best.c) {
			explorados.remove(actual);
			actual.h = second_best.h + second_best.c;
			explorados.add(actual);
		}
		
		ArrayList<ACTIONS> plan = new ArrayList<ACTIONS>();
		
		plan.add(best.accion);
		
		if(!best.orientation.equals(actual.orientation))
			plan.add(best.accion);
			
		return plan;		
	}

}



