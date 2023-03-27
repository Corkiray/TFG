package pddl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class metricff_call {

    public static void main(String[] args) {

        ProcessBuilder processBuilder = new ProcessBuilder();

        processBuilder.command("cmd.exe", "/c", 
        		"wsl Metric-FF/ff"
        		+ " -o "
        		+ "domains/dominioMochilero.pddl"
        		+ " -f "
        		+ "output/problems/problem.pddl"
        		);

        try {

            Process process = processBuilder.start();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            boolean print=false;
            String line;        
            while ((line = reader.readLine()) != null) {
            	if(line.contains("step")) {
            		line = line.replaceFirst("step", "");
            		print=true;
            	}
            	if(!line.contains(":")) print=false;
            	if(print){
            		line = line.trim();
            		line = line.replaceFirst("[0-9]+: ", "");
            		System.out.println(line);
            	}
            }

            int exitCode = process.waitFor();
            System.out.println("\nExited with error code : " + exitCode);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}