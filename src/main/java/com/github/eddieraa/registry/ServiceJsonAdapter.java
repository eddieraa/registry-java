package com.github.eddieraa.registry;

import java.io.IOException;


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
        out.endObject();
    }

    @Override
    public Service read(JsonReader in) throws IOException {
        Service s = new Service();
        in.beginObject();
        boolean inObject = false;
        while (in.hasNext()) {
            JsonToken token = in.peek();
            if (token == JsonToken.BEGIN_OBJECT) {
                in.beginObject();
                inObject = true;
                continue;
            }
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
                    s.timestamp = s.new Timestamps();
                    break;
                case "Registered":
                case "registered":
                    s.timestamp.registered = in.nextLong();
                    break;
                case "Duration":
                case "duration":
                    s.timestamp.duration = in.nextInt();
                    break;
                    
                default:
                    in.skipValue();
                    break;
            }
            if (inObject) {
                token = in.peek();
                if (token == JsonToken.END_OBJECT) {
                    inObject = false;
                    in.endObject();
                }
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
