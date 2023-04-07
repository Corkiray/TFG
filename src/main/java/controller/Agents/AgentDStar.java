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

public class AgentDStar{
	
	public static ACTIONS[] PosibleActions = new ACTIONS[] {ACTIONS.ACTION_UP, ACTIONS.ACTION_LEFT, 
			ACTIONS.ACTION_DOWN, ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_USE};

	public static Vector2d fescala;
	
	boolean hayPlan;
	ArrayList <Node> plan;
	
	int nExpandidos; //Número de nodos que han sido expandidos(se ha comprobado si es objetivo)
	int maxMem; //Número máximo de nodos almacenados en memoria
	int tamRuta; //Número de nodos transitados por el agente
	double runTime; //Tiempo, en milisegundos, usado para calcular el plan
	
	PriorityQueue<Node> abiertos;
	//Lista donde se guardarán los nodos explorados
	ArrayList<Node> explorados;

	Node inicial;
	Node goal;

	
	/**
	 * initialize all variables for the agent
	 * @param stateObs Observation of the current state.
	 * @param elapsedTimer Timer when the action returned is due.
	 */
	public AgentDStar() {	
		//Inicializo el plan a vacío
		hayPlan = false;
		plan = new ArrayList<Node>();
		
		//Inicializo los resultados a 0
		nExpandidos = 0;
		maxMem = 0;
		tamRuta = 0;
		
		abiertos  = new PriorityQueue<Node>(Node.LPA_NodeComparator);

		explorados = new ArrayList<Node>();
	}
		
	
	public void plan(StateObservation state, ElapsedCpuTimer time){
		//Genero el nodo inicial y le inicio las variables de heurística
		inicial = new Node(state);
		inicial.rhs= Double.POSITIVE_INFINITY;
		inicial.g = Double.POSITIVE_INFINITY;
		inicial.h=0;
		
		System.out.print("\nPosición del jugador: [" + inicial.x+ " " + inicial.y + "]\n"
				+ "Posición del objetivo: [" + Node.target_x+ " " +Node.target_y + "]\n");

		//Llamo al algoritmo de búsqueda, que devolverá el último nodo del camino encontrado
		long tInicio = System.nanoTime();
		System.out.print("Entramos en astar\n");
		ComputeShortestPath();
		System.out.print("salimos de astar\n");
		runTime = (System.nanoTime()-tInicio)/1000000.0;
		hayPlan = true;

		//Recorro los padres desde el nodo dado hasta el nodo inicial, y voy guardándolos al principio del plan
		plan.clear();
		Node ultimo = inicial;
		while(!ultimo.equals(goal)) {
			System.out.print(ultimo);
			plan.add(0, ultimo);
			ultimo = ultimo.hijo;			
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
	public Node act(StateObservation state) {
		Node next;
		if(!plan.isEmpty()) {
			next = plan.get(0);
			if(next.is_able(state)) {
				plan.remove(0);							
				return next;
			}
		}

		return null;
	}
	
	public void initialize(StateObservation state) {	
		abiertos.clear();
		explorados.clear();
		
		inicial = new Node(state);
		inicial.rhs= Double.POSITIVE_INFINITY;
		inicial.g = Double.POSITIVE_INFINITY;
		inicial.h=0;
		
		maxMem++;
		
		goal = Node.GoalNode(state);
		goal.h = goal.get_relative_h_from(inicial);
		goal.g = Double.POSITIVE_INFINITY;
		goal.rhs = 0;
		
		abiertos.add(goal);
		explorados.add(goal);

	}
	
	public void UpdateVertex(Node node) {	
		abiertos.remove(node);
		if(node.g != node.rhs) abiertos.add(node);
	}
	
	//Algoritmo AStar
	public void ComputeShortestPath() {
		//Cola donde se guardarán, por orden de prioridad, los nodos a explorar
		Node actual; //Nodo que se está explorando
		double g_old = -1000;
		
		while (true){
			boolean recalculate = false;
			
			//System.out.print(abiertos.toString());

			actual = abiertos.poll();

			nExpandidos++;

			System.out.print("NODO PADRE: " + actual);
	
			if(inicial.rhs != Double.POSITIVE_INFINITY) {
				if((Node.LPA_NodeComparator.compare(actual, inicial) >= 0)
						&&	(inicial.rhs == inicial.g)) {
					break;
				}
			}
			else if(actual.equals(inicial)) inicial=actual;

			
			if(actual.g > actual.rhs) {
				actual.g = actual.rhs;
			}
			else {
				recalculate=true;
				g_old = actual.g;
				actual.g = Double.POSITIVE_INFINITY;
				if((!actual.equals(goal))
				&& (actual.rhs == g_old+1))
					actual.rhs = actual.update_bestChild().g+1;
				UpdateVertex(actual);
			}
						

			//Exploro las acciones:
			for (Node father : actual.generate_pred()) {
				
				Node node = father.getFrom(explorados);
				if(node!=null) {
					father=node;
				}
				else {
					father.g = Double.POSITIVE_INFINITY;
					father.h = father.get_relative_h_from(inicial);
					maxMem++;					
				}

				father.childs.put(actual, father.accion);

				if(recalculate) {
					if((!father.equals(goal)) 
					&& (father.rhs == g_old+1)) {
						father.hijo = father.update_bestChild();
						if(father.hijo == null) {
							father.rhs = Double.POSITIVE_INFINITY;
						}
						else {
							father.rhs = father.update_bestChild().g + 1;							
						}
					}	
				}
				else if(!father.equals(goal)) {
					if(actual.g+1 < father.rhs) {
						father.rhs = actual.g+1;
						father.hijo = actual;
					}
				}
				
				UpdateVertex(father);
				
				explorados.remove(node);					
				explorados.add(father);
				
				System.out.print("HIJO: " + father);
			}
		}
		
	}
	
	public void updateCosts(StateObservation state, ElapsedCpuTimer timer) {
		Node.lastState=state;
		boolean isAnyChange = false;
		for(Node node : explorados) {
			if(node.exists_in(state, "wall", node.x, node.y)) {
				isAnyChange=true;
				node.rhs=Double.POSITIVE_INFINITY;
				UpdateVertex(node);
			}
		}
		
		if(isAnyChange) {
			plan(state, timer);
		}
	}

}
