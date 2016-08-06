package buttondevteam.serverrunner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class ServerRunner {
	private static final String SERVER_VERSION = "1.9.2";

	public static void main(String[] args) throws IOException {
		System.out.println("Starting server...");
		Process p = Runtime.getRuntime().exec(
				new String[] { "java", "-Xms512M", "-Xmx1024M", "-XX:MaxPermSize=128M", "-jar", "spigot-1.9.2.jar" });
		Thread t = new Thread() {
			@Override
			public void run() {
				BufferedWriter output = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				String outline;
				try {
					while ((outline = br.readLine()) != null)
						output.write(outline + "\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		t.setName("InputThread");
		t.start();
		BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line;
		while ((line = input.readLine()) != null) {
			System.out.println(line);
		}
		input.close();
	}
}
