import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import controller.Agents.LPAStar.Node;

public class arraylistTest {
	public static void main(String[] args) {
		Map<Node, Integer> lista = new HashMap();
		
		Node node = new Node();
		node.g = 10;
		
		Node node2 = new Node();
		node2.g =5;
		
		lista.put(node, 1);
		lista.put(node2, 2);
		
		node.g = 1;
		
		System.out.print(lista.toString());	
	}
}


