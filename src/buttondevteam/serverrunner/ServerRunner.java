package buttondevteam.serverrunner;

import jline.console.ConsoleReader;
import jline.console.CursorBuffer;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Collections;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class ServerRunner {
	private static final int RESTART_MESSAGE_COUNT = 60;

	private static final int interval = 24; // hours

	private static volatile boolean stop = false;
	private static int restartcounter = RESTART_MESSAGE_COUNT;
	private static volatile Process serverprocess;
	private static volatile PrintWriter serveroutput;
	private static volatile Thread rt;
	private static volatile ConsoleReader reader;
	private static volatile PrintWriter runnerout;

	private static volatile boolean customrestartfailed = false;

	public static void main(String[] args) throws IOException {
		Yaml yaml = new Yaml();
		File f=new File("plugins/ServerRunner/config.yml");
		f.getParentFile().mkdirs();
		final Config config;
		if(!f.exists())
			Files.write(f.toPath(), Collections.singleton(yaml.dump(config = new Config())));
		else
			config=yaml.load(new FileInputStream(f));
		if (!new File("spigot-" + config.serverVersion + ".jar").exists()) {
			System.out.println("The server JAR for " + config.serverVersion + " cannot be found!");
			return;
		}
		reader = new ConsoleReader();
		reader.setPrompt("Runner>");
		runnerout = new PrintWriter(reader.getOutput());
		writeToScreen("Starting server...");
		serverprocess = startServer(config);
		serveroutput = new PrintWriter(serverprocess.getOutputStream());
		rt = Thread.currentThread();
		final Thread it = new Thread() {
			@Override
			public void run() {
				try {
					String readLine;
					while (!stop) {
						try {
							if ((readLine = reader.readLine()) == null)
								break;
							if (readLine.equalsIgnoreCase("stop"))
								ServerRunner.stop();
							serveroutput.println(readLine);
							serveroutput.flush();
						} catch (Exception e) {
							e.printStackTrace();
							Thread.sleep(100); //Sleep a bit and keep going
						}
					}
				} catch (InterruptedException e) {
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
							new InputStreamReader(serverprocess.getInputStream(), StandardCharsets.UTF_8));
					String line;
					while (true) {
						if ((line = serverinput.readLine()) != null) {
							writeToScreen(line);
							if (line.contains("FAILED TO BIND TO PORT")) {
								ServerRunner.stop();
								writeToScreen("A server is already running!");
							}
							if (Pattern.matches(
									"\\[\\d\\d:\\d\\d:\\d\\d INFO]: Unknown command. Type \"/help\" for help.\\s+", line))
								customrestartfailed = true;

						} else if (!stop) {
							try {
								serverinput.close();
							} catch (Exception e) {
								e.printStackTrace();
							}
							try {
								serveroutput.close();
							} catch (Exception e) {
								e.printStackTrace();
							}
							writeToScreen("Server stopped! Restarting...");
							serverprocess = startServer(config);
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
		};
		ot.setName("OutputThread");
		ot.start();
		Thread.currentThread().setName("RestarterThread");
		long starttime = syncStart(config.restartAt);
		System.out.println("Restart scheduled in " + starttime / 3600000f);
		boolean firstrun = true;
		while (!stop) {
			try {
				if (restartcounter >= 0) {
					if (restartcounter == RESTART_MESSAGE_COUNT) {
						if (firstrun) {
							// writeToScreen("Sleeping for " + starttime);
							Thread.sleep(starttime);
							firstrun = false;
						} else
							Thread.sleep(interval * 3600000);
						customrestartfailed = false;
						serveroutput.println("schrestart");
						serveroutput.flush();
					} else if (restartcounter > 0) {
						if (customrestartfailed) {
							if (restartcounter % 10 == 0)
								sendMessage(serveroutput, "red",
										"-- Server restarting in " + restartcounter + " seconds!");
							Thread.sleep(1000);
						} else {
							restartcounter = RESTART_MESSAGE_COUNT;
							continue; // Don't decrement the counter so it will sleep the full time
						}
					} else {
						Thread.sleep(500);
						if (customrestartfailed) {
							writeToScreen("Stopping server for restart...");
							serveroutput.println("restart");
							serveroutput.flush();
							customrestartfailed = false;
						}
						Thread.sleep(5000); // Don't run needless cycles
					}
					restartcounter--;
				}
			} catch (InterruptedException e) { // The while checks if stop is true and then stops
			}
		}
		writeToScreen("Stopped " + Thread.currentThread().getName());
	}

	private static Process startServer(Config config) throws IOException {
		return Runtime.getRuntime().exec(("java "+config.serverParams+" -jar spigot-" + config.serverVersion + ".jar").split(" "));
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

	private static double hoursOf(Calendar parsedTime) {
		return parsedTime.get(Calendar.HOUR_OF_DAY) + parsedTime.get(Calendar.MINUTE) / 60.
				+ parsedTime.get(Calendar.SECOND) / 3600.;
	}

	private static long syncStart(double startHour) { // Copied original code from SimpleBackup
		double now = hoursOf(Calendar.getInstance(TimeZone.getTimeZone("GMT")));
		double diff = now - startHour;
		if (diff < 0) {
			diff += 24;
		}
		double intervalPart = diff - Math.floor(diff / interval) * interval;
		double remaining = interval - intervalPart;
		return (long) (remaining * 3600000);
	}

}
