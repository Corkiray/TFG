package controller.Agents;


import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Comparator;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class AgentLRTAStar{
	
	ArrayList <ACTIONS> plan;
	
	ArrayList<Node> explorados;
	
	int nExpandidos; //N�mero de nodos que han sido expandidos
	int maxMem; //N�mero m�ximo de nodos almacenados en memoria
	int numPlans; //N�mero de veces que se ha planificado
	int tamRuta; //Tama�o del plan calculado
	double runTime;
	double totalRunTime; //Tiempo, en milisegundos, total utilizado
	
	/**
	 * initialize all variables for the agent
	 * @param stateObs Observation of the current state.
	 * @param elapsedTimer Timer when the action returned is due.
	 */
	public AgentLRTAStar() {			
		//Inicializo lista de nodos
		explorados = new ArrayList<Node>();
		
		//Inicializo el plan a vac�o
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
	 * @return 	la acci�n a realizar en esta iteraci�n
	 */
	public ArrayList<ACTIONS> act(StateObservation state, ElapsedCpuTimer timer) {
		//Genero el nodo inicial y le inicio las variables de heur�stica
		Node root = new Node(state);

		System.out.print("\nPosici�n del jugador: [" + root.x+ " " + root.y + "]\n"
				+ "Posici�n del objetivo: [" + Node.target_x+ " " +Node.target_y + "]\n");
		
		if(root.calcular_h() == 0) {
			ArrayList<ACTIONS> ret = new ArrayList<ACTIONS> ();
			ret.add(ACTIONS.ACTION_NIL);
			return ret;
		}

		//Llamo al algoritmo de b�squeda, que devolver� la acci�n a realizar
		long tInicio = System.nanoTime();
		ArrayList<ACTIONS> plan = LRTAStar(root);
		//Como se llama m�ltiples veces al algoritmo, y el Runtime es acumulado, voy sum�ndolos
		runTime = (System.nanoTime()-tInicio); 
		tamRuta = plan.size();

		numPlans++;
		totalRunTime += runTime/1000000.0;
		if(explorados.size() > maxMem) maxMem = explorados.size();

		//Imprimo los datos de la planificaci�n
		System.out.print(" Runtime: " + runTime + 
				",\n Runtime total (ms): " + totalRunTime +
				",\n Tama�o de la ruta calculada: " + tamRuta +
				",\n N�mero de nodos expandidos: " + nExpandidos +
				",\n M�ximo n�mero de nodos en memoria: " + maxMem +
				",\n N�mero de veces que se ha planificado: " + numPlans +
				"\n");

		
		return plan;
	}
	
	//Algoritmo RTAstar
	public ArrayList<ACTIONS> LRTAStar(Node actual) {
		nExpandidos++; //Cada vez que llamo al algoritmo, expando el nodo en el que est� el avatar.
		

		//Si un nodo no est� explorado, inicializo la heur�stica actual por la generada matem�ticamente.
		Node aux = actual.getFrom(explorados);
		if(aux!=null) {
			actual.h = aux.h;
		}
		else {
			actual.calcular_h();
			explorados.add(actual);
		}
		
		System.out.print("NODO PADRE: " + actual);

		//Cola de sucesores, que se ordenar�n por prioridad
		PriorityQueue<Node> sucesores = new PriorityQueue<Node>(Node.RTA_NodeComparator);
		
		for (Node child : actual.generate_rta_succ()) {

			//Si ya est� visitado,le establezco la heur�stica como la que tiene el algoritmo guardada para esa posici�n
			aux = child.getFrom(explorados);
			if(aux!=null) {
				child.h = aux.h;
			}
			else {
				child.calcular_h();
				explorados.add(child);
			}

			System.out.print("HIJO: " + child);

			//En cualquier caso, si se ha completado la acci�n, se a�ade a la cola de sucesores
			sucesores.add(child);		
		}
		
		//Extraigo el hijo que tenga menor coste (seg�n la heur�stica)
		Node best = sucesores.poll();	
				
		//Si la nueva heur�stica es mejor que la que ten�a almacenado el algoritmo, se actualiza
		if(actual.h < best.h + best.c) {
			explorados.remove(actual);
			actual.h = best.h + best.c;
			explorados.add(actual);
		}
		
		ArrayList<ACTIONS> plan = new ArrayList<ACTIONS>();
		
		plan.add(best.accion);
		
		if(!best.orientation.equals(actual.orientation))
			plan.add(best.accion);
		
		return plan;		
	}

}



