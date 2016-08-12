package buttondevteam.serverrunner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class ServerRunner {
	private static final String SERVER_VERSION = "1.9.2";

	private static boolean stop = false;

	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println("Starting server...");
		Process p = Runtime.getRuntime().exec(new String[] { "java", "-Xms512M", "-Xmx1024M", "-XX:MaxPermSize=128M",
				"-jar", "spigot-" + SERVER_VERSION + ".jar" });
		final Thread it = new Thread() {
			@Override
			public void run() {
				PrintWriter output = new PrintWriter(p.getOutputStream());
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				try {
					while (!stop) {
						String readLine = br.readLine();
						output.println(readLine);
						output.flush();
						if (readLine.contains("stop"))
							stop = true;
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
					BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
					String line;
					while ((line = input.readLine()) != null && !stop) {
						System.out.println(line);
					}
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				stop = true;
				System.out.println("Stopped " + Thread.currentThread().getName());
			}
		};
		ot.setName("OutputThread");
		ot.start();
		Thread.currentThread().setName("RestarterThread");
		while (!stop) {
			Thread.sleep(10000);
			System.out.println("RESTART");
		}
		System.out.println("Stopped " + Thread.currentThread().getName());
	}
}
