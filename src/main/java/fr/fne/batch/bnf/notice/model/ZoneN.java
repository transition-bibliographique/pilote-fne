package fr.fne.batch.bnf.notice.model;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.Objects;

/**
 * Modele abstrait représentant une <b>zone d'une notice</b> {@link Notice}.<br>
 * Le N de {@link ZoneN} est pour "Notice".
 * Attention, le code de la zone <b>correspond à tag</b>.
 * 
 * @author Thibaud SENALADA
 * 
 * @author Mickael KWINE KWOR MAN
 *
 */
public class ZoneN {

	private static final String WILDCARD = "*";

	private String tag;

	private String ind1;

	private String ind2;

	// On utilise une map pour réduire la recherche sur les sous element
	// On utilise une list de sous element dans la map pour traiter le cas de sous element répétable
	private Map<String, List<SousElementN>> sousElementNs;

	private List<SousElementN> sousElementNsOrdonnes;

	/**
	 * Constructeur
	 */
	public ZoneN() {
		this.sousElementNs = new LinkedHashMap<>();
		this.sousElementNsOrdonnes = new ArrayList<>();
	}

	/**
	 * Constructeur
	 * @param tag
	 * 		Tag de la zone
	 */
	public ZoneN(String tag) {
		this.tag = tag;
		this.sousElementNs = new LinkedHashMap<>();
		this.sousElementNsOrdonnes = new ArrayList<>();
	}

	/*
	 *      __  __ ______ _______ _    _  ____  _____   _____ 
	 *     |  \/  |  ____|__   __| |  | |/ __ \|  __ \ / ____|
	 *     | \  / | |__     | |  | |__| | |  | | |  | | (___  
	 *     | |\/| |  __|    | |  |  __  | |  | | |  | |\___ \ 
	 *     | |  | | |____   | |  | |  | | |__| | |__| |____) |
	 *     |_|  |_|______|  |_|  |_|  |_|\____/|_____/|_____/ 
	 *                                                        
	 *                                                        
	 */

	/**
	 * Retourne la liste des sous element N correspondant à un code donné.<br>
	 * (Permet de récupérer les sous elements répétables)<br>
	 * Supporte le caractère spécial <b>"*"</b> pour retourner <b>tout les sous éléments N</b>.
	 * 
	 * @param code
	 *            {@link String} Code/Tag des sous element
	 * @return
	 *         {@link List} {@link SousElementN} correspondant au code
	 */
	public List<SousElementN> getSousElementNsByCode(String code) {
		requireNonNull(code);

		List<SousElementN> allSousElementNList = new ArrayList<>();

		// Si le caractère "joker" est donné, on aplatit la map en list et on renvoie la totalité des zones.
		if (WILDCARD.equals(code)) {
			for (Entry<String, List<SousElementN>> entry : this.sousElementNs.entrySet()) {
				allSousElementNList.addAll(entry.getValue());
			}
		} else {
			if (this.sousElementNs.get(code) != null) {
				allSousElementNList.addAll(this.sousElementNs.get(code));
			}
		}

		return allSousElementNList;
	}

	/**
	 * Récupère la première sous element correspondant au code donné
	 * (Ne permet pas de récupérer les sous zones répétable)
	 * 
	 * @param code
	 *            {@link String} Code/Tag du sous element
	 * @return
	 *         Première {@link SousElementN} correspondant au code, retourne <code>null</code> si vide
	 */
	public SousElementN getFirstSousElementNByCode(String code) {
		requireNonNull(code);

		if (this.sousElementNs.get(code) == null) {
			return null;
		}
		for (SousElementN i : this.sousElementNs.get(code)) {
			if (i != null) {
				return i;
			}
		}
		return null;
	}
	
	public SousElementN getFirstSousElementNByCode(String codeSousZone, String valeurSousZone) {
		requireNonNull(codeSousZone);

		if (this.sousElementNs.get(codeSousZone) == null) {
			return null;
		}
		for (SousElementN i : this.sousElementNs.get(codeSousZone)) {
			if (i != null && valeurSousZone.equals(i.getValue())) {
				return i;
			}
			// Gestion Sens pour les sous zones à position ref #62208
			if (i instanceof SousZoneN) {
				SousZoneN sousZone = (SousZoneN) i;
				for (PosN pos : sousZone.getPosNs()) {
					if (pos.getValue().equals(valeurSousZone)) {
						return pos;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Retourne le nombre de sous élément pour un code donné.
	 * 
	 * @param code
	 * @return
	 */
	public int countSousElementNByCode(String code) {
		return this.getSousElementNsByCode(code).size();
	}

	/**
	 * Retourne true si au moins un sous-element correspond au code et à la valeur donnée.
	 * 
	 * @param code
	 * @param value
	 * @return
	 */
	public Boolean doesNotContainsSousElementWithCodeAndValue(String code, String value) {

		Boolean doesNotContainsSousElementWithCodeAndValue = true;

		List<SousElementN> sousElementList = this.sousElementNs.get(code);

		Iterator<SousElementN> it = sousElementList.iterator();
		while (it.hasNext()) {

			SousElementN sousElement = it.next();

			// Si au moins un element contient le code et la valeur donné, on stop l'itération et on retourne false.
			if (sousElement.getValue().equals(value)) {
				doesNotContainsSousElementWithCodeAndValue = false;
				break;
			}
		}
		return doesNotContainsSousElementWithCodeAndValue;
	}

	/**
	 * Test si la valeur d'une sous zone est égale à la valeur passé en paramètre quelque soit la casse
	 * 
	 * @param codeSousZone
	 * 			Code de la sous zone
	 * @param value
	 * 			Valeur à tester
	 * @return
	 */
	public boolean isValueInElementsNLowerCase(String codeSousZone, String value) {

		List<SousElementN> sousZones = this.getSousElementNs().get(codeSousZone);
		if (sousZones != null) {
			for (SousElementN sousZone : sousZones) {
				if (value.equalsIgnoreCase(sousZone.getValue())) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Test si la valeur d'une sous zone est égale aux valeurs passé en paramètre quelque soit la casse
	 * 
	 * @param codeSousZone
	 * 			Code de la sous zone
	 * @param value
	 * 			Valeurs à tester
	 * @return
	 */
	public boolean isValueInElementsNLowerCase(String codeSousZone, String... values) {
		return Stream.of(values).anyMatch(s -> isValueInElementsNLowerCase(codeSousZone, s));
	}

	/**
	 * Permet de tester si une sous zone commence par une valeur donnée.
	 * 
	 * @param codeSousZone
	 * @param values
	 * @return
	 */
	public boolean isValueStartWithInElementsN(String codeSousZone, String... values) {

		List<SousElementN> sousZones = this.getSousElementNs().get(codeSousZone);
		if (sousZones != null) {
			for (SousElementN sousZone : sousZones) {

				if (Stream.of(values).anyMatch(s -> sousZone.getValue().startsWith(s))) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Permet de tester si une sous-zone a une valeur comprise dans un groupe de valeurs.
	 * 
	 * @param codeSousZone
	 * @param values
	 * @return
	 */
	public boolean isOneValueInSousElementsN(String codeSousElement, String... values) {		
		List<SousElementN> sousElements = this.getSousElementNs().get(codeSousElement);
		if (sousElements != null) {
			for (SousElementN sousElement : sousElements) {

				if (Stream.of(values).anyMatch(s -> sousElement.getValue().equals(s))) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean isValueInPosN(String codeSousZone, String codePosN, String value) {
		return this.sousElementNs.get(codeSousZone).stream().anyMatch(sz -> ((SousZoneN) sz).isValueInPosN(codePosN, value));
	}

	public boolean isOneValueInPosN(String codeSousZone, String codePosN, String... values) {
		return this.sousElementNs.get(codeSousZone).stream().anyMatch(sz -> ((SousZoneN) sz).isOneValueInPosN(codePosN, values));
	}

	/**
	 * Permet de tester si une sous zone commence par une valeur donnée en mettant en minuscule la valeur de la zone
	 * 
	 * @param codeSousZone
	 * @param values
	 * @return
	 */
	public boolean isValueStartWithInElementsNLowerCase(String codeSousZone, String... values) {

		List<SousElementN> sousZones = this.getSousElementNs().get(codeSousZone);
		if (sousZones != null) {
			for (SousElementN sousZone : sousZones) {
				if (Stream.of(values).anyMatch(s -> sousZone.getValue().toLowerCase().startsWith(s))) {
					return true;
				}
			}
		}

		return false;
	}

	public void addSousElementN(SousElementN sousElementN) {
		// si la map ne le contient pas alors je l'ajoute et c'est la première occurence
		if (this.getSousElementNs().containsKey(sousElementN.getCode())) {
			this.getSousElementNs().get(sousElementN.getCode()).add(sousElementN);
		} else { // si la map le contient deja, je l'ajoute en tant que 1 + n occurence (car répétabilité)
			List<SousElementN> listSousElementNs = new ArrayList<>();
			listSousElementNs.add(sousElementN);
			this.getSousElementNs().put(sousElementN.getCode(), listSousElementNs);
		}
		this.sousElementNsOrdonnes.add(sousElementN);
	}

	/*
	 *       _____ ______ _______ _______ ______ _____   _____    _____ ______ _______ _______ ______ _____   _____ 
	 *      / ____|  ____|__   __|__   __|  ____|  __ \ / ____|  / ____|  ____|__   __|__   __|  ____|  __ \ / ____|
	 *     | |  __| |__     | |     | |  | |__  | |__) | (___   | (___ | |__     | |     | |  | |__  | |__) | (___  
	 *     | | |_ |  __|    | |     | |  |  __| |  _  / \___ \   \___ \|  __|    | |     | |  |  __| |  _  / \___ \ 
	 *     | |__| | |____   | |     | |  | |____| | \ \ ____) |  ____) | |____   | |     | |  | |____| | \ \ ____) |
	 *      \_____|______|  |_|     |_|  |______|_|  \_\_____/  |_____/|______|  |_|     |_|  |______|_|  \_\_____/ 
	 *                                                                                                              
	 *                                                                                                              
	 */

	public String getTag() {
		return this.tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getInd1() {
		return this.ind1;
	}

	public void setInd1(String ind1) {
		// Les valeurs ' ' et '.' sont équivalentent à '#' en intermarc pour les indicateurs
		if(" ".equals(ind1) || ".".equals(ind1))
			ind1 = "#";
		this.ind1 = ind1;
	}

	public String getInd2() {

		return this.ind2;
	}

	public void setInd2(String ind2) {
		// Les valeurs ' ' et '.' sont équivalentent à '#' en intermarc pour les indicateurs
		if(" ".equals(ind2) || ".".equals(ind2))
			ind2 = "#";
		this.ind2 = ind2;
	}

	public Map<String, List<SousElementN>> getSousElementNs() {
		return this.sousElementNs;
	}

	public void setSousElementNs(Map<String, List<SousElementN>> sousElementNs) {
		this.sousElementNs = sousElementNs;
	}

	public List<SousElementN> getSousElementNsOrdonnes() {
		return sousElementNsOrdonnes;
	}

	/*
	 *      ______ ____  _    _         _       _____ 
	 *     |  ____/ __ \| |  | |  /\   | |     / ____|
	 *     | |__ | |  | | |  | | /  \  | |    | (___  
	 *     |  __|| |  | | |  | |/ /\ \ | |     \___ \ 
	 *     | |___| |__| | |__| / ____ \| |____ ____) |
	 *     |______\___\_\\____/_/    \_\______|_____/ 
	 *                                                
	 *                                                
	 */

	@Override
	public int hashCode() {
		return Objects.hash(this.ind1, this.ind2, this.sousElementNs, this.tag);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ZoneN)) {
			return false;
		}
		ZoneN other = (ZoneN) obj;
		return Objects.equals(this.ind1, other.ind1) && Objects.equals(this.ind2, other.ind2) && Objects.equals(this.sousElementNs, other.sousElementNs)
				&& Objects.equals(this.tag, other.tag);
	}

	@Override
	public String toString() {
		return "ZoneN [tag=" + tag + ", ind1=" + ind1 + ", ind2=" + ind2 + ", sousElementNs=" + sousElementNs
				+ ", sousElementNsOrdonnes=" + sousElementNsOrdonnes + "]";
	}


}
