package agent;


import java.util.Random;

import controller.PDDL.PDDLInterface;
import controller.MainController;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import tools.Utils;
import tracks.ArcadeMachine;

@Command(name = "GVGAI-PDDL", description = "Launches a new GVGAI game played by a planning agent or by a human.",
		 mixinStandardHelpOptions = true, version = "1.0")
public class Main {

	private int gameIdx = 122;

	private int levelIdx = 0;

	private String configurationFile = "config/mochilero.yaml";

	private boolean debugMode = false;

	private boolean saveOutput = true;

    public static void main(String[] args) {
    	// Load commandline arguments
    	Main test = new Main();
    	CommandLine commandLine = new CommandLine(test);
    	commandLine.parseArgs(args);

    	// Display help or version information
    	if (commandLine.isUsageHelpRequested()) {
    		commandLine.usage(System.out);
    		return;
		} else if (commandLine.isVersionHelpRequested()) {
    		commandLine.printVersionHelp(System.out);
    		return;
		}

    	// Load available games
		String spGamesCollection = "examples/all_games_sp.csv";
    	String[][] games = Utils.readGames(spGamesCollection);

    	// Game settings
		boolean visuals = true;
		int seed = new Random().nextInt();

		// Game and level to play
		String gameName = games[test.gameIdx][1];
		String game = games[test.gameIdx][0];
		String level = game.replace(gameName, gameName + "_lvl" + test.levelIdx);

		// Controller
		String controller = "controller.MainController";

		// Find out if the game has to be played by a human or by the agent
		boolean humanPlayer = false;

		// Play game
		if (humanPlayer) {
			ArcadeMachine.playOneGame(game, level, null, seed);
		} else {
			MainController.setGameConfigFile(test.configurationFile);
			PDDLInterface.setDebugMode(test.debugMode);
			PDDLInterface.setSaveInformation(test.saveOutput);
			ArcadeMachine.runOneGame(game, level, visuals, controller, null, seed, 0);
			PDDLInterface.displayStats();
		}
    }
}
