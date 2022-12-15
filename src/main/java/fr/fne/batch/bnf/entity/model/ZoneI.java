package fr.fne.batch.bnf.entity.model;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Classe représentant une zone d'un intermarcNG
 * <ul>
 * 		<li>Contient un code</li>
 * 		<li>Contient une liste de {@link SousZoneI} </li>
 * </ul>
 * 
 * @author Mickael KWINE KWOR MAN
 * @author Thibaud SENALADA
 *
 */
public class ZoneI {
	
	private String code;
	private List<SousZoneI> sousZones;

	public ZoneI() {
		this.sousZones = new ArrayList<>();
	}

	public ZoneI(String code) {
		this.code = code;
		this.sousZones = new ArrayList<>();
	}

	/**
	 * Ajoute une sous-zone
	 * 
	 * @param sousZones
	 * 			{@link SousZoneI} la sous zone a ajouter
	 */
	public void addSousZone(SousZoneI sousZone) {		
			this.sousZones.add(sousZone);		
	}

	public List<SousZoneI> getSousZones() {
		return this.sousZones;
	}

	public void setSousZones(List<SousZoneI> sousZones) {
		this.sousZones = sousZones;
	}

	/**
	 * @return the code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @param code the code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ZonesI : ").append(this.code).append(" [ \n\t");
		this.sousZones.stream().forEach(sousZone -> sb.append(sousZone).append("\n\t"));
		sb.deleteCharAt(sb.length() - 1).append("]");
		return sb.toString();
	}	

	@Override
	public int hashCode() {
		return Objects.hash(code, sousZones);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ZoneI)) {
			return false;
		}
		ZoneI other = (ZoneI) obj;
		return Objects.equals(code, other.code) && Objects.equals(sousZones, other.sousZones);
	}

	/**
	 * Ajoute une sous zone à une zone à un indice donnée.
	 * 
	 * @param i
	 * 			{@link int} Indice de la sous zone à ajouter
	 * @param sousZone
	 * 			{@link SousZoneI} sous zone à ajouter
	 * 
	 */
	public void addSousZone(int i, SousZoneI sousZone) {		
			this.sousZones.add(i, sousZone);
	}
	
	public void addSousZone(String code, String valeur) {
			SousZoneI sz = new SousZoneI(code, valeur);
			this.sousZones.add(sz);
	}
	
	public void addSousZones(List<SousZoneI> sousZones) {
		this.sousZones.addAll(sousZones);
	}
	
	/**
	 * Déduit la zone à partir d'une sous zone
	 * Exemple : souszone = 100$a donc la zone est 100.
	 * 
	 * Attention on peut avoir des souszone $A sans étiquette de zone.
	 * 
	 * @param sousZone
	 * @return
	 */
	public static String getZoneFromSousZone(String sousZone) {
		
		requireNonNull(sousZone);
		
		if (sousZone.length() == 2) { // Cas des $A, $E etc.
			return "";
		}		
		return sousZone.substring(0, 3);
	}
}
