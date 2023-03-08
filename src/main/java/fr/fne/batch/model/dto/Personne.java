package fr.fne.batch.model.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@Entity
public class Personne {
    @XmlElement(name = "contenu")
    private String contenu;

    @XmlElement(name = "label")
    @Id
    private String label;

    @XmlElement(name = "description")
    private String description;

    @XmlElement(name = "urlPerenne")
    private String urlPerenne;

    @XmlElement(name = "type")
    private String type;

    @XmlElement(name = "idISNI")
    private String idISNI;

    @XmlElement(name = "nom")
    private String nom;

    @XmlElement(name = "prenom")
    private String prenom;

    @XmlElement(name = "dateNaissance")
    private String dateNaissance;

    @XmlElement(name = "dateDeces")
    private String dateDeces;

    @XmlElement(name = "activite")
    private String activite;

    @XmlElement(name = "noteBio")
    private String noteBio;

    @XmlElement(name = "titreOeuvre")
    private String titreOeuvre;

    @XmlElement(name = "langue")
    private String langue;

    @XmlElement(name = "pointAcces")
    private String pointAcces;
}
