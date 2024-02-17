package utils;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Curator {

    @JsonProperty("name")
    private String name;

    @JsonProperty("id")
    private String id;


    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
}
