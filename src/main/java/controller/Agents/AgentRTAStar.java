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

	public static Vector2d fescala;

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
	public AgentRTAStar(StateObservation state, ElapsedCpuTimer timer) {	
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
	
	public void setObjetive(ArrayList<String> objetive, StateObservation state) {
		//Calculo el factor de escala píxeles -> grid)
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
	 * @return 	la acción a realizar en esta iteración
	 */
	public Node act(StateObservation state, ElapsedCpuTimer timer) {
		//Genero el nodo inicial y le inicio las variables de heurística
		Node root = new Node(state);
		root.g = 0;
		root.calcular_h();
		root.calcular_f();

		System.out.print("\nPosición del jugador: [" + root.x+ " " + root.y + "]\n"
				+ "Posición del objetivo: [" + Node.target_x+ " " +Node.target_y + "]\n");


		//Llamo al algoritmo de búsqueda, que devolverá la acción a realizar
		long tInicio = System.nanoTime();
		Node child = RTAStar(root, state);
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
	
	//Algoritmo RTAstar
	public Node RTAStar(Node actual, StateObservation state) {
		nExpandidos++; //Cada vez que llamo al algoritmo, expando el nodo en el que está el avatar.
		
		System.out.print("NODO PADRE: " + actual);

		
		//Si un nodo no está explorado, inicializo la heurística actual por la generada matemáticamente.
		if(!explorados.contains(actual)) {
			actual.calcular_h();
			explorados.add(actual);
		}
		
		//Cola de sucesores, que se ordenarán por prioridad
		PriorityQueue<Node> sucesores = new PriorityQueue<Node>(Node.NodeComparator);
		
		for (ACTIONS action : PosibleActions) {
			Node child = new Node(actual);
			child = child.advance(action);
			child.padre = actual;
			child.accion = action;
			child.g = actual.g+1;

			boolean encontrado=false;
			//Si ya está visitado,le establezco la heurística como la que tiene el algoritmo guardada para esa posición
			for(Node node : explorados) {
				if(node.equals(child)) {
					child.h = node.h;
					child.calcular_f();
					encontrado=true;
					break;
				}
			}
			if(!encontrado) {
				child.calcular_h();
				child.calcular_f();
				explorados.add(child);
			}

			System.out.print("HIJO: " + child);

			//En cualquier caso, si se ha completado la acción, se añade a la cola de sucesores
			if(!child.equals(actual))
				sucesores.add(child);		
		}
		
		//Extraigo el hijo que tenga menor coste (según la heurística)
		Node best = sucesores.poll();	
		
		//Si puedo, extraigo el segundo
		if(!sucesores.isEmpty())
			best = sucesores.poll();
		
		actual.h = best.h + 1; //La nueva heurística es la del hijo + el coste de desplazamiento que, como es constante, será 1
		
		//Si la nueva heurística es mejor que la que tenía almacenado el algoritmo, se actualiza
		for(Node node : explorados) {
			if(node.equals(actual)) {
				if(node.h < actual.h) {
					explorados.remove(node);
					explorados.add(actual);
				}
				break;
			}
		}
		
		return best;		
	}

}


