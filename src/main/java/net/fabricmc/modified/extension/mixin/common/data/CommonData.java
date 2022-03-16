/*
 * Copyright (c) 2016, 2018, Player, asie
 * Copyright (c) 2021, FabricMC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.fabricmc.modified.extension.mixin.common.data;

import java.util.Objects;

import net.fabricmc.modified.api.TrEnvironment;
import net.fabricmc.modified.api.TrMember;
import net.fabricmc.modified.extension.mixin.common.Logger;
import net.fabricmc.modified.extension.mixin.common.MapUtility;
import net.fabricmc.modified.extension.mixin.common.ResolveUtility;

public final class CommonData {
	private final net.fabricmc.modified.api.TrEnvironment environment;
	public final net.fabricmc.modified.extension.mixin.common.Logger logger;

	public final net.fabricmc.modified.extension.mixin.common.ResolveUtility resolver;
	public final net.fabricmc.modified.extension.mixin.common.MapUtility mapper;

	public CommonData(TrEnvironment environment, Logger logger) {
		this.environment = Objects.requireNonNull(environment);
		this.logger = Objects.requireNonNull(logger);

		this.resolver = new ResolveUtility(environment, logger);
		this.mapper = new MapUtility(environment.getRemapper(), logger);
	}

	public void propagate(TrMember member, String newName) {
		this.environment.propagate(member, newName);
	}
}
