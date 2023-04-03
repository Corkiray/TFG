package controller.Agents.LPAStar;


import java.util.ArrayList;
import java.util.PriorityQueue;

import controller.Agents.LPAStar.Node;

import java.util.Comparator;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import core.vgdl.VGDLRegistry;

public class AgentLPAStar{
	
	public static ACTIONS[] PosibleActions = new ACTIONS[] {ACTIONS.ACTION_UP, ACTIONS.ACTION_LEFT, 
			ACTIONS.ACTION_DOWN, ACTIONS.ACTION_RIGHT, ACTIONS.ACTION_USE};

	public static Vector2d fescala;
	
	boolean hayPlan;
	ArrayList <ACTIONS> plan;
	
	int nExpandidos; //Número de nodos que han sido expandidos(se ha comprobado si es objetivo)
	int maxMem; //Número máximo de nodos almacenados en memoria
	int tamRuta; //Número de nodos transitados por el agente
	double runTime; //Tiempo, en milisegundos, usado para calcular el plan
	
	PriorityQueue<Node> abiertos;
	//Lista donde se guardarán los nodos explorados
	ArrayList<Node> explorados;

	Node inicial;

	
	/**
	 * initialize all variables for the agent
	 * @param stateObs Observation of the current state.
	 * @param elapsedTimer Timer when the action returned is due.
	 */
	public AgentLPAStar() {	
		//Inicializo el plan a vacío
		hayPlan = false;
		plan = new ArrayList<ACTIONS>();
		
		//Inicializo los resultados a 0
		nExpandidos = 0;
		maxMem = 0;
		tamRuta = 0;
		
		abiertos  = new PriorityQueue<Node>(Node.LPA_NodeComparator);

		explorados = new ArrayList<Node>();
	}
		
	
	public void plan(StateObservation state, ElapsedCpuTimer time){
		//Genero el nodo inicial y le inicio las variables de heurística
		Node root = new Node(state);
		
		System.out.print("\nPosición del jugador: [" + root.x+ " " + root.y + "]\n"
				+ "Posición del objetivo: [" + Node.target_x+ " " +Node.target_y + "]\n");

		//Llamo al algoritmo de búsqueda, que devolverá el último nodo del camino encontrado
		long tInicio = System.nanoTime();
		System.out.print("Entramos en astar\n");
		Node ultimo = LPAStar(state);
		System.out.print("salimos de astar\n");
		runTime = (System.nanoTime()-tInicio)/1000000.0;
		hayPlan = true;

		//Recorro los padres desde el nodo dado hasta el nodo inicial, y voy guardándolos al principio del plan
		while(!ultimo.equals(root)) {
			System.out.print(ultimo);
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
	
	public void initialize(StateObservation state, ArrayList<String> objetive) {
		//Calculo el factor de escala píxeles -> grid)
		Node.fescala = new Vector2d(
				state.getWorldDimension().width / state.getObservationGrid().length,
				state.getWorldDimension().height / state.getObservationGrid()[0].length );

		//Establezco el objetivo
		Node.setObjetive(objetive, state);
		
		abiertos.clear();
		explorados.clear();
		
		inicial = new Node(state);
		inicial.rhs=0;
		inicial.g = Double.POSITIVE_INFINITY;
		inicial.calcular_h();
		
		abiertos.add(inicial);
		explorados.add(inicial);
		maxMem++;
	}
	
	public void UpdateVertex(Node node) {	
		abiertos.remove(node);
		if(node.g != node.rhs) abiertos.add(node);
	}
	
	//Algoritmo AStar
	public Node LPAStar(StateObservation state) {
		//Cola donde se guardarán, por orden de prioridad, los nodos a explorar
		Node actual; //Nodo que se está explorando
		double g_old = -1000;
		
		while (true){
			boolean recalculate = false;
			
			actual = abiertos.poll();

			nExpandidos++;
			
			if(actual.h==0) return actual;
			
			if(actual.g > actual.rhs) {
				actual.g = actual.rhs;
			}
			else {
				recalculate=true;
				g_old = actual.g;
				actual.g = Double.POSITIVE_INFINITY;
				if(!actual.equals(inicial))
					actual.rhs = actual.update_father().g+1;
				UpdateVertex(actual);
			}
						
			System.out.print("NODO PADRE: " + actual);

			//Exploro las acciones:
			for (ACTIONS action : PosibleActions) {
				Node child = new Node(actual);
				child = child.advance(action);
				
				if(child.equals(actual)) continue;
				
				Node node = child.getFrom(explorados);
				if(node!=null) {
					child=node;
				}
				else {
					child.g = Double.POSITIVE_INFINITY;
					child.calcular_h();
					child.accion = action;
					child.padre = actual;
					maxMem++;					
				}
				child.pred.put(actual, action);
				
				if(recalculate) {
					if((!child.equals(inicial)) 
					&& (child.rhs == g_old+1)) {
						child.rhs = child.update_father().g + 1;
					}	
				}
				else if(!child.equals(inicial)) {
					child.rhs = Math.min(child.rhs, actual.g+1);					
				}
				
				UpdateVertex(child);
				
				explorados.remove(node);					
				explorados.add(child);
				
				System.out.print("HIJO: " + child);
			}
		}
	}
}
