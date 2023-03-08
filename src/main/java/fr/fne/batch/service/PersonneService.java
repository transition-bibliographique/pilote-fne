package fr.fne.batch.service;

import fr.fne.batch.model.dto.Personne;
import fr.fne.batch.repository.PersonneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonneService {
    @Autowired
    private PersonneRepository personneRepository;

    public List<Personne> getPersonne() {
        return personneRepository.findAllByOrderByLabelAsc();
    }

    public void savePersonne(String nom){
        personneRepository.insertPersonne(nom);
    }
}
