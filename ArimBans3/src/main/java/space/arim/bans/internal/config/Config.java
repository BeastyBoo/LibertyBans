/*
 * ArimBans, a punishment plugin for minecraft servers
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.internal.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.yaml.snakeyaml.Yaml;

import com.google.common.io.ByteStreams;

import space.arim.bans.ArimBans;
import space.arim.bans.api.exception.ConfigLoadException;
import space.arim.bans.api.util.ToolsUtil;

public class Config implements ConfigMaster {
	
	private final ArimBans center;
	
	private final File configYml;
	private final File messagesYml;
	private final Map<String, Object> configDefaults;
	private final Map<String, Object> messageDefaults;
	private ConcurrentHashMap<String, Object> configValues = new ConcurrentHashMap<String, Object>();
	private ConcurrentHashMap<String, Object> messageValues = new ConcurrentHashMap<String, Object>();
	
	private static final String CONFIG_PATH = "/src/main/resources/config.yml";
	private static final String MESSAGES_PATH = "/src/main/resources/messages.yml";
	
	private static final int CONFIG_VERSION = 1;
	private static final int MESSAGES_VERSION = 1;

	public Config(ArimBans center) {
		this.center = center;
		this.configYml = new File(center.dataFolder(), "config.yml");
		this.messagesYml = new File(center.dataFolder(), "messages.yml");
		
		Yaml yaml = new Yaml();
		
		// Save files if nonexistent
		saveIfNotExist(configYml, CONFIG_PATH);
		saveIfNotExist(messagesYml, MESSAGES_PATH);
		
		// Load config defaults
		configDefaults = loadDefaults(CONFIG_PATH, yaml);
		messageDefaults = loadDefaults(MESSAGES_PATH, yaml);
		configValues.putAll(configDefaults);
		messageValues.putAll(messageDefaults);
		
		// Load config values
		configValues.putAll(loadFile(configYml, yaml));
		messageValues.putAll(loadFile(messagesYml, yaml));
		
		// Check config versions
		configVersion();
		messagesVersion();
	}
	
	private void saveIfNotExist(File target, String source) {
		if (!target.exists()) {
			if (!ToolsUtil.generateFile(target)) {
				ConfigLoadException exception = new ConfigLoadException(target);
				center.logError(exception);
				throw exception;
			}
			try (InputStream input = getClass().getResourceAsStream(source); FileOutputStream output = new FileOutputStream(target)) {
				ByteStreams.copy(input, output);
			} catch (IOException ex) {
				ConfigLoadException exception = new ConfigLoadException("Could not save " + target.getPath() + " from " + source, ex);
				center.logError(exception);
				throw exception;
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> loadDefaults(String source, Yaml yaml) {
		try (InputStream input = getClass().getResourceAsStream(source)) {
			return (Map<String, Object>) yaml.load(input);
		} catch (IOException ex) {
			ConfigLoadException exception = new ConfigLoadException("Could not load internal resource " + source, ex);
			center.logError(exception);
			throw exception;
		}
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> loadFile(File source, Yaml yaml) {
		try (FileReader reader = new FileReader(source)) {
			return (Map<String, Object>) yaml.load(reader);
		} catch (IOException ex) {
			ConfigLoadException exception = new ConfigLoadException(source, ex);
			center.logError(exception);
			throw exception;
		}
	}
	
	private void configVersion() {
		Object ver = configValues.get("config-version");
		if (ver instanceof Integer) {
			if ((Integer) ver == CONFIG_VERSION) {
				return;
			}
		}
		saveIfNotExist(configYml, CONFIG_PATH);
	}
	
	private void messagesVersion() {
		Object ver = messageValues.get("messages-version");
		if (ver instanceof Integer) {
			if ((Integer) ver == MESSAGES_VERSION) {
				return;
			}
		}
		saveIfNotExist(messagesYml, MESSAGES_PATH);
	}
	
	@Override
	public void refresh(boolean fromFile) {
		if (fromFile) {
			Yaml yaml = new Yaml();
			configValues.putAll(loadFile(configYml, yaml));
			messageValues.putAll(loadFile(messagesYml, yaml));
			configVersion();
			messagesVersion();
		}
	}
	
	@Override
	public void refreshConfig(boolean fromFile) {
		if (fromFile) {
			configValues.putAll(loadFile(configYml, new Yaml()));
			configVersion();
		}
	}
	
	@Override
	public void refreshMessages(boolean fromFile) {
		if (fromFile) {
			messageValues.putAll(loadFile(messagesYml, new Yaml()));
			messagesVersion();
		}
	}

	private void warning(String message) {
		center.log(message);
		center.environment().logger().warning(message);
	}
	
	private void configWarning(String key, Class<?> type, File file) {
		warning("Configuration " + key + " does not map to a " + type.getSimpleName() + " in " + file.getPath());
	}
	
	private void configWarning(String key, Class<?> type) {
		configWarning(key, type, configYml);
	}
	
	@Override
	public String getConfigString(String key) {
		return cfgGet(key, String.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<String> getConfigStrings(String key) {
		if (configValues.containsKey(key)) {
			Object obj = configValues.get(key);
			if (obj instanceof List<?>) {
				return (List<String>) obj;
			}
			configWarning(key, List.class);
		}
		return (List<String>) configDefaults.get(key);
	}
	
	@Override
	public boolean getConfigBoolean(String key) {
		return cfgGet(key, Boolean.class);
	}

	@Override
	public int getConfigInt(String key) {
		return cfgGet(key, Integer.class);
	}
	
	@SuppressWarnings("unchecked")
	private <T> T cfgGet(String key, Class<T> type) {
		if (configValues.containsKey(key)) {
			Object obj = configValues.get(key);
			if (type.isInstance(obj)) {
				return (T) obj;
			}
			configWarning(key, type);
		}
		return (T) configDefaults.get(key);
	}
	
	@SuppressWarnings("unchecked")
	private <T> T msgGet(String key, Class<T> type) {
		if (messageValues.containsKey(key)) {
			Object obj = messageValues.get(key);
			if (type.isInstance(obj)) {
				return (T) obj;
			}
			configWarning(key, type, messagesYml);
		}
		return (T) messageDefaults.get(key);
	}
	
	@Override
	public String getMessagesString(String key) {
		return msgGet(key, String.class);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<String> getMessagesStrings(String key) {
		if (messageValues.containsKey(key)) {
			Object obj = messageValues.get(key);
			if (obj instanceof List<?>) {
				return (List<String>) obj;
			}
			configWarning(key, List.class, messagesYml);
		}
		return (List<String>) messageDefaults.get(key);
	}
	
	@Override
	public boolean getMessagesBoolean(String key) {
		return msgGet(key, Boolean.class);
	}
	
	@Override
	public int getMessagesInt(String key) {
		return msgGet(key, Integer.class);
	}
	
}
