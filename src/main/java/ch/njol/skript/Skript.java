/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Copyright 2011-2017 Peter Güttinger and contributors
 */
package ch.njol.skript;

import ch.njol.skript.Updater.UpdateState;
import ch.njol.skript.aliases.Aliases;
import ch.njol.skript.bukkitutil.Workarounds;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Comparator;
import ch.njol.skript.classes.Converter;
import ch.njol.skript.classes.data.*;
import ch.njol.skript.command.Commands;
import ch.njol.skript.doc.Documentation;
import ch.njol.skript.events.EvtSkript;
import ch.njol.skript.hooks.Hook;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.function.Functions;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Message;
import ch.njol.skript.log.*;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.Comparators;
import ch.njol.skript.registrations.Converters;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.timings.SkriptTimings;
import ch.njol.skript.util.*;
import ch.njol.skript.util.chat.ChatMessages;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Closeable;
import ch.njol.util.Kleenean;
import ch.njol.util.NullableChecker;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.util.coll.iterator.CheckedIterator;
import ch.njol.util.coll.iterator.EnumerationIterable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

// TODO meaningful error if someone uses an %expression with percent signs% outside of text or a variable

/**
 * <b>Skript</b> - A Bukkit plugin to modify how Minecraft behaves without having to write a single line of code (You'll likely be writing some code though if you're reading this
 * =P)
 * <p>
 * Use this class to extend this plugin's functionality by adding more {@link Condition conditions}, {@link Effect effects}, {@link SimpleExpression expressions}, etc.
 * <p>
 * If your plugin.yml contains <tt>'depend: [Skript]'</tt> then your plugin will not start at all if Skript is not present. Add <tt>'softdepend: [Skript]'</tt> to your plugin.yml
 * if you want your plugin to work even if Skript isn't present, but want to make sure that Skript gets loaded before your plugin.
 * <p>
 * If you use 'softdepend' you can test whether Skript is loaded with <tt>'Bukkit.getPluginManager().getPlugin(&quot;Skript&quot;) != null'</tt>
 * <p>
 * Once you made sure that Skript is loaded you can use <code>Skript.getInstance()</code> whenever you need a reference to the plugin, but you likely won't need it since all API
 * methods are static.
 * 
 * @author Peter Güttinger
 * @see #registerAddon(JavaPlugin)
 * @see #registerCondition(Class, String...)
 * @see #registerEffect(Class, String...)
 * @see #registerExpression(Class, Class, ExpressionType, String...)
 * @see #registerEvent(String, Class, Class, String...)
 * @see EventValues#registerEventValue(Class, Class, Getter, int)
 * @see Classes#registerClass(ClassInfo)
 * @see Comparators#registerComparator(Class, Class, Comparator)
 * @see Converters#registerConverter(Class, Class, Converter)
 */
public final class Skript extends JavaPlugin implements Listener {
	
	// ================ PLUGIN ================
	
	@Nullable
	private static Skript instance = null;
	
	private static boolean disabled = false;
	
	public static Skript getInstance() {
		final Skript i = instance;
		if (i == null)
			throw new IllegalStateException();
		return i;
	}
	
	public Skript() throws IllegalStateException {
		if (instance != null)
			throw new IllegalStateException("Cannot create multiple instances of Skript!");
		instance = this;
	}
	
	@Nullable
	private static Version version = null;
	
	public static Version getVersion() {
		final Version v = version;
		if (v == null)
			throw new IllegalStateException();
		return v;
	}
	
	public final static Message m_invalid_reload = new Message("skript.invalid reload"),
			m_finished_loading = new Message("skript.finished loading");
	
	public static ServerPlatform getServerPlatform() {
		if (classExists("co.aikar.timings.Timings")) {
			return ServerPlatform.BUKKIT_PAPER; // Could be Sponge, but it doesn't work at all at the moment
		} else if (classExists("org.spigotmc.SpigotConfig")) {
			if (classExists("java.net.glowstone.GlowServer")) {
				return ServerPlatform.BUKKIT_GLOWSTONE;
			} else {
				return ServerPlatform.BUKKIT_SPIGOT;
			}
		} else if (classExists("org.bukkit.craftbukkit.CraftServer")) {
			return ServerPlatform.BUKKIT_CRAFTBUKKIT;
		} else {
			return ServerPlatform.BUKKIT_UNKNOWN;
		}
	}
	
	@Override
	public void onLoad() {
		ServerPlatform platform = getServerPlatform();
		if (!platform.works) {
			Skript.error("It seems that this server platform (" + platform.name + ") does not work with Skript.");
			if (SkriptConfig.allowUnsafePlatforms.value()) {
				Skript.error("However, you have chosen to ignore this. Skript will probably still not work.");
			} else {
				Skript.error("To prevent potentially unsafe behaviour, Skript has been disabled.");
				Skript.error("You may re-enable it by adding a configuration option 'allow unsafe platforms: true'");
				Skript.error("Note that it is unlikely that Skript works correctly even if you do so.");
				Skript.error("A better idea would be to install Paper or Spigot in place of your current server.");
				getPluginLoader().disablePlugin(this); // Really disable Skript now
			}
		} else if (!platform.supported) {
			Skript.warning("This server platform (" + platform.name + ") is not supported by Skript.");
			Skript.warning("It will still probably work, but if it does not, you are on your own.");
			Skript.warning("Skript officially supports Paper and Spigot.");
		}
	}
	
	@Override
	public void onEnable() {
		if (disabled) {
			Skript.error(m_invalid_reload.toString());
			setEnabled(false);
			return;
		}
		



		ChatMessages.registerListeners();
		Language.loadDefault(getAddonInstance());
		
		Workarounds.init();
		
		version = new Version("" + getDescription().getVersion());
		runningCraftBukkit = Bukkit.getServer().getClass().getName().equals("org.bukkit.craftbukkit.CraftServer");
		runningSpigot = classExists("net.md_5.bungee.api.chat.TextComponent"); // Check for Bungee's chat
		runningPaper = classExists("co.aikar.timings.Timings"); // Check for Paper timings
		final String bukkitV = Bukkit.getBukkitVersion();
		final Matcher m = Pattern.compile("\\d+\\.\\d+(\\.\\d+)?").matcher(bukkitV);
		if (!m.find()) {
			Skript.error("The Bukkit version '" + bukkitV + "' does not contain a version number which is required for Skript to enable or disable certain features. " +
					"Skript will still work, but you might get random errors if you use features that are not available in your version of Bukkit.");
			minecraftVersion = new Version(666, 0, 0);
		} else {
			minecraftVersion = new Version("" + m.group());
		}

		Skript.info("CUSTOM GATT VERSION LOADED");

		if (!getDataFolder().isDirectory())
			getDataFolder().mkdirs();
		
		final File scripts = new File(getDataFolder(), SCRIPTSFOLDER);
		if (!scripts.isDirectory()) {
			ZipFile f = null;
			try {
				if (!scripts.mkdirs())
					throw new IOException("Could not create the directory " + scripts);
				f = new ZipFile(getFile());
				for (final ZipEntry e : new EnumerationIterable<ZipEntry>(f.entries())) {
					if (e.isDirectory())
						continue;
					File saveTo = null;
					if (e.getName().startsWith(SCRIPTSFOLDER + "/")) {
						final String fileName = e.getName().substring(e.getName().lastIndexOf('/') + 1);
						saveTo = new File(scripts, (fileName.startsWith("-") ? "" : "-") + fileName);
					} else if (e.getName().equals("config.sk")) {
						final File cf = new File(getDataFolder(), e.getName());
						if (!cf.exists())
							saveTo = cf;
					} else if (e.getName().startsWith("aliases-") && e.getName().endsWith(".sk") && !e.getName().contains("/")) {
						final File af = new File(getDataFolder(), e.getName());
						if (!af.exists())
							saveTo = af;
					} else if (e.getName().startsWith("features.sk")) {
						final File af = new File(getDataFolder(), e.getName());
						if (!af.exists())
							saveTo = af;
					}
					if (saveTo != null) {
						final InputStream in = f.getInputStream(e);
						try {
							assert in != null;
							FileUtils.save(in, saveTo);
						} finally {
							in.close();
						}
					}
				}
				info("Successfully generated the config, the example scripts and the aliases files.");
			} catch (final ZipException e) {} catch (final IOException e) {
				error("Error generating the default files: " + ExceptionUtils.toString(e));
			} finally {
				if (f != null) {
					try {
						f.close();
					} catch (final IOException e) {}
				}
			}
		}
		
		
		getCommand("skript").setExecutor(new SkriptCommand());
		
		new JavaClasses();
		new BukkitClasses();
		new BukkitEventValues();
		new SkriptClasses();
		
		new DefaultComparators();
		new DefaultConverters();
		new DefaultFunctions();
		
		
		try {
			getAddonInstance().loadClasses("ch.njol.skript", "conditions", "effects", "events", "expressions", "entity");
		} catch (final Exception e) {
			exception(e, "Could not load required .class files: " + e.getLocalizedMessage());
			setEnabled(false);
			return;
		}
		
		SkriptConfig.load();
		Language.setUseLocal(true);
		
		if (SkriptConfig.checkForNewVersion.value()) // We only start updater automatically if it was asked
			Updater.start();
		
		Aliases.load();
		
		Commands.registerListeners();
		
		if (logNormal())
			info(" " + Language.get("skript.copyright"));
		
		final long tick = testing() ? Bukkit.getWorlds().get(0).getFullTime() : 0;
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void run() {
				assert Bukkit.getWorlds().get(0).getFullTime() == tick;
				
				// load hooks
				try {
					final JarFile jar = new JarFile(getFile());
					try {
						for (final JarEntry e : new EnumerationIterable<>(jar.entries())) {
							if (e.getName().startsWith("ch/njol/skript/hooks/") && e.getName().endsWith("Hook.class") && StringUtils.count("" + e.getName(), '/') <= 5) {
								final String c = e.getName().replace('/', '.').substring(0, e.getName().length() - ".class".length());
								try {
									final Class<?> hook = Class.forName(c, true, getClassLoader());
									if (hook != null && Hook.class.isAssignableFrom(hook) && !hook.isInterface() && Hook.class != hook) {
										hook.getDeclaredConstructor().setAccessible(true);
										hook.getDeclaredConstructor().newInstance();
									}
								} catch (final ClassNotFoundException ex) {
									Skript.exception(ex, "Cannot load class " + c);
								} catch (final ExceptionInInitializerError err) {
									Skript.exception(err.getCause(), "Class " + c + " generated an exception while loading");
								}
								continue;
							}
						}
					} finally {
						try {
							jar.close();
						} catch (final IOException e) {}
					}
				} catch (final Exception e) {
					error("Error while loading plugin hooks" + (e.getLocalizedMessage() == null ? "" : ": " + e.getLocalizedMessage()));
					Skript.exception(e);
				}
				
				Language.setUseLocal(false);
				
				stopAcceptingRegistrations();
				
				
				Documentation.generate(); // TODO move to test classes?
				
				if (logNormal())
					info("Loading variables...");
				final long vls = System.currentTimeMillis();
				
				final LogHandler h = SkriptLogger.startLogHandler(new ErrorDescLogHandler() {
//					private final List<LogEntry> log = new ArrayList<LogEntry>();
					
					@Override
					public LogResult log(final LogEntry entry) {
						super.log(entry);
						if (entry.level.intValue() >= Level.SEVERE.intValue()) {
							logEx(entry.message); // no [Skript] prefix
							return LogResult.DO_NOT_LOG;
						} else {
//							log.add(entry);
//							return LogResult.CACHED;
							return LogResult.LOG;
						}
					}
					
					@Override
					protected void beforeErrors() {
						logEx();
						logEx("===!!!=== Skript variable load error ===!!!===");
						logEx("Unable to load (all) variables:");
					}
					
					@Override
					protected void afterErrors() {
						logEx();
						logEx("Skript will work properly, but old variables might not be available at all and new ones may or may not be saved until Skript is able to create a backup of the old file and/or is able to connect to the database (which requires a restart of Skript)!");
						logEx();
					}
					
					@Override
					protected void onStop() {
						super.onStop();
//						SkriptLogger.logAll(log);
					}
				});
				final CountingLogHandler c = SkriptLogger.startLogHandler(new CountingLogHandler(SkriptLogger.SEVERE));
				try {
					if (!Variables.load())
						if (c.getCount() == 0)
							error("(no information available)");
				} finally {
					c.stop();
					h.stop();
				}
				
				final long vld = System.currentTimeMillis() - vls;
				if (logNormal())
					info("Loaded " + Variables.numVariables() + " variables in " + ((vld / 100) / 10.) + " seconds");
				
				ScriptLoader.loadScripts();
				
				Skript.info(m_finished_loading.toString());
				
				EvtSkript.onSkriptStart();
				
				final Metrics metrics = new Metrics(Skript.this);
				
				metrics.addCustomChart(new Metrics.SimplePie("pluginLanguage") {
					
					@Override
					public String getValue() {
						return Language.getName();
					}
				});
				metrics.addCustomChart(new Metrics.SimplePie("effectCommands") {
					
					@Override
					public String getValue() {
						return "" + SkriptConfig.enableEffectCommands.value();
					}
				});
				metrics.addCustomChart(new Metrics.SimplePie("uuidsWithPlayers") {
					
					@Override
					public String getValue() {
						return "" + SkriptConfig.usePlayerUUIDsInVariableNames.value();
					}
				});
				metrics.addCustomChart(new Metrics.SimplePie("playerVariableFix") {
					
					@Override
					public String getValue() {
						return "" + SkriptConfig.enablePlayerVariableFix.value();
					}
				});
				metrics.addCustomChart(new Metrics.SimplePie("logVerbosity") {
					
					@Override
					public String getValue() {
						return "" + SkriptConfig.verbosity.value().name().toLowerCase().replace('_', ' ');
					}
				});
				metrics.addCustomChart(new Metrics.SimplePie("pluginPriority") {
					
					@Override
					public String getValue() {
						return "" + SkriptConfig.defaultEventPriority.value().name().toLowerCase().replace('_', ' ');
					}
				});
				metrics.addCustomChart(new Metrics.SimplePie("logPlayerCommands") {
					
					@Override
					public String getValue() {
						return "" + SkriptConfig.logPlayerCommands.value();
					}
				});
				metrics.addCustomChart(new Metrics.SimplePie("maxTargetDistance") {
					
					@Override
					public String getValue() {
						return "" + SkriptConfig.maxTargetBlockDistance.value();
					}
				});
				metrics.addCustomChart(new Metrics.SimplePie("softApiExceptions") {
					
					@Override
					public String getValue() {
						return "" + SkriptConfig.apiSoftExceptions.value();
					}
				});
				metrics.addCustomChart(new Metrics.SimplePie("timingsStatus") {
					
					@Override
					public String getValue() {
						if (!Skript.classExists("co.aikar.timings.Timings"))
							return "unsupported";
						else
							return "" + SkriptConfig.enableTimings.value();
					}
				});
				metrics.addCustomChart(new Metrics.SimplePie("parseLinks") {
					
					@Override
					public String getValue() {
						return "" + ChatMessages.linkParseMode.name().toLowerCase();
					}
				});
				metrics.addCustomChart(new Metrics.SimplePie("colorResetCodes") {
					
					@Override
					public String getValue() {
						return "" + SkriptConfig.colorResetCodes.value();
					}
				});
				
				Skript.metrics = metrics;
				
				// suppresses the "can't keep up" warning after loading all scripts
				final Filter f = new Filter() {
					@Override
					public boolean isLoggable(final @Nullable LogRecord record) {
						if (record == null)
							return false;
						if (record.getMessage() != null && record.getMessage().toLowerCase().startsWith("can't keep up!"))
							return false;
						return true;
					}
				};
				BukkitLoggerFilter.addFilter(f);
				Bukkit.getScheduler().scheduleSyncDelayedTask(Skript.this, new Runnable() {
					@Override
					public void run() {
						BukkitLoggerFilter.removeFilter(f);
					}
				}, 1);
			}
		});
		
		Bukkit.getPluginManager().registerEvents(new Listener() {
			@EventHandler
			public void onJoin(final PlayerJoinEvent e) {
				if (e.getPlayer().hasPermission("skript.admin")) {
					new Task(Skript.this, 0) {
						@SuppressWarnings("incomplete-switch")
						@Override
						public void run() {
							Player p = e.getPlayer();
							if (p == null)
								return;
							
							switch (Updater.state) {
								case UPDATE_AVAILABLE:
									Skript.info(p, "" + Updater.m_update_available);
									break;
								case DOWNLOADED:
									Skript.info(p, "" + Updater.m_downloaded);
							}
						}
					};
				}
			}
		}, this);
		
		// Tell Timings that we are here!
		SkriptTimings.setSkript(this);
	}
	
	private static Version minecraftVersion = new Version(666);
	private static boolean runningCraftBukkit = false;
	private static boolean runningSpigot = false;
	private static boolean runningPaper = false;
	
	public static Version getMinecraftVersion() {
		return minecraftVersion;
	}
	
	/**
	 * @return Whether this server is running CraftBukkit
	 */
	public static boolean isRunningCraftBukkit() {
		return runningCraftBukkit;
	}
	
	/**
	 * @return Whether this server is running Minecraft <tt>major.minor</tt> or higher
	 */
	public static boolean isRunningMinecraft(final int major, final int minor) {
		return minecraftVersion.compareTo(major, minor) >= 0;
	}
	
	public static boolean isRunningMinecraft(final int major, final int minor, final int revision) {
		return minecraftVersion.compareTo(major, minor, revision) >= 0;
	}
	
	public static boolean isRunningMinecraft(final Version v) {
		return minecraftVersion.compareTo(v) >= 0;
	}
	
	/**
	 * Used to test whether certain Bukkit features are supported.
	 * 
	 * @param className
	 * @return Whether the given class exists.
	 * @deprecated use {@link #classExists(String)}
	 */
	@Deprecated
	public final static boolean supports(final String className) {
		return classExists(className);
	}
	
	/**
	 * Tests whether a given class exists in the classpath.
	 * 
	 * @param className The {@link Class#getCanonicalName() canonical name} of the class
	 * @return Whether the given class exists.
	 */
	public final static boolean classExists(final String className) {
		try {
			Class.forName(className);
			return true;
		} catch (final ClassNotFoundException e) {
			return false;
		}
	}
	
	/**
	 * Tests whether a method exists in the given class.
	 * 
	 * @param c The class
	 * @param methodName The name of the method
	 * @param parameterTypes The parameter types of the method
	 * @return Whether the given method exists.
	 */
	public final static boolean methodExists(final Class<?> c, final String methodName, final Class<?>... parameterTypes) {
		try {
			c.getDeclaredMethod(methodName, parameterTypes);
			return true;
		} catch (final NoSuchMethodException e) {
			return false;
		} catch (final SecurityException e) {
			return false;
		}
	}
	
	/**
	 * Tests whether a method exists in the given class, and whether the return type matches the expected one.
	 * <p>
	 * Note that this method doesn't work properly if multiple methods with the same name and parameters exist but have different return types.
	 * 
	 * @param c The class
	 * @param methodName The name of the method
	 * @param parameterTypes The parameter types of the method
	 * @param returnType The expected return type
	 * @return Whether the given method exists.
	 */
	public final static boolean methodExists(final Class<?> c, final String methodName, final Class<?>[] parameterTypes, final Class<?> returnType) {
		try {
			final Method m = c.getDeclaredMethod(methodName, parameterTypes);
			return m.getReturnType() == returnType;
		} catch (final NoSuchMethodException e) {
			return false;
		} catch (final SecurityException e) {
			return false;
		}
	}
	
	/**
	 * Tests whether a field exists in the given class.
	 * 
	 * @param c The class
	 * @param fieldName The name of the field
	 * @return Whether the given field exists.
	 */
	public final static boolean fieldExists(final Class<?> c, final String fieldName) {
		try {
			c.getDeclaredField(fieldName);
			return true;
		} catch (final NoSuchFieldException e) {
			return false;
		} catch (final SecurityException e) {
			return false;
		}
	}
	
	@Nullable
	static Metrics metrics;
	
	@Nullable
	public static Metrics getMetrics() {
		return metrics;
	}
	
	/**
	 * Clears triggers, commands, functions and variable names
	 */
	final static void disableScripts() {
		VariableString.variableNames.clear();
		SkriptEventHandler.removeAllTriggers();
		Commands.clearCommands();
		Functions.clearFunctions();
	}
	
	/**
	 * Prints errors from reloading the config & scripts
	 */
	final static void reload() {
		if (!ScriptLoader.loadAsync)
			disableScripts();
		reloadMainConfig();
		reloadAliases();
		ScriptLoader.loadScripts();
	}
	
	/**
	 * Prints errors
	 */
	final static void reloadScripts() {
		if (!ScriptLoader.loadAsync)
			disableScripts();
		ScriptLoader.loadScripts();
	}
	
	/**
	 * Prints errors
	 */
	final static void reloadMainConfig() {
		SkriptConfig.load();
	}
	
	/**
	 * Prints errors
	 */
	final static void reloadAliases() {
		Aliases.clear();
		Aliases.load();
	}
	
	@SuppressWarnings("null")
	private final static Collection<Closeable> closeOnDisable = Collections.synchronizedCollection(new ArrayList<Closeable>());
	
	/**
	 * Registers a Closeable that should be closed when this plugin is disabled.
	 * <p>
	 * All registered Closeables will be closed after all scripts have been stopped.
	 * 
	 * @param closeable
	 */
	public static void closeOnDisable(final Closeable closeable) {
		closeOnDisable.add(closeable);
	}
	
	@Override
	public void onDisable() {
		if (disabled)
			return;
		disabled = true;
		
		EvtSkript.onSkriptStop(); // TODO [code style] warn user about delays in Skript stop events
		
		disableScripts();
		
		Bukkit.getScheduler().cancelTasks(this);
		
		for (final Closeable c : closeOnDisable) {
			try {
				c.close();
			} catch (final Exception e) {
				Skript.exception(e, "An error occurred while shutting down.", "This might or might not cause any issues.");
			}
		}
		
		// unset static fields to prevent memory leaks as Bukkit reloads the classes with a different classloader on reload
		// async to not slow down server reload, delayed to not slow down server shutdown
		final Thread t = newThread(new Runnable() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void run() {
				try {
					Thread.sleep(10000);
				} catch (final InterruptedException e) {}
				try {
					final Field modifiers = Field.class.getDeclaredField("modifiers");
					modifiers.setAccessible(true);
					final JarFile jar = new JarFile(getFile());
					try {
						for (final JarEntry e : new EnumerationIterable<>(jar.entries())) {
							if (e.getName().endsWith(".class")) {
								try {
									final Class<?> c = Class.forName(e.getName().replace('/', '.').substring(0, e.getName().length() - ".class".length()), false, getClassLoader());
									for (final Field f : c.getDeclaredFields()) {
										if (Modifier.isStatic(f.getModifiers()) && !f.getType().isPrimitive()) {
											if (Modifier.isFinal(f.getModifiers())) {
												modifiers.setInt(f, f.getModifiers() & ~Modifier.FINAL);
											}
											f.setAccessible(true);
											f.set(null, null);
										}
									}
								} catch (final Throwable ex) {
									if (testing())
										ex.printStackTrace();
								}
							}
						}
					} finally {
						jar.close();
					}
				} catch (final Throwable ex) {
					if (testing())
						ex.printStackTrace();
				}
			}
		}, "Skript cleanup thread");
		t.setPriority(Thread.MIN_PRIORITY);
		t.setDaemon(true);
		t.start();
	}
	
	// ================ CONSTANTS, OPTIONS & OTHER ================
	
	public final static String SCRIPTSFOLDER = "scripts";
	
	public static void outdatedError() {
		error("Skript v" + getInstance().getDescription().getVersion() + " is not fully compatible with Bukkit " + Bukkit.getVersion() + ". Some feature(s) will be broken until you update Skript.");
	}
	
	public static void outdatedError(final Exception e) {
		outdatedError();
		if (testing())
			e.printStackTrace();
	}
	
	/**
	 * A small value, useful for comparing doubles or floats.
	 * <p>
	 * E.g. to test whether two floating-point numbers are equal:
	 * 
	 * <pre>
	 * Math.abs(a - b) &lt; Skript.EPSILON
	 * </pre>
	 * 
	 * or whether a location is within a specific radius of another location:
	 * 
	 * <pre>
	 * location.distanceSquared(center) - radius * radius &lt; Skript.EPSILON
	 * </pre>
	 * 
	 * @see #EPSILON_MULT
	 */
	public final static double EPSILON = 1e-10;
	/**
	 * A value a bit larger than 1
	 * 
	 * @see #EPSILON
	 */
	public final static double EPSILON_MULT = 1.00001;
	
	/**
	 * The maximum ID a block can have in Minecraft.
	 */
	public final static int MAXBLOCKID = 255;
	/**
	 * The maximum data value of Minecraft, i.e. Short.MAX_VALUE - Short.MIN_VALUE.
	 */
	public final static int MAXDATAVALUE = Short.MAX_VALUE - Short.MIN_VALUE;
	
	// TODO localise Infinity, -Infinity, NaN (and decimal point?)
	public final static String toString(final double n) {
		return StringUtils.toString(n, SkriptConfig.numberAccuracy.value());
	}
	
	public final static UncaughtExceptionHandler UEH = new UncaughtExceptionHandler() {
		@Override
		public void uncaughtException(final @Nullable Thread t, final @Nullable Throwable e) {
			Skript.exception(e, "Exception in thread " + (t == null ? null : t.getName()));
		}
	};
	
	/**
	 * Creates a new Thread and sets its UncaughtExceptionHandler. The Thread is not started automatically.
	 */
	public final static Thread newThread(final Runnable r, final String name) {
		final Thread t = new Thread(r, name);
		t.setUncaughtExceptionHandler(UEH);
		return t;
	}
	
	// ================ REGISTRATIONS ================
	
	private static boolean acceptRegistrations = true;
	
	public static boolean isAcceptRegistrations() {
		return acceptRegistrations;
	}
	
	public static void checkAcceptRegistrations() {
		if (!acceptRegistrations)
			throw new SkriptAPIException("Registering is disabled after initialisation!");
	}
	
	private static void stopAcceptingRegistrations() {
		acceptRegistrations = false;
		
		Converters.createMissingConverters();
		
		Classes.onRegistrationsStop();
	}
	
	// ================ ADDONS ================
	
	private final static HashMap<String, SkriptAddon> addons = new HashMap<>();
	
	/**
	 * Registers an addon to Skript. This is currently not required for addons to work, but the returned {@link SkriptAddon} provides useful methods for registering syntax elements
	 * and adding new strings to Skript's localization system (e.g. the required "types.[type]" strings for registered classes).
	 * 
	 * @param p The plugin
	 */
	public static SkriptAddon registerAddon(final JavaPlugin p) {
		checkAcceptRegistrations();
		if (addons.containsKey(p.getName()))
			throw new IllegalArgumentException("The plugin " + p.getName() + " is already registered");
		final SkriptAddon addon = new SkriptAddon(p);
		addons.put(p.getName(), addon);
		return addon;
	}
	
	@Nullable
	public static SkriptAddon getAddon(final JavaPlugin p) {
		return addons.get(p.getName());
	}
	
	@Nullable
	public static SkriptAddon getAddon(final String name) {
		return addons.get(name);
	}
	
	@SuppressWarnings("null")
	public static Collection<SkriptAddon> getAddons() {
		return Collections.unmodifiableCollection(addons.values());
	}
	
	@Nullable
	private static SkriptAddon addon;
	
	/**
	 * @return A {@link SkriptAddon} representing Skript.
	 */
	public static SkriptAddon getAddonInstance() {
		final SkriptAddon a = addon;
		if (a == null)
			return addon = new SkriptAddon(Skript.getInstance())
					.setLanguageFileDirectory("lang");
		else
			return a;
	}
	
	// ================ CONDITIONS & EFFECTS ================
	
	private final static Collection<SyntaxElementInfo<? extends Condition>> conditions = new ArrayList<>(500);
	private final static Collection<SyntaxElementInfo<? extends Effect>> effects = new ArrayList<>(500);
	private final static Collection<SyntaxElementInfo<? extends Statement>> statements = new ArrayList<>(1000);
	
	/**
	 * registers a {@link Condition}.
	 * 
	 * @param condition The condition's class
	 * @param patterns Skript patterns to match this condition
	 */
	public static <E extends Condition> void registerCondition(final Class<E> condition, final String... patterns) throws IllegalArgumentException {
		checkAcceptRegistrations();
		final SyntaxElementInfo<E> info = new SyntaxElementInfo<>(patterns, condition);
		conditions.add(info);
		statements.add(info);
	}
	
	/**
	 * Registers an {@link Effect}.
	 * 
	 * @param effect The effect's class
	 * @param patterns Skript patterns to match this effect
	 */
	public static <E extends Effect> void registerEffect(final Class<E> effect, final String... patterns) throws IllegalArgumentException {
		checkAcceptRegistrations();
		final SyntaxElementInfo<E> info = new SyntaxElementInfo<>(patterns, effect);
		effects.add(info);
		statements.add(info);
	}
	
	public static Collection<SyntaxElementInfo<? extends Statement>> getStatements() {
		return statements;
	}
	
	public static Collection<SyntaxElementInfo<? extends Condition>> getConditions() {
		return conditions;
	}
	
	public static Collection<SyntaxElementInfo<? extends Effect>> getEffects() {
		return effects;
	}
	
	// ================ EXPRESSIONS ================
	
	private final static List<ExpressionInfo<?, ?>> expressions = new ArrayList<>(100);
	
	private final static int[] expressionTypesStartIndices = new int[ExpressionType.values().length];
	
	/**
	 * Registers an expression.
	 * 
	 * @param c The expression's class
	 * @param returnType The superclass of all values returned by the expression
	 * @param type The expression's {@link ExpressionType type}. This is used to determine in which order to try to parse expressions.
	 * @param patterns Skript patterns that match this expression
	 * @throws IllegalArgumentException if returnType is not a normal class
	 */
	public static <E extends Expression<T>, T> void registerExpression(final Class<E> c, final Class<T> returnType, final ExpressionType type, final String... patterns) throws IllegalArgumentException {
		checkAcceptRegistrations();
		if (returnType.isAnnotation() || returnType.isArray() || returnType.isPrimitive())
			throw new IllegalArgumentException("returnType must be a normal type");
		final ExpressionInfo<E, T> info = new ExpressionInfo<>(patterns, returnType, c);
		for (int i = type.ordinal() + 1; i < ExpressionType.values().length; i++) {
			expressionTypesStartIndices[i]++;
		}
		expressions.add(expressionTypesStartIndices[type.ordinal()], info);
	}
	
	@SuppressWarnings("null")
	public static Iterator<ExpressionInfo<?, ?>> getExpressions() {
		return expressions.iterator();
	}
	
	public static Iterator<ExpressionInfo<?, ?>> getExpressions(final Class<?>... returnTypes) {
		return new CheckedIterator<>(getExpressions(), new NullableChecker<ExpressionInfo<?, ?>>() {
			@Override
			public boolean check(final @Nullable ExpressionInfo<?, ?> i) {
				if (i == null || i.returnType == Object.class)
					return true;
				for (final Class<?> returnType : returnTypes) {
					assert returnType != null;
					if (Converters.converterExists(i.returnType, returnType))
						return true;
				}
				return false;
			}
		});
	}
	
	// ================ EVENTS ================
	
	private final static Collection<SkriptEventInfo<?>> events = new ArrayList<>(50);
	
	/**
	 * Registers an event.
	 * 
	 * @param name Capitalised name of the event without leading "On" which is added automatically (Start the name with an asterisk to prevent this). Used for error messages and
	 *            the documentation.
	 * @param c The event's class
	 * @param event The Bukkit event this event applies to
	 * @param patterns Skript patterns to match this event
	 * @return A SkriptEventInfo representing the registered event. Used to generate Skript's documentation.
	 */
	@SuppressWarnings({"unchecked"})
	public static <E extends SkriptEvent> SkriptEventInfo<E> registerEvent(final String name, final Class<E> c, final Class<? extends Event> event, final String... patterns) {
		checkAcceptRegistrations();
		final SkriptEventInfo<E> r = new SkriptEventInfo<>(name, patterns, c, CollectionUtils.array(event));
		events.add(r);
		return r;
	}
	
	/**
	 * Registers an event.
	 * 
	 * @param name The name of the event, used for error messages
	 * @param c The event's class
	 * @param events The Bukkit events this event applies to
	 * @param patterns Skript patterns to match this event
	 * @return A SkriptEventInfo representing the registered event. Used to generate Skript's documentation.
	 */
	public static <E extends SkriptEvent> SkriptEventInfo<E> registerEvent(final String name, final Class<E> c, final Class<? extends Event>[] events, final String... patterns) {
		checkAcceptRegistrations();
		final SkriptEventInfo<E> r = new SkriptEventInfo<>(name, patterns, c, events);
		Skript.events.add(r);
		return r;
	}
	
	public final static Collection<SkriptEventInfo<?>> getEvents() {
		return events;
	}
	
	// ================ COMMANDS ================
	
	/**
	 * Dispatches a command with calling command events
	 * 
	 * @param sender
	 * @param command
	 * @return Whether the command was run
	 */
	public final static boolean dispatchCommand(final CommandSender sender, final String command) {
		try {
			if (sender instanceof Player) {
				final PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent((Player) sender, "/" + command);
				Bukkit.getPluginManager().callEvent(e);
				if (e.isCancelled() || e.getMessage() == null || !e.getMessage().startsWith("/"))
					return false;
				return Bukkit.dispatchCommand(e.getPlayer(), e.getMessage().substring(1));
			} else {
				final ServerCommandEvent e = new ServerCommandEvent(sender, command);
				Bukkit.getPluginManager().callEvent(e);
				if (e.getCommand() == null || e.getCommand().isEmpty())
					return false;
				return Bukkit.dispatchCommand(e.getSender(), e.getCommand());
			}
		} catch (final Exception ex) {
			ex.printStackTrace(); // just like Bukkit
			return false;
		}
	}
	
	// ================ LOGGING ================
	
	public final static boolean logNormal() {
		return SkriptLogger.log(Verbosity.NORMAL);
	}
	
	public final static boolean logHigh() {
		return SkriptLogger.log(Verbosity.HIGH);
	}
	
	public final static boolean logVeryHigh() {
		return SkriptLogger.log(Verbosity.VERY_HIGH);
	}
	
	public final static boolean debug() {
		return SkriptLogger.debug();
	}
	
	public final static boolean testing() {
		return debug() || Skript.class.desiredAssertionStatus();
	}
	
	public final static boolean log(final Verbosity minVerb) {
		return SkriptLogger.log(minVerb);
	}
	
	public static void debug(final String info) {
		if (!debug())
			return;
		SkriptLogger.log(SkriptLogger.DEBUG, info);
	}
	
	/**
	 * @see SkriptLogger#log(Level, String)
	 */
	@SuppressWarnings("null")
	public static void info(final String info) {
		SkriptLogger.log(Level.INFO, info);
	}
	
	/**
	 * @see SkriptLogger#log(Level, String)
	 */
	@SuppressWarnings("null")
	public static void warning(final String warning) {
		SkriptLogger.log(Level.WARNING, warning);
	}
	
	/**
	 * @see SkriptLogger#log(Level, String)
	 */
	@SuppressWarnings("null")
	public static void error(final @Nullable String error) {
		if (error != null)
			SkriptLogger.log(Level.SEVERE, error);
	}
	
	/**
	 * Use this in {@link Expression#init(Expression[], int, Kleenean, ch.njol.skript.lang.SkriptParser.ParseResult)} (and other methods that are called during the parsing) to log
	 * errors with a specific {@link ErrorQuality}.
	 * 
	 * @param error
	 * @param quality
	 */
	public static void error(final String error, final ErrorQuality quality) {
		SkriptLogger.log(new LogEntry(SkriptLogger.SEVERE, quality, error));
	}
	
	private final static String EXCEPTION_PREFIX = "#!#! ";
	
	/**
	 * Used if something happens that shouldn't happen
	 * 
	 * @param info Description of the error and additional information
	 * @return an EmptyStacktraceException to throw if code execution should terminate.
	 */
	public final static EmptyStacktraceException exception(final String... info) {
		return exception(null, info);
	}
	
	public final static EmptyStacktraceException exception(final @Nullable Throwable cause, final String... info) {
		return exception(cause, null, null, info);
	}
	
	public final static EmptyStacktraceException exception(final @Nullable Throwable cause, final @Nullable Thread thread, final String... info) {
		return exception(cause, thread, null, info);
	}
	
	public final static EmptyStacktraceException exception(final @Nullable Throwable cause, final @Nullable TriggerItem item, final String... info) {
		return exception(cause, null, item, info);
	}
	
	/**
	 * Maps Java packages of plugins to descriptions of said plugins.
	 * This is only done for plugins that depend or soft-depend on Skript.
	 */
	private static Map<String, PluginDescriptionFile> pluginPackages = new HashMap<>();
	private static boolean checkedPlugins = false;
	
	/**
	 * Used if something happens that shouldn't happen
	 * 
	 * @param cause exception that shouldn't occur
	 * @param info Description of the error and additional information
	 * @return an EmptyStacktraceException to throw if code execution should terminate.
	 */
	public final static EmptyStacktraceException exception(@Nullable Throwable cause, final @Nullable Thread thread, final @Nullable TriggerItem item, final String... info) {
		
		// First error: gather plugin package information
		if (!checkedPlugins) { 
			for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
				if (plugin.getName().equals("Skript")) // Don't track myself!
					continue;
				
				PluginDescriptionFile desc = plugin.getDescription();
				if (desc.getDepend().contains("Skript") || desc.getSoftDepend().contains("Skript")) {
					// Take actual main class out from the qualified name
					String[] parts = desc.getMain().split("\\."); // . is special in regexes...
					StringBuilder name = new StringBuilder(desc.getMain().length());
					for (int i = 0; i < parts.length - 1; i++) {
						name.append(parts[i]).append('.');
					}
					
					// Put this to map
					pluginPackages.put(name.toString(), desc);
					if (Skript.debug())
						Skript.info("Identified potential addon: " + desc.getFullName() + " (" + name.toString() + ")");
				}
			}
			
			checkedPlugins = true; // No need to do this next time
		}
		
		String issuesUrl = "https://github.com/bensku/Skript/issues";
		
		logEx();
		logEx("[Skript] Severe Error:");
		logEx(info);
		logEx();
		logEx("Something went horribly wrong with Skript.");
		logEx("This issue is NOT your fault! You probably can't fix it yourself, either.");
		
		// Parse something useful out of the stack trace
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		Set<PluginDescriptionFile> stackPlugins = new HashSet<>();
		for (StackTraceElement s : stackTrace) { // Look through stack trace
			for (Entry<String,PluginDescriptionFile> e : pluginPackages.entrySet()) { // Look through plugins
				if (s.getClassName().contains(e.getKey())) // Hey, is this plugin in that stack trace?
					stackPlugins.add(e.getValue()); // Yes? Add it to list
			}
		}
		
		// Check if server platform is supported
		if (!isRunningMinecraft(1, 9) || !runningSpigot) {
			logEx("Your Minecraft version or server software appears to be unsupported by Skript (bensku's version).");
			logEx("Currently only supported servers are Spigot and its forks for Minecraft 1.9 or newer.");
			logEx("Other versions might work, but since you're getting this error message something is NOT working,");
			logEx("nor it will work, unless you switch to supported platform.");
			logEx("Issue tracker: " + issuesUrl + " (only if you know what you're doing!)");
		} else if (Updater.state == UpdateState.UPDATE_AVAILABLE) {
			logEx("You're running outdated version of Skript! Please try updating it NOW; it might fix this.");
			logEx("You may download new version of Skript at https://github.com/bensku/Skript/releases");
			logEx("You will be given instructions how to report this error if it persists with latest Skript.");
			logEx("Issue tracker: " + issuesUrl + " (only if you know what you're doing!)");
		} else {
			if (pluginPackages.isEmpty()) {
				logEx("You should report it at " + issuesUrl + ". Please copy paste this report there (or use paste service).");
				logEx("This ensures that your issue is noticed and will be fixed as soon as possible.");
			} else {
				logEx("It looks like you are using some plugin(s) that alter how Skript works (addons).");
				if (stackPlugins.isEmpty()) {
					logEx("Here is full list of them:");
					StringBuilder pluginsMessage = new StringBuilder();
					for (PluginDescriptionFile desc : pluginPackages.values()) {
						pluginsMessage.append(desc.getName());
						if (desc.getWebsite() != null && !desc.getWebsite().isEmpty()) // Add website if found
							pluginsMessage.append(" (").append(desc.getWebsite()).append(")");
						
						pluginsMessage.append(" ");
					}
					logEx(pluginsMessage.toString());
					logEx("We could not identify which of those are specially related, so this might also be Skript issue.");
				} else {
					logEx("Following plugins are probably related to this error in some way:");
					StringBuilder pluginsMessage = new StringBuilder();
					for (PluginDescriptionFile desc : stackPlugins) {
						pluginsMessage.append(desc.getName());
						if (desc.getWebsite() != null && !desc.getWebsite().isEmpty()) // Add website if found
							pluginsMessage.append(" (").append(desc.getWebsite()).append(")");
						
						pluginsMessage.append(" ");
					}
					logEx(pluginsMessage.toString());
				}
				
				logEx("You should try disabling those plugins one by one, trying to find which one causes it.");
				logEx("If the error doesn't disappear even after disabling all listed plugins, it is probably Skript issue.");
				logEx("In that case, you will be given instruction on how should you report it.");
				logEx("On the other hand, if the error disappears when disabling some plugin, report it to author of that plugin.");
				logEx("Only if the author tells you to do so, report it as Skript issue (url below)");
				logEx("Issue tracker: " + issuesUrl + " (only if you know what you're doing!)");
			}
		}
		
		logEx();
		logEx("Stack trace:");
		if (cause == null || cause.getStackTrace().length == 0) {
			logEx("  warning: no/empty exception given, dumping current stack trace instead");
			cause = new Exception(cause);
		}
		boolean first = true;
		while (cause != null) {
			logEx((first ? "" : "Caused by: ") + cause.toString());
			for (final StackTraceElement e : cause.getStackTrace())
				logEx("    at " + e.toString());
			cause = cause.getCause();
			first = false;
		}
		
		logEx();
		logEx("Version Information:");
		logEx("  Skript: " + getVersion() + (Updater.state == Updater.UpdateState.RUNNING_LATEST ? " (latest)"
				: Updater.state == Updater.UpdateState.UPDATE_AVAILABLE ? " (OUTDATED)"
				: Updater.state == Updater.UpdateState.RUNNING_CUSTOM ? " (custom version)" : ""));
		logEx("  Bukkit: " + Bukkit.getBukkitVersion());
		logEx("  Minecraft: " + getMinecraftVersion());
		logEx("  Java: " + System.getProperty("java.version") + " (" + System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version") + ")");
		logEx("  OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch") + " " + System.getProperty("os.version"));
		logEx();
		logEx("Running CraftBukkit: " + runningCraftBukkit);
		logEx("Running Spigot (or compatible): " + runningSpigot);
		logEx("Running Paper (or compatible): " + runningPaper);
		logEx();
		logEx("Current node: " + SkriptLogger.getNode());
		logEx("Current item: " + (item == null ? "null" : item.toString(null, true)));
		logEx("Thread: " + (thread == null ? Thread.currentThread() : thread).getName());
		logEx();
		logEx("Language: " + Language.getName());
		logEx("Link parse mode: " + ChatMessages.linkParseMode);
		logEx();
		logEx("End of Error.");
		logEx();
		
		return new EmptyStacktraceException();
	}
	
	final static void logEx() {
		SkriptLogger.LOGGER.severe(EXCEPTION_PREFIX);
	}
	
	final static void logEx(final String... lines) {
		for (final String line : lines)
			SkriptLogger.LOGGER.severe(EXCEPTION_PREFIX + line);
	}
	
	public static String SKRIPT_PREFIX = ChatColor.GRAY + "[" + ChatColor.GOLD + "Skript" + ChatColor.GRAY + "]" + ChatColor.RESET + " ";
	
//	static {
//		Language.addListener(new LanguageChangeListener() {
//			@Override
//			public void onLanguageChange() {
//				final String s = Language.get_("skript.prefix");
//				if (s != null)
//					SKRIPT_PREFIX = Utils.replaceEnglishChatStyles(s) + ChatColor.RESET + " ";
//			}
//		});
//	}
	
	public static void info(final CommandSender sender, final String info) {
		sender.sendMessage(SKRIPT_PREFIX + Utils.replaceEnglishChatStyles(info));
	}
	
	/**
	 * @param message
	 * @param permission
	 * @see #adminBroadcast(String)
	 */
	public static void broadcast(final String message, final String permission) {
		Bukkit.broadcast(SKRIPT_PREFIX + Utils.replaceEnglishChatStyles(message), permission);
	}
	
	public static void adminBroadcast(final String message) {
		Bukkit.broadcast(SKRIPT_PREFIX + Utils.replaceEnglishChatStyles(message), "skript.admin");
	}
	
	/**
	 * Similar to {@link #info(CommandSender, String)} but no [Skript] prefix is added.
	 * 
	 * @param sender
	 * @param info
	 */
	public static void message(final CommandSender sender, final String info) {
		sender.sendMessage(Utils.replaceEnglishChatStyles(info));
	}
	
	public static void error(final CommandSender sender, final String error) {
		sender.sendMessage(SKRIPT_PREFIX + ChatColor.DARK_RED + Utils.replaceEnglishChatStyles(error));
	}
	
	/**
	 * Indicates if Skript is running prerelease build.
	 * @return Boolean.
	 */
	public static boolean isPrerelease() {
		return true;
	}
	
}
