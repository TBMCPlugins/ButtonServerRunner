package buttondevteam.serverrunner;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.Main;
import org.bukkit.craftbukkit.v1_9_R1.CraftServer;
import org.xeustechnologies.jcl.ClasspathResources;
import org.xeustechnologies.jcl.Configuration;
import org.xeustechnologies.jcl.JarClassLoader;
import org.xeustechnologies.jcl.JclObjectFactory;
import org.xeustechnologies.jcl.proxy.CglibProxyProvider;
import org.xeustechnologies.jcl.proxy.ProxyProviderFactory;

public class ServerRunner {
	private static final String SERVER_VERSION = "1.9.2";

	@SuppressWarnings("static-access")
	public static void main(String[] args) {
		try {
			System.out.println("Loading server classes...");
			JarClassLoader jcl = new JarClassLoader();
			jcl.getSystemLoader().setEnabled(true);
			jcl.getCurrentLoader().setEnabled(false);
			jcl.getParentLoader().setEnabled(false);
			jcl.getOsgiBootLoader().setEnabled(false);
			jcl.getThreadLoader().setEnabled(false);
			jcl.getLocalLoader().setEnabled(true);
			jcl.add("spigot-" + SERVER_VERSION + ".jar");
			jcl.add("craftbukkit-" + SERVER_VERSION + ".jar");
			System.out.println("Starting server...");
			// Set default to cglib (from version 2.2.1)
			ProxyProviderFactory.setDefaultProxyProvider(new CglibProxyProvider());

			// Create a factory of castable objects/proxies
			JclObjectFactory factory = JclObjectFactory.getInstance(true);

			// Create and cast object of loaded class
			Main serverinstance = (Main) factory.create(jcl, "buttondevteam.serverrunner.ServerMain");
			serverinstance.main(new String[0]);
			Thread.sleep(5000); // Wait for primaryThread to be set
			while (Bukkit.getServer() == null || ((CraftServer) Bukkit.getServer()).getServer() == null
					|| ((CraftServer) Bukkit.getServer()).getServer().primaryThread == null)
				;
			((CraftServer) Bukkit.getServer()).getServer().primaryThread.join();
			/*
			 * for (Thread t : Thread.getAllStackTraces().keySet()) if
			 * (t.getName().equals("Server Infinisleeper")) t.join();
			 */ // TODO: After stopping the server it stops
			System.out.println("Unloading classes...");
			for (String entry : jcl.getLoadedClasses().keySet())
				jcl.unloadClass(entry);
			System.out.println("Done!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
