package fr.fne.batch.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.fne.batch.model.autorite.Controlfield;
import fr.fne.batch.model.autorite.Datafield;
import fr.fne.batch.model.autorite.Record;
import fr.fne.batch.model.autorite.Subfield;
import fr.fne.batch.model.dto.Personne;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class DtoAutoriteToItem {

    private final Logger logger = LoggerFactory.getLogger(DtoAutoriteToItem.class);

    /**
     * DTO :
     * Notice MarcXML vers Personne
     * @param r
     * @param proprietes
     * @return
     */
    public Personne unmarshallerNotice(Record r)
    {
        Personne p = new Personne();
        ObjectMapper objectMapper = new ObjectMapper();

        try {

            p.setContenu(objectMapper.writeValueAsString(r));

            //Il s'agit d'un type d'entité Personne (Q1)
            p.setType("Personne");

            //200$a, 200$b, 200$f [001]
            String label = "";
            //001
            String ppn = "";
            //<valeur de zone 010 $a>
            //<valeur de zone 003>
            //<valeur de zone 033 $a>
            String description = "";

            //String alias = "";
            //ArrayList<String> listeAlias = new ArrayList<>();

            //construction label
            int posTranslit = -1;
            int pos200 = 0;
            for (Datafield d : r.getDatafieldList()) {
                if (d.getTag().equalsIgnoreCase("200")) {
                    pos200++;
                    for (Subfield s : d.getSubfieldList()) {
                        if (s.getCode().equalsIgnoreCase("7") &&
                                s.getValue().length() > 5 &&
                                s.getValue().substring(4, 6).equalsIgnoreCase("ba")) {
                            posTranslit=pos200;
                        }
                    }
                }
            }

            pos200 = 0;
            for (Datafield d : r.getDatafieldList()) {
                if (d.getTag().equalsIgnoreCase("200")) {
                    pos200++;
                    if ((posTranslit!=-1 && posTranslit==pos200) || posTranslit==-1) {
                        for (Subfield s : d.getSubfieldList()) {
                            switch (s.getCode().toLowerCase()) {
                                case "a": label += s.getValue(); break;
                                case "b" : label += ", " + s.getValue(); break;
                                case "d" : label += " " + s.getValue(); break;
                                case "f" : label += ", " + s.getValue(); break;
                            }
                        }
                    }
                    /*else {
                        alias = "";
                        for (Subfield s : d.getSubfieldList()) {
                            switch (s.getCode().toLowerCase()) {
                                case "a": alias += s.getValue(); break;
                                case "b": alias += ", " + s.getValue(); break;
                                case "d": alias += " " + s.getValue(); break;
                                case "f": alias += ", " + s.getValue(); break;
                            }
                        }
                        listeAlias.add(alias);
                    }*/
                }
            }
            // construction description
            String isni = r.getDatafieldList().stream()
                    .filter(z -> z.getTag().equals("010"))
                    .flatMap(sz -> sz.getSubfieldList().stream())
                    .filter(sz -> sz.getCode().equals("a"))
                    .map(sz -> sz.getValue())
                    .findFirst()
                    .orElse("");

            String idref = r.getControlfieldList().stream()
                    .filter(c -> c.getTag().equals("003"))
                    .map(c -> c.getValue())
                    .findFirst()
                    .orElse("");

            String ark = r.getDatafieldList().stream()
                    .filter(z -> z.getTag().equals("033"))
                    .filter(z -> z.getSubfieldList().stream().anyMatch(sz -> sz.getCode().equals("2") &&  sz.getValue().equalsIgnoreCase("BNF")))
                    .flatMap(sz -> sz.getSubfieldList().stream())
                    .filter(sz -> sz.getCode().equals("a"))
                    .map(sz -> sz.getValue())
                    .findFirst()
                    .orElse("");

            if (!isni.isEmpty()) {
                description += isni ;
            }
            if (!idref.isEmpty()) {
                if (!description.isEmpty()){
                    description += " - ";
                }
                description += idref ;
            }
            if (!ark.isEmpty()) {
                if (!description.isEmpty()){
                    description += " - ";
                }
                description += ark ;
            }
            p.setDescription(description);

            //construction aliases
            // Nom 	<valeur de 700 $a>
            // Prénom 	<valeur de 700 $b>
            // Langue de l'interface	par défaut fr

            /*for (Datafield d : r.getDatafieldList()) {
                if (d.getTag().equalsIgnoreCase("400") || d.getTag().equalsIgnoreCase("700")) {
                    alias = "";
                    for (Subfield s : d.getSubfieldList()) {
                       switch (s.getCode().toLowerCase()) {
                           case "a": alias += s.getValue(); break;
                           case "b" : alias += ", " + s.getValue(); break;
                           case "d" : alias += " " + s.getValue(); break;
                           case "f" : alias += ", " + s.getValue(); break;
                        }
                    }
                    listeAlias.add(alias);
                }
            }

            List<String> aliasNoDoublon = listeAlias.stream().distinct().collect(Collectors.toList());
            for (String al : aliasNoDoublon){
                itemDocumentBuilder = itemDocumentBuilder.withAlias(al, "fr");
            }*/

            //Leader :
            //itemDocumentBuilder = this.addStmtString(itemDocumentBuilder,"Zoneleader", r.getLeader(), "leader", objectMapper.writeValueAsString(r.getLeader()));

            //ControlFields :
            for (Controlfield c : r.getControlfieldList()){
                if (c.getTag().equalsIgnoreCase("001")){
                    ppn = c.getValue(); //Pour construire le label
                }

                if (c.getTag().equalsIgnoreCase("003")){
                    p.setUrlPerenne(c.getValue());
                }
            }

            //DataFields :
            for (Datafield d : r.getDatafieldList()){

                if (d.getTag().equalsIgnoreCase("010")) {
                    for (Subfield s : d.getSubfieldList()) {
                        if (s.getCode().equalsIgnoreCase("a")) {
                            p.setIdISNI(s.getValue());
                        }
                    }
                }

                if (d.getTag().equalsIgnoreCase("101")) {
                    for (Subfield s : d.getSubfieldList()) {
                        if (s.getCode().equalsIgnoreCase("a")) {
                            p.setLangue(s.getValue());
                        }
                    }
                }

                if (d.getTag().equalsIgnoreCase("103")) {
                    for (Subfield s : d.getSubfieldList()) {
                        if (s.getCode().equalsIgnoreCase("a")) {
                            p.setDateNaissance(s.getValue());
                        }
                        else if (s.getCode().equalsIgnoreCase("b")) {
                            p.setDateDeces(s.getValue());
                        }
                    }
                }

                if (d.getTag().equalsIgnoreCase("200")){
                    for (Subfield s : d.getSubfieldList()){
                        if (s.getCode().equalsIgnoreCase("a")){
                            p.setNom(s.getValue());
                        }
                        else if (s.getCode().equalsIgnoreCase("b")){
                            p.setPrenom(s.getValue());
                        }
                    }
                }

                if (d.getTag().equalsIgnoreCase("240")){
                    for (Subfield s : d.getSubfieldList()){
                        if (s.getCode().equalsIgnoreCase("t")){
                            p.setTitreOeuvre(s.getValue());
                        }
                    }
                }

                if (d.getTag().equalsIgnoreCase("300")){
                    for (Subfield s : d.getSubfieldList()){
                        if (s.getCode().equalsIgnoreCase("a")){
                            p.setNoteBio(s.getValue());
                        }
                    }
                }

                if (d.getTag().equalsIgnoreCase("340")){
                    for (Subfield s : d.getSubfieldList()){
                        if (s.getCode().equalsIgnoreCase("a")){
                            p.setActivite(s.getValue());
                        }
                    }
                }

                if (d.getTag().equalsIgnoreCase("500")){
                    for (Subfield s : d.getSubfieldList()){
                        if (s.getCode().equalsIgnoreCase("3")){
                            p.setPointAcces(s.getValue());
                        }
                    }
                }

                //Ajout de statements pour tous les dataFields (la valeur affichée est celle du premier subfield trouvé d.getSubfieldList().get(0).getValue()):
                /*if (d.getSubfieldList().size()>0) {
                    itemDocumentBuilder = this.addStmtString(itemDocumentBuilder, "Zone" + d.getTag(), d.getSubfieldList().get(0).getValue(), d.getTag(), objectMapper.writeValueAsString(d));
                }*/
            }


            //Ajout du label construit : 200$a, 200$b, 200$f [001]
            p.setLabel(label+" ["+ppn+"]");
        }
        catch (Exception e) {
            logger.error("Erreur sur la notice : " + r + " :" + e.getMessage());
            e.printStackTrace();
        }
        return p;
    }

}
