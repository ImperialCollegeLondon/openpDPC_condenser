/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Paras_Presets;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author localuser
 */
public class Editable_Preset {

    public static void main(String[] args) {
        Editable_Preset preset = new Editable_Preset();
        preset.add_update_para(new Para("para1", "Integer", "", "124", "-1", true));
        preset.add_update_para(new Para("para2", "Integer", "", "124", "-1", false));
        System.out.println(preset);
        Editable_Preset preset1 = new Editable_Preset(preset.toString());
        System.out.println(preset1);
        System.out.println(preset1.get_current_preset_json(true));
        System.out.println(preset1.get_current_preset_json(false));
    }
///////////////////////////////////////////////////////////////////////////////
    public final String preset_name_str = "preset name";

    protected HashMap<String, Para> current_paras = new HashMap<>();
    protected ArrayList<String> para_names = new ArrayList<>();

///////////////////////////////////////////////////////////////////////////////
    public Editable_Preset() {
        Para preset_para = new Para(preset_name_str, "String", "", "", "", false);
        current_paras.put(preset_name_str, preset_para);
        para_names.add(preset_name_str);
    }

    public Editable_Preset(String jstr) {
        update_from_jstr(jstr);
    }

    public ArrayList<String> get_para_names_by_mode(boolean hideadv) {
        return hideadv ? get_basic_paranames() : get_all_paranames();
    }

    protected void update_from_jstr(String jstr) {
        current_paras.clear();
        para_names.clear();

        String[] para_lines = jstr.split("\n");

        for (String para_line : para_lines) {
            Para temppara = new Para(para_line, true);
            current_paras.put(temppara.get_name(), temppara);
            para_names.add(temppara.get_name());
        }

        if (!para_names.contains(preset_name_str)) {
            throw new IllegalArgumentException("Mandantory para \"".concat(preset_name_str)
                    .concat("\" not found in input string"));
        }
    }

///////////////////////////////////////////////////////////////////////////////
    @Override
    public String toString() {
        ArrayList<String> allparanames = get_all_paranames();
        ArrayList<String> lines = new ArrayList<>();

        for (String para_name : allparanames) {
            Para thispara = get_para(para_name);
            if (thispara != null) {
                lines.add(thispara.toSimpleString());
            }
        }

        return String.join("\n", lines);
    }

///////////////////////////////////////////////////////////////////////////////
    public String get_current_preset_json(boolean include_adv) {
        String jstr = "{";
        ArrayList<String> allparanames = include_adv ? get_all_paranames() : get_basic_paranames();
        Gson gson = new Gson();

        int cnt = allparanames.size();
        int i = 0;
        for (String paraname : allparanames) {
            i++;
            Para thispara = get_para(paraname);
            String thisparavalue = get_current_value(paraname);

            if (thispara != null && thisparavalue != null) {
                if (thispara.get_type_name().equalsIgnoreCase("string")) {
                    thisparavalue = gson.toJson(thisparavalue, String.class
                    );
                } else {
                    thisparavalue = thisparavalue.isEmpty() ? "null" : thisparavalue;
                }

                jstr = jstr.concat(gson.toJson(paraname))
                        .concat(Para.sepkvjson)
                        .concat(thisparavalue)
                        .concat(i == cnt ? "" : Para.seplinejson);
            }
        }

        jstr = jstr.concat("}");

        return jstr;
    }

    public HashMap<String, String> get_current_preset() {
        HashMap<String, String> current_preset = new HashMap<>();
        ArrayList<String> all_paranames = get_all_paranames();
        for (String paraname : all_paranames) {
            String value = get_current_value(paraname);
            if (value != null) {
                current_preset.put(paraname, value);
            }
        }
        return current_preset;
    }

    public boolean set_current_preset(HashMap<String, String> preset) {
        if (preset == null) {
            return false;
        }

        ArrayList<String> all_paranames = get_all_paranames();
        if (!preset.keySet().containsAll(all_paranames)) {
            return false;
        }

        for (String paraname : all_paranames) {
            String value = preset.getOrDefault(paraname, null);
            if (value == null) {
                return false;
            }
            set_current_value(paraname, value);
        }

        return true;
    }

///////////////////////////////////////////////////////////////////////////////
    public String get_current_value(String paraname) {
        Para thispara = get_para(paraname);
        if (thispara != null) {
            return thispara.get_current_value();
        } else {
            return null;
        }
    }

    public String get_current_preset_name() {
        return get_current_value(preset_name_str);
    }

    public String set_current_preset_name(String preset_name) {
        if (preset_name != null && !preset_name.isEmpty() && !preset_name.equalsIgnoreCase("null")) {
            set_current_value(preset_name_str, preset_name);
        }
        return get_current_preset_name();
    }

    public String set_current_value(String paraname, String value) {
        Para thispara = get_para(paraname);
        if (thispara != null) {
            thispara.set_current_value(value);
            current_paras.put(paraname, thispara);
        }
        return get_current_value(paraname);
    }

    public void clear_current_values() {
        ArrayList<String> allparanames = get_all_paranames();
        for (String para_name : allparanames) {
            set_current_value(para_name, "");
        }
    }

///////////////////////////////////////////////////////////////////////////////
    public ArrayList<String> get_basic_paranames() {
        String[] current_para_names = para_names.toArray(new String[0]);
        ArrayList<String> basic_para_names = new ArrayList<>();

        for (String paraname : current_para_names) {
            Para thispara = get_para(paraname);
            if (!thispara.get_isadvparam()) {
                basic_para_names.add(paraname);
            }
        }

        return basic_para_names;
    }

    public ArrayList<String> get_adv_paranames() {
        String[] current_para_names = para_names.toArray(new String[0]);
        ArrayList<String> adv_para_names = new ArrayList<>();

        for (String paraname : current_para_names) {
            Para thispara = get_para(paraname);
            if (thispara.get_isadvparam()) {
                adv_para_names.add(paraname);
            }
        }

        return adv_para_names;
    }

    public ArrayList<String> get_all_paranames() {
        ArrayList<String> all_paranames = get_basic_paranames();
        all_paranames.addAll(get_adv_paranames());
        return all_paranames;
    }

    public Para get_para(String paraname) {
        if (paraname == null || paraname.isEmpty() || paraname.equalsIgnoreCase("null")) {
            return null;
        }

        if (para_names.contains(paraname)) {
            if (!current_paras.containsKey(paraname)) {
                if (paraname.equals(preset_name_str)) {
                    current_paras.put(paraname,
                            new Para(preset_name_str, "String", "", "", "", false));
                    return current_paras.get(paraname);
                } else {
                    para_names.remove(paraname);
                    return null;
                }
            } else {
                return current_paras.get(paraname);
            }
        } else {
            return null;
        }
    }

    public Para add_update_para(Para para) {
        if (para == null) {
            return null;
        }

        String paraname = para.get_name();
        Para existing_para = get_para(paraname);

        if (existing_para == null) {
            // add new para
            current_paras.put(paraname, para);
            para_names.add(paraname);
        } else {
            // update existing para unless it's preset_name or type conflict
            if (!paraname.equals(preset_name_str)) {
                if (!para.get_type_name().equals(existing_para.get_type_name())) {
                    throw new IllegalArgumentException("CANNOT change the type of an existing Para");
                }

                para.set_current_value(existing_para.get_current_value());
                current_paras.put(paraname, para);
            }
        }

        return get_para(paraname);
    }

    public Para remove_para(String paraname) {
        Para existing_para = get_para(paraname);

        if (existing_para != null && !paraname.equals(preset_name_str)) {
            existing_para = current_paras.remove(paraname);
            para_names.remove(paraname);
        }

        return existing_para;
    }

}
