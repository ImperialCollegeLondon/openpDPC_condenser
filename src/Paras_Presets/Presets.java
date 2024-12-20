/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2023, Imperial College London 
 * All rights reserved.

 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package Paras_Presets;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;


public class Presets extends Editable_Preset {
    
    public static void main(String[] args) {
        Presets presets = new Presets();
        
        presets.add_update_para(new Para("para1", "Double", "-0.2345", "", "-100", false));
        presets.add_update_para(new Para("para2", "Integer", "45", "100", "-100", true));
        presets.set_current_preset_name("preset1");
        presets.addupdate_current_preset_to_allpresets();
        
        Presets presets1 = new Presets();
        String jstr = presets.toString();
        presets1.update_allpresets_from_jstr(jstr);
        
        System.out.println(presets1.get_preset(presets1.get_current_preset_name()));
    }

///////////////////////////////////////////////////////////////////////////////
    // allpresets: {para_name -> {preset_name -> para_value for this preset}}
    // preset_names: all preset names in a list (ordered)
    HashMap<String, HashMap<String, String>> allpresets = new HashMap<>();
    ArrayList<String> preset_names = new ArrayList<>();
    
    public Presets() {
        super();
        allpresets.put(preset_name_str, new HashMap<>());
    }
    
    public Presets(String jstr) {
        update_allpresets_from_jstr(jstr);
    }

///////////////////////////////////////////////////////////////////////////////
    public void update_allpresets_from_jstr(String jstr) {
        String[] lines1 = jstr.split("\nallpresets:\n");
        
        super.update_from_jstr(lines1[0].replaceFirst("current_preset:\n", ""));
        
        String[] lines2 = lines1[1].split("\npreset_names:\n");
        
        Gson gson = new Gson();
        Type type_allpresets = new TypeToken<HashMap<String, HashMap<String, String>>>() {
        }.getType();
        allpresets = gson.fromJson(lines2[0], type_allpresets);
        
        Type StringArrayListType = new TypeToken<ArrayList<String>>() {}.getType();
        preset_names = gson.fromJson(lines2[1], StringArrayListType);
    }
    
    @Override
    public String toString() {
        String jstr = "";
        
        jstr = jstr.concat("current_preset:")
                .concat("\n").concat(super.toString())
                .concat("\n");
        
        Gson gson = new Gson();
        jstr = jstr.concat("allpresets:").concat("\n").concat(gson.toJson(allpresets, HashMap.class))
                .concat("\n");
        
        jstr = jstr.concat("preset_names:")
                .concat("\n").concat(gson.toJson(preset_names, ArrayList.class));
        
        return jstr;
    }
     
///////////////////////////////////////////////////////////////////////////////
    public ArrayList<String> get_all_preset_names() {
        return preset_names;
    }
    
    public void remove_preset(String preset_name) {
        for (Map.Entry<String, HashMap<String, String>> entry : allpresets.entrySet()) {
            HashMap<String, String> presetvalues = entry.getValue();
            presetvalues.remove(preset_name);
            entry.setValue(presetvalues);
            allpresets.put(entry.getKey(), entry.getValue());
        }
        preset_names.remove(preset_name);
    }
    
    public boolean load_preset_to_current_preset(String preset_name) {
        HashMap<String, String> preset = get_preset(preset_name);
        if (preset != null) {
            return set_current_preset(preset);
        }
        return false;
    }
    
    public boolean addupdate_current_preset_to_allpresets() {
        String current_preset_name = get_current_preset_name();
        if (current_preset_name != null && !current_preset_name.isEmpty()
                && !current_preset_name.equalsIgnoreCase("null")) {
            HashMap<String, String> current_preset = get_current_preset();
            
            for (Map.Entry<String, String> entry : current_preset.entrySet()) {
                String paraname = entry.getKey();
                HashMap<String, String> thispara_values = allpresets.get(paraname);
                thispara_values.put(current_preset_name, entry.getValue());
                allpresets.put(paraname, thispara_values);
            }
            
            if (!preset_names.contains(current_preset_name)) {
                preset_names.add(current_preset_name);
            }
            
            return true;
        } else {
            return false;
        }
    }
    
    public HashMap<String, String> get_preset(String preset_name) {
        if (preset_name != null && !preset_name.isEmpty()
                && !preset_name.equalsIgnoreCase("null")) {
            if (preset_names.contains(preset_name)) {
                ArrayList<String> all_paranames = get_all_paranames();
                
                HashMap<String, String> preset = new HashMap<>();
                for (String paraname : all_paranames) {
                    String thispara_value = allpresets.get(paraname).getOrDefault(preset_name, null);
                    if (thispara_value == null) {
                        preset_names.remove(preset_name);
                        return null;
                    }
                    preset.put(paraname, thispara_value);
                }
                
                return preset;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

///////////////////////////////////////////////////////////////////////////////
// whatever below should make sure allpresets.getKeySet() == get_all_paranames()
///////////////////////////////////////////////////////////////////////////////
    @Override
    public Para get_para(String paraname) {
        Para thispara = super.get_para(paraname);
        
        if (thispara == null && allpresets.containsKey(paraname)) {
            allpresets.remove(paraname);
        }
        
        return thispara;
    }
    
    @Override
    public Para add_update_para(Para para) {
        if (para == null) {
            return null;
        }
        
        String paraname = para.get_name();
        Para thispara = super.add_update_para(para);

        // if thispara is not null  
        // update all preset values unless it's preset_name para
        if (thispara != null && !paraname.equals(preset_name_str)) {
            HashMap<String, String> existing_para_values
                    = allpresets.getOrDefault(paraname, new HashMap<>());
            
            for (String presetname : preset_names) {
                String current_value = existing_para_values.getOrDefault(presetname, "");
                thispara.set_current_value(current_value);
                existing_para_values.put(presetname, thispara.get_current_value());
            }
            allpresets.put(paraname, existing_para_values);
        }
        
        return get_para(paraname);
    }
    
    @Override
    public Para remove_para(String paraname) {
        Para thispara = super.remove_para(paraname);
        
        if (paraname != null & !paraname.equals(preset_name_str)) {
            allpresets.remove(paraname);
        }
        
        return thispara;
    }
    
}
