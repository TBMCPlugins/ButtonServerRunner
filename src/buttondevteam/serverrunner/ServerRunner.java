package buttondevteam.serverrunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import jline.console.ConsoleReader;
import jline.console.CursorBuffer;

public class ServerRunner {
	private static final int RESTART_MESSAGE_COUNT = 30;

	private static volatile String server_version = "1.9.2";

	private static volatile boolean stop = false;
	private static volatile int restartcounter = RESTART_MESSAGE_COUNT;
	private static volatile Process serverprocess;
	private static volatile PrintWriter serveroutput;
	private static volatile Thread rt;
	private static volatile ConsoleReader reader;
	private static volatile PrintWriter runnerout;

	public static void main(String[] args) throws IOException, InterruptedException {
		String minmem = "512M";
		String maxmem = "1G";
		if (args.length == 3) {
			if ((!args[0].contains("G") && !args[0].contains("M"))
					|| (!args[1].contains("G") && !args[0].contains("M"))) {
				System.out.println("Error: Invalid arguments.");
				System.out.println("Usage: java -jar ServerRunner.jar <minmem> <maxmem> <version>");
				System.out.println("Example: java -jar ServerRunner.jar 1G 2G");
				return;
			}
			minmem = args[0];
			maxmem = args[1];
			server_version = args[2];
		} else {
			System.out.println("Error: Wrong number of arguments.");
			System.out.println("Usage: java -jar ServerRunner.jar <minmem> <maxmem> <version>");
			System.out.println("Example: java -jar ServerRunner.jar 1G 2G 1.9.2");
			return;
		}
		if (!new File("spigot-" + server_version + ".jar").exists()) {

		}
		final String fminmem = minmem;
		final String fmaxmem = maxmem;
		reader = new ConsoleReader();
		reader.setPrompt("Runner>");
		runnerout = new PrintWriter(reader.getOutput());
		writeToScreen("Starting server...");
		serverprocess = startServer(minmem, maxmem);
		serveroutput = new PrintWriter(serverprocess.getOutputStream());
		rt = Thread.currentThread();
		final Thread it = new Thread() {
			@Override
			public void run() {
				try {
					String readLine;
					while (!stop && (readLine = reader.readLine()) != null) {
						/*
						 * if (readLine.equalsIgnoreCase("restart")) output.println("stop"); else {
						 */
						if (readLine.equalsIgnoreCase("stop"))
							ServerRunner.stop();
						serveroutput.println(readLine);
						// } // TODO: RunnerStates, stop Input- and OutputThread and restart them after backup?
						serveroutput.flush();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				ServerRunner.stop();
				writeToScreen("Stopped " + Thread.currentThread().getName());
			}
		};
		it.setName("InputThread");
		it.start();
		final Thread ot = new Thread() {
			@Override
			public void run() {
				try {
					BufferedReader serverinput = new BufferedReader(
							new InputStreamReader(serverprocess.getInputStream()));
					String line;
					while (true) {
						if ((line = serverinput.readLine()) != null) {
							writeToScreen(line);
							if (line.contains("FAILED TO BIND TO PORT")) {
								ServerRunner.stop();
								writeToScreen("A server is already running!");
							}
						} else if (!stop) {
							try {
								serverinput.close();
							} catch (Exception e) {
							}
							try {
								serveroutput.close();
							} catch (Exception e) {
							}
							writeToScreen("Server stopped! Restarting...");
							serverprocess = startServer(fminmem, fmaxmem);
							serverinput = new BufferedReader(new InputStreamReader(serverprocess.getInputStream()));
							serveroutput = new PrintWriter(serverprocess.getOutputStream());
							restartcounter = RESTART_MESSAGE_COUNT;
						} else
							break;
					}
					serverinput.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				ServerRunner.stop();
				writeToScreen("Stopped " + Thread.currentThread().getName());
			}
		}; // TODO: Rename start.sh and put empty one
		ot.setName("OutputThread");
		ot.start();
		Thread.currentThread().setName("RestarterThread");
		while (!stop) {
			try {
				if (restartcounter >= 0) {
					if (restartcounter == RESTART_MESSAGE_COUNT)
						Thread.sleep(24 * 60 * 60 * 1000);
					// Thread.sleep(10000);
					else if (restartcounter > 0) {
						sendMessage(serveroutput, "red", "-- Server restarting in " + restartcounter + " seconds!");
						Thread.sleep(1000); // TODO: Change to bossbar? (plugin)
					} else {
						writeToScreen("Stopping server for restart...");
						serveroutput.println("restart");
						serveroutput.flush();
					}
					restartcounter--;
				}
			} catch (InterruptedException e) { // The while checks if stop is true and then stops
			}
		}
		writeToScreen("Stopped " + Thread.currentThread().getName());
	}

	private static Process startServer(String minmem, String maxmem) throws IOException {
		return Runtime.getRuntime().exec(new String[] { "java", "-Xms" + minmem, "-Xmx" + maxmem,
				"-XX:MaxPermSize=128M", "-jar", "spigot-" + server_version + ".jar" });
	}

	private static void sendMessage(PrintWriter output, String color, String text) {
		output.println("tellraw @a {\"text\":\"" + text + "\",\"color\":\"" + color + "\"}");
		output.flush();
		writeToScreen(text);
	}

	private static void stop() {
		stop = true;
		rt.interrupt(); // The restarter thread sleeps for a long time and keeps the program running
	}

	private static void writeToScreen(String line) {
		stashLine();
		runnerout.println(line);
		unstashLine();
	}

	private static CursorBuffer stashed;

	private static void stashLine() {
		stashed = reader.getCursorBuffer().copy();
		try {
			reader.getOutput().write("\u001b[1G\u001b[K");
			reader.flush();
		} catch (IOException e) {
			// ignore
		}
	}

	private static void unstashLine() {
		try {
			reader.resetPromptLine(reader.getPrompt(), stashed.toString(), stashed.cursor);
		} catch (IOException e) {
			// ignore
		}
	}
}
