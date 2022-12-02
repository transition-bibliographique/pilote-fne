package fr.fne.batch.model.fne;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "mainsnak",
        "type",
        "rank"
})
public class Claims {

    @JsonProperty("mainsnak")
    private Mainsnak mainsnak;
    @JsonProperty("type")
    private String type;
    @JsonProperty("rank")
    private String rank;

    @JsonProperty("mainsnak")
    public Mainsnak getMainsnak() {
        return mainsnak;
    }

    @JsonProperty("mainsnak")
    public void setMainsnak(Mainsnak mainsnak) {
        this.mainsnak = mainsnak;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("rank")
    public String getRank() {
        return rank;
    }

    @JsonProperty("rank")
    public void setRank(String rank) {
        this.rank = rank;
    }


}
