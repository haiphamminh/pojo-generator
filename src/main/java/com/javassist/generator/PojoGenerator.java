package com.javassist.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;

public class PojoGenerator {
    static final AtomicInteger counter = new AtomicInteger(1);
    static final String MAIN_GENERATED_POJO_NAME = "com.javassist.generator.GeneratedPojo";
    static final String NESTED_GENERATED_POJO_NAME = "com.javassist.generator.NestedGeneratedPojo";

    public static void main(String[] args)
            throws IOException, NotFoundException, CannotCompileException, IllegalAccessException,
                   InstantiationException {
        String json = "{\n"
                + "  \"key1\": 1,\n"
                + "  \"key2\": true,\n"
                + "  \"key3\": {\n"
                + "    \"key31\": \"value31\",\n"
                + "    \"key32\": \"value32\"\n"
                + "  },\n"
                + "  \"key4\": \"value4\",\n"
                + "  \"key5\": [\n"
                + "    {\n"
                + "      \"key51\": \"value511\",\n"
                + "      \"key51\": \"value512\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"key51\": \"value513\",\n"
                + "      \"key51\": \"value514\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = mapper.readValue(json, Map.class);
        Map<String, Class<?>> properties = new HashMap<>();
        generateProperties(map, properties);
        Class clazz = generate(MAIN_GENERATED_POJO_NAME, properties);

        Object obj = clazz.newInstance();

        System.out.println("Clazz: " + clazz);
        System.out.println("Object: " + obj);
        System.out.println("Serializable? " + (obj instanceof Serializable));

        for (final Method method : clazz.getDeclaredMethods()) {
            System.out.println(method);
        }
    }

    static void generateProperties(Map<String, Object> map, Map<String, Class<?>> properties)
            throws IOException, CannotCompileException, NotFoundException {
        for (Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            Class<?> cls = String.class;
            if (isInstance(value, List.class)) {
                cls = List.class;
            }
            if (isInstance(value, Map.class)) {
                Map<String, Class<?>> nestedProperties = new HashMap<>();
                generateProperties((Map) value, nestedProperties);
                String nestedClassName = NESTED_GENERATED_POJO_NAME + counter.getAndIncrement();
                cls = generate(nestedClassName, nestedProperties);
            } else if (isInstance(value, Integer.class)) {
                cls = Integer.class;
            } else if (isInstance(value, Long.class)) {
                cls = Long.class;
            } else if (isInstance(value, Float.class)) {
                cls = Float.class;
            } else if (isInstance(value, Double.class)) {
                cls = Double.class;
            } else if (isInstance(value, Boolean.class)) {
                cls = Boolean.class;
            }
            properties.put(entry.getKey(), cls);
        }
    }

    public static Class generate(String className, Map<String, Class<?>> properties) throws NotFoundException,
                                                                                            CannotCompileException,
                                                                                            IOException {

        ClassPool pool = ClassPool.getDefault();
        CtClass cc = pool.makeClass(className);

        // add this to define a super class to extend
        // cc.setSuperclass(resolveCtClass(MySuperClass.class));

        // add this to define an interface to implement
        cc.addInterface(resolveCtClass(Serializable.class));

        for (Entry<String, Class<?>> entry : properties.entrySet()) {

            cc.addField(new CtField(resolveCtClass(entry.getValue()), entry.getKey(), cc));

            // add getter
            cc.addMethod(generateGetter(cc, entry.getKey(), entry.getValue()));

            // add setter
            cc.addMethod(generateSetter(cc, entry.getKey(), entry.getValue()));
        }

        return cc.toClass();
    }

    private static CtMethod generateGetter(CtClass declaringClass, String fieldName, Class fieldClass)
            throws CannotCompileException {

        String getterName = "get" + fieldName.substring(0, 1)
                                             .toUpperCase()
                + fieldName.substring(1);

        StringBuffer sb = new StringBuffer();
        sb.append("public ")
          .append(fieldClass.getName())
          .append(" ")
          .append(getterName)
          .append("(){")
          .append("return this.")
          .append(fieldName)
          .append(";")
          .append("}");
        return CtMethod.make(sb.toString(), declaringClass);
    }

    private static CtMethod generateSetter(CtClass declaringClass, String fieldName, Class fieldClass)
            throws CannotCompileException {

        String setterName = "set" + fieldName.substring(0, 1)
                                             .toUpperCase()
                + fieldName.substring(1);

        StringBuffer sb = new StringBuffer();
        sb.append("public void ")
          .append(setterName)
          .append("(")
          .append(fieldClass.getName())
          .append(" ")
          .append(fieldName)
          .append(")")
          .append("{")
          .append("this.")
          .append(fieldName)
          .append("=")
          .append(fieldName)
          .append(";")
          .append("}");
        return CtMethod.make(sb.toString(), declaringClass);
    }

    private static CtClass resolveCtClass(Class clazz) throws NotFoundException {
        ClassPool pool = ClassPool.getDefault();
        return pool.get(clazz.getName());
    }

    static boolean isInstance(Object value, Class<?> cls) {
        return value != null && cls.isInstance(value);
    }
}
