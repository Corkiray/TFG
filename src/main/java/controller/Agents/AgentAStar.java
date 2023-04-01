package controller.Agents;


import java.util.ArrayList;
import java.util.PriorityQueue;

import controller.Agents.Node;

import java.util.Comparator;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import core.vgdl.VGDLRegistry;

public class AgentAStar{
	
	public static ACTIONS[] PosibleActions = new ACTIONS[] {ACTIONS.ACTION_UP, ACTIONS.ACTION_LEFT, 
			ACTIONS.ACTION_DOWN, ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_USE};

	public static Vector2d fescala;
	
	boolean hayPlan;
	ArrayList <ACTIONS> plan;
	
	int nExpandidos; //Número de nodos que han sido expandidos(se ha comprobado si es objetivo)
	int maxMem; //Número máximo de nodos almacenados en memoria
	int tamRuta; //Número de nodos transitados por el agente
	double runTime; //Tiempo, en milisegundos, usado para calcular el plan
	
	

	
	/**
	 * initialize all variables for the agent
	 * @param stateObs Observation of the current state.
	 * @param elapsedTimer Timer when the action returned is due.
	 */
	public AgentAStar() {	
		//Inicializo el plan a vacío
		hayPlan = false;
		plan = new ArrayList<ACTIONS>();
		
		//Inicializo los resultados a 0
		nExpandidos = 0;
		maxMem = 0;
		tamRuta = 0;
		
	}
		
	
	public void plan(StateObservation state, ElapsedCpuTimer time, ArrayList<String> objetive){
		//Establezco el objetivo
		//Calculo el factor de escala píxeles -> grid)
		fescala = new Vector2d(
				state.getWorldDimension().width / state.getObservationGrid().length,
				state.getWorldDimension().height / state.getObservationGrid()[0].length );
		Node.setObjetive(objetive, state);
		
		//Genero el nodo inicial y le inicio las variables de heurística
		Node root = new Node(state);
		root.g = 0;
		root.calcular_h();
		root.calcular_f();
		
		System.out.print("\nPosición del jugador: [" + root.x+ " " + root.y + "]\n"
				+ "Posición del objetivo: [" + Node.target_x+ " " +Node.target_y + "]\n");

		//Llamo al algoritmo de búsqueda, que devolverá el último nodo del camino encontrado
		long tInicio = System.nanoTime();
		System.out.print("Entramos en astar\n");
		Node ultimo = AStar(root, state);
		System.out.print("salimos de astar\n");
		runTime = (System.nanoTime()-tInicio)/1000000.0;
		hayPlan = true;

		//Recorro los padres desde el nodo dado hasta el nodo inicial, y voy guardándolos al principio del plan
		while(ultimo != root) {
			plan.add(0, ultimo.accion);
			ultimo = ultimo.padre;			
		}
		tamRuta = plan.size();

		//Imprimo los datos de la planificación
		System.out.print(" Runtime(ms): " + runTime + 
				",\n Tamaño de la ruta calculada: " + tamRuta +
				",\n Número de nodos expandidos: " + nExpandidos +
				",\n Máximo número de nodos en memoria: " + maxMem +
				"\n");
	}

	
	
	/**
	 * Sigue un camino generado por A*
	 * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
	 * @return la próxima acción a realizar
	 */
	public ACTIONS act() {
		//Acción que devolverá el algoritmo
		ACTIONS accion = ACTIONS.ACTION_NIL;
		
		if(!plan.isEmpty()) {
			accion = plan.get(0);
			plan.remove(0);			
		}
	
		return accion;
	}
	
	//Algoritmo AStar
	public Node AStar(Node inicial, StateObservation state) {
		//Cola donde se guardarán, por orden de prioridad, los nodos a explorar
		PriorityQueue<Node> abiertos = new PriorityQueue<Node>(Node.NodeComparator); 

		//Lista donde se guardarán los nodos explorados
		ArrayList<Node> cerrados = new ArrayList<Node>();
		Node actual; //Nodo que se está explorando
	
		//Cuando añado un nodo a la cola, ya está visitado. Además, aumento el número de nodos en memoria.
		//Aunque el coste de ir al nodo inicial teóricamente es 0, lo pongo como 1 y dejo el 0 reservado para indicar que ese nodo no está explorado
		abiertos.add(inicial);
		maxMem++;
		

		while (true){
			
			actual = abiertos.poll();
			
			//System.out.print("NODO PADRE: " + actual);

			nExpandidos++;
			
			if(actual.h==0) return actual;
						
			//Exploro las acciones:
			for (ACTIONS action : PosibleActions) {
				Node child = new Node(actual);
				child = child.advance(action);
				child.padre = actual;
				child.accion = action;
				child.g = actual.g+1;
				child.calcular_h();
				child.calcular_f();
				
				//System.out.print("HIJO: " + child);

				
				boolean encontrado = false;
				
				//Si su coste es mejor que el mínimo, se quita el nodo de la cola en la que estaba y se añade a abiertos con el nuevo coste
				for(Node node : abiertos) {
					if(node.equals(child)) {
						if(child.g < node.g) {
							abiertos.remove(node);
							abiertos.add(child);
						}
						encontrado=true;
						break;
					}		
				}
				
				if(!encontrado)
				for(Node node : cerrados) {
					if(node.equals(child)) {
						if(child.g < node.g) {
							cerrados.remove(node);
							abiertos.add(child);
						}
						encontrado=true;
						break;
					}		
				}
				
				if(!encontrado) {
					abiertos.add(child);
					maxMem++;
				}
			}
						
			//Como el nodo ya ha sido explorado, se añade a cerrados
			cerrados.add(actual);
		}
	}
}
