package com.trashsoftware.gravity2.utils;

import com.trashsoftware.gravity2.physics.CelestialObject;
import com.trashsoftware.gravity2.physics.Simulator;
import com.trashsoftware.gravity2.presets.SystemPresets;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.*;

public class JsonUtil {
    
    public static <T> T jsonToObject(Class<T> clazz, JSONObject jsonObject) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        Constructor<?> longest = constructors[0];
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() > longest.getParameterCount()) {
                longest = constructor;
            }
        }
        Object[] args = new Object[longest.getParameterCount()];
        Parameter[] parameters = longest.getParameters();
        for (int i = 0; i < args.length; i++) {
            Parameter parameter = parameters[i];
            String paramName = parameter.getName();
            Class<?> paramType = parameter.getType();
            if (paramType == Integer.class) {
                args[i] = jsonObject.getInt(paramName);
            } else if (paramType == Double.class) {
                args[i] = jsonObject.getDouble(paramName);
            } else if (paramType == Float.class) {
                args[i] = jsonObject.getFloat(paramName);
            } else if (paramType == Long.class) {
                args[i] = jsonObject.getLong(paramName);
            } else if (paramType == Short.class) {
                args[i] = (short) jsonObject.getInt(paramName);
            } else if (paramType == Character.class) {
                args[i] = (char) jsonObject.getInt(paramName);
            } else if (paramType == Byte.class) {
                args[i] = (byte) jsonObject.getInt(paramName);
            } else if (paramType == Boolean.class) {
                args[i] = jsonObject.getBoolean(paramName);
            } else if (paramType == String.class) {
                String str;
                if (jsonObject.has(paramName)) {
                    str = jsonObject.getString(paramName);
                } else {
                    str = null;
                }
                args[i] = str;
            } else if (paramType.isArray()) {
                if (paramType == double[].class) {
                    args[i] = jsonArrayToDoubleArray(jsonObject.getJSONArray(paramName));
                }
            }
        }
        T instance = (T) longest.newInstance(args);
        return instance;
    }
    
    public static JSONObject objectToJson(Object object) throws IllegalAccessException {
        JSONObject json = new JSONObject();
//        System.out.println("Class: " + object.getClass());
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                if (Modifier.isTransient(field.getModifiers())) continue;
                if (Modifier.isStatic(field.getModifiers())) continue;
//            System.out.println(field.getName());
                field.setAccessible(true);

                String name = field.getName();
                if (field.getType().isPrimitive()) {
                    json.put(name, field.get(object));
                } else if (field.getType() == String.class) {
                    json.put(name, field.get(object));
                } else if (field.getType().isArray()) {
                    Object arr = field.get(object);
                    if (arr instanceof double[] doubles) {
                        JSONArray jsonArray = arrayToJson(doubles);
                        json.put(name, jsonArray);
                    }
                } else if (field.getType().isEnum()) {
                    Enum<?> en = (Enum<?>) field.get(object);
                    json.put(name, en.name());
                } else {
                    JSONObject nested = objectToJson(field.get(object));
                    json.put(name, nested);
                }
            } catch (Exception e) {
                System.err.println(object.getClass().getName() + " " + field.getName() + "=" + field.get(object));
                throw new RuntimeException(e);
            }
        }
        return json;
    }
    
    public static JSONArray arrayToJson(double[] array) {
        return new JSONArray(array);
    }

    public static double[] jsonArrayToDoubleArray(JSONArray jsonArray) {
        double[] res = new double[jsonArray.length()];
        for (int i = 0; i < res.length; i++) {
            res[i] = jsonArray.getDouble(i);
        }
        return res;
    }

    public static void main(String[] args) throws Exception {
        Simulator simulator = new Simulator();
        CelestialObject jup = SystemPresets.createObjectPreset(
                simulator,
                SystemPresets.jupiter,
                new double[3],
                new double[3],
                1.0
        );
        JSONObject jo = objectToJson(jup);
        System.out.println(jo.toString(2));
        
        CelestialObject recovered = CelestialObject.fromJson(jo);
        System.out.println(recovered);
    }
}
