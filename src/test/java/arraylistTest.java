import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import controller.Agents.Node;

public class arraylistTest {
	public static void main(String[] args) {
		Map<Integer, Integer> lista = new HashMap<>();
		
		int a = 0;
		
		int b = 1;
		
		lista.put(a, 0);
		lista.put(b, 0);
		
		Map<Integer, Integer> lista2 = new HashMap<Integer, Integer>(lista);
		lista2.remove(a);
		
		ArrayList<Integer> lista3 = new ArrayList<Integer>();
				
		for(int i : lista3) {
			System.out.print(i);				
		}
		System.out.print(a);				

		
	}
}


