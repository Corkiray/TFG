package tracks.singlePlayer;

import java.util.Random;

import controller.PDDL.PDDLInterface;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import tools.Utils;
import tracks.ArcadeMachine;

/**
 * Created with IntelliJ IDEA. User: Diego Date: 04/10/13 Time: 16:29 This is a
 * Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
@Command(name = "GVGAI-PDDL", description = "Launches a new GVGAI game played by a planning agent or by a human.",
		 mixinStandardHelpOptions = true, version = "1.0")
public class Test {

	private int gameIdx = 122;

	private int levelIdx = 0;

	private String configurationFile = "config/mochilero.yaml";

	private boolean debugMode = false;

	private boolean saveOutput = true;

	private boolean localHost = false;

    public static void main(String[] args) {
    	// Load commandline arguments
    	Test test = new Test();
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
		String controller = "controller.MainAgent";

		// Find out if the game has to be played by a human or by the agent
		boolean humanPlayer = test.configurationFile == null;

		// Play game
		if (humanPlayer) {
			ArcadeMachine.playOneGame(game, level, null, seed);
		} else {
			PDDLInterface.setDebugMode(test.debugMode);
			PDDLInterface.setSaveInformation(test.saveOutput);
			ArcadeMachine.runOneGame(game, level, visuals, controller, null, seed, 0);
			PDDLInterface.displayStats();
		}
    }
}
