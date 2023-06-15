package controller.Agents;


import java.util.ArrayList;
import java.util.PriorityQueue;

import controller.Agents.Node;

import java.util.Comparator;
import java.util.ListIterator;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import core.vgdl.VGDLRegistry;

public class AgentDStarLite{
	
	public static ACTIONS[] PosibleActions = new ACTIONS[] {ACTIONS.ACTION_UP, ACTIONS.ACTION_LEFT, 
			ACTIONS.ACTION_DOWN, ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_USE};

	public static Vector2d fescala;
	
	boolean hayPlan;
	ArrayList <Node> plan;
	
	int nExpandidos; //Número de nodos que han sido expandidos
	int maxMem; //Número máximo de nodos almacenados en memoria
	int numPlans; //Número de veces que se ha planificado
	int tamRuta; //Tamaño del plan calculado
	double totalRunTime; //Tiempo, en milisegundos, total utilizado
	double runTime;
	
	PriorityQueue<Node> abiertos;
	//Lista donde se guardarán los nodos explorados
	ArrayList<Node> explorados;

	Node inicial;
	Node goal;
	
	int km;

	
	/**
	 * initialize all variables for the agent
	 * @param stateObs Observation of the current state.
	 * @param elapsedTimer Timer when the action returned is due.
	 */
	public AgentDStarLite() {	
		//Inicializo el plan a vacío
		hayPlan = false;
		plan = new ArrayList<Node>();
		
		//Inicializo los resultados a 0
		nExpandidos = 0;
		maxMem = 0;
		numPlans = 0;
		tamRuta = 0;
		totalRunTime=0;
		
		abiertos  = new PriorityQueue<Node>(Node.D_NodeComparator);

		explorados = new ArrayList<Node>();
	}
		
	
	public void plan(StateObservation state, ElapsedCpuTimer time){
		
		System.out.print("\nPosición del jugador: [" + inicial.x+ " " + inicial.y + "]\n"
				+ "Posición del objetivo: [" + Node.target_x+ " " +Node.target_y + "]\n");

		//Llamo al algoritmo de búsqueda, que devolverá el último nodo del camino encontrado
		long tInicio = System.nanoTime();
		System.out.print("Entramos en Dstar\n");
		ComputeShortestPath();
		System.out.print("salimos de Dstar\n");
		runTime = (System.nanoTime()-tInicio)/1000000.0;
		hayPlan = true;

		//Recorro los padres desde el nodo dado hasta el nodo inicial, y voy guardándolos al principio del plan
		plan.clear();
		Node ultimo = inicial;
		while(!ultimo.equals(goal)) {
			//System.out.print(ultimo);
			plan.add(ultimo);
			ultimo = ultimo.hijo;			
		}
		tamRuta = plan.size();
		
		numPlans++;
		if(explorados.size() > maxMem) maxMem = explorados.size();
		totalRunTime += runTime;
		
		//Imprimo los datos de la planificación
		System.out.print(" Runtime(ms): " + runTime + 	
				",\n Runtime total: " + totalRunTime +
				",\n Tamaño de la ruta calculada: " + tamRuta +
				",\n Número de nodos expandidos: " + nExpandidos +
				",\n Máximo número de nodos en memoria: " + maxMem +
				",\n Número de veces que se ha planificado: " + numPlans +
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
			next = plan.remove(0);
			if(next.is_able(state)) {
				return next;
			}
		}
		else {
			Node aux = new Node(state);
			if(is_goal(aux))
				return aux;
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
		
		goal = Node.GoalNode(state);
		goal.h = goal.get_relative_h_from(inicial);
		goal.g = Double.POSITIVE_INFINITY;
		goal.rhs = 0;
		
		abiertos.add(goal);
		explorados.add(goal);

		maxMem++;
		
		km=0;

	}
	
	public void UpdateVertex(Node node) {	
		if(abiertos.contains(node)) abiertos.remove(node);
		node.update_key(km, inicial);
		if(node.g != node.rhs) abiertos.add(node);
		//System.out.print(abiertos);
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

			//System.out.print("NODO PADRE: " + actual);
			
			if(actual.equals(inicial)) 
				inicial=actual;
			inicial.update_key(km, inicial);
			if((Node.D_NodeComparator.compare(actual, inicial) >= 0)
					&&	(inicial.rhs <= inicial.g)) {
				break;
			}			
			
			if(actual.last_km < km) {
				actual.update_key(km, inicial);
				abiertos.add(actual);
				continue;
			}
			else if(actual.g > actual.rhs) {
				actual.g = actual.rhs;
			}
			else {
				recalculate=true;
				g_old = actual.g;
				actual.g = Double.POSITIVE_INFINITY;
				if((!actual.equals(goal))
				&& (actual.rhs == g_old))
					actual.rhs = actual.update_bestChild().g+1;
				UpdateVertex(actual);
			}
						

			//Exploro las acciones:
			for (Node father : actual.generate_pred()) {
				ACTIONS action = father.accion;
				
				Node node = father.getFrom(explorados);
				if(node!=null) {
					father=node;
					father.h = father.get_relative_h_from(inicial);
					explorados.remove(node);					
				}
				else {
					father.g = Double.POSITIVE_INFINITY;
					father.rhs = Double.POSITIVE_INFINITY;
					father.h = father.get_relative_h_from(inicial);
				}

				father.childs.put(actual, action);

				if(recalculate) {
					if(father.rhs == g_old+1) {
						father.hijo = father.update_bestChild();
						if(father.hijo == null) {
							father.rhs = Double.POSITIVE_INFINITY;
						}
						else {
							father.rhs = father.hijo.g + 1;							
						}
					}	
				}
				else if(actual.g+1 < father.rhs) {
					father.rhs = actual.g+1;
					father.hijo = actual;
					father.accion = action;
				}

				UpdateVertex(father);
				
				explorados.add(father);
				
				//System.out.print("HIJO: " + father);
				
			}
		}
		
	}
	
	public void updateCosts(StateObservation state, ElapsedCpuTimer timer) {
		Node.lastState=state;
		boolean isAnyChange = false;
		
		ListIterator<Node> lir = explorados.listIterator();
		
		while (lir.hasNext()) {
			Node node = lir.next();
			if(node.exists_in(state, "wall", node.x, node.y)) {
				isAnyChange=true;
				node.rhs=Double.POSITIVE_INFINITY;
				UpdateVertex(node);
				explorados.remove(node);
				lir = explorados.listIterator();
			}

		}
		
		if(isAnyChange) {
			Node aux = new Node(state);
			aux = aux.getFrom(explorados);
			km += inicial.get_relative_h_from(aux);
			inicial = aux;
			
			plan(state, timer);
		}
	}


	public boolean is_goal(Node actual) {
		if(actual.x == goal.x && actual.y == goal.y)
			return true;
		else
			return false;
	}

}
