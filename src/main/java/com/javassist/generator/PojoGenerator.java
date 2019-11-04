package com.javassist.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;

public class PojoGenerator {
    public static void main(String[] args) throws IOException, NotFoundException, CannotCompileException {
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
                + "      \"key511\": \"value511\",\n"
                + "      \"key512\": \"value512\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"key521\": \"value521\",\n"
                + "      \"key522\": \"value522\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = mapper.readValue(json, Map.class);
        Map<String, Class<?>> properties = new HashMap<>();
        generateProperties(map, properties);
        byte[] clazz = generate("com.javassist.generator.GeneratedPojo", properties);
        System.out.println(mapper.writeValueAsString(new String(clazz)));
    }

    static void generateProperties(Map<String, Object> map, Map<String, Class<?>> properties) {
        for (Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue()
                     .getClass()
                     .isAssignableFrom(Map.class)) {

            } else if (entry.getValue()
                            .getClass()
                            .isAssignableFrom(Number.class)) {
                properties.put(entry.getKey(), Number.class);
            } else if (entry.getValue()
                            .getClass()
                            .isAssignableFrom(Boolean.class)) {
                properties.put(entry.getKey(), Boolean.class);
            } else {
                properties.put(entry.getKey(), String.class);
            }
        }
    }

    public static byte[] generate(String className, Map<String, Class<?>> properties) throws NotFoundException,
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

        return cc.toBytecode();
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
}
