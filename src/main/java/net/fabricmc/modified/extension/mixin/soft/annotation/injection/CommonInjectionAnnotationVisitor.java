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

package net.fabricmc.modified.extension.mixin.soft.annotation.injection;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import net.fabricmc.modified.api.TrClass;
import net.fabricmc.modified.api.TrMember;
import net.fabricmc.modified.extension.mixin.common.IMappable;
import net.fabricmc.modified.extension.mixin.common.ResolveUtility;
import net.fabricmc.modified.extension.mixin.common.data.*;
import net.fabricmc.modified.extension.mixin.soft.annotation.FirstPassAnnotationVisitor;
import net.fabricmc.modified.extension.mixin.soft.data.MemberInfo;
import org.objectweb.asm.AnnotationVisitor;

import net.fabricmc.modified.extension.mixin.common.data.Pair;

/**
 * If the {@code method} element does not contain a name, then do not remap it; If the
 * {@code method} element has multiple matches (i.e. no desc), then the non-synthetic
 * method with the first occurrence in ASM will be remapped.
 */
class CommonInjectionAnnotationVisitor extends FirstPassAnnotationVisitor {
	private final net.fabricmc.modified.extension.mixin.common.data.CommonData data;
	private final AnnotationVisitor delegate;
	private final List<String> targets;

	CommonInjectionAnnotationVisitor(String descriptor, net.fabricmc.modified.extension.mixin.common.data.CommonData data, AnnotationVisitor delegate, boolean remap, List<String> targets) {
		super(descriptor, remap);

		this.data = Objects.requireNonNull(data);
		this.delegate = Objects.requireNonNull(delegate);
		this.targets = Objects.requireNonNull(targets);
	}

	@Override
	public void visitEnd() {
		// The second pass is needed regardless of remap, because it may have
		// children annotation need to remap.
		this.accept(new CommonInjectionSecondPassAnnotationVisitor(data, delegate, remap, targets));

		super.visitEnd();
	}

	private static class InjectMethodMappable implements IMappable<net.fabricmc.modified.extension.mixin.soft.data.MemberInfo> {
		private final net.fabricmc.modified.extension.mixin.common.data.CommonData data;
		private final net.fabricmc.modified.extension.mixin.soft.data.MemberInfo info;
		private final List<net.fabricmc.modified.api.TrClass> targets;

		InjectMethodMappable(net.fabricmc.modified.extension.mixin.common.data.CommonData data, net.fabricmc.modified.extension.mixin.soft.data.MemberInfo info, List<String> targets) {
			this.data = Objects.requireNonNull(data);
			this.info = Objects.requireNonNull(info);

			if (info.getOwner().isEmpty()) {
				this.targets = Objects.requireNonNull(targets).stream()
						.map(data.resolver::resolveClass)
						.filter(Optional::isPresent)
						.map(Optional::get)
						.collect(Collectors.toList());
			} else {
				this.targets = data.resolver.resolveClass(info.getOwner())
						.map(Collections::singletonList)
						.orElse(Collections.emptyList());
			}
		}

		private Optional<TrMember> resolvePartial(TrClass owner, String name, String desc) {
			Objects.requireNonNull(owner);

			name = name.isEmpty() ? null : name;
			desc = desc.isEmpty() ? null : desc;

			return data.resolver.resolveMethod(owner, name, desc, net.fabricmc.modified.extension.mixin.common.ResolveUtility.FLAG_FIRST | ResolveUtility.FLAG_NON_SYN).map(m -> m);
		}

		@Override
		public net.fabricmc.modified.extension.mixin.soft.data.MemberInfo result() {
			if (targets.isEmpty() || info.getName().isEmpty()) {
				return info;
			}

			List<Pair<String, String>> collection = targets.stream()
					.map(target -> resolvePartial(target, info.getName(), info.getDesc()))
					.filter(Optional::isPresent)
					.map(Optional::get)
					.map(m -> Pair.of(data.mapper.mapName(m), data.mapper.mapDesc(m)))
					.distinct().collect(Collectors.toList());

			if (collection.size() > 1) {
				data.logger.error(String.format(net.fabricmc.modified.extension.mixin.common.data.Message.CONFLICT_MAPPING, info.getName(), collection));
			} else if (collection.isEmpty()) {
				data.logger.warn(String.format(Message.NO_MAPPING_NON_RECURSIVE, info.getName(), targets));
			}

			return collection.stream().findFirst()
					.map(pair -> new net.fabricmc.modified.extension.mixin.soft.data.MemberInfo(data.mapper.asTrRemapper().map(info.getOwner()), pair.first(), info.getQuantifier(), pair.second()))
					.orElse(info);
		}
	}

	private static class CommonInjectionSecondPassAnnotationVisitor extends AnnotationVisitor {
		private final net.fabricmc.modified.extension.mixin.common.data.CommonData data;

		private final boolean remap;
		private final List<String> targets;

		CommonInjectionSecondPassAnnotationVisitor(CommonData data, AnnotationVisitor delegate, boolean remap, List<String> targets) {
			super(net.fabricmc.modified.extension.mixin.common.data.Constant.ASM_VERSION, delegate);

			this.data = Objects.requireNonNull(data);

			this.targets = Objects.requireNonNull(targets);
			this.remap = remap;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String name, String descriptor) {
			AnnotationVisitor av = super.visitAnnotation(name, descriptor);

			if (name.equals(net.fabricmc.modified.extension.mixin.common.data.AnnotationElement.AT)) {	// @ModifyArg, @ModifyArgs, @Redirect, @ModifyVariable
				if (!descriptor.equals(net.fabricmc.modified.extension.mixin.common.data.Annotation.AT)) {
					throw new RuntimeException("Unexpected annotation " + descriptor);
				}

				av = new net.fabricmc.modified.extension.mixin.soft.annotation.injection.AtAnnotationVisitor(data, av, remap);
			} else if (name.equals(net.fabricmc.modified.extension.mixin.common.data.AnnotationElement.SLICE)) {	// @ModifyArg, @ModifyArgs, @Redirect, @ModifyVariable
				if (!descriptor.equals(net.fabricmc.modified.extension.mixin.common.data.Annotation.SLICE)) {
					throw new RuntimeException("Unexpected annotation " + descriptor);
				}

				av = new SliceAnnotationVisitor(data, av, remap);
			}

			return av;
		}

		@Override
		public AnnotationVisitor visitArray(String name) {
			AnnotationVisitor av = super.visitArray(name);

			if (name.equals(net.fabricmc.modified.extension.mixin.common.data.AnnotationElement.METHOD)) {	// All
				return new AnnotationVisitor(net.fabricmc.modified.extension.mixin.common.data.Constant.ASM_VERSION, av) {
					@Override
					public void visit(String name, Object value) {
						if (remap) {
							Optional<net.fabricmc.modified.extension.mixin.soft.data.MemberInfo> info = Optional.ofNullable(MemberInfo.parse(Objects.requireNonNull((String) value).replaceAll("\\s", "")));

							value = info.map(i -> new InjectMethodMappable(data, i, targets).result().toString()).orElse((String) value);
						}

						super.visit(name, value);
					}
				};
			} else if (name.equals(net.fabricmc.modified.extension.mixin.common.data.AnnotationElement.AT)) {	// @Inject
				return new AnnotationVisitor(net.fabricmc.modified.extension.mixin.common.data.Constant.ASM_VERSION, av) {
					@Override
					public AnnotationVisitor visitAnnotation(String name, String descriptor) {
						if (!descriptor.equals(net.fabricmc.modified.extension.mixin.common.data.Annotation.AT)) {
							throw new RuntimeException("Unexpected annotation " + descriptor);
						}

						AnnotationVisitor av1 = super.visitAnnotation(name, descriptor);
						return new net.fabricmc.modified.extension.mixin.soft.annotation.injection.AtAnnotationVisitor(data, av1, remap);
					}
				};
			} else if (name.equals(AnnotationElement.SLICE)) {	// @Inject @ModifyConstant
				return new AnnotationVisitor(Constant.ASM_VERSION, av) {
					@Override
					public AnnotationVisitor visitAnnotation(String name, String descriptor) {
						if (!descriptor.equals(Annotation.SLICE)) {
							throw new RuntimeException("Unexpected annotation " + descriptor);
						}

						AnnotationVisitor av1 = super.visitAnnotation(name, descriptor);
						return new SliceAnnotationVisitor(data, av1, remap);
					}
				};
			}

			return av;
		}
	}
}
