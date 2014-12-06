/*
 * Copyright (C) 2014 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.javawriter;

import com.google.common.collect.ImmutableList;
import com.google.testing.compile.CompilationRule;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assert_;
import static com.squareup.javawriter.TestUtil.isJava8;
import static org.junit.Assume.assumeTrue;

@RunWith(JUnit4.class)
public class TypeNamesTest {
  @Rule public final CompilationRule compilation = new CompilationRule();

  private TypeElement getElement(Class<?> clazz) {
    return compilation.getElements().getTypeElement(clazz.getCanonicalName());
  }

  private TypeMirror getType(Class<?> clazz) {
    return getElement(clazz).asType();
  }

  @Test
  public void forTypeMirror_basicTypes() {
    assert_().that(TypeNames.forTypeMirror(getType(Object.class)))
        .isEqualTo(ClassName.fromClass(Object.class));
    assert_().that(TypeNames.forTypeMirror(getType(Charset.class)))
        .isEqualTo(ClassName.fromClass(Charset.class));
    assert_().that(TypeNames.forTypeMirror(getType(TypeNamesTest.class)))
        .isEqualTo(ClassName.fromClass(TypeNamesTest.class));
  }

  @Test
  public void forTypeMirror_parameterizedType() {
    DeclaredType setType =
        compilation.getTypes().getDeclaredType(getElement(Set.class), getType(Object.class));
    assert_().that(TypeNames.forTypeMirror(setType))
        .isEqualTo(ParameterizedTypeName.create(Set.class, ClassName.fromClass(Object.class)));
  }

  static class Parameterized<
      Simple,
      ExtendsClass extends Number,
      ExtendsInterface extends Runnable,
      ExtendsTypeVariable extends Simple,
      Intersection extends Number & Runnable> {}

  @Test
  public void forTypeMirror_typeVariable() {
    List<? extends TypeParameterElement> typeVariables =
        getElement(Parameterized.class).getTypeParameters();

    assert_().that(TypeNames.forTypeMirror(typeVariables.get(0).asType()))
        .isEqualTo(TypeVariableName.named("Simple"));
    assert_().that(TypeNames.forTypeMirror(typeVariables.get(1).asType()))
        .isEqualTo(new TypeVariableName("ExtendsClass", ImmutableList.<TypeName>of(
            ClassName.fromClass(Number.class))));
    assert_().that(TypeNames.forTypeMirror(typeVariables.get(2).asType()))
        .isEqualTo(new TypeVariableName("ExtendsInterface", ImmutableList.<TypeName>of(
            ClassName.fromClass(Runnable.class))));
    assert_().that(TypeNames.forTypeMirror(typeVariables.get(3).asType()))
        .isEqualTo(new TypeVariableName("ExtendsTypeVariable", ImmutableList.<TypeName>of(
            TypeVariableName.named("Simple"))));
  }

  @Test
  public void forTypeMirror_intersectionType() {
    assumeTrue(!isJava8());

    List<? extends TypeParameterElement> typeVariables =
        getElement(Parameterized.class).getTypeParameters();
    assert_().that(TypeNames.forTypeMirror(typeVariables.get(4).asType()))
        .isEqualTo(new TypeVariableName("Intersection", ImmutableList.<TypeName>of(
            ClassName.fromClass(Number.class),
            ClassName.fromClass(Runnable.class))));
  }

  @Test
  public void forTypeMirror_intersectionTypeJava8() {
    assumeTrue(isJava8());

    List<? extends TypeParameterElement> typeVariables =
        getElement(Parameterized.class).getTypeParameters();
    assert_().that(TypeNames.forTypeMirror(typeVariables.get(4).asType()))
        .isEqualTo(new TypeVariableName("Intersection", ImmutableList.<TypeName>of(
            new IntersectionTypeName(ImmutableList.<TypeName>of(ClassName.fromClass(Number.class),
                ClassName.fromClass(Runnable.class))))));
  }

  @Test
  public void forTypeMirror_primitive() {
    assert_().that(TypeNames.forTypeMirror(compilation.getTypes().getPrimitiveType(TypeKind.BOOLEAN)))
        .isEqualTo(PrimitiveName.BOOLEAN);
    assert_().that(TypeNames.forTypeMirror(compilation.getTypes().getPrimitiveType(TypeKind.BYTE)))
        .isEqualTo(PrimitiveName.BYTE);
    assert_().that(TypeNames.forTypeMirror(compilation.getTypes().getPrimitiveType(TypeKind.SHORT)))
        .isEqualTo(PrimitiveName.SHORT);
    assert_().that(TypeNames.forTypeMirror(compilation.getTypes().getPrimitiveType(TypeKind.INT)))
        .isEqualTo(PrimitiveName.INT);
    assert_().that(TypeNames.forTypeMirror(compilation.getTypes().getPrimitiveType(TypeKind.LONG)))
        .isEqualTo(PrimitiveName.LONG);
    assert_().that(TypeNames.forTypeMirror(compilation.getTypes().getPrimitiveType(TypeKind.CHAR)))
        .isEqualTo(PrimitiveName.CHAR);
    assert_().that(TypeNames.forTypeMirror(compilation.getTypes().getPrimitiveType(TypeKind.FLOAT)))
        .isEqualTo(PrimitiveName.FLOAT);
    assert_().that(TypeNames.forTypeMirror(compilation.getTypes().getPrimitiveType(TypeKind.DOUBLE)))
        .isEqualTo(PrimitiveName.DOUBLE);
  }

  @Test
  public void forTypeMirror_arrays() {
    assert_().that(TypeNames.forTypeMirror(compilation.getTypes().getArrayType(getType(Object.class))))
        .isEqualTo(new ArrayTypeName(ClassName.fromClass(Object.class)));
  }

  @Test public void forClass_array() {
    assert_().that(TypeNames.forClass(Object[].class))
        .isEqualTo(new ArrayTypeName(ClassName.fromClass(Object.class)));
    assert_().that(TypeNames.forClass(int[].class))
        .isEqualTo(new ArrayTypeName(PrimitiveName.INT));
  }

  @Test
  public void forTypeMirror_void() {
    assert_().that(TypeNames.forTypeMirror(compilation.getTypes().getNoType(TypeKind.VOID)))
        .isEqualTo(VoidName.VOID);
  }

  @Test public void forClass_void() {
    assert_().that(TypeNames.forClass(void.class)).isEqualTo(VoidName.VOID);
    assert_().that(TypeNames.forClass(Void.class)).isNotEqualTo(VoidName.VOID);
  }

  @Test
  public void forTypeMirror_null() {
    assert_().that(TypeNames.forTypeMirror(compilation.getTypes().getNullType()))
        .isEqualTo(NullName.NULL);
  }
}
