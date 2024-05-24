package com.offer.services.admin;

import com.offer.entity.AppelOffre;
import com.offer.entity.Categorie;
import com.offer.entity.Offre;
import com.offer.repository.AppelOffreRepository;
import com.offer.repository.CategorieRepository;
import com.offer.repository.CategorieRepository;
import com.offer.repository.OffreRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
	 @Autowired
    private final AppelOffreRepository appelOffreRepository;
	 @Autowired
    private OffreRepository offreRepository;
	 @Autowired
	 private final CategorieRepository categorieRepository;

	  @PostConstruct
	    public void initCategories() {
	        createCategoryIfNotExists("Développement et Maintenance de Logiciels", "Création de systèmes sur mesure, maintenance de systèmes existants.");
	        createCategoryIfNotExists("Infrastructure IT", "Mise en place et maintenance de serveurs, réseaux.");
	        createCategoryIfNotExists("Cybersécurité", "Solutions de sécurité informatique, audits de sécurité.");
	        createCategoryIfNotExists("Cloud et Hébergement", "Services de cloud computing, hébergement de données.");
	    }
	  private void createCategoryIfNotExists(String name, String description) {
	        List<Categorie> existingCategories = categorieRepository.findAll();
	        boolean exists = existingCategories.stream().anyMatch(c -> c.getNomcategorie().equals(name));
	        if (!exists) {
	            Categorie newCategory = new Categorie();
	            newCategory.setNomcategorie(name);
	            newCategory.setDescription(description);
	            categorieRepository.save(newCategory);
	        }
	    }
	  @Override
	  public List<Categorie> getAllCategories() {
	        return categorieRepository.findAll();
	    }
	  @Override
	    public List<AppelOffre> getAppelOffresByCategorieId(UUID categorieId) {
	        return appelOffreRepository.findByCategorieId(categorieId);
	    }

    @Override
    public AppelOffre createAppelOffre(AppelOffre appelOffre) {
        try {
            AppelOffre newAppelOffer = new AppelOffre();
            newAppelOffer.setTitre(appelOffre.getTitre());
            newAppelOffer.setDescription(appelOffre.getDescription());
            newAppelOffer.setDatelimitesoumission(appelOffre.getDatelimitesoumission());
            newAppelOffer.setEntrepriseId(appelOffre.getEntrepriseId());
            newAppelOffer.setImg(appelOffre.getImg());
            newAppelOffer.setLocalisation(appelOffre.getLocalisation());
            newAppelOffer.setDocument(appelOffre.getDocument());
            return appelOffreRepository.save(appelOffre);
        } catch (Exception e) {
            return null;
        }

    }
    public boolean canDeleteOrUpdateAppelOffre(UUID appelOffreId) {
        List<Offre> offres = offreRepository.findByAppeloffre_Id(appelOffreId);
        return offres.isEmpty(); // Returns true if there are no offers, indicating delete/update is allowed
    }

    @Override
    public List<Offre> getOffresByUserId(String userid) {
        return offreRepository.findByUserid(userid);
    }
    @Override
    public List<AppelOffre> getAllAppelOffres() {
        return appelOffreRepository.findAll();
    }

    @Override
    public Optional<AppelOffre> getAppelOffreById(UUID id) {
        return appelOffreRepository.findById(id);
    }

    @Override
    public AppelOffre updateAppelOffre(UUID appelOffreId, AppelOffre updatedAppelOffre) {
        if (canDeleteOrUpdateAppelOffre(appelOffreId)) {
            return appelOffreRepository.save(updatedAppelOffre);
        } else {
            throw new IllegalStateException("Cannot update AppelOffre as there are related Offres");
        }
    }
    @Override
    public void deleteAppelOffre(UUID appelOffreId) {
        if (canDeleteOrUpdateAppelOffre(appelOffreId)) {
            appelOffreRepository.deleteById(appelOffreId);
        } else {
            throw new IllegalStateException("Cannot delete AppelOffre as there are related Offres");
        }
    }
    @Override
    public List<AppelOffre> getAppelOffresByEntrepriseId(UUID entrepriseId) {
        return appelOffreRepository.findByEntrepriseId(entrepriseId);
    }
    
//    GESTION OFFRE
    
    @Override
    public Offre createOffre(Offre offre) {
        return offreRepository.save(offre);
    }

    
    @Override
    public Offre updateOffre(Offre offre) {
        return offreRepository.save(offre);
    }

    @Override
    public void deleteOffre(UUID id) {
        offreRepository.deleteById(id);
    }

    @Override
    public List<Offre> getAllOffres() {
        return offreRepository.findAll();
    }

    @Override
    public Optional<Offre> getOffreById(UUID id) {
        return offreRepository.findById(id);
    }

    @Override
    public List<Offre> listAllOffresByAppelOffreId(UUID appelOffreId) {
        return offreRepository.findByAppeloffre_Id(appelOffreId);
    }
}
