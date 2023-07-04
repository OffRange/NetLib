package de.davis.net.gson;

import com.google.gson.*;
import de.davis.net.annotations.ModelType;

import java.io.File;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypeAdapter implements JsonDeserializer<Object>, JsonSerializer<Object> {
    private final Map<Integer, Class<?>> modelClasses;

    public TypeAdapter() {
        modelClasses = new HashMap<>();
        scanModelClasses();
    }

    @Override
    public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        int type = jsonObject.get("type").getAsInt();

        Class<?> modelClass = modelClasses.get(type);
        if (modelClass == null) {
            throw new JsonParseException("Unknown type number: " + type);
        }

        return context.deserialize(json, modelClass);
    }

    @Override
    public JsonElement serialize(Object src, Type typeOfSrc, JsonSerializationContext context) {
        JsonElement jsonObject = context.serialize(src);
        jsonObject.getAsJsonObject().addProperty("type", getKey(modelClasses, src.getClass()));
        return jsonObject;
    }

    public <K, V> K getKey(Map<K, V> map, V value) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void scanModelClasses() {
        Package pkg = TypeAdapter.class.getPackage();
        String packageName = pkg.getName();

        for (Class<?> modelClass : getAllClasses(packageName)) {
            ModelType annotation = modelClass.getAnnotation(ModelType.class);
            if (annotation != null) {
                int type = annotation.type();
                modelClasses.put(type, modelClass);
            }
        }
    }

    private Class<?>[] getAllClasses(String packageName) {
        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            URL resource = classLoader.getResource(path);
            if (resource != null) {
                File dir = new File(resource.toURI());
                if (dir.exists() && dir.isDirectory()) {
                    return processFiles(dir.listFiles(), packageName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Class<?>[0];
    }

    private Class<?>[] processFiles(java.io.File[] files, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        for (File file : files) {
            if (file.isDirectory()) {
                classes.addAll(java.util.Arrays.asList(processFiles(file.listFiles(), packageName + "." + file.getName())));
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                classes.add(Class.forName(className));
            }
        }
        return classes.toArray(new Class<?>[0]);
    }
}