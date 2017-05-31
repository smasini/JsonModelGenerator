package com.moxun.generator;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;

import static com.moxun.generator.ClassNameUtil.getKeyName;
import static com.moxun.generator.ClassNameUtil.suffixToUppercase;

/**
 * Parsing json and generating code
 * Created by moxun on 15/12/9.
 */
public class JSONParser {
    private Stack<String> path = new Stack<String>();
    private List<String> allNodes = new ArrayList<String>();
    private boolean needGenSample = false;
    private GeneratorEngine engine;
    private boolean isArrayToList = false;
    private boolean genGetter;
    private boolean genSetter;
    private boolean genConstructor;

    public void reset(Project proj, PsiDirectory dir) {
        path.clear();
        allNodes.clear();
        engine = new GeneratorEngine(proj, dir);
    }

    public void init(String mainClassName, String pkg, String[] its, boolean isArrayToList) {
        push(suffixToUppercase(mainClassName));
        engine.init(pkg, its);
        this.isArrayToList = isArrayToList;
    }

    public void setGenSample(boolean has) {
        needGenSample = has;
    }

    public String decodeJSONObject(JSONObject json) {
        String className = null;
        Iterator<String> keys = json.keys();
        JSONObject current = null;
        Object value;
        StringBuilder constructor = new StringBuilder();
        if(genConstructor){
            constructor
                    .append("public ")
                    .append(path.peek())
                    .append("(JSONObject obj) {\n");
        }
        String key;
        className = createClass();
        while (keys.hasNext()) {
            key = keys.next();
            value = json.get(key);
            key = ClassNameUtil.getName(key);
            if(genConstructor){
                constructor.append(getConstructorLineForParams(key, value));
            }
            if (value instanceof JSONObject) {
                String validName = ClassNameUtil.getName(suffixToUppercase(key));
                String modifier = getModifier();
                append(modifier + validName + " " + getKeyName(key) + ";\n");
                push(validName);
                current = (JSONObject) value;
                if (current.keySet().size() > 0) {
                    decodeJSONObject(current);
                } else {
                    String last1 = "";
                    if (path.size() > 1) {
                        last1 = path.get(path.size() - 2);
                    }
                    engine.preGen(path.peek(), last1);
                    append("// TODO: complemented needed maybe.");
                    Logger.warn("Success to generating file " + path.peek() + ".java but it have no field");
                    path.pop();
                }
            } else if (value instanceof JSONArray) {
                JSONArray v = (JSONArray) value;
                if (v.size() > 0 && !(v.get(0) instanceof JSONObject)) {
                    Object firstValue = v.get(0);
                    String field = getModifier() + getArrayType(getNameForArrayModel(key, firstValue), isArrayToList) + " " + getKeyName(key) + ";\n";
                    append(field);
                } else {
                    //处理对象数组
                    if (isArrayToList) {
                        append(getModifier() + "List<" + getNameForClassArray(key) + ">" + getKeyName(key) + ";\n");
                    } else {
                        append(getModifier() + getNameForClassArray(key) + "[] " + getKeyName(key) + ";\n");
                    }
                }
                if(v.size() > 0 && !(v.get(0) instanceof JSONArray)) {
                    push(suffixToUppercase(key));
                }
                decodeJSONArray((JSONArray) value);
            } else {
                //处理基本数据类型和String
                String field = getModifier();
                field += decisionValueType(key, value, false) + " " + getKeyName(key) + ";";
                if (needGenSample) {
                    String v = String.valueOf(value);
                    v = v.replaceAll("\n", "");
                    if (v.length() > 15) {
                        v = v.substring(0, 15);
                    }
                    field = field + "\t// " + v;
                }
                append(field);
            }
        }

        if(genConstructor && constructor.length() > 0){
            constructor.append("}");
            appendConstructor(constructor.toString());
        }
        Logger.info("Success to generating file " + path.peek() + ".java");
        if (!path.isEmpty()) {
            path.pop();
        }
        return className;
    }

    private String getModifier() {
        if (!genGetter && !genSetter) {
            return "public ";
        } else {
            return "private ";
        }
    }

    private String getNameForArrayModel(String key, Object value){
        if(value instanceof JSONArray){
            String validName = ClassNameUtil.getName(suffixToUppercase(key + "List"));
            push(validName);
            return validName;
        }
        return decisionValueType(key, value, true);
    }

    private String decisionValueType(/*not uesd*/String key, Object value, boolean formArray) {
        if (formArray) {
            return value.getClass().getSimpleName();
        } else {
            if (value instanceof Integer) {
                return "int";
            } else if (value instanceof Long) {
                return "long";
            } else if (value instanceof Double) {
                return "double";
            } else if (value instanceof Boolean) {
                return "boolean";
            }
        }
        return "String";
    }

    @Deprecated
    private String __inferValueType(String key, String value, boolean formArray) {
        String type = "String";
        if (isNumeric(value)) {
            if (isInteger(value)) {
                if (value.length() > 8 || key.contains("Id") || key.contains("id") || key.contains("ID")) {
                    if (formArray) {
                        return "Long";
                    }
                    return "long";
                } else {
                    if (formArray) {
                        return "Integer";
                    }
                    return "int";
                }
            } else {
                String[] tmp = value.split("\\.");
                int fLength = 0;
                if (tmp.length > 1) {
                    fLength = value.split("\\.")[1].length();
                } else {
                    Logger.error(value);
                }

                if (fLength > 8) {
                    if (formArray) {
                        return "Double";
                    } else {
                        return "double";
                    }
                } else {
                    if (formArray) {
                        return "Float";
                    } else {
                        return "float";
                    }
                }
            }
        } else if (value.equals("true") || value.equals("false")) {
            if (formArray) {
                return "Boolean";
            } else {
                return "boolean";
            }
        }
        return type;
    }

    private String getArrayType(String baseType, boolean isArrayToList) {
        if (isArrayToList) {
            return "List<" + baseType + ">";
        } else {
            return baseType + "[]";
        }
    }

    private String createClass(){
        String last = "";
        if (path.size() > 1) {
            last = path.get(path.size() - 2);
        }
        return engine.preGen(path.peek(), last);
    }

    public void decodeJSONArray(JSONArray jsonArray) {
        Object item = jsonArray.get(0);
        if (item instanceof JSONObject) {
            push(getNameForClassArray(path.peek()));
            JSONObject obj = new JSONObject();
            for(int i = 0; i < jsonArray.size(); i++){
                JSONObject objTmp = jsonArray.optJSONObject(i);
                if(objTmp!=null){
                    Iterator<String> keys = objTmp.keys();
                    while (keys.hasNext()){
                        String key = keys.next();
                        if(!obj.has(key) || obj.opt(key) == null){
                            Object o = objTmp.opt(key);
                            if(!o.toString().equals("null")) {
                                obj.put(key, o);
                            }else{
                                obj.put(key, null);
                            }
                        }
                    }
                }
            }
            decodeJSONObject(obj);
        } else if (item instanceof JSONArray) {
            String className = createClass();
            //classe creata <key>List
            String internalValidName = ClassNameUtil.getName(suffixToUppercase(className + "Item"));
            String listName = getKeyName(internalValidName);
            Object firstValue = ((JSONArray) item).get(0);
            //creo la classe interna
            if(firstValue instanceof JSONArray){
                decodeJSONArray((JSONArray) ((JSONArray) item).get(0));
            }
            else if(firstValue instanceof JSONObject){
                push(internalValidName);
                decodeJSONObject(((JSONArray) item).optJSONObject(0));
                String field = getModifier() + getArrayType(internalValidName, isArrayToList) + " " + listName + ";\n";
                append(field);
            }else{
                String field = getModifier() + getArrayType(decisionValueType(null, firstValue, true), isArrayToList) + " " + listName + ";\n";
                append(field);
            }
            //constructor with JSONArray
            StringBuilder constructor = new StringBuilder();
            if(genConstructor){
                constructor.append("public ")
                        .append(path.peek())
                        .append("(JSONArray array) {\n")
                        .append("this.")
                        .append(listName)
                        .append(" = new ArrayList<>();")
                        .append("for(int i = 0; i < array.lenght(); i++){")
                        .append(listName)
                        .append(".add(");

                if(firstValue instanceof JSONObject){
                    constructor.append("new ")
                            .append(className)
                            .append("(array.optJSONObject(i))");
                }else{
                    constructor.append("array.opt");
                    if(firstValue instanceof Integer){
                        constructor.append("Int");
                    }else if (firstValue instanceof Long) {
                        constructor.append("Long");
                    } else if (firstValue instanceof Double) {
                        constructor.append("Double");
                    } else if (firstValue instanceof Boolean) {
                        constructor.append("Boolean");
                    }else{
                        constructor.append("String");
                    }
                    constructor.append("(i)");
                }
                constructor.append(");")
                        .append("}")//close for
                        .append("}");//close constructor
                appendConstructor(constructor.toString());
            }
            //push(internalValidName);
            //decodeJSONArray((JSONArray) item);
        }

        if (!path.isEmpty()) {
            path.pop();
        }
    }

    private String getConstructorLineForParams(String key, Object value){
        StringBuilder builder = new StringBuilder();
        builder.append("this.")
                .append(ClassNameUtil.getKeyName(key))
                .append(" = ");
        if(value instanceof JSONArray){
            JSONArray v = (JSONArray) value;
            if (v.size() > 0) {
                String arrayName = "jsonArray" + suffixToUppercase(key);
                String objName = "jsonObject" + suffixToUppercase(key);
                Object val = v.get(0);
                if (isArrayToList) {
                    builder.append("new ArrayList<>();\n");
                    builder.append("JSONArray ")
                            .append(arrayName)
                            .append(" = obj.optJSONArray(\"")
                            .append(key)
                            .append("\");")
                            .append("if(")
                            .append(arrayName)
                            .append(" != null){")
                            .append("for(int i = 0; i < ")
                            .append(arrayName)
                            .append(".length();i++){");
                    if(val instanceof JSONArray){
                        builder.append("JSONArray ")
                                .append(objName)
                                .append(" = ")
                                .append(arrayName)
                                .append(".optJSONArray(i);")
                                .append("if(")
                                .append(objName)
                                .append(" != null){");
                    }
                    else if(val instanceof JSONObject) {
                        builder.append("JSONObject ")
                                .append(objName)
                                .append(" = ")
                                .append(arrayName)
                                .append(".optJSONObject(i);")
                                .append("if(")
                                .append(objName)
                                .append(" != null){");
                    }else if(val instanceof Integer){
                        builder.append("int ")
                                .append(objName)
                                .append(" = ")
                                .append(arrayName)
                                .append(".optInt(i);");
                    }else if(val instanceof Long){
                        builder.append("long ")
                                .append(objName)
                                .append(" = ")
                                .append(arrayName)
                                .append(".optLong(i);");
                    }else if(val instanceof Double){
                        builder.append("double ")
                                .append(objName)
                                .append(" = ")
                                .append(arrayName)
                                .append(".optDouble(i);");
                    }else if(val instanceof Boolean){
                        builder.append("boolean ")
                                .append(objName)
                                .append(" = ")
                                .append(arrayName)
                                .append(".optBoolean(i);");
                    }else if(val instanceof String){
                        builder.append("String ")
                                .append(objName)
                                .append(" = ")
                                .append(arrayName)
                                .append(".optString(i);");
                    }
                    builder.append("this.")
                            .append(getKeyName(key))
                            .append(".add(");
                    if(val instanceof JSONArray){
                        builder.append("new ")
                                .append(ClassNameUtil.getName(suffixToUppercase(key + "List")))//TODO devo prendere il nome della classe corretto quindi dalla key aggiungo List
                                .append("(");
                    }
                    else if(val instanceof JSONObject){
                        builder.append("new ")
                                .append(getNameForClassArray(key))
                                .append("(");
                    }

                    builder.append(objName);
                    if(val instanceof JSONObject){
                        builder.append(")");
                    }
                    builder.append(");");
                    if(val instanceof JSONObject || val instanceof JSONArray){
                        builder.append("}");
                    }
                    builder.append("}}\n");
                } else {
                    //TODO handle simple array
                   // builder.append(new )
                  //  append(getModifier() + suffixToUppercase(key) + "Item[] " + getKeyName(key) + ";\n");
                }

            }
        }else {
            if(value instanceof JSONObject){
                String validName = ClassNameUtil.getName(suffixToUppercase(key));
                builder.append("new ");
                builder.append(validName);
                builder.append("(obj.optJSONObject(\"").append(key).append("\"));");
            }else{
                builder.append("obj.opt");
                if (value instanceof Integer) {
                    builder.append("Int");
                } else if (value instanceof Long) {
                    builder.append("Long");
                } else if (value instanceof Double) {
                    builder.append("Double");
                } else if (value instanceof Boolean) {
                    builder.append("Boolean");
                } else if (value instanceof String) {
                    builder.append("String");
                }
                builder.append("(\"")
                        .append(key)
                        .append("\");");
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    private String getNameForClassArray(String key){
        String name = suffixToUppercase(ClassNameUtil.fromPlural(key));
        //TODO generate name in singolar
        return ClassNameUtil.getName(name);
    }

    //正负整数,浮点数
    public boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("-?[0-9]+\\.?[0-9]*");
        return pattern.matcher(str).matches();
    }

    private boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("-?[0-9]+");
        return pattern.matcher(str).matches();
    }

    public void appendConstructor(String constructor)
    {
        engine.append(constructor, path.peek(), true);
    }

    public void append(String field) {
        engine.append(field, path.peek());
    }

    private void push(String name) {
        String uniqueName = ClassNameUtil.getName(name);
        if (allNodes.contains(name)) {
            uniqueName = path.peek() + name;
        }

        if (allNodes.contains(uniqueName)) {
            for (int i = 1; i <= 50; i++) {
                uniqueName = uniqueName + i;
                if (!allNodes.contains(uniqueName)) {
                    break;
                }
            }
        }

        allNodes.add(uniqueName);
        path.push(uniqueName);
    }

    void setGenGetter(boolean genGetter) {
        this.genGetter = genGetter;
        engine.setGenGetter(genGetter);
    }

    void setGenSetter(boolean genSetter) {
        this.genSetter = genSetter;
        engine.setGenSetter(genSetter);
    }

    public void setGenConstructor(boolean genConstructor) {
        this.genConstructor = genConstructor;
    }
}
