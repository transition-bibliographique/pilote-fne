package fr.fne.batch.bnf.notice.model;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableInt;

/**
 * Modele pourvant représenter <b>une notice d'autorité ou une notice bibliographique</b>.<br>
 * Elle utilise le format Intermarc ce qui explique qu'elle possède des zones {@link ZoneN} et sous zones {@link SousElementN}.<br>
 * Ce modele est utilisé pour contenir le résultat du parse d'un XML intermarcXMarc provenant soit du webservice "noticeservice" soit du
 * filesysem "le puit".
 * 
 * @author Thibaud SENALADA
 * 
 * @author Mickael KWINE KWOR MAN
 *
 */
public class Notice {

	private static final String WILDCARD = "*";

	private String id;

	private String numero;

	private String format;

	private String type;

	private List<Notice> notices;

	// On utilise une map pour réduire la recherche sur les zone
	// On utilise une list de zone dans la map pour traiter le cas de zone répétable
	private Map<String, List<ZoneN>> zoneNs;

	private List<PexN> pexNs;

	private List<AlterN> alterNs;

	private List<AttrN> attrNs;

	public Notice() {
		this.notices = new ArrayList<>();
		this.zoneNs = new LinkedHashMap<>();
		this.pexNs = new ArrayList<>();
		this.alterNs = new ArrayList<>();
		this.attrNs = new ArrayList<>();
	}

	/**
	 * Retourne {@link true} si la notice contient des analytiques, sinon {@link false}
	 * @return
	 */
	public boolean hasAnalytic() {
		return (this.getNbAnalytics() > 0);
	}

	/**
	 * Retourne le nombre d'analytique de niveau 1
	 * @return
	 */
	public int getNbAnalytics() {
		return this.notices.size();
	}

	/**
	 * Retourne le nombre d'anlytique de niveau 2
	 * @return
	 */
	public int getNbAnalyticsN2() {

		Integer sum = 0;

		// plus le nombre de niv 2
		for (Notice noticeN1 : this.notices) {
			sum += noticeN1.getNbAnalytics();
		}
		return sum;
	}

	/**
	 * Retourne vrai si la notice ne contient que des zones avec le code donné en paramètre.
	 * 
	 * @param codeZone
	 * @return
	 */
	public Boolean doesNoticeContainOnlyZone(String... codeZone) {

		List<String> codeList = Arrays.asList(codeZone);

		// Si le caractère "joker" est donné on ne filtre pas les analytique sur les zones.
		if (codeList.contains(WILDCARD)) {
			return true;
		}

		List<ZoneN> allZones = this.getZoneNsByCode("*");

		// Dans le cas où il n'y a pas de zone, nul besoin de poursuivre.
		if (allZones.isEmpty()) {
			return false;
		}

		List<ZoneN> filteredZones = this.getZoneNsByCode(codeZone);

		// Si la liste de toutes les zones et différente en taille de la liste des zones filtrées par leur code
		// alors la notice ne contient pas que des zones au code souhaité.
		return (allZones.size() == filteredZones.size());
	}

	/**
	 * Compte le nombre d'analytique en excluant les analytiques ne contenant que des zones dont les codes sont donnés.
	 * 
	 * @param codeZone
	 * @return
	 */
	public Integer countAnalyticsExcludingZone(String... codeZone) {

		MutableInt count = new MutableInt(0);
		Integer countAllAnalytic = this.countAnalytics(count, "*");

		MutableInt countWithZone = new MutableInt(0);
		Integer countAnalyticWithCorrespondingZone = this.countAnalytics(countWithZone, codeZone);

		return countAllAnalytic - countAnalyticWithCorrespondingZone;
	}

	/**
	 * Retourne le nombre d'analytiques contenant uniquement la/les codes de zone passé en paramètre.<br>
	 * 
	 * Supporte le caractère spécial <b>"*"</b> pour retourner <b>toutes les zones</b>.
	 * 
	 * @param codeZone
	 * @return
	 */
	public Integer countAnalytics(String... codeZone) {

		MutableInt count = new MutableInt(0);
		this.countAnalytics(count, codeZone);

		return count.toInteger();

	}

	/**
	 * Méthode recursive comptant les analytiques composée uniquement des code de zones donné.
	 */
	private Integer countAnalytics(MutableInt count, String... codeZone) {

		Iterator<Notice> it = this.notices.iterator();

		while (it.hasNext()) {
			Notice notice = it.next();

			// Si la notice actuel ne contient que des zones du type donné, +1 au compteur.
			if (Boolean.TRUE.equals(this.doesNoticeContainOnlyZone(codeZone))) {
				count.increment();
			}

			// Récursion sur les notices.
			if (notice.hasAnalytic()) {
				notice.countAnalytics(count, codeZone);
			}
		}
		return count.toInteger();
	}

	/**
	 * Retourne le nombre de sous élément pour un code donné.
	 * 
	 * @param code
	 * @return
	 */
	public int countZonesNbyCode(String codeZone) {
		return this.getZoneNsByCode(codeZone).size();
	}

	/**
	 * Permet de compter le nombre de {@link SousElementN} selon un code zone et un code sous element
	 * 
	 * @param codeZone
	 *            Code de la {@link ZoneN}
	 * @param codeSousElement
	 *            Code du {@link SousElementN}
	 * @return
	 *         Le nombre de {@link SousElementN}
	 */
	public Integer countSousElementNbyCode(String codeZone, String codeSousElement) {
		int nbrSousElement = 0;
		for (ZoneN zoneN : this.getZoneNsByCode(codeZone)) {
			for (SousElementN sousElementN : zoneN.getSousElementNsByCode(codeSousElement)) {
				if (sousElementN != null) {
					nbrSousElement++;
				}
			}
		}
		return nbrSousElement;
	}

	/**
	 * 
	 * Retourne la liste des zones correspondant à des codes donnés.<br>
	 * (Permet de récupérer les zones répétable)<br>
	 * Supporte le caractère spécial <b>"*"</b> pour retourner <b>toutes les zones</b>.
	 * Supporte les zones 009x.
	 * 
	 * 
	 * @param codeArray
	 *            {@link String[]} tableau de code de zones.
	 * @return
	 *         {@link List} {@link ZoneN} correspondant aux codes
	 */
	public List<ZoneN> getZoneNsByCode(String... codeArray) {
		requireNonNull(codeArray);

		List<ZoneN> zoneNList = new ArrayList<>();

		List<String> codeList = Arrays.asList(codeArray);

		// Si le caractère "joker" est donné, on aplatit la map en list et on renvoie la totalité des zones.
		if (codeList.contains(WILDCARD)) {
			for (Entry<String, List<ZoneN>> entry : this.zoneNs.entrySet()) {
				zoneNList.addAll(entry.getValue());
			}
		} else {
			for (String code : codeList) {
				// Cas des zones 009a, 009b, etc.
				if (code.startsWith("009") && code.length() == 4) {
					List<ZoneN> zones009 = this.zoneNs.get("009");
					if (zones009 != null) {
						zones009.stream().filter(z -> z.getFirstSousElementNByCode("00").getValue().equals(code.substring(3))).forEach(zoneNList::add);
					}
				}
				// Cas standard
				if (this.zoneNs.get(code) != null) {
					zoneNList.addAll(this.zoneNs.get(code));
				}

			}
		}

		return zoneNList;
	}

	/**
	 * Récupère la première zone correspondant au code donné
	 * (Ne permet pas de récupérer les zones répétable)
	 * 
	 * @param code
	 *            {@link String} Code/Tag de la zone
	 * @return
	 *         Première {@link ZoneN} correspondant au code, retourne <code>null</code> si vide
	 */
	public ZoneN getFirstZoneNByCode(String code) {
		requireNonNull(code);
		
		for (ZoneN i : this.getZoneNsByCode(code)) {
			if (i != null) {
				return i;
			}
		}
		return null;
	}

	/**
	 * Récupère le premier sous element correspondant aux codes donnés
	 * 
	 * @param codeZone
	 *            {@link String} Code/tag de la zone
	 * @param codeSousElt
	 *            {@link String} Code/Tag de la sous zone
	 * @return
	 *         Premier sous element correspondant aux code zone/souszone
	 */
	public SousElementN getFirstSousElementByCode(String codeZone, String codeSousElt) {
		for (ZoneN i : this.getZoneNsByCode(codeZone)) {
			if (i != null) {
				return i.getFirstSousElementNByCode(codeSousElt);
			}
		}
		return null;
	}

	/**
	 * Ajoute une {@link ZoneN} à la {@link Notice}
	 * 
	 * @param zoneN
	 */
	public void addZoneN(ZoneN zoneN) {
		if (this.getZoneNs().containsKey(zoneN.getTag())) {
			this.getZoneNs().get(zoneN.getTag()).add(zoneN);
		} else {
			List<ZoneN> listZoneNs = new ArrayList<>();
			listZoneNs.add(zoneN);
			if (zoneN.getTag() == null) { // Le champs leader ne possede pas de tag
				zoneN.setTag("000");
			}
			this.getZoneNs().put(zoneN.getTag(), listZoneNs);
		}
	}

	/**
	 * Permet de tester si une sous zone équivaut à une valeur donnée.
	 * 
	 * @param codeZone
	 * @param codeSousZone
	 * @param value
	 * @return
	 */
	public boolean isValueInElementsN(String codeZone, String codeSousZone, String value) {

		List<ZoneN> zonesN = this.getZoneNsByCode(codeZone);
		if (zonesN == null) {
			return false;
		}

		for (ZoneN zoneN : zonesN) {
			List<SousElementN> sousZones = zoneN.getSousElementNs().get(codeSousZone);
			if (sousZones != null) {
				for (SousElementN sousZone : sousZones) {
					if (value.equals(sousZone.getValue())) {
						return true;
					}
				}
			}
		}

		return false;
	}
	
	/**
	 * Test si on retrouve dans l'indicateur d'une zone une valeur donnée.
	 * 
	 * @param codeZone
	 * 			{@link String} Code de la zone
	 * @param codeIndicateur
	 * 			{@link String} Peut prendre comme valeur ind1 ou ind2
	 * @param value
	 * 			{@link String} Valeur qu'on souhaite retrouvée
	 * @return
	 * 			{@link boolean}
	 */
	public boolean isValueInIndicateur(String codeZone, String codeIndicateur, String value) {

		requireNonNull(codeZone);
		requireNonNull(codeIndicateur);
		requireNonNull(value);

		List<ZoneN> zonesN = this.getZoneNsByCode(codeZone);

		for (ZoneN zoneN : zonesN) {
			if (codeIndicateur.equals("ind1") && value.equals(zoneN.getInd1())) {
				return true;
			}
			if (codeIndicateur.equals("ind2") && value.equals(zoneN.getInd2())) {
				return true;
			}
		}

		return false;
	}

	public boolean isValueInIndicateur(String codeZone, String codeIndicateur, String... values) {
		requireNonNull(codeZone);
		requireNonNull(codeIndicateur);
		requireNonNull(values);

		return Stream.of(values).anyMatch(s -> isValueInIndicateur(codeZone, codeIndicateur, s));
	}
	
	/**
	 * Test si la valeur d'une sous zone est contenue dans la valeur passée en paramètre quelque soit la casse
	 * 
	 * @param codeZone
	 * 			Code de la zone
	 * @param codeSousZone
	 * 			Code de la sous zone
	 * @param value
	 * 			Valeur à tester
	 * @return
	 */
	public boolean isValueInElementsNLowerCase(String codeZone, String codeSousZone, String value) {

		List<ZoneN> zonesN = this.getZoneNsByCode(codeZone);

		for (ZoneN zoneN : zonesN) {
			List<SousElementN> sousZones = zoneN.getSousElementNs().get(codeSousZone);
			if (sousZones != null) {
				for (SousElementN sousZone : sousZones) {					
					if (sousZone.getValue().toLowerCase().contains(value.toLowerCase())) {
						return true;
					}
				}
			}

		}

		return false;
	}
	
	/**
	 * Test si la valeur est contenu dans la valeur d'une sous zone quelque soit la casse
	 * 
	 * @param codeZone
	 * 			Code de la zone
	 * @param codeSousZone
	 * 			Code de la sous zone
	 * @param value
	 * 			Valeur à tester
	 * @return
	 */
	public boolean containsValueInElementsNLowerCase(String codeZone, String codeSousZone, String value) {

		String valueLower = value.toLowerCase();
		
		List<ZoneN> zonesN = this.getZoneNsByCode(codeZone);

		for (ZoneN zoneN : zonesN) {
			List<SousElementN> sousZones = zoneN.getSousElementNs().get(codeSousZone);
			if (sousZones != null) {
				for (SousElementN sousZone : sousZones) {
					if (sousZone.getValue().toLowerCase().contains(valueLower)) {
						return true;
					}
				}
			}

		}

		return false;
	}

	/**
	 * Test si la valeur d'une sous zone est égale aux valeurs passé en paramètre quelque soit la casse
	 * 
	 * @param codeZone
	 * 			Code de la zone
	 * @param codeSousZone
	 * 			Code de la sous zone
	 * @param value
	 * 			Valeurs à tester
	 * @return
	 */
	public boolean isValueInElementsNLowerCase(String codeZone, String codeSousZone, String... values) {
		return Stream.of(values).anyMatch(s -> isValueInElementsNLowerCase(codeZone, codeSousZone, s));
	}
	
	/**
	 * Permet de tester si une sous zone commence par une valeur donnée.
	 * 
	 * @param codeZone
	 * @param codeSousZone
	 * @param values
	 * @return
	 */
	public boolean isValueStartWithInElementsN(String codeZone, String codeSousZone, String... values) {

		List<ZoneN> zonesN = this.getZoneNsByCode(codeZone);

		for (ZoneN zoneN : zonesN) {
			List<SousElementN> sousZones = zoneN.getSousElementNs().get(codeSousZone);
			if (sousZones != null) {
				for (SousElementN sousZone : sousZones) {

					if (Stream.of(values).anyMatch(s -> sousZone.getValue().startsWith(s))) {
						return true;
					}
				}
			}
		}

		return false;
	}
	
	/**
	 * Permet de tester si une sous-zone a une valeur comprise dans un groupe de valeurs.
	 * 
	 * @param codeZone
	 * @param codeSousZone
	 * @param values
	 * @return
	 */
	public boolean isOneValueInElementsN(String codeZone, String codeSousZone, String... values) {

		List<ZoneN> zonesN = this.getZoneNsByCode(codeZone);

		for (ZoneN zoneN : zonesN) {
			List<SousElementN> sousZones = zoneN.getSousElementNs().get(codeSousZone);
			if (sousZones != null) {
				for (SousElementN sousZone : sousZones) {

					if (Stream.of(values).anyMatch(s -> sousZone.getValue().equals(s))) {
						return true;
					}
				}
			}
		}

		return false;
	}
	
	public boolean isValueInPosN(String codeZone, String codeSousZone, String codePosN, String value) {
		return this.getZoneNsByCode(codeZone).stream().anyMatch(z -> z.isValueInPosN(codeSousZone, codePosN, value));
	}
	
	public boolean isOneValueInPosN(String codeZone, String codeSousZone, String codePosN, String... values) {
		return this.getZoneNsByCode(codeZone).stream().anyMatch(z -> z.isOneValueInPosN(codeSousZone, codePosN, values));
	}

	/**
	 * Permet de tester si une sous zone commence par une valeur donnée en mettant en minuscule la valeur de la zone
	 * 
	 * @param codeZone
	 * @param codeSousZone
	 * @param values
	 * @return
	 */
	public boolean isValueStartWithInElementsNLowerCase(String codeZone, String codeSousZone, String... values) {

		List<ZoneN> zonesN = this.getZoneNsByCode(codeZone);

		for (ZoneN zoneN : zonesN) {
			List<SousElementN> sousZones = zoneN.getSousElementNs().get(codeSousZone);
			if (sousZones != null) {
				for (SousElementN sousZone : sousZones) {
					if (Stream.of(values).anyMatch(s -> sousZone.getValue().toLowerCase().startsWith(s))) {
						return true;
					}
				}
			}
		}

		return false;
	}
	
	public boolean isValueEqualsElementsNLowerCase(String codeZone, String codeSousZone, String... values) {

		List<ZoneN> zonesN = this.getZoneNsByCode(codeZone);

		for (ZoneN zoneN : zonesN) {
			List<SousElementN> sousZones = zoneN.getSousElementNs().get(codeSousZone);
			if (sousZones != null) {
				for (SousElementN sousZone : sousZones) {
					if (Stream.of(values).anyMatch(s -> sousZone.getValue().equalsIgnoreCase(s))) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Compte le nombre de zone données dans les ANLs de niveau 1
	 * @param codeZone
	 * @return le nombre de zone trouvées dans les ANLs de niveau 1
	 */
	public int countZoneInAnalyticsNiv1(String codeZone) {
		int count = 0;
		for (Notice notice : this.notices) {
			count += notice.countZonesNbyCode(codeZone);
		}
		return count;
	}
	
	/**
	 * Compte le nombre de sous zone données dans les ANLs de niveau 1
	 * @param codeZone
	 * @param codeSubZone
	 * @return le nombre de sous zone trouvées dans les ANLs de niveau 1
	 */
	public int countSubZoneInAnalyticsNiv1(String codeZone, String codeSubZone) {
		int count = 0;
		for(Notice notice : this.notices) {
			count += notice.countSousElementNbyCode(codeZone, codeSubZone);
		}
		return count;
	}
	
	/**
	 * Test si deux sous zones sont équivalentes
	 * @return
	 */
	public boolean testTwoSubZones(String codeZone, String subZone1, String value1, String subZone2, String value2) {

		List<ZoneN> listZones = this.getZoneNsByCode(codeZone);

		for (ZoneN zone : listZones) {
			boolean result = ((zone.getFirstSousElementNByCode(subZone1) != null && value1.equals(zone.getFirstSousElementNByCode(subZone1).getValue()))
					&&
					(zone.getFirstSousElementNByCode(subZone2) != null && value2.equals(zone.getFirstSousElementNByCode(subZone2).getValue())));
			if (result) {
				return true;
			}
		}

		return false;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getNumero() {
		return this.numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	public String getFormat() {
		return this.format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<Notice> getNotices() {
		return this.notices;
	}

	public void setNotices(List<Notice> notices) {
		this.notices = notices;
	}

	public Map<String, List<ZoneN>> getZoneNs() {
		return this.zoneNs;
	}

	public void setZoneNs(Map<String, List<ZoneN>> zoneNs) {
		this.zoneNs = zoneNs;
	}

	public List<PexN> getPexNs() {
		return this.pexNs;
	}

	public void setPexNs(List<PexN> pexNs) {
		this.pexNs = pexNs;
	}

	public List<AlterN> getAlterNs() {
		return this.alterNs;
	}

	public void setAlterNs(List<AlterN> alterNs) {
		this.alterNs = alterNs;
	}

	public List<AttrN> getAttrNs() {
		return this.attrNs;
	}

	public void setAttrNs(List<AttrN> attrNs) {
		this.attrNs = attrNs;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.alterNs, this.attrNs, this.format, this.id, this.notices, this.numero, this.pexNs, this.type, this.zoneNs);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Notice)) {
			return false;
		}
		Notice other = (Notice) obj;
		return Objects.equals(this.alterNs, other.alterNs) && Objects.equals(this.attrNs, other.attrNs) && Objects.equals(this.format, other.format)
				&& Objects.equals(this.id, other.id) && Objects.equals(this.notices, other.notices) && Objects.equals(this.numero, other.numero)
				&& Objects.equals(this.pexNs, other.pexNs) && Objects.equals(this.type, other.type) && Objects.equals(this.zoneNs, other.zoneNs);
	}

	@Override
	public String toString() {
		return "Notice [id=" + id + ", numero=" + numero + ", zoneNs=" + zoneNs + "]";
	}
	
////plus d'1 traducteur (au moins deux zones 700 dont le $4 vaut 0680)
	//and eval($notice.countZonesHasSousZoneValue("700","4","0680")>=2)
	
	/**
	 * Compte le nombre de zones possedant au moins une sous zone qui a pour valeur value.
	 * 
	 * @param codeZone
	 * 			{@link String} Etiquette de la zone
	 * @param codeSousZone
	 * 			{@link String} Etiquette de la sous zone
	 * @param value
	 * 			{@link String} Valeur de la sous zone recherchée
	 * @return
	 */
	public int countZonesHasSousZoneValue(String codeZone, String codeSousZone, String value) {
		int count =0;
		
		List<ZoneN> listZonesWithSameCodeZone = this.zoneNs.get(codeZone);
		
		for (ZoneN zone : listZonesWithSameCodeZone) {
			SousElementN sousZone = zone.getFirstSousElementNByCode(codeSousZone, value);
			if (sousZone != null) {
				count++;
			}
		}		
		return count;
	}
	
	public boolean allAnlsSameAuthor100110() {
		
		Set<String> auteurs = new HashSet<>();
		
		if (this.getNotices().size() == 1) {
			// Si une seule ANL alors faux
			return false;
		}
		
		for(Notice anlNiv1 : this.getNotices()) {
			Optional<String> auteur  = this.findAuteur(anlNiv1);
			if (auteur.isPresent()) {
				auteurs.add(auteur.get());
			} else {
				auteurs.add("inconnu");
			}
		}	
		
		return auteurs.size() == 1;
	}

	private Optional<String> findAuteur(Notice notice) {
		
		// 1er essai avec 100
		List<ZoneN> zones = notice.getZoneNsByCode("100");
		if (!zones.isEmpty()) {
			ZoneN firstZone = zones.get(0);
			SousElementN sousElement = firstZone.getFirstSousElementNByCode("3");
			if (sousElement != null) {
				return Optional.ofNullable(sousElement.getValue());
			}
		}
		// Si toujours pas trouvé, deuxieme essai avec 110
		zones = notice.getZoneNsByCode("110");
		if (!zones.isEmpty()) {
			ZoneN firstZone = zones.get(0);
			SousElementN sousElement = firstZone.getFirstSousElementNByCode("3");
			if (sousElement != null) {
				return Optional.ofNullable(sousElement.getValue());
			}
		}
		return Optional.empty();
	}
	
	/**
	 * 100, 110, 700, 710, 702
	 * @return
	 */
	public boolean allAnlsSameAuthorCpl() {
		
		Set<GroupeAuteur> auteurs = new HashSet<>();
		
		List<String> auteursMere = this.findAuteurCpl(this);
		GroupeAuteur groupeAuteursMere = new GroupeAuteur(auteursMere);
		auteurs.add(groupeAuteursMere);		
		
		int count = 0;
		for(Notice anlNiv1 : this.getNotices()) {
			List<String> auteursAnl  = this.findAuteurCpl(anlNiv1);
			if (!auteursAnl.isEmpty()) {
				count++;
			}
			GroupeAuteur groupeAuteursAnl = new GroupeAuteur(auteursAnl);
			auteurs.add(groupeAuteursAnl);		
		}	
		if (count == 0) { // Cas mere possede auteur, ANLs ne possedent pas auteur
			return true;
		}
		
		return auteurs.size() == 1;
	}
	
	private List<String> findAuteurCpl(Notice notice) {

		List<String> result = new ArrayList<>();
		
		// 1er essai avec 100
		List<ZoneN> zones = notice.getZoneNsByCode("100");
		addPotentialLinkToResult(result, zones);
		// Si toujours pas trouvé, deuxieme essai avec 110
		zones = notice.getZoneNsByCode("110");
		addPotentialLinkToResult(result, zones);
		// Si toujours pas trouvé, 3eme essai avec 700
		zones = notice.getZoneNsByCode("700");
		addPotentialLinkToResult(result, zones);
		// Si toujours pas trouvé, 4eme essai avec 710
		zones = notice.getZoneNsByCode("710");
		addPotentialLinkToResult(result, zones);

		// Si toujours pas trouvé, 5eme essai avec 702
		zones = notice.getZoneNsByCode("702");
		addPotentialLinkToResult(result, zones);
		return result;
	}

	private void addPotentialLinkToResult(List<String> result, List<ZoneN> zones) {
		if (!zones.isEmpty()) {
			ZoneN firstZone = zones.get(0);
			SousElementN sousElement = firstZone.getFirstSousElementNByCode("3");
			if (sousElement != null) {
				result.add(sousElement.getValue());
			}
		}
	}
	
	public class GroupeAuteur {
		
		private List<String> auteurs;
		
		public GroupeAuteur(List<String> auteurs) {
			this.auteurs = auteurs;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getEnclosingInstance().hashCode();
			result = prime * result + Objects.hash(auteurs);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof GroupeAuteur)) {
				return false;
			}
			GroupeAuteur other = (GroupeAuteur) obj;
			if (!getEnclosingInstance().equals(other.getEnclosingInstance())) {
				return false;
			}
			return Objects.equals(auteurs, other.auteurs);
		}

		private Notice getEnclosingInstance() {
			return Notice.this;
		}
		
		
	}
}
