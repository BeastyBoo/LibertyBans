/* 
 * LibertyBans-env-spigot
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-spigot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-spigot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-spigot. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.spigot;

import java.nio.file.Path;

import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.bootstrap.PlatformLauncher;
import space.arim.libertybans.core.ApiBindModule;
import space.arim.libertybans.core.PillarOneBindModule;
import space.arim.libertybans.core.PillarTwoBindModule;

import space.arim.injector.Identifier;
import space.arim.injector.InjectorBuilder;

import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import space.arim.omnibus.Omnibus;
import space.arim.omnibus.OmnibusProvider;

public class SpigotLauncher implements PlatformLauncher {

	private final JavaPlugin plugin;
	private final Path folder;
	private final Omnibus omnibus;

	public SpigotLauncher(JavaPlugin plugin, Path folder) {
		this(plugin, folder, OmnibusProvider.getOmnibus());
	}

	public SpigotLauncher(JavaPlugin plugin, Path folder, Omnibus omnibus) {
		this.plugin = plugin;
		this.folder = folder;
		this.omnibus = omnibus;
	}

	@Override
	public BaseFoundation launch() {
		return new InjectorBuilder()
				.bindInstance(JavaPlugin.class, plugin)
				.bindInstance(Server.class, plugin.getServer())
				.bindInstance(Identifier.ofTypeAndNamed(Path.class, "folder"), folder)
				.bindInstance(Omnibus.class, omnibus)
				.addBindModules(
						new ApiBindModule(),
						new PillarOneBindModule(),
						new PillarTwoBindModule(),
						new SpigotBindModule())
				.build()
				.request(BaseFoundation.class);
	}

}
