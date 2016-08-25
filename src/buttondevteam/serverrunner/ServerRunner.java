package buttondevteam.serverrunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class ServerRunner {
	private static final String SERVER_VERSION = "1.9.2";

	private static volatile boolean stop = false;
	private static volatile int restartcounter = 30;
	private static volatile Process serverprocess;
	private static volatile PrintWriter output;

	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println("Starting server...");
		serverprocess = startServer();
		output = new PrintWriter(serverprocess.getOutputStream());
		final Thread it = new Thread() {
			@Override
			public void run() {
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				try {
					while (!stop) {
						String readLine = br.readLine();
						if (readLine.equalsIgnoreCase("restart"))
							output.println("stop");
						else {
							if (readLine.equalsIgnoreCase("stop"))
								stop = true;
							output.println(readLine);
							System.out.println("Read line: " + readLine);
						} // TODO: RunnerStates, stop Input- and OutputThread and restart them after backup
						output.flush();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				stop = true;
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
								stop = true;
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
							restartcounter = 30;
						} else
							break;
					}
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				stop = true;
				System.out.println("Stopped " + Thread.currentThread().getName());
			}
		}; // TODO: Rename start.sh
		ot.setName("OutputThread");
		ot.start();
		Thread.currentThread().setName("RestarterThread");
		while (!stop) {
			if (restartcounter >= 0) {
				if (restartcounter == 30)
					Thread.sleep(10000);
				else if (restartcounter > 0) {
					sendMessage(output, "red", "-- Server restarting in " + restartcounter + " seconds!");
					Thread.sleep(1000); // TODO: Change to bossbar (plugin)
				} else {
					System.out.println("Stopping server...");
					output.println("stop");
					output.flush();
				}
				restartcounter--;
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
		System.out.println(text);
	}
}
