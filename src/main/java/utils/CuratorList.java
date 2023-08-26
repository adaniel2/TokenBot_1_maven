package utils;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CuratorList {
    @JsonProperty("curators")
    private List<Curator> curators;


    public List<Curator> getCurators() {
        return this.curators;
    }

    public void setCurators(List<Curator> curators) {
        this.curators = curators;
    }

}