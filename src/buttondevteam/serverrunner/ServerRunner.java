package buttondevteam.serverrunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class ServerRunner {
	private static final int RESTART_MESSAGE_COUNT = 30;

	private static final String SERVER_VERSION = "1.9.2";

	private static volatile boolean stop = false;
	private static volatile int restartcounter = RESTART_MESSAGE_COUNT;
	private static volatile Process serverprocess;
	private static volatile PrintWriter output;
	private static volatile Thread rt;

	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println("Starting server...");
		serverprocess = startServer();
		output = new PrintWriter(serverprocess.getOutputStream());
		rt = Thread.currentThread();
		final Thread it = new Thread() {
			@Override
			public void run() {
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				try {
					while (!stop) {
						String readLine = br.readLine();
						/*
						 * if (readLine.equalsIgnoreCase("restart")) output.println("stop"); else {
						 */
						if (readLine.equalsIgnoreCase("stop"))
							ServerRunner.stop();
						output.println(readLine);
						System.out.println("Read line: " + readLine);
						// } // TODO: RunnerStates, stop Input- and OutputThread and restart them after backup?
						output.flush();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				ServerRunner.stop();
				System.out.println("Stopped " + Thread.currentThread().getName());
			}
		};
		it.setName("InputThread");
		it.start();
		final Thread ot = new Thread() {
			@Override
			public void run() {
				try {
					BufferedReader input = new BufferedReader(new InputStreamReader(serverprocess.getInputStream()));
					String line;
					while (true) {
						if ((line = input.readLine()) != null) {
							System.out.println(line);
							if (line.contains("FAILED TO BIND TO PORT")) {
								ServerRunner.stop();
								System.out.println("A server is already running!");
							}
						} else if (!stop) {
							try {
								input.close();
							} catch (Exception e) {
							}
							try {
								output.close();
							} catch (Exception e) {
							}
							System.out.println("Server stopped! Restarting...");
							serverprocess = startServer();
							input = new BufferedReader(new InputStreamReader(serverprocess.getInputStream()));
							output = new PrintWriter(serverprocess.getOutputStream());
							restartcounter = RESTART_MESSAGE_COUNT;
						} else
							break;
					}
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				ServerRunner.stop();
				System.out.println("Stopped " + Thread.currentThread().getName());
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
						sendMessage(output, "red", "-- Server restarting in " + restartcounter + " seconds!");
						Thread.sleep(1000); // TODO: Change to bossbar? (plugin)
					} else {
						System.out.println("Stopping server for restart...");
						output.println("restart");
						output.flush();
					}
					restartcounter--;
				}
			} catch (InterruptedException e) { // The while checks if stop is true and then stops
			}
		}
		System.out.println("Stopped " + Thread.currentThread().getName());
	}

	private static Process startServer() throws IOException {
		return Runtime.getRuntime().exec(new String[] { "java", "-Xms512M", "-Xmx1024M", "-XX:MaxPermSize=128M", "-jar",
				"spigot-" + SERVER_VERSION + ".jar" });
	}

	private static void sendMessage(PrintWriter output, String color, String text) {
		output.println("tellraw @a {\"text\":\"" + text + "\",\"color\":\"" + color + "\"}");
		output.flush();
		System.out.println(text);
	}

	private static void stop() {
		stop = true;
		rt.interrupt(); // The restarter thread sleeps for a long time and keeps the program running
	}
}
