package fr.fne.batch.model.fne;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "fr"
})
public class Label {
    @JsonProperty("fr")
    private Fr fr;

    @JsonProperty("fr")
    public Fr getFr() {
        return fr;
    }

    @JsonProperty("fr")
    public void setFr(Fr fr) {
        this.fr = fr;
    }


}

