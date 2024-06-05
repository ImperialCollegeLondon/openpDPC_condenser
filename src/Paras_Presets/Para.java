/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Paras_Presets;

import com.google.gson.Gson;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;

/**
 *
 * @author h.liu
 */
public class Para {

    public static void main(String[] args) {
        Para para1 = new Para("test", "Boolean", "", "", "", false);
        System.out.println(para1.toSimpleString());
        Para para2 = new Para(para1.toSimpleString(), true);
        System.out.println(para2.toSimpleString());
        System.out.println(para2.equals(para1));

        HashSet<Para> paras = new HashSet<>();
        paras.add(para2);
        System.out.println(paras.contains(para1));

        System.out.println(para1.name_);

    }

// -------------------------    -------------------------   -------------------------
    private static final Class[] Allowed_Para_Type = {String.class, Double.class, Integer.class, Boolean.class};
    public static final String[] STR_Allowed_Para_Type = {"String", "Double", "Integer", "Boolean"};
    private static final Boolean[] Para_Has_Limit = {false, true, true, false};
    public static final String[] Para_Js_Keys = {"\"name\"", "\"type\"", "\"current value\"",
        "\"max value\"", "\"min value\"", "\"isadvparam\""};

    public static final ArrayList<Class> ALLOWED_PARA_TYPE = new ArrayList<>(Arrays.asList(Allowed_Para_Type));
    public static final ArrayList<String> STR_ALLOWED_PARA_TYPE = new ArrayList<>(Arrays.asList(STR_Allowed_Para_Type));
    public static final ArrayList<Boolean> PARA_HAS_LIMIT = new ArrayList<>(Arrays.asList(Para_Has_Limit));
    public static final ArrayList<String> PARA_JS_KEYS = new ArrayList<>(Arrays.asList(Para_Js_Keys));

    public static final String sepkvjson = ":";
    public static final String seplinejson = ",";
    public static final String sepsimplestr = "@";

// -------------------------    -------------------------   -------------------------
    private String name_;
    private int type_id_ = 0;

    private String current_value_ = null;
    private String max_value_ = null;
    private String min_value_ = null;

    private boolean isadvparam_ = false;

    public Para(String name, String type, String value, String max_value, String min_value, boolean isadvparam) {
        if (name == null || name.isEmpty()
                || (name.equalsIgnoreCase("null")
                || name.contains(sepsimplestr))) {
            throw new IllegalArgumentException("Para name MUST NOT be null or empty or contain \""
                    + sepsimplestr + "\" but got input name=\"".concat(String.valueOf(name)).concat("\""));
        }

        name_ = name;
        type_id_ = STR_ALLOWED_PARA_TYPE.indexOf(type) == -1 ? 0 : STR_ALLOWED_PARA_TYPE.indexOf(type);
        set_current_value(value);
        set_max_value(max_value);
        set_min_value(min_value);
        set_isadvparam(isadvparam);
    }

    public Para(String js, boolean issimplestr) {
        if (issimplestr) {
            // simple form
            String[] eles = js.split(sepsimplestr);
            Gson gson = new Gson();
            int i = 0;
            for (String ele : eles) {
                eles[i] = gson.fromJson(ele, String.class);
                i++;
            }

            name_ = eles[0];
            String type = eles[1];
            type_id_ = STR_ALLOWED_PARA_TYPE.indexOf(type) == -1 ? 0 : STR_ALLOWED_PARA_TYPE.indexOf(type);
            set_current_value(eles[2]);
            set_max_value(eles[3]);
            set_min_value(eles[4]);
            set_isadvparam(Boolean.parseBoolean(eles[5]));
        } else {
            // proper json form (wo intendents)
            String[] temps = js.split("\n");
            for (int i = 0; i < PARA_JS_KEYS.size(); i++) {
                String key = PARA_JS_KEYS.get(i);
                String line = temps[i + 1];
                String value_str = line.substring(key.length() + sepkvjson.length(),
                        line.length() - ((i < (PARA_JS_KEYS.size() - 1)) ? seplinejson.length() : 0));

                switch (i) {
                    case 0:
                        name_ = value_str.substring(1, value_str.length() - 1);
                        break;
                    case 1:
                        String type = value_str.substring(1, value_str.length() - 1);
                        type_id_ = STR_ALLOWED_PARA_TYPE.indexOf(type) == -1 ? 0 : STR_ALLOWED_PARA_TYPE.indexOf(type);
                        break;
                    case 2:
                        set_current_value(value_str);
                        break;
                    case 3:
                        set_max_value(value_str);
                        break;
                    case 4:
                        set_min_value(value_str);
                        break;
                    case 5:
                        set_isadvparam(Boolean.parseBoolean(value_str));
                        break;
                }
            }
        }
    }

    @Override
    public boolean equals(Object para1) {
        if (this == para1) {
            return true;
        }
        if (para1 == null) {
            return this == null;
        }

        if (para1.getClass() != this.getClass()) {
            return false;
        }

        Para para1_rightclass = (Para) para1;
        return (toString() == null ? para1_rightclass.toString() == null : toString().equals(para1_rightclass.toString()));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.name_);
        hash = 89 * hash + Objects.hashCode(this.get_type_name());
        hash = 89 * hash + Objects.hashCode(this.get_current_value());
        hash = 89 * hash + Objects.hashCode(this.get_max_value());
        hash = 89 * hash + Objects.hashCode(this.get_min_value());
        hash = 89 * hash + (this.isadvparam_ ? 1 : 0);
        return hash;
    }

    public String toSimpleString() {
        String[] allstrs = new String[PARA_JS_KEYS.size()];
        Gson gson = new Gson();

        for (int i = 0; i < PARA_JS_KEYS.size(); i++) {
            String value = "";
            switch (i) {
                case 0:
                    value = name_;
                    break;
                case 1:
                    value = get_type_name();
                    break;
                case 2:
                    value = get_current_value();
                    break;
                case 3:
                    value = get_max_value();
                    break;
                case 4:
                    value = get_min_value();
                    break;
                case 5:
                    value = String.valueOf(get_isadvparam());
                    break;
            }
            allstrs[i] = gson.toJson(value, String.class);
        }

        return String.join(sepsimplestr, allstrs);
    }

    @Override
    public String toString() {

        String str = "{\n";
        for (int i = 0; i < PARA_JS_KEYS.size(); i++) {
            String key = PARA_JS_KEYS.get(i);
            String value = "";
            switch (i) {
                case 0:
                    value = "\"" + name_ + "\"";
                    break;
                case 1:
                    value = "\"" + get_type_name() + "\"";
                    break;
                case 2:
                    value = get_current_value();
                    value = value.isEmpty() ? "null" : value;
                    break;
                case 3:
                    value = get_max_value();
                    value = value.isEmpty() ? "null" : value;
                    break;
                case 4:
                    value = get_min_value();
                    value = value.isEmpty() ? "null" : value;
                    break;
                case 5:
                    value = String.valueOf(get_isadvparam());
                    break;
            }
            String line = key + sepkvjson + value
                    + ((i < (PARA_JS_KEYS.size() - 1)) ? seplinejson : "")
                    + "\n";
            str = str.concat(line);
        }
        str = str.concat("}");

        return str;
    }

    public String get_name() {
        return name_;
    }

    public String get_current_value() {
        return "null".equalsIgnoreCase(String.valueOf(current_value_)) ? "" : String.valueOf(current_value_);
    }

    public String get_max_value() {
        return "null".equalsIgnoreCase(String.valueOf(max_value_)) ? "" : String.valueOf(max_value_);
    }

    public String get_min_value() {
        return "null".equalsIgnoreCase(String.valueOf(min_value_)) ? "" : String.valueOf(min_value_);
    }

    public String get_type_name() {
        return type_id_ == -1 ? "" : STR_ALLOWED_PARA_TYPE.get(type_id_);
    }

    public boolean get_isadvparam() {
        return isadvparam_;
    }

    public String set_current_value(String value) {
        String value_to_set = get_value_to_set(value, current_value_);
        if (value_to_set != null && value_to_set.contains(sepsimplestr)) {
            throw new IllegalArgumentException("Value string MUST NOT contain \""
                    .concat(sepsimplestr).concat("\" but got input value=")
                    .concat(value_to_set));
        }

        if (check_max_min(value_to_set, true, true)) {
            current_value_ = value_to_set;
        }

        return current_value_;
    }

    public String set_max_value(String value) {
        if (!PARA_HAS_LIMIT.get(type_id_)) {
            max_value_ = null;
        } else {

            String value_to_set = get_value_to_set(value, max_value_);
            if (value_to_set != null && value_to_set.contains(sepsimplestr)) {
                throw new IllegalArgumentException("Max value string MUST NOT contain \""
                        .concat(sepsimplestr).concat("\" but got input max_value=")
                        .concat(value_to_set));
            }

            if (check_max_min(value_to_set, false, true)) {
                max_value_ = value_to_set;
            }

            if (!check_max_min(current_value_, true, true)) {
                current_value_ = null;
            }
        }

        return get_max_value();
    }

    public String set_min_value(String value) {
        if (!PARA_HAS_LIMIT.get(type_id_)) {
            min_value_ = null;
        } else {
            String value_to_set = get_value_to_set(value, min_value_);
            if (value_to_set != null && value_to_set.contains(sepsimplestr)) {
                throw new IllegalArgumentException("Min value string MUST NOT contain \""
                        .concat(sepsimplestr).concat("\" but got input min_value=")
                        .concat(value_to_set));
            }

            if (check_max_min(value_to_set, true, false)) {
                min_value_ = value_to_set;
            }

            if (!check_max_min(current_value_, true, true)) {
                current_value_ = null;
            }
        }

        return get_min_value();
    }

    public boolean set_isadvparam(boolean isadvparam) {
        isadvparam_ = isadvparam;
        return get_isadvparam();
    }

    // -------------------------    -------------------------   -------------------------
    private boolean check_max_min(String value, boolean check_max, boolean check_min) {
        boolean flag = true;
        if (!PARA_HAS_LIMIT.get(type_id_)) {
            return true;
        }

        boolean check_max_ = check_max & value != null & max_value_ != null;
        boolean check_min_ = check_min & value != null & min_value_ != null;

        switch (type_id_) {
            case 1:
                if (check_max_) {
                    if (Double.valueOf(value) > Double.valueOf(max_value_)) {
                        flag = false;
                    }
                }
                if (check_min_) {
                    if (Double.valueOf(value) < Double.valueOf(min_value_)) {
                        flag = false;
                    }
                }
                break;
            case 2:
                if (check_max_) {
                    if (Integer.valueOf(value) > Integer.valueOf(max_value_)) {
                        flag = false;
                    }
                }
                if (check_min_) {
                    if (Integer.valueOf(value) < Integer.valueOf(min_value_)) {
                        flag = false;
                    }
                }
                break;
        }

        return flag;
    }

    private String get_value_to_set(String value, String default_value) {
        value = "".equals(value) ? null : value;

        String value_to_set = default_value;
        try {
            Class<?> myClassType = Class.forName(ALLOWED_PARA_TYPE.get(type_id_).getName());
            Constructor<?> construct_ut = myClassType.getConstructor(new Class<?>[]{String.class});
            value_to_set = String.valueOf(construct_ut.newInstance(value));
        } catch (Exception e) {
            if (value == null || value.equalsIgnoreCase("null") || value.equals("")) {
                value_to_set = null;
            }
        }
        return value_to_set;
    }

}
