package fr.fne.batch.repository;

import fr.fne.batch.model.dto.Personne;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PersonneRepository extends JpaRepository<Personne, Long> {
        @Query( value = "select i.code_1 as codecourt, l.* " +
                "from LANG_LABEL l left join LANG_ISO_639_2_TO_1 i ON l.code=i.code_2 " +
                "order by l.label asc", nativeQuery = true)
        List<Personne> findAllByOrderByLabelAsc();

        @Modifying
        @Query(value =  "select * from ag_catalog.cypher ('family_tree', $$\n" +
                        "        create (\\:Person {" +
                        "               name\\:':nom'," +
                        "               titles\\:['Test']," +
                        "               year_born\\: 1980," +
                        "               year_died\\: 2068" +
                        "        })\n" +
                        "$$) as (person ag_catalog.agtype)",
                nativeQuery = true)
        public int insertPersonne(@Param("nom")String nom);
}
