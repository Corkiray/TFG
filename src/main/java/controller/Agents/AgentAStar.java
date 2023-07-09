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
		
	boolean hayPlan;
	ArrayList <Node> plan;
	
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
	public AgentAStar() {	
		//Inicializo el plan a vacío
		hayPlan = false;
		plan = new ArrayList<Node>();
		
		//Inicializo los resultados a 0
		nExpandidos = 0;
		maxMem = 0;
		numPlans = 0;
		tamRuta = 0;
		totalRunTime = 0;
	}
		
	
	public void plan(StateObservation state, ElapsedCpuTimer time){	
		//Genero el nodo inicial y le inicio las variables de heurística
		Node root = new Node(state);
		root.g = 0;
		root.calcular_h();
		root.calcular_f();
		
		System.out.print("\nPosición del jugador: [" + root.x+ " " + root.y + "]\n"
				+ "Posición del objetivo: [" + Node.target_x+ " " +Node.target_y + "]\n");

		//Llamo al algoritmo de búsqueda, que devolverá el último nodo del camino encontrado
		long tInicio = System.nanoTime();
		System.out.print("Entramos en Astar\n");
		Node ultimo = AStar(root);
		System.out.print("salimos de Astar\n");
		runTime = (System.nanoTime()-tInicio)/1000000.0;
		hayPlan = true;

		//Recorro los padres desde el nodo dado hasta el nodo inicial, y voy guardándolos al principio del plan
		plan.clear();
		while(!ultimo.equals(root)) {
			plan.add(0, ultimo);
			ultimo = ultimo.padre;			
		}
		tamRuta = plan.size();
		if(tamRuta==0) plan.add(root);
		
		
		numPlans++;
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

		return null;
	}
	
	//Algoritmo AStar
	public Node AStar(Node inicial) {
		//Cola donde se guardarán, por orden de prioridad, los nodos a explorar
		PriorityQueue<Node> abiertos = new PriorityQueue<Node>(Node.NodeComparator); 
		//ArrayList<Node> cerrados = new ArrayList<Node>();

		//Lista donde se guardarán los nodos explorados
		ArrayList<Node> explorados = new ArrayList<Node>();
		Node actual; //Nodo que se está explorando
	
		//Cuando añado un nodo a la cola, ya está visitado. Además, aumento el número de nodos en memoria.
		//Aunque el coste de ir al nodo inicial teóricamente es 0, lo pongo como 1 y dejo el 0 reservado para indicar que ese nodo no está explorado
		abiertos.add(inicial);
		explorados.add(inicial);
		

		while (true){
			
			actual = abiertos.poll();
			
			//System.out.print("NODO PADRE: " + actual);

			nExpandidos++;
			
			if(actual.h==0) {
				if(explorados.size() > maxMem) maxMem = explorados.size();
				return actual;
			}
					
			//Exploro las acciones:
			for (Node child : actual.generate_succ()) {
				
				//System.out.print("HIJO: " + child);

				Node aux = child.getFrom(explorados);
				if(aux!=null) {
					if(child.g < aux.g) {
						abiertos.remove(aux);
						explorados.remove(aux);
						abiertos.add(child);
						explorados.add(child);
					}
				}
				else {
					abiertos.add(child);
					explorados.add(child);
				}
				
				/*
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
				
				if(!encontrado) {
					Node aux = child.getFrom(cerrados);
					if(aux!=null) {
						if(child.g < aux.g) {
							cerrados.remove(aux);
							abiertos.add(child);	
						}
						encontrado=true;
					}
				}
				
				if(!encontrado) {
					abiertos.add(child);
					maxMem++;
				}
				*/
						
			}
		//Como el nodo ya ha sido explorado, se añade a cerrados
		//cerrados.add(actual);
		}		
	}
}
