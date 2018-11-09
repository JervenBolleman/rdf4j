/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.RDF4J;
import org.eclipse.rdf4j.common.app.AppConfiguration;
import org.eclipse.rdf4j.common.app.AppVersion;
import org.eclipse.rdf4j.common.app.util.ConfigurationUtil;

import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;

import org.eclipse.rdf4j.console.command.Clear;
import org.eclipse.rdf4j.console.command.Close;
import org.eclipse.rdf4j.console.command.Connect;
import org.eclipse.rdf4j.console.command.ConsoleCommand;
import org.eclipse.rdf4j.console.command.Convert;
import org.eclipse.rdf4j.console.command.Create;
import org.eclipse.rdf4j.console.command.Disconnect;
import org.eclipse.rdf4j.console.command.Drop;
import org.eclipse.rdf4j.console.command.Export;
import org.eclipse.rdf4j.console.command.Federate;
import org.eclipse.rdf4j.console.command.Load;
import org.eclipse.rdf4j.console.command.Open;
import org.eclipse.rdf4j.console.command.PrintHelp;
import org.eclipse.rdf4j.console.command.PrintInfo;
import org.eclipse.rdf4j.console.command.QueryEvaluator;
import org.eclipse.rdf4j.console.command.Serql;
import org.eclipse.rdf4j.console.command.SetParameters;
import org.eclipse.rdf4j.console.command.Show;
import org.eclipse.rdf4j.console.command.Sparql;
import org.eclipse.rdf4j.console.command.Verify;

import org.eclipse.rdf4j.console.setting.ConsoleSetting;
import org.eclipse.rdf4j.console.setting.ConsoleWidth;
import org.eclipse.rdf4j.console.setting.DefaultConsoleParameters;
import org.eclipse.rdf4j.console.setting.QueryPrefix;
import org.eclipse.rdf4j.console.setting.ShowPrefix;


/**
 * The RDF4J Console is a command-line application for interacting with RDF4J. 
 * It reads commands from standard input and prints feedback to standard output. 
 * Available options include loading and querying of data in
 * repositories, repository creation and verification of RDF files.
 *
 * @author Jeen Broekstra
 * @author Arjohn Kampman
 * @author Bart Hanssens
 */
public class Console {
	private final static AppVersion VERSION = AppVersion.parse(RDF4J.getVersion());
	private final static String APP_NAME = "Console";
	private final static AppConfiguration APP_CFG = new AppConfiguration(APP_NAME, VERSION);
	private final static ConsoleState STATE = new DefaultConsoleState(APP_CFG);
	
	private final static String PROP_PREFIX = "org.eclipse.rdf4j.console.setting.";
	
	private static boolean exitOnError;

	private final ConsoleIO consoleIO;

	private final SortedMap<String,ConsoleCommand> commandMap = new TreeMap<>();
	private final SortedMap<String,ConsoleSetting> settingMap = new TreeMap<>();

		
	// "Core" commands
	private final Connect connect;
	private final Disconnect disconnect;
	private final Open open;
	private final Close close;
	
	/**
	 * Get console state
	 * 
	 * @return basic console state
	 */
	public ConsoleState getState() {
		return STATE;
	}
	
	/**
	 * Get console IO
	 * 
	 * @return console
	 */
	public ConsoleIO getConsoleIO() {
		return this.consoleIO;
	}
	
	
	/**
	 * Set exit on error mode
	 * 
	 * @param mode true when error should exit 
	 */
	protected void setExitOnError(boolean mode) {
		Console.exitOnError = mode;
	}
	
	/**
	 * Main
	 * 
	 * @param args command line arguments
	 * @throws IOException 
	 */
	public static void main(final String[] args) throws IOException {
		final Console console = new Console();
		
		CmdLineParser parser = new CmdLineParser(console);
		if (parser.parse(args) == null) {
			System.exit(-1);
		}
		
		if (! parser.handleInfoOptions()) {
			System.exit(0);
		}
		parser.handleEchoOptions();
		parser.handleExitOption();

		String location = parser.handleLocationGroup();
		
		if (! parser.handleCautionGroup()) {
			System.exit(3);
		}
		
		String otherArg = parser.handleOtherArg();
		
		connectAndOpen(console, parser.getSelectedLocation(), location, otherArg);
		console.start();
	}

	/**
	 * Connect to (and open) a repository, exit when connection fails
	 * 
	 * @param console
	 * @param selectedLocation s for server, d for local directory 
	 * @param location
	 * @param otherArg last argument, if any 
	 */
	private static void connectAndOpen(Console console, String selectedLocation, String location,
			String otherArg) {
		boolean connected;
		if ("s".equals(selectedLocation)) {
			connected = console.connect.connectRemote(location);
		} else if ("d".equals(selectedLocation)) {
			connected = console.connect.connectLocal(location);
		} else {
			connected = console.connect.connectDefault();
		}
		if (!connected) {
			System.exit(2);
		}
		if (!otherArg.isEmpty()) {
			console.open.openRepository(otherArg);
		}
	}

	/**
	 * Add command to register of known commands
	 * 
	 * @param cmd command to be added
	 */
	public final void register(ConsoleCommand cmd) {
		commandMap.put(cmd.getName(), cmd);
	}

	/**
	 * Add setting to register of known settings
	 * 
	 * @param setting setting to be added
	 */
	public final void register(ConsoleSetting setting) {
		settingMap.put(setting.getName(), setting);
	}
	
	/**
	 * Constructor
	 * 
	 * @throws IOException 
	 */
	public Console() throws IOException {
		APP_CFG.init();
		
		// Basic console parameters
		register(new ConsoleWidth(80));
		register(new QueryPrefix(true));
		register(new ShowPrefix(true));
		// FIXME: to be removed in 3.0 release + pass a Map to Commands (breaks signature)
		DefaultConsoleParameters PARAMS = new DefaultConsoleParameters(settingMap);

		consoleIO = new ConsoleIO(STATE);

		this.close = new Close(consoleIO, STATE);
		this.disconnect = new Disconnect(consoleIO, STATE, close);
		this.connect = new Connect(consoleIO, STATE, disconnect);
		this.open = new Open(consoleIO, STATE, close);
			
		// "core" commands for connnecting
		register(open);
		register(close);
		register(connect);
		register(disconnect);
		// querying
		register(new Federate(consoleIO, STATE));
		register(new Sparql(consoleIO, STATE, PARAMS));
		register(new Serql(consoleIO, STATE, PARAMS));
		// information
		register(new PrintHelp(consoleIO, commandMap));
		register(new PrintInfo(consoleIO, STATE));
		register(new Show(consoleIO, STATE));
		// repository management
		register(new Create(consoleIO, STATE));
		register(new Drop(consoleIO, STATE, close));
		// handling data
		register(new Verify(consoleIO));
		register(new Load(consoleIO, STATE));
		register(new Clear(consoleIO, STATE));
		register(new Export(consoleIO, STATE));
		register(new Convert(consoleIO, STATE));
		// parameters
		register(new SetParameters(consoleIO, STATE, PARAMS));
	}

	/**
	 * Load settings from properties file (application.properties)
	 */
	private void loadSettings() {
		Properties prop = APP_CFG.getProperties();
		settingMap.forEach((k,v) -> {
				String val = prop.getProperty(PROP_PREFIX + k, "");
				try {
					v.set(val);
				} catch (IllegalArgumentException iae) {
					consoleIO.writeError("Could not load property for " + k);
				}
			});
	}
	
	/**
	 * Save settings to default properties file (application.properties)
	 */
	private void saveSettings() {
		Properties prop = APP_CFG.getProperties();
		settingMap.forEach((k,v) -> {
				String oldval = prop.getProperty(PROP_PREFIX + k, "");
				String newval = v.get() != null ? String.valueOf(v.get()) : oldval;
				prop.setProperty(PROP_PREFIX + k, newval);
			});
		//ConfigurationUtil.saveConfigurationProperties(prop, file, exitOnError);
	}
	
	
	/**
	 * Start the interactive console, return error code on exit
	 * 
	 * @throws IOException 
	 */
	public void start() throws IOException {
		consoleIO.writeln(APP_CFG.getFullName());
		consoleIO.writeln();
		consoleIO.writeln(RDF4J.getVersion());
		consoleIO.writeln("Type 'help' for help.");
		
		loadSettings();
		
		int exitCode = 0;
		try {
			boolean exitFlag = false;
			while (!exitFlag) {
				final String command = consoleIO.readCommand();
				if (command == null) {
					// EOF
					break;
				}
				exitFlag = executeCommand(command);
				if (exitOnError && consoleIO.wasErrorWritten()) {
					exitCode = 2;
					exitFlag = true;
				}
			}
		} catch (UserInterruptException | EndOfFileException e) {
			exitCode = 0;
		} finally {
			disconnect.execute(false);
		}
		
		saveSettings();
		
		if (exitCode != 0) {
			System.exit(exitCode);
		}
		consoleIO.writeln("Bye");
	}

	/**
	 * Execute a command
	 * 
	 * @param command
	 * @return true when exit/quit command is entered
	 * @throws IOException 
	 */
	private boolean executeCommand(String command) throws IOException {
		boolean exit = false;

		// only try to parse the command if non-empty.
		if (0 < command.length()) {
			final String[] tokens = parse(command);
			final String operation = tokens[0].toLowerCase(Locale.ENGLISH);
			
			exit = "quit".equals(operation) || "exit".equals(operation);
			if (!exit) {
				ConsoleCommand cmd = commandMap.getOrDefault(operation, 
									commandMap.get("sparql"));
				if (cmd instanceof QueryEvaluator) {
					((QueryEvaluator) cmd).executeQuery(command, operation);
				} else {
					cmd.execute(tokens);
				}
			}
		}
		return exit;
	}

	/**
	 * Split a command into an array of tokens
	 * 
	 * @param command command to parse
	 * @return array of strings
	 */
	private String[] parse(String command) {
		final Pattern pattern = Pattern.compile("\"([^\"]*)\"|(\\S+)");
		final Matcher matcher = pattern.matcher(command);
		final List<String> tokens = new ArrayList<>();
		
		while (matcher.find()) {
			if (matcher.group(1) == null) {
				tokens.add(matcher.group());
			} else {
				tokens.add(matcher.group(1));
			}
		}
		return tokens.toArray(new String[tokens.size()]);
	}
}
