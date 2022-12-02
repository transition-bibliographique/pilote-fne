package fr.fne.batch.model.fne;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "snaktype",
        "property",
        "datavalue"
})
public class Mainsnak {

    @JsonProperty("snaktype")
    private String snaktype;
    @JsonProperty("property")
    private String property;
    @JsonProperty("datavalue")
    private Datavalue datavalue;

    @JsonProperty("snaktype")
    public String getSnaktype() {
        return snaktype;
    }

    @JsonProperty("snaktype")
    public void setSnaktype(String snaktype) {
        this.snaktype = snaktype;
    }

    @JsonProperty("property")
    public String getProperty() {
        return property;
    }

    @JsonProperty("property")
    public void setProperty(String property) {
        this.property = property;
    }

    @JsonProperty("datavalue")
    public Datavalue getDatavalue() {
        return datavalue;
    }

    @JsonProperty("datavalue")
    public void setDatavalue(Datavalue datavalue) {
        this.datavalue = datavalue;
    }


}