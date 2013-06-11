package logisticspipes.ticks;

import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

import logisticspipes.LogisticsPipes;
import logisticspipes.utils.ObfuscationHelper;
import logisticspipes.utils.ObfuscationHelper.NAMES;
import net.minecraft.server.integrated.IntegratedServer;
import cpw.mods.fml.common.FMLCommonHandler;
// Based on
// https://raw.github.com/MinecraftPortCentral/MCPC-Plus/1acfe8e4d668b3fbc91b8d835451c5c56c74e7db/src/minecraft/org/spigotmc/WatchdogThread.java
public class Watchdog extends Thread {
	public static final int TIMEOUT = 30000;
	public static long timeStempServer = System.currentTimeMillis();
	public static long timeStempClient = System.currentTimeMillis();
	public static boolean toggledByCommand = false;
	private final boolean isClient;
	private Field isGamePaused = null;
	
	public Watchdog(boolean isClient) {
		super("LP Watchdog");
		this.setDaemon(true);
		this.start();
		this.isClient = isClient;
		if(isClient) {
			try {
				isGamePaused = ObfuscationHelper.getDeclaredField(NAMES.isGamePausedServer, IntegratedServer.class);
				isGamePaused.setAccessible(true);
			} catch(NoSuchFieldException e) {
				e.printStackTrace();
			} catch(SecurityException e) {
				e.printStackTrace();
			}
		}
	}

	public static void tickServer() {
		timeStempServer = System.currentTimeMillis();
	}
	
	public static void tickClient() {
		timeStempClient = System.currentTimeMillis();
	}
	
	public void run() {
		while(true) {
			boolean triggered = toggledByCommand;
			if(isClient) {
				boolean serverPaused = false;
				if(FMLCommonHandler.instance().getMinecraftServerInstance() != null) {
					if(FMLCommonHandler.instance().getMinecraftServerInstance() instanceof IntegratedServer) {
						try {
							serverPaused = isGamePaused.getBoolean(FMLCommonHandler.instance().getMinecraftServerInstance());
						} catch(IllegalArgumentException e) {
							e.printStackTrace();
						} catch(IllegalAccessException e) {
							e.printStackTrace();
						}
					}
					triggered |= (timeStempServer + TIMEOUT < System.currentTimeMillis() && !serverPaused && FMLCommonHandler.instance().getMinecraftServerInstance().isServerRunning());
				} else {
					timeStempServer = System.currentTimeMillis();
				}
				triggered |= timeStempClient + TIMEOUT < System.currentTimeMillis();
			} else {
				if(FMLCommonHandler.instance().getMinecraftServerInstance() != null && FMLCommonHandler.instance().getMinecraftServerInstance().isServerRunning()) {
					triggered |= timeStempServer + TIMEOUT < System.currentTimeMillis();
				} else {
					timeStempServer = System.currentTimeMillis();
				}
			}
 			if(triggered) {
				Logger log = LogisticsPipes.log;
				log.log(Level.SEVERE, "The server has stopped responding!");
				log.log(Level.SEVERE, "Please report this to https://github.com/RS485/LogisticsPipes/issues");
				log.log(Level.SEVERE, "Be sure to include ALL relevant console errors and Minecraft crash reports");
				log.log(Level.SEVERE, "LP version: " + LogisticsPipes.VERSION);
				log.log(Level.SEVERE, "Current Thread State:");
				ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
				for(ThreadInfo thread: threads) {
					log.log(Level.SEVERE, "------------------------------");
					log.log(Level.SEVERE, "Current Thread: " + thread.getThreadName());
					log.log(Level.SEVERE, "\tPID: " + thread.getThreadId()
							+ " | Suspended: " + thread.isSuspended()
							+ " | Native: " + thread.isInNative()
							+ " | State: " + thread.getThreadState());
					if(thread.getLockedMonitors().length != 0) {
						log.log(Level.SEVERE, "\tThread is waiting on monitor(s):");
						for(MonitorInfo monitor: thread.getLockedMonitors()) {
							log.log(Level.SEVERE, "\t\tLocked on:" + monitor.getLockedStackFrame());
						}
					}
					if(thread.getThreadState() == Thread.State.WAITING) {
						log.log(Level.SEVERE, "\tWAITING ON: " + thread.getLockInfo().toString());
					}
					log.log(Level.SEVERE, "\tStack:");
					StackTraceElement[] stack = thread.getStackTrace();
					for(int line = 0; line < stack.length; line++) {
						log.log(Level.SEVERE, "\t\t" + stack[line].toString());
					}
				}
				log.log(Level.SEVERE, "------------------------------");
				if(!toggledByCommand) {
					break;
				}
				toggledByCommand = false;
			}
			try {
				Thread.sleep(10000);
			} catch(Exception e) {}
		}
	}
}
