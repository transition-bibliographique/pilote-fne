package fr.fne.batch.conf;

import fr.fne.batch.model.dto.Personne;
import org.json.JSONObject;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PersonneRowMapper implements RowMapper<Personne> {
    @Override
    public Personne mapRow(ResultSet rs, int rowNum) throws SQLException {
        Personne personne = new Personne();

        JSONObject json = new JSONObject(rs.getString(1));
        JSONObject props = json.optJSONObject("properties");
        if (props != null) {
            personne.setPpn(props.getString("ppn"));
            personne.setLabel(props.getString("label"));
            personne.setDescription(props.getString("description"));
            personne.setUrlPerenne(props.getString("urlPerenne"));
            personne.setType(props.getString("type"));
            personne.setIdISNI(props.getString("idISNI"));
            personne.setNom(props.getString("nom"));
            personne.setPrenom(props.getString("prenom"));
            personne.setDateNaissance(props.getString("dateNaissance"));
            personne.setDateDeces(props.getString("dateDeces"));
            personne.setActivite(props.getString("activite"));
            personne.setNoteBio(props.getString("noteBio"));
            personne.setTitreOeuvre(props.getString("titreOeuvre"));
            personne.setLangue(props.getString("langue"));
            personne.setPointAcces(props.getString("pointAcces"));
        }
        return personne;
    }
}
