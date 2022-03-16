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

package net.fabricmc.modified.extension.mixin.common;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import net.fabricmc.modified.api.TrClass;
import net.fabricmc.modified.api.TrEnvironment;
import net.fabricmc.modified.api.TrField;
import net.fabricmc.modified.api.TrMethod;
import net.fabricmc.modified.extension.mixin.common.data.Message;

public final class ResolveUtility {
	/**
	 * Raise error if the result is not unique.
	 */
	public static int FLAG_UNIQUE = 0x1;
	/**
	 * Return first member based on asm ordinal if the result is not unique.
	 */
	public static int FLAG_FIRST = 0x2;
	/**
	 * Recursively resolve the the member including super-class and super-interface.
	 */
	public static int FLAG_RECURSIVE = 0x4;
	/**
	 * Prefer non-synthetic member. This has higher priority than {@link ResolveUtility#FLAG_FIRST};
	 */
	public static int FLAG_NON_SYN = 0x8;

	private final net.fabricmc.modified.api.TrEnvironment environment;
	private final Logger logger;

	public ResolveUtility(TrEnvironment environment, Logger logger) {
		this.environment = Objects.requireNonNull(environment);
		this.logger = Objects.requireNonNull(logger);
	}

	public Optional<net.fabricmc.modified.api.TrClass> resolveClass(String name) {
		net.fabricmc.modified.api.TrClass _class = environment.getClass(name);

		if (_class == null && !net.fabricmc.modified.extension.mixin.common.StringUtility.isInternalClassName(name)) {
			logger.error(String.format(Message.CANNOT_RESOLVE_CLASS, name));
		}

		return Optional.ofNullable(_class);
	}

	private <T extends net.fabricmc.modified.api.TrMember> Optional<T> resolveMember0(net.fabricmc.modified.api.TrClass owner, String name, String desc, int flag, Supplier<Collection<T>> get, Supplier<Collection<T>> resolve) {
		if ((flag & (FLAG_UNIQUE | FLAG_FIRST)) == 0) {
			throw new RuntimeException("Unspecified resolution strategy, please use FLAG_UNIQUE or FLAG_FIRST.");
		} else if (owner == null) {
			return Optional.empty();
		}

		Collection<T> collection;

		if ((flag & FLAG_RECURSIVE) != 0) {
			collection = resolve.get();
		} else {
			collection = get.get();
		}

		if ((flag & FLAG_UNIQUE) != 0) {
			if (collection.size() > 1) {
				throw new RuntimeException(String.format("The member %s:%s is ambiguous in class %s for FLAG_UNIQUE. Please use FLAG_FIRST.", name, desc, owner.getName()));
			} else {
				return collection.stream().findFirst();
			}
		}

		Comparator<T> comparator;

		if ((flag & FLAG_NON_SYN) != 0) {
			comparator = (x, y) -> Boolean.compare(x.isSynthetic(), y.isSynthetic()) != 0
					? Boolean.compare(x.isSynthetic(), y.isSynthetic()) : Integer.compare(x.getIndex(), y.getIndex());
		} else {
			comparator = Comparator.comparingInt(net.fabricmc.modified.api.TrMember::getIndex);
		}

		return collection.stream().min(comparator);
	}

	public Optional<net.fabricmc.modified.api.TrField> resolveField(net.fabricmc.modified.api.TrClass owner, String name, String desc, int flag) {
		return resolveMember0(owner, name, desc, flag,
				() -> owner.getFields(name, desc, false, null, null),
				() -> owner.resolveFields(name, desc, false, null, null));
	}

	public Optional<TrField> resolveField(String owner, String name, String desc, int flag) {
		return resolveClass(owner).flatMap(cls -> resolveField(cls, name, desc, flag));
	}

	public Optional<net.fabricmc.modified.api.TrMethod> resolveMethod(net.fabricmc.modified.api.TrClass owner, String name, String desc, int flag) {
		return resolveMember0(owner, name, desc, flag,
				() -> owner.getMethods(name, desc, false, null, null),
				() -> owner.resolveMethods(name, desc, false, null, null));
	}

	public Optional<TrMethod> resolveMethod(String owner, String name, String desc, int flag) {
		return resolveClass(owner).flatMap(cls -> resolveMethod(cls, name, desc, flag));
	}

	public Optional<net.fabricmc.modified.api.TrMember> resolveMember(TrClass owner, String name, String desc, int flag) {
		if (desc == null) throw new RuntimeException("desc cannot be null for resolveMember. Please use resolveMethod or resolveField.");

		net.fabricmc.modified.api.TrMember.MemberType type = StringUtility.getTypeByDesc(desc);

		if (type.equals(net.fabricmc.modified.api.TrMember.MemberType.FIELD)) {
			return resolveField(owner, name, desc, flag).map(m -> m);
		} else if (type.equals(net.fabricmc.modified.api.TrMember.MemberType.METHOD)) {
			return resolveMethod(owner, name, desc, flag).map(m -> m);
		} else {
			throw new RuntimeException(String.format("Unknown member type %s", type.name()));
		}
	}

	public Optional<net.fabricmc.modified.api.TrMember> resolveMember(String owner, String name, String desc, int flag) {
		return resolveClass(owner).flatMap(cls -> resolveMember(cls, name, desc, flag));
	}
}
