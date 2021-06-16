package com.github.eddieraa.registry;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.github.eddieraa.registry.Service.Timestamps;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class ServiceJsonAdapter extends TypeAdapter<Service> {

    @Override
    public void write(JsonWriter out, Service s) throws IOException {
        out.beginObject();
        write(out, "name", s.name);
        write(out, "net", s.network);
        write(out, "url", s.url);
        write(out, "v",s.version);
        write(out, "h",s.host);
        write(out, "add",s.address);
        if (s.timestamp!=null) {
            out.name("t");
            out.beginObject();
            out.name("registered").value(s.timestamp.registered);
            out.name("duration").value(s.timestamp.duration);
            out.endObject();
        }
        if (s.kv != null && !s.kv.isEmpty()) {
            out.name("kv");
            out.beginObject();
            for (Entry<String,String> e : s.kv.entrySet()) {
                out.name(e.getKey()).value(e.getValue());
            }
            out.endObject();
        }
        out.endObject();
    }

    Map<String,String> readKV(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.BEGIN_OBJECT) {
            in.beginObject();
        } 
        Map<String, String> res = new HashMap<>();
        while (in.peek() != JsonToken.END_OBJECT) {
            if (in.peek() == JsonToken.NAME) {
                String k = in.nextName();
                if (in.peek() == JsonToken.STRING) {
                    String v = in.nextString();
                    res.put(k, v);
                }
            } else {
                in.skipValue();
            }
        }
        in.endObject();
        return res;
    }

    Timestamps readTimestamps (JsonReader in, Service s) throws IOException {
        Timestamps t =s.new Timestamps();
        in.beginObject();
        while (in.peek() != JsonToken.END_OBJECT) {
            if (in.peek() == JsonToken.NAME) {
                String name = in.nextName();
                switch (name) {
                    case "Registered":
                    case "registered":
                        t.registered = in.nextLong();
                        break;
                    case "Duration":
                    case "duration":
                        t.duration = in.nextInt();
                        break;
                }
            } else {
                in.skipValue();
            }
        }
        in.endObject();
        return t;
    }


    @Override
    public Service read(JsonReader in) throws IOException {
        Service s = new Service();
        in.beginObject();
        
        while (in.hasNext() && in.peek()!=JsonToken.END_OBJECT) {
            String name = in.nextName();
            
            switch (name) {
                case "name":
                    s.name = in.nextString();
                    break;
                case "net":
                    s.network = in.nextString();
                    break;
                case "url":
                    s.url = in.nextString();
                    break;
                case "add":
                    s.address = in.nextString();
                    break;
                case "v":
                    s.version = in.nextString();
                    break;
                case "h":
                    s.host = in.nextString();
                    break;
                case "t":
                    s.timestamp = readTimestamps(in, s);
                    break;
                
                case "kv":
                case "KV":
                    s.kv = readKV(in);
                    break;
                default:
                    in.skipValue();
                    break;
            }
            
        }
        in.endObject();
        
        
        return s;
    }
    void write(JsonWriter out, String jsonName, String value) throws IOException {
       if (value == null || value.isEmpty()) return;
       out.name(jsonName).value(value);
    }
    
}
